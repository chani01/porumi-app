// analyze-photo 의 HTTP 처리. Supabase·Claude 호출은 Deps 로 주입받아 테스트에서 가짜로 바꿀 수 있다.
//
// 요청:  POST { image_path: "<uid>/<uuid>.jpg", pet_id?: string, taken_at?: ISO8601 }
//        Authorization: Bearer <사용자 access token>
// 응답:  200 analyses row | 401 | 400 | 422 { code: "NO_PET_FACE" | "REFUSED" } | 429 | 502
// 앱의 AnalysisRepository.analyze 가 이 상태 코드를 한국어 메시지로 바꾼다 — 바꾸면 함께 수정.

import Anthropic from "@anthropic-ai/sdk";
import {
  type AnalysisOutput,
  type AnalysisRow,
  type AnalyzeRequestBody,
  buildAnalysisRow,
  isAnalyzable,
  isOwnImagePath,
  toBase64,
} from "./analysis.ts";

/** 요청자의 JWT 로 동작하는 작업들 (RLS 적용) */
export interface UserScope {
  /** 토큰이 유효하면 사용자 id, 아니면 null */
  getUserId(token: string): Promise<string | null>;
  download(path: string): Promise<{ bytes: Uint8Array; type: string } | null>;
  remove(path: string): Promise<void>;
  insert(row: AnalysisRow): Promise<{ row: unknown } | { error: unknown }>;
}

export type AnalyzeResult = { refused: true } | { refused: false; output: AnalysisOutput | null };

export interface Deps {
  forUser(authHeader: string): UserScope;
  /** Claude 호출. Anthropic SDK 오류는 그대로 throw 한다 */
  analyze(image: { base64: string; mediaType: "image/png" | "image/jpeg" }): Promise<AnalyzeResult>;
}

export function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

export function createHandler(deps: Deps): (req: Request) => Promise<Response> {
  return async (req) => {
    if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);

    const authHeader = req.headers.get("Authorization");
    if (!authHeader) return json({ error: "Unauthorized" }, 401);

    const user = deps.forUser(authHeader);
    const token = authHeader.replace(/^Bearer\s+/i, "");
    const userId = await user.getUserId(token);
    if (!userId) return json({ error: "Unauthorized" }, 401);

    let body: AnalyzeRequestBody;
    try {
      body = await req.json();
    } catch {
      return json({ error: "Invalid JSON" }, 400);
    }
    const imagePath = body.image_path;
    if (!isOwnImagePath(imagePath, userId)) return json({ error: "Invalid image_path" }, 400);

    const file = await user.download(imagePath);
    if (!file) return json({ error: "Image not found" }, 400);
    const mediaType = file.type === "image/png" ? "image/png" : "image/jpeg";

    let result: AnalysisOutput;
    try {
      const response = await deps.analyze({ base64: toBase64(file.bytes), mediaType });
      if (response.refused) {
        return json({ code: "REFUSED", error: "분석할 수 없는 사진이에요." }, 422);
      }
      if (!response.output) {
        return json({ error: "Analysis output could not be parsed" }, 502);
      }
      result = response.output;
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

    if (!isAnalyzable(result)) {
      // 분석 불가 사진은 저장하지 않고 업로드 파일도 정리
      await user.remove(imagePath);
      return json({ code: "NO_PET_FACE", error: "강아지나 고양이의 얼굴이 잘 보이는 사진으로 다시 시도해 주세요." }, 422);
    }

    const inserted = await user.insert(buildAnalysisRow(result, body, imagePath));
    if ("error" in inserted) {
      console.error("insert failed", inserted.error);
      return json({ error: "Failed to save analysis" }, 400);
    }
    return json(inserted.row);
  };
}
