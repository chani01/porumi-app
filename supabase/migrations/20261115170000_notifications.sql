-- ─────────────────────────────────────────────
-- notifications (알림 기록)
--   알림 생성은 서버(Edge Function / service_role)만 한다 → authenticated 에는 insert 권한 없음.
--   사용자는 자기 알림을 조회하고, 읽음 처리(read_at 만 수정)하고, 삭제할 수 있다.
-- ─────────────────────────────────────────────
create table public.notifications (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null references auth.users (id) on delete cascade,
  kind        text not null check (kind in ('REMINDER', 'WEEKLY_REPORT', 'ANALYSIS', 'NOTICE', 'EVENT')),
  title       text not null check (char_length(title) between 1 and 100),
  body        text check (body is null or char_length(body) <= 500),
  pet_id      uuid references public.pets (id) on delete set null,
  created_at  timestamptz not null default now(),
  read_at     timestamptz
);

-- 목록 조회: 내 알림 최신순
create index notifications_user_created_idx on public.notifications (user_id, created_at desc);
-- 안 읽은 알림 개수
create index notifications_user_unread_idx on public.notifications (user_id) where read_at is null;

alter table public.notifications enable row level security;

create policy "notifications: select own" on public.notifications
  for select to authenticated
  using ((select auth.uid()) = user_id);

create policy "notifications: update own" on public.notifications
  for update to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

create policy "notifications: delete own" on public.notifications
  for delete to authenticated
  using ((select auth.uid()) = user_id);

-- 수정은 read_at 컬럼만 허용 (제목·내용 위조 방지)
grant select, delete on public.notifications to authenticated;
grant update (read_at) on public.notifications to authenticated;
