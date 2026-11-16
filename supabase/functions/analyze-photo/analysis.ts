// analyze-photo 의 출력 스키마·프롬프트·순수 함수. 네트워크/런타임 의존이 없어 단위 테스트 대상.
//
// 감정 목록(EMOTIONS, scores)은 앱 domain/Models.kt 의 Emotion, DB analyses.primary_emotion CHECK 와
// 반드시 같아야 한다 (scripts/harness/check-invariants.sh 가 검사).

import { z } from "zod";

export const BUCKET = "pet-photos";
export const MODEL = "claude-opus-5-5";

export const EMOTIONS = ["HAPPY", "RELAXED", "EXCITED", "CURIOUS", "SLEEPY", "ANXIOUS", "ANNOYED"] as const;
export type Emotion = typeof EMOTIONS[number];

export const AnalysisSchema = z.object({
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
export type AnalysisOutput = z.infer<typeof AnalysisSchema>;

export const SYSTEM_PROMPT = `당신은 강아지와 고양이의 얼굴 표정을 읽는 동물행동 전문가입니다.
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

export const MAX_CUES = 4;

export interface AnalyzeRequestBody {
  image_path?: string;
  pet_id?: string | null;
  taken_at?: string | null;
}

/** Storage 경로가 요청자 본인 폴더({uid}/...) 아래인지. 경로 조작(..)은 거부 */
export function isOwnImagePath(path: string | undefined, userId: string): path is string {
  return !!path && path.startsWith(`${userId}/`) && !path.includes("..");
}

export function toBase64(bytes: Uint8Array): string {
  let binary = "";
  const chunk = 0x8000;
  for (let i = 0; i < bytes.length; i += chunk) {
    binary += String.fromCharCode(...bytes.subarray(i, i + chunk));
  }
  return btoa(binary);
}

/** 각 점수를 0~1 로 자르고 합이 1 이 되게 정규화 (소수 셋째 자리 반올림). 합이 0 이면 자른 값 그대로 */
export function normalizeScores(scores: AnalysisOutput["scores"]): Record<string, number> {
  const clamped = Object.fromEntries(
    Object.entries(scores).map(([k, v]) => [k, Math.min(1, Math.max(0, Number.isFinite(v) ? v : 0))]),
  );
  const sum = Object.values(clamped).reduce((a, b) => a + b, 0);
  if (sum <= 0) return clamped;
  return Object.fromEntries(Object.entries(clamped).map(([k, v]) => [k, Math.round((v / sum) * 1000) / 1000]));
}

/** 강아지/고양이 얼굴이 보이는 사진만 저장 대상 */
export function isAnalyzable(result: AnalysisOutput): boolean {
  return result.is_pet && result.face_visible && result.species !== "OTHER";
}

/** analyses 테이블에 넣을 행 (user_id 는 DB 기본값 auth.uid()) */
export function buildAnalysisRow(
  result: AnalysisOutput,
  body: AnalyzeRequestBody,
  imagePath: string,
  now: Date = new Date(),
) {
  const scores = normalizeScores(result.scores);
  const primary = result.primary_emotion;
  return {
    pet_id: body.pet_id ?? null,
    image_path: imagePath,
    species: result.species,
    primary_emotion: primary,
    confidence: scores[primary] ?? 0,
    scores,
    cues: result.cues.slice(0, MAX_CUES),
    summary: result.summary,
    taken_at: body.taken_at ?? now.toISOString(),
  };
}
export type AnalysisRow = ReturnType<typeof buildAnalysisRow>;
