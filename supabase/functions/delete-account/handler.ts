// delete-account 의 HTTP 처리. Supabase 호출은 Deps 로 주입받아 테스트에서 가짜로 바꿀 수 있다.
//
// 요청:  POST (본문 없음)  Authorization: Bearer <사용자 access token>
// 처리:  1) 토큰으로 본인 확인
//        2) Storage pet-photos/{uid}/ 아래 사진 전부 삭제
//        3) auth 사용자 삭제 → pets / analyses / notifications 는 FK on delete cascade 로 함께 삭제
// 응답:  200 { deleted: true } | 401 | 500

export const PAGE_SIZE = 100;

export interface Deps {
  /** 사용자 토큰으로 본인 확인. 유효하면 사용자 id, 아니면 null */
  getUserId(authHeader: string, token: string): Promise<string | null>;
  /** 이하 관리자 권한 작업 (서버 내부 전용) */
  listFiles(folder: string, limit: number): Promise<{ name: string }[]>;
  removeFiles(paths: string[]): Promise<void>;
  deleteUser(userId: string): Promise<void>;
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

    // 1) 본인 확인
    const token = authHeader.replace(/^Bearer\s+/i, "");
    const userId = await deps.getUserId(authHeader, token);
    if (!userId) return json({ error: "Unauthorized" }, 401);

    // 2) 사진 삭제 (페이지 단위로 반복). 경로는 항상 본인 폴더 아래로 만든다
    try {
      while (true) {
        const files = await deps.listFiles(userId, PAGE_SIZE);
        if (files.length === 0) break;
        await deps.removeFiles(files.map((f) => `${userId}/${f.name}`));
        if (files.length < PAGE_SIZE) break;
      }
    } catch (error) {
      console.error("storage cleanup failed", error);
      return json({ error: "Failed to delete photos" }, 500);
    }

    // 3) 계정 삭제 (나머지 데이터는 cascade)
    try {
      await deps.deleteUser(userId);
    } catch (error) {
      console.error("delete user failed", error);
      return json({ error: "Failed to delete account" }, 500);
    }

    return json({ deleted: true });
  };
}
