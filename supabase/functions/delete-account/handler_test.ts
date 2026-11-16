import { assertEquals } from "@std/assert";
import { createHandler, type Deps, PAGE_SIZE } from "./handler.ts";

const UID = "11111111-1111-1111-1111-111111111111";

function setup(opts: {
  userId?: string | null;
  fileCount?: number;
  listError?: boolean;
  removeError?: boolean;
  deleteError?: boolean;
} = {}) {
  let remaining = Array.from({ length: opts.fileCount ?? 0 }, (_, i) => ({ name: `${i}.jpg` }));
  const calls = { listed: [] as string[], removed: [] as string[], deletedUsers: [] as string[] };
  const deps: Deps = {
    getUserId: () => Promise.resolve(opts.userId === undefined ? UID : opts.userId),
    listFiles: (folder, limit) => {
      calls.listed.push(folder);
      if (opts.listError) return Promise.reject(new Error("list failed"));
      return Promise.resolve(remaining.slice(0, limit));
    },
    removeFiles: (paths) => {
      if (opts.removeError) return Promise.reject(new Error("remove failed"));
      calls.removed.push(...paths);
      remaining = remaining.filter((f) => !paths.includes(`${UID}/${f.name}`));
      return Promise.resolve();
    },
    deleteUser: (id) => {
      if (opts.deleteError) return Promise.reject(new Error("delete failed"));
      calls.deletedUsers.push(id);
      return Promise.resolve();
    },
  };
  return { handler: createHandler(deps), calls, remaining: () => remaining.length };
}

const post = (headers: Record<string, string> = { Authorization: "Bearer t" }) =>
  new Request("http://localhost/delete-account", { method: "POST", headers });

function quiet<T>(fn: () => Promise<T>): Promise<T> {
  const orig = console.error;
  console.error = () => {};
  return fn().finally(() => (console.error = orig));
}

Deno.test("405 / 401: 메서드·토큰 검사, 실패 시 아무것도 삭제하지 않음", async () => {
  const a = setup({ fileCount: 3 });
  assertEquals((await a.handler(new Request("http://localhost/", { method: "GET" }))).status, 405);
  assertEquals((await a.handler(post({}))).status, 401);
  const b = setup({ userId: null, fileCount: 3 });
  assertEquals((await b.handler(post())).status, 401);
  for (const { calls } of [a, b]) {
    assertEquals(calls.removed.length + calls.deletedUsers.length, 0);
  }
});

Deno.test("200: 사진이 없어도 계정 삭제", async () => {
  const { handler, calls } = setup();
  const res = await handler(post());
  assertEquals(res.status, 200);
  assertEquals(await res.json(), { deleted: true });
  assertEquals(calls.deletedUsers, [UID]);
});

Deno.test("200: 여러 페이지(250장) 사진을 본인 폴더 경로로 모두 삭제한 뒤 계정 삭제", async () => {
  const t = setup({ fileCount: PAGE_SIZE * 2 + 50 });
  assertEquals((await t.handler(post())).status, 200);
  assertEquals(t.remaining(), 0);
  assertEquals(t.calls.removed.length, PAGE_SIZE * 2 + 50);
  assertEquals(t.calls.removed.every((p) => p.startsWith(`${UID}/`)), true);
  assertEquals(t.calls.listed.every((f) => f === UID), true);
  assertEquals(t.calls.deletedUsers, [UID]);
});

Deno.test("200: 정확히 한 페이지(100장)면 빈 목록 확인 후 종료", async () => {
  const t = setup({ fileCount: PAGE_SIZE });
  assertEquals((await t.handler(post())).status, 200);
  assertEquals(t.remaining(), 0);
});

Deno.test("500: 사진 삭제 실패 시 계정은 지우지 않음 (사진이 고아로 남지 않게)", async () => {
  for (const opts of [{ listError: true }, { removeError: true }]) {
    const { handler, calls } = setup({ fileCount: 5, ...opts });
    assertEquals((await quiet(() => handler(post()))).status, 500);
    assertEquals(calls.deletedUsers, []);
  }
});

Deno.test("500: 계정 삭제 실패", async () => {
  const { handler } = setup({ deleteError: true });
  assertEquals((await quiet(() => handler(post()))).status, 500);
});
