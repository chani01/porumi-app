// analyze-photo: 업로드된 반려동물 사진의 표정을 Claude 로 분석하고 analyses 테이블에 저장한다.
//
// 요청:  POST { image_path: "<uid>/<uuid>.jpg", pet_id?: string, taken_at?: ISO8601 }
//        Authorization: Bearer <사용자 access token>
// 응답:  200 analyses row | 401 | 400 | 422 { code: "NO_PET_FACE" | "REFUSED" } | 502
//
// 필요 secret: ANTHROPIC_API_KEY  (supabase secrets set ANTHROPIC_API_KEY=...)

import "@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "@supabase/supabase-js";
import Anthropic from "@anthropic-ai/sdk";
import { betaZodOutputFormat } from "@anthropic-ai/sdk/helpers/beta/zod";
import { z } from "zod";

const BUCKET = "pet-photos";
const MODEL = "claude-opus-5-5";

const EMOTIONS = ["HAPPY", "RELAXED", "EXCITED", "CURIOUS", "SLEEPY", "ANXIOUS", "ANNOYED"] as const;

const AnalysisSchema = z.object({
  is_pet: z.boolean().describe("사진에 강아지 또는 고양이가 있는지"),
  species: z.enum(["DOG", "CAT", "OTHER"]),
  face_visible: z.boolean().describe("표정을 판단할 만큼 얼굴이 보이는지"),
  primary_emotion: z.enum(EMOTIONS),
  scores: z.object({
    HAPPY: z.number(),
    RELAXED: z.number(),
    EXCITED: z.number(),
    CURIOUS: z.number(),
    SLEEPY: z.number(),
    ANXIOUS: z.number(),
    ANNOYED: z.number(),
  }).describe("각 감정의 확률 (0~1, 합계 1)"),
  cues: z.array(z.string()).describe("판단 근거가 된 얼굴 신호 2~4개, 한국어 한 문장씩"),
  summary: z.string().describe("반려동물의 기분을 보호자에게 전하는 다정한 한국어 한 줄"),
});
type AnalysisOutput = z.infer<typeof AnalysisSchema>;

const SYSTEM_PROMPT = `당신은 강아지와 고양이의 얼굴 표정을 읽는 동물행동 전문가입니다.
사진 속 반려동물의 얼굴 신호(귀 위치, 눈 모양과 동공, 입·혀, 수염, 이마 주름, 고개 각도)를 근거로 현재 감정을 추정하세요.

감정 정의:
- HAPPY: 기쁨, 만족 (강아지: 입 벌림·혀·부드러운 눈 / 고양이: 반쯤 감은 눈, 귀 정면)
- RELAXED: 편안, 이완 (이완된 입, 처진 귀, 고양이의 느린 눈깜빡임, 이완된 수염)
- EXCITED: 신남, 장난 (크게 벌린 입, 쫑긋한 귀, 고양이의 동공 확장 + 귀 정면)
- CURIOUS: 호기심, 집중 (고개 갸웃, 귀와 수염이 앞으로)
- SLEEPY: 졸림, 휴식 (눈 감김, 무거운 눈꺼풀)
- ANXIOUS: 불안, 긴장 (흰자 보임, 입술 핥기, 귀 뒤로/옆으로, 수염 뒤로)
- ANNOYED: 경계, 불만 (이빨 드러냄, 코 주름, 귀 납작, 하악)

규칙:
- 강아지/고양이가 없거나 얼굴이 판단 불가할 정도로 안 보이면 is_pet 또는 face_visible 을 false 로 하고 나머지는 가장 그럴듯한 값으로 채우세요.
- scores 는 0~1 사이 확률이고 합계가 1이 되게 하세요. primary_emotion 은 가장 높은 점수의 감정입니다.
- cues 는 사진에서 실제로 보이는 신호만 쓰세요. 보이지 않는 꼬리나 몸짓을 지어내지 마세요.
- summary 는 의학적 진단처럼 말하지 말고 따뜻한 말투로 쓰세요.`;

const anthropic = new Anthropic(); // ANTHROPIC_API_KEY 환경변수 사용

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function toBase64(bytes: Uint8Array): string {
  let binary = "";
  const chunk = 0x8000;
  for (let i = 0; i < bytes.length; i += chunk) {
    binary += String.fromCharCode(...bytes.subarray(i, i + chunk));
  }
  return btoa(binary);
}

