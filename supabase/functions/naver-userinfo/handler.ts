// naver-userinfo 의 HTTP 처리. 네이버 API 호출(fetch)을 주입받아 테스트에서 가짜로 바꿀 수 있다.
//
// 요청:  GET   Authorization: Bearer <네이버 access token>   (Supabase Auth 서버가 호출)
// 응답:  200 { sub, email, email_verified, name, nickname, picture } | 401 | 502

export const NAVER_PROFILE_URL = "https://openapi.naver.com/v1/nid/me";

export interface NaverProfile {
  id: string;
  email?: string;
  name?: string;
  nickname?: string;
  profile_image?: string;
}

export interface NaverProfileResponse {
  resultcode?: string;
  message?: string;
  response?: NaverProfile;
}

export function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

/** 네이버 프로필을 Supabase Auth 가 읽는 OIDC userinfo 클레임으로 펼친다 */
export function toOidcClaims(profile: NaverProfile) {
  return {
    sub: profile.id,
    email: profile.email,
    // 네이버는 인증된 이메일만 내려준다
    email_verified: profile.email != null,
    name: profile.name ?? profile.nickname,
    nickname: profile.nickname,
    picture: profile.profile_image,
  };
}

export function createHandler(fetchFn: typeof fetch = fetch): (req: Request) => Promise<Response> {
  return async (req) => {
    if (req.method !== "GET" && req.method !== "POST") return json({ error: "Method not allowed" }, 405);

    const authHeader = req.headers.get("Authorization");
    if (!authHeader?.toLowerCase().startsWith("bearer ")) return json({ error: "Unauthorized" }, 401);

    let res: Response;
    try {
      res = await fetchFn(NAVER_PROFILE_URL, { headers: { Authorization: authHeader } });
    } catch (e) {
      console.error("naver profile request failed", e);
      return json({ error: "Upstream unavailable" }, 502);
    }

    if (res.status === 401) return json({ error: "Unauthorized" }, 401);
    if (!res.ok) {
      console.error("naver profile error", res.status);
      return json({ error: "Upstream error" }, 502);
    }

    let body: NaverProfileResponse;
    try {
      body = await res.json();
    } catch {
      console.error("naver profile invalid json");
      return json({ error: "Invalid profile" }, 502);
    }
    const profile = body.response;
    if (body.resultcode !== "00" || !profile?.id) {
      console.error("naver profile unexpected", body.resultcode, body.message);
      return json({ error: "Invalid profile" }, 502);
    }

    return json(toOidcClaims(profile));
  };
}
