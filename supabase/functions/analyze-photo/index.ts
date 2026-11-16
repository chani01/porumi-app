// analyze-photo: 업로드된 반려동물 사진의 표정을 Claude 로 분석하고 analyses 테이블에 저장한다.
//
// 이 파일은 실제 의존성(Supabase 클라이언트, Anthropic SDK)을 연결하는 진입점이다.
//   analysis.ts  출력 스키마·프롬프트·순수 함수
//   handler.ts   HTTP 처리와 상태 코드 계약 (handler_test.ts 로 검증)
//
// 필요 secret: ANTHROPIC_API_KEY  (supabase secrets set ANTHROPIC_API_KEY=...)

import "@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "@supabase/supabase-js";
import Anthropic from "@anthropic-ai/sdk";
import { betaZodOutputFormat } from "@anthropic-ai/sdk/helpers/beta/zod";
import { AnalysisSchema, BUCKET, MODEL, SYSTEM_PROMPT } from "./analysis.ts";
import { createHandler } from "./handler.ts";

const anthropic = new Anthropic(); // ANTHROPIC_API_KEY 환경변수 사용

Deno.serve(createHandler({
  forUser(authHeader) {
    // 사용자 JWT 로 동작하는 클라이언트 → Storage/DB 모두 RLS 가 그대로 적용된다
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_ANON_KEY")!,
      { global: { headers: { Authorization: authHeader } } },
    );
    return {
      async getUserId(token) {
        const { data, error } = await supabase.auth.getUser(token);
        return error || !data.user ? null : data.user.id;
      },
      async download(path) {
        const { data, error } = await supabase.storage.from(BUCKET).download(path);
        if (error || !data) return null;
        return { bytes: new Uint8Array(await data.arrayBuffer()), type: data.type };
      },
      async remove(path) {
        await supabase.storage.from(BUCKET).remove([path]);
      },
      async insert(row) {
        const { data, error } = await supabase.from("analyses").insert(row).select().single();
        return error ? { error } : { row: data };
      },
    };
  },

  async analyze({ base64, mediaType }) {
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
          { type: "image", source: { type: "base64", media_type: mediaType, data: base64 } },
          { type: "text", text: "이 반려동물의 표정을 분석해 주세요." },
        ],
      }],
    });
    if (response.stop_reason === "refusal") return { refused: true };
    return { refused: false, output: response.parsed_output ?? null };
  },
}));