function normalizeScores(scores: AnalysisOutput["scores"]): Record<string, number> {
  const clamped = Object.fromEntries(
    Object.entries(scores).map(([k, v]) => [k, Math.min(1, Math.max(0, Number.isFinite(v) ? v : 0))]),
  );
  const sum = Object.values(clamped).reduce((a, b) => a + b, 0);
  if (sum <= 0) return clamped;
  return Object.fromEntries(Object.entries(clamped).map(([k, v]) => [k, Math.round((v / sum) * 1000) / 1000]));
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);

  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return json({ error: "Unauthorized" }, 401);

  // 사용자 JWT 로 동작하는 클라이언트 → Storage/DB 모두 RLS 가 그대로 적용된다
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: authHeader } } },
  );

  const token = authHeader.replace(/^Bearer\s+/i, "");
  const { data: userData, error: userError } = await supabase.auth.getUser(token);
  if (userError || !userData.user) return json({ error: "Unauthorized" }, 401);
  const userId = userData.user.id;

  let body: { image_path?: string; pet_id?: string | null; taken_at?: string | null };
  try {
    body = await req.json();
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }
  const imagePath = body.image_path;
  if (!imagePath || !imagePath.startsWith(`${userId}/`) || imagePath.includes("..")) {
    return json({ error: "Invalid image_path" }, 400);
  }

  const { data: file, error: dlError } = await supabase.storage.from(BUCKET).download(imagePath);
  if (dlError || !file) return json({ error: "Image not found" }, 400);
  const mediaType = file.type === "image/png" ? "image/png" : "image/jpeg";
  const imageBase64 = toBase64(new Uint8Array(await file.arrayBuffer()));

  let result: AnalysisOutput;
  try {
    const response = await anthropic.beta.messages.parse({
      model: MODEL,
      // 안전 분류기가 거절하면 서버가 다른 모델로 자동 재시도
      betas: ["server-side-fallback-2026-07-01"],
      fallbacks: "default",
      max_tokens: 4096,
      system: SYSTEM_PROMPT,
      output_config: {
        effort: "low",
        format: betaZodOutputFormat(AnalysisSchema),
      },
      messages: [{
        role: "user",
        content: [
          { type: "image", source: { type: "base64", media_type: mediaType, data: imageBase64 } },
          { type: "text", text: "이 반려동물의 표정을 분석해 주세요." },
        ],
      }],
    });

    if (response.stop_reason === "refusal") {
      return json({ code: "REFUSED", error: "분석할 수 없는 사진이에요." }, 422);
    }
    if (!response.parsed_output) {
      return json({ error: "Analysis output could not be parsed" }, 502);
    }
    result = response.parsed_output;
  } catch (error) {
    if (error instanceof Anthropic.RateLimitError) {
      return json({ error: "잠시 후 다시 시도해 주세요." }, 429);
    }
    if (error instanceof Anthropic.APIError) {
      console.error(`Anthropic API error ${error.status}:`, error.message);
      return json({ error: "Analysis service error" }, 502);
    }
    throw error;
  }

  if (!result.is_pet || !result.face_visible || result.species === "OTHER") {
    // 분석 불가 사진은 저장하지 않고 업로드 파일도 정리
    await supabase.storage.from(BUCKET).remove([imagePath]);
    return json({ code: "NO_PET_FACE", error: "강아지나 고양이의 얼굴이 잘 보이는 사진으로 다시 시도해 주세요." }, 422);
  }

  const scores = normalizeScores(result.scores);
  const primary = result.primary_emotion;

  const { data: row, error: insertError } = await supabase
    .from("analyses")
    .insert({
      pet_id: body.pet_id ?? null,
      image_path: imagePath,
      species: result.species,
      primary_emotion: primary,
      confidence: scores[primary] ?? 0,
      scores,
      cues: result.cues.slice(0, 4),
      summary: result.summary,
      taken_at: body.taken_at ?? new Date().toISOString(),
    })
    .select()
    .single();

  if (insertError) {
    console.error("insert failed", insertError);
    return json({ error: "Failed to save analysis" }, 400);
  }
  return json(row);
});
