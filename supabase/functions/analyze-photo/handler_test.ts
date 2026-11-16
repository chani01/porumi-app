// analyze-photo 의 HTTP 계약 테스트. 앱(AnalysisRepository.analyze)이 의존하는 상태 코드를 고정한다.
import { assertEquals, assertRejects } from "@std/assert";
import Anthropic from "@anthropic-ai/sdk";
import type { AnalysisRow } from "./analysis.ts";
import { type AnalyzeResult, createHandler, type Deps } from "./handler.ts";
import { sampleOutput } from "./test_fixtures.ts";

const UID = "11111111-1111-1111-1111-111111111111";
const PATH = `${UID}/photo.jpg`;

interface Calls {
  removed: string[];
  inserted: AnalysisRow[];
  analyzed: number;
}

function setup(opts: {
  userId?: string | null;
  file?: { bytes: Uint8Array; type: string } | null;
  analyze?: () => Promise<AnalyzeResult>;
  insertError?: unknown;
  referenced?: boolean;
} = {}) {
  const calls: Calls = { removed: [], inserted: [], analyzed: 0 };
  const deps: Deps = {
    forUser: () => ({
      getUserId: () => Promise.resolve(opts.userId === undefined ? UID : opts.userId),
      download: () =>
        Promise.resolve(opts.file === undefined ? { bytes: new Uint8Array([1, 2, 3]), type: "image/jpeg" } : opts.file),
      remove: (p) => {
        calls.removed.push(p);
        return Promise.resolve();
      },
      isReferenced: () => Promise.resolve(opts.referenced ?? false),
      insert: (row) => {
        calls.inserted.push(row);
        return Promise.resolve(opts.insertError ? { error: opts.insertError } : { row: { id: "a1", ...row } });
      },
    }),
    analyze: () => {
      calls.analyzed++;
      return opts.analyze ? opts.analyze() : Promise.resolve({ refused: false, output: sampleOutput() });
    },
  };
  return { handler: createHandler(deps), calls };
}

function post(body: unknown = { image_path: PATH }, headers: Record<string, string> = { Authorization: "Bearer t" }) {
  return new Request("http://localhost/analyze-photo", {
    method: "POST",
    headers,
    body: typeof body === "string" ? body : JSON.stringify(body),
  });
}

async function call(handler: (r: Request) => Promise<Response>, req: Request) {
  const res = await handler(req);
  return { status: res.status, body: await res.json() };
}

// console.error 소음 억제
function quiet<T>(fn: () => Promise<T>): Promise<T> {
  const orig = console.error;
  console.error = () => {};
  return fn().finally(() => (console.error = orig));
}

Deno.test("405: POST 가 아니면 거부", async () => {
  const { handler } = setup();
  const res = await handler(new Request("http://localhost/", { method: "GET" }));
  assertEquals(res.status, 405);
});

Deno.test("401: Authorization 헤더 없음", async () => {
  const { handler } = setup();
  assertEquals((await call(handler, post(undefined, {}))).status, 401);
});

Deno.test("401: 토큰이 유효하지 않음", async () => {
  const { handler, calls } = setup({ userId: null });
  assertEquals((await call(handler, post())).status, 401);
  assertEquals(calls.analyzed, 0);
});

Deno.test("400: JSON 이 아닌 본문", async () => {
  const { handler } = setup();
  assertEquals((await call(handler, post("not json"))).status, 400);
});

Deno.test("400: 다른 사용자 폴더·경로 조작 이미지는 Claude 호출 전에 거부", async () => {
  for (const image_path of [undefined, "other-user/a.jpg", `${UID}/../x.jpg`]) {
    const { handler, calls } = setup();
    assertEquals((await call(handler, post({ image_path }))).status, 400, String(image_path));
    assertEquals(calls.analyzed, 0);
  }
});

Deno.test("400: Storage 에 이미지가 없음", async () => {
  const { handler } = setup({ file: null });
  assertEquals((await call(handler, post())).status, 400);
});

Deno.test("422 REFUSED: Claude 가 거절", async () => {
  const { handler, calls } = setup({ analyze: () => Promise.resolve({ refused: true }) });
  const { status, body } = await call(handler, post());
  assertEquals(status, 422);
  assertEquals(body.code, "REFUSED");
  assertEquals(calls.inserted.length, 0);
  assertEquals(calls.removed, [PATH]);
});

