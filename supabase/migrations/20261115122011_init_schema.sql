-- Porumi 초기 스키마: 반려동물, 표정 분석 기록, 사진 스토리지
-- 모든 데이터는 "본인 것만" 접근 가능 (RLS + 소유자 조건)

-- ─────────────────────────────────────────────
-- pets
-- ─────────────────────────────────────────────
create table public.pets (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null default auth.uid() references auth.users (id) on delete cascade,
  name        text not null check (char_length(name) between 1 and 30),
  species     text not null check (species in ('DOG', 'CAT')),
  created_at  timestamptz not null default now()
);

create index pets_user_id_idx on public.pets (user_id);

alter table public.pets enable row level security;

create policy "pets: select own" on public.pets
  for select to authenticated
  using ((select auth.uid()) = user_id);

create policy "pets: insert own" on public.pets
  for insert to authenticated
  with check ((select auth.uid()) = user_id);

create policy "pets: update own" on public.pets
  for update to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

create policy "pets: delete own" on public.pets
  for delete to authenticated
  using ((select auth.uid()) = user_id);

-- ─────────────────────────────────────────────
-- analyses (표정 분석 결과)
--   insert 는 Edge Function(analyze-photo)이 사용자 JWT 로 수행 → RLS 그대로 적용됨
-- ─────────────────────────────────────────────
create table public.analyses (
  id               uuid primary key default gen_random_uuid(),
  user_id          uuid not null default auth.uid() references auth.users (id) on delete cascade,
  pet_id           uuid references public.pets (id) on delete set null,
  image_path       text not null,
  species          text not null check (species in ('DOG', 'CAT')),
  primary_emotion  text not null check (primary_emotion in
                     ('HAPPY', 'RELAXED', 'EXCITED', 'CURIOUS', 'SLEEPY', 'ANXIOUS', 'ANNOYED')),
  confidence       real not null check (confidence between 0 and 1),
  scores           jsonb not null default '{}'::jsonb,
  cues             text[] not null default '{}',
  summary          text not null default '',
  memo             text,
  taken_at         timestamptz not null default now(),
  created_at       timestamptz not null default now()
);

-- 피드: 내 기록 최신순 / 반려동물별 최신순, 주간 집계: taken_at 범위 조회
create index analyses_user_taken_idx on public.analyses (user_id, taken_at desc);
create index analyses_pet_taken_idx  on public.analyses (pet_id, taken_at desc);

alter table public.analyses enable row level security;

create policy "analyses: select own" on public.analyses
  for select to authenticated
  using ((select auth.uid()) = user_id);

create policy "analyses: insert own" on public.analyses
  for insert to authenticated
  with check (
    (select auth.uid()) = user_id
    and (pet_id is null or exists (
      select 1 from public.pets p where p.id = pet_id and p.user_id = (select auth.uid())
    ))
  );

create policy "analyses: update own" on public.analyses
  for update to authenticated
  using ((select auth.uid()) = user_id)
  with check (
    (select auth.uid()) = user_id
    and (pet_id is null or exists (
      select 1 from public.pets p where p.id = pet_id and p.user_id = (select auth.uid())
    ))
  );

create policy "analyses: delete own" on public.analyses
  for delete to authenticated
  using ((select auth.uid()) = user_id);

-- Data API 노출 설정에 따라 자동 grant 가 없을 수 있으므로 명시 (anon 에는 주지 않음)
grant select, insert, update, delete on public.pets     to authenticated;
grant select, insert, update, delete on public.analyses to authenticated;

-- ─────────────────────────────────────────────
-- Storage: pet-photos (비공개). 경로 규칙: {user_id}/{uuid}.jpg
-- ─────────────────────────────────────────────
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('pet-photos', 'pet-photos', false, 5242880, array['image/jpeg', 'image/png'])
on conflict (id) do nothing;

create policy "pet-photos: select own" on storage.objects
  for select to authenticated
  using (bucket_id = 'pet-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);

create policy "pet-photos: insert own" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'pet-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);

create policy "pet-photos: delete own" on storage.objects
  for delete to authenticated
  using (bucket_id = 'pet-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);
