// naver-userinfo: Supabase "사용자 지정 OAuth 제공자(custom:naver)"의 userinfo_url 로 쓰는 변환 프록시.
//
// 네이버 회원 프로필 API(https://openapi.naver.com/v1/nid/me)는
//   { resultcode, message, response: { id, email, name, nickname, profile_image, ... } }
// 처럼 한 단계 감싼 응답을 주기 때문에, Supabase Auth 가 읽는 표준 OIDC userinfo 형태로 펼쳐서 돌려준다.
//
// 요청:  GET   Authorization: Bearer <네이버 access token>   (Supabase Auth 서버가 호출)
// 응답:  200 { sub, email, email_verified, name, nickname, picture } | 401 | 502
//
// 사용자 JWT 가 아니라 네이버 토큰으로 호출되므로 config.toml 에서 verify_jwt = false.
// 받은 토큰을 네이버에 그대로 전달할 뿐 저장하거나 기록하지 않는다.

import "@supabase/functions-js/edge-runtime.d.ts";

const NAVER_PROFILE_URL = "https://openapi.naver.com/v1/nid/me";

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

interface NaverProfile {
  id: string;
  email?: string;
  name?: string;
  nickname?: string;
  profile_image?: string;
}

Deno.serve(async (req) => {
  if (req.method !== "GET" && req.method !== "POST") return json({ error: "Method not allowed" }, 405);

  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.toLowerCase().startsWith("bearer ")) return json({ error: "Unauthorized" }, 401);

  let res: Response;
  try {
    res = await fetch(NAVER_PROFILE_URL, { headers: { Authorization: authHeader } });
  } catch (e) {
    console.error("naver profile request failed", e);
    return json({ error: "Upstream unavailable" }, 502);
  }

  if (res.status === 401) return json({ error: "Unauthorized" }, 401);
  if (!res.ok) {
    console.error("naver profile error", res.status);
    return json({ error: "Upstream error" }, 502);
  }

  const body = await res.json() as { resultcode?: string; message?: string; response?: NaverProfile };
  const profile = body.response;
  if (body.resultcode !== "00" || !profile?.id) {
    console.error("naver profile unexpected", body.resultcode, body.message);
    return json({ error: "Invalid profile" }, 502);
  }

  return json({
    sub: profile.id,
    email: profile.email,
    // 네이버는 인증된 이메일만 내려준다
    email_verified: profile.email != null,
    name: profile.name ?? profile.nickname,
    nickname: profile.nickname,
    picture: profile.profile_image,
  });
});