Deno.test("502: 구조화 출력 파싱 실패", async () => {
  const { handler } = setup({ analyze: () => Promise.resolve({ refused: false, output: null }) });
  assertEquals((await call(handler, post())).status, 502);
});

Deno.test("429: Anthropic rate limit", async () => {
  const { handler } = setup({
    analyze: () => Promise.reject(new Anthropic.RateLimitError(429, undefined, "rate limited", new Headers())),
  });
  assertEquals((await call(handler, post())).status, 429);
});

Deno.test("502: 그 밖의 Anthropic API 오류", async () => {
  const { handler } = setup({
    analyze: () => Promise.reject(new Anthropic.InternalServerError(500, undefined, "boom", new Headers())),
  });
  assertEquals((await quiet(() => call(handler, post()))).status, 502);
});

Deno.test("Anthropic 오류가 아닌 예외는 그대로 전파", async () => {
  const { handler } = setup({ analyze: () => Promise.reject(new TypeError("bug")) });
  await assertRejects(() => handler(post()), TypeError);
});

Deno.test("422 NO_PET_FACE: 저장하지 않고 업로드 파일 삭제", async () => {
  for (const output of [
    sampleOutput({ is_pet: false }),
    sampleOutput({ face_visible: false }),
    sampleOutput({ species: "OTHER" }),
  ]) {
    const { handler, calls } = setup({ analyze: () => Promise.resolve({ refused: false, output }) });
    const { status, body } = await call(handler, post());
    assertEquals(status, 422);
    assertEquals(body.code, "NO_PET_FACE");
    assertEquals(calls.removed, [PATH]);
    assertEquals(calls.inserted.length, 0);
  }
});

Deno.test("200: 정규화된 행을 저장하고 그대로 반환", async () => {
  const { handler, calls } = setup();
  const { status, body } = await call(handler, post({ image_path: PATH, pet_id: "pet-1", taken_at: "2026-11-16T00:00:00Z" }));
  assertEquals(status, 200);
  assertEquals(calls.inserted.length, 1);
  const row = calls.inserted[0];
  assertEquals(row.image_path, PATH);
  assertEquals(row.pet_id, "pet-1");
  assertEquals(row.taken_at, "2026-11-16T00:00:00Z");
  assertEquals(row.confidence, row.scores.HAPPY);
  assertEquals(body.id, "a1");
  assertEquals(calls.removed, []);
});

Deno.test("400: DB 저장 실패 (RLS 등)", async () => {
  const { handler } = setup({ insertError: { message: "rls" } });
  assertEquals((await quiet(() => call(handler, post()))).status, 400);
});

Deno.test("저장되지 않는 오류 결과(502·429·DB 실패·예외)에서도 업로드 파일 정리", async () => {
  const cases: Parameters<typeof setup>[0][] = [
    { analyze: () => Promise.resolve({ refused: false, output: null }) },
    { analyze: () => Promise.reject(new Anthropic.RateLimitError(429, undefined, "rate", new Headers())) },
    { analyze: () => Promise.reject(new Anthropic.InternalServerError(500, undefined, "boom", new Headers())) },
    { insertError: { message: "rls" } },
  ];
  for (const opts of cases) {
    const { handler, calls } = setup(opts);
    await quiet(() => handler(post()));
    assertEquals(calls.removed, [PATH]);
  }
  const { handler, calls } = setup({ analyze: () => Promise.reject(new TypeError("bug")) });
  await assertRejects(() => handler(post()), TypeError);
  assertEquals(calls.removed, [PATH]);
});

Deno.test("Storage 에 없거나 남의 경로면 삭제를 시도하지 않음", async () => {
  for (const [opts, body] of [[{ file: null }, { image_path: PATH }], [{}, { image_path: "other/a.jpg" }]] as const) {
    const { handler, calls } = setup(opts);
    await handler(post(body));
    assertEquals(calls.removed, []);
  }
});

Deno.test("같은 경로의 기록이 이미 저장돼 있으면 실패해도 사진을 지우지 않음", async () => {
  const { handler, calls } = setup({
    referenced: true,
    analyze: () => Promise.resolve({ refused: false, output: null }),
  });
  assertEquals((await handler(post())).status, 502);
  assertEquals(calls.removed, []);
});
