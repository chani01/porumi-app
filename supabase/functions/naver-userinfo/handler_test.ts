import { assertEquals } from "@std/assert";
import { createHandler, NAVER_PROFILE_URL, type NaverProfileResponse } from "./handler.ts";

function fakeFetch(respond: () => Response | Promise<Response>) {
  const calls: { url: string; auth: string | null }[] = [];
  const fn = ((input: string | URL | Request, init?: RequestInit) => {
    calls.push({ url: String(input), auth: new Headers(init?.headers).get("Authorization") });
    return Promise.resolve(respond());
  }) as typeof fetch;
  return { fn, calls };
}

const naverOk = (body: NaverProfileResponse) => () => Response.json(body);
const req = (headers: Record<string, string> = { Authorization: "Bearer naver-token" }, method = "GET") =>
  new Request("http://localhost/naver-userinfo", { method, headers });

function quiet<T>(fn: () => Promise<T>): Promise<T> {
  const orig = console.error;
  console.error = () => {};
  return fn().finally(() => (console.error = orig));
}

Deno.test("200: 네이버 프로필을 OIDC 클레임으로 펼치고 토큰을 그대로 전달", async () => {
  const { fn, calls } = fakeFetch(naverOk({
    resultcode: "00",
    message: "success",
    response: { id: "n123", email: "a@naver.com", name: "홍길동", nickname: "길동", profile_image: "https://img" },
  }));
  const res = await createHandler(fn)(req());
  assertEquals(res.status, 200);
  assertEquals(await res.json(), {
    sub: "n123",
    email: "a@naver.com",
    email_verified: true,
    name: "홍길동",
    nickname: "길동",
    picture: "https://img",
  });
  assertEquals(calls, [{ url: NAVER_PROFILE_URL, auth: "Bearer naver-token" }]);
});

Deno.test("200: 이메일이 없으면 email_verified=false, 이름이 없으면 닉네임 사용", async () => {
  const { fn } = fakeFetch(naverOk({ resultcode: "00", response: { id: "n1", nickname: "길동" } }));
  const body = await (await createHandler(fn)(req())).json();
  assertEquals(body.email_verified, false);
  assertEquals(body.name, "길동");
});

Deno.test("POST 도 허용 (Supabase Auth 호출 방식 대비), 그 외 메서드는 405", async () => {
  const { fn } = fakeFetch(naverOk({ resultcode: "00", response: { id: "n1" } }));
  assertEquals((await createHandler(fn)(req(undefined, "POST"))).status, 200);
  assertEquals((await createHandler(fn)(req(undefined, "DELETE"))).status, 405);
});

Deno.test("401: Bearer 토큰 없음 — 네이버를 호출하지 않음", async () => {
  const cases: Record<string, string>[] = [{}, { Authorization: "Basic abc" }];
  for (const headers of cases) {
    const { fn, calls } = fakeFetch(naverOk({}));
    assertEquals((await createHandler(fn)(req(headers))).status, 401);
    assertEquals(calls.length, 0);
  }
});

Deno.test("401: 네이버가 토큰을 거부", async () => {
  const { fn } = fakeFetch(() => new Response("", { status: 401 }));
  assertEquals((await createHandler(fn)(req())).status, 401);
});

Deno.test("502: 네이버 오류·네트워크 실패·이상한 응답", async () => {
  const cases: (() => Response | Promise<Response>)[] = [
    () => new Response("", { status: 500 }),
    () => Promise.reject(new TypeError("network")),
    () => new Response("not json", { status: 200 }),
    naverOk({ resultcode: "024", message: "Authentication failed" }),
    naverOk({ resultcode: "00", response: { id: "" } }),
  ];
  for (const respond of cases) {
    const { fn } = fakeFetch(respond);
    assertEquals((await quiet(() => createHandler(fn)(req()))).status, 502);
  }
});
