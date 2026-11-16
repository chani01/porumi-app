// delete-account: 요청한 사용자 본인의 계정과 모든 데이터를 삭제한다.
// 처리 로직과 응답 계약은 handler.ts (handler_test.ts 로 검증). 이 파일은 Supabase 클라이언트 연결만 한다.

import "@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "@supabase/supabase-js";
import { createHandler } from "./handler.ts";

const BUCKET = "pet-photos";
const url = Deno.env.get("SUPABASE_URL")!;

// 관리자 권한 클라이언트 (서버 내부에서만 사용, 앱에 노출되지 않음)
const admin = createClient(url, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
  auth: { persistSession: false, autoRefreshToken: false },
});

Deno.serve(createHandler({
  async getUserId(authHeader, token) {
    const userClient = createClient(url, Deno.env.get("SUPABASE_ANON_KEY")!, {
      global: { headers: { Authorization: authHeader } },
    });
    const { data, error } = await userClient.auth.getUser(token);
    return error || !data.user ? null : data.user.id;
  },
  async listFiles(folder, limit) {
    const { data, error } = await admin.storage.from(BUCKET).list(folder, { limit });
    if (error) throw error;
    return data ?? [];
  },
  async removeFiles(paths) {
    const { error } = await admin.storage.from(BUCKET).remove(paths);
    if (error) throw error;
  },
  async deleteUser(userId) {
    const { error } = await admin.auth.admin.deleteUser(userId);
    if (error) throw error;
  },
}));
