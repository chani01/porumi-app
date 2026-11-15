// delete-account: 요청한 사용자 본인의 계정과 모든 데이터를 삭제한다.
//
// 요청:  POST (본문 없음)  Authorization: Bearer <사용자 access token>
// 처리:  1) 토큰으로 본인 확인
//        2) Storage pet-photos/{uid}/ 아래 사진 전부 삭제
//        3) auth 사용자 삭제 → pets / analyses 는 FK on delete cascade 로 함께 삭제
// 응답:  200 { deleted: true } | 401 | 500

import "@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "@supabase/supabase-js";

const BUCKET = "pet-photos";

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);

  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return json({ error: "Unauthorized" }, 401);

  const url = Deno.env.get("SUPABASE_URL")!;

  // 1) 본인 확인 (사용자 토큰)
  const userClient = createClient(url, Deno.env.get("SUPABASE_ANON_KEY")!, {
    global: { headers: { Authorization: authHeader } },
  });
  const token = authHeader.replace(/^Bearer\s+/i, "");
  const { data: userData, error: userError } = await userClient.auth.getUser(token);
  if (userError || !userData.user) return json({ error: "Unauthorized" }, 401);
  const userId = userData.user.id;

  // 이후 작업은 관리자 권한 필요 (서버 내부에서만 사용, 앱에 노출되지 않음)
  const admin = createClient(url, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  // 2) 사진 삭제 (페이지 단위로 반복)
  try {
    while (true) {
      const { data: files, error } = await admin.storage.from(BUCKET).list(userId, { limit: 100 });
      if (error) throw error;
      if (!files || files.length === 0) break;
      const { error: removeError } = await admin.storage
        .from(BUCKET)
        .remove(files.map((f) => `${userId}/${f.name}`));
      if (removeError) throw removeError;
      if (files.length < 100) break;
    }
  } catch (error) {
    console.error("storage cleanup failed", error);
    return json({ error: "Failed to delete photos" }, 500);
  }

  // 3) 계정 삭제 (pets, analyses 는 cascade 로 삭제됨)
  const { error: deleteError } = await admin.auth.admin.deleteUser(userId);
  if (deleteError) {
    console.error("delete user failed", deleteError);
    return json({ error: "Failed to delete account" }, 500);
  }

  return json({ deleted: true });
});
