-- RLS 격리 테스트 (pgTAP). 실행: supabase test db   (로컬 Supabase 필요: supabase start)
-- 사용자 A 로 접속해 사용자 B 의 데이터를 읽기·쓰기·삭제할 수 없는지 확인한다.
-- 새 테이블이나 Storage 정책을 추가하면 여기에도 격리 테스트를 추가한다.
begin;
create extension if not exists pgtap with schema extensions;
select plan(39);

-- ── 고정 데이터 (superuser 로 삽입) ───────────────────────────────────────
\set uid_a '''aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'''
\set uid_b '''bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'''

insert into auth.users (id, email, aud, role)
values (:uid_a, 'a@test.local', 'authenticated', 'authenticated'),
       (:uid_b, 'b@test.local', 'authenticated', 'authenticated');

insert into public.pets (id, user_id, name, species) values
  ('a0000000-0000-0000-0000-000000000001', :uid_a, '초코', 'DOG'),
  ('b0000000-0000-0000-0000-000000000001', :uid_b, '나비', 'CAT');

insert into public.analyses (id, user_id, pet_id, image_path, species, primary_emotion, confidence) values
  ('a0000000-0000-0000-0000-0000000000a1', :uid_a, 'a0000000-0000-0000-0000-000000000001',
   'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/1.jpg', 'DOG', 'HAPPY', 0.9),
  ('b0000000-0000-0000-0000-0000000000b1', :uid_b, 'b0000000-0000-0000-0000-000000000001',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb/1.jpg', 'CAT', 'SLEEPY', 0.8);

insert into public.notifications (id, user_id, kind, title) values
  ('a0000000-0000-0000-0000-0000000000c1', :uid_a, 'NOTICE', 'A 알림'),
  ('b0000000-0000-0000-0000-0000000000c1', :uid_b, 'NOTICE', 'B 알림');

insert into storage.objects (bucket_id, name) values
  ('pet-photos', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/1.jpg'),
  ('pet-photos', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb/1.jpg');

-- ── 스키마 전체 규칙 ──────────────────────────────────────────────────────
select is(
  (select coalesce(string_agg(tablename, ', '), '') from pg_tables where schemaname = 'public' and not rowsecurity),
  '',
  'public 스키마의 모든 테이블에 RLS 가 켜져 있다'
);
-- 새 테이블은 Supabase 기본 권한으로 anon 에 전부 열린다 → 마이그레이션에서 revoke 했는지 검사
select is(
  (select coalesce(string_agg(distinct table_name, ', '), '') from information_schema.role_table_grants
    where table_schema = 'public' and grantee = 'anon'),
  '',
  'public 스키마의 어떤 테이블에도 anon 권한이 없다'
);

-- ── 비로그인(anon) ───────────────────────────────────────────────────────
set local role anon;
select set_config('request.jwt.claims', '{"role":"anon"}', true);
-- 앱 테이블은 권한 단계에서 막는다 (Supabase 기본 권한 회수: 20261116133822_tighten_table_grants)
select throws_ok('select * from public.pets', '42501', null, 'anon: pets 권한 없음');
select throws_ok('select * from public.analyses', '42501', null, 'anon: analyses 권한 없음');
select throws_ok('select * from public.notifications', '42501', null, 'anon: notifications 권한 없음');
select is((select count(*)::int from storage.objects where bucket_id = 'pet-photos'), 0, 'anon: 사진 목록 안 보임');
reset role;

-- ── 사용자 A 로 전환 ──────────────────────────────────────────────────────
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa","role":"authenticated"}', true);

-- RLS 를 우회하는 TRUNCATE 는 권한이 없어야 한다
select throws_ok('truncate public.pets cascade', '42501', null, 'authenticated: pets TRUNCATE 불가');
select throws_ok('truncate public.analyses', '42501', null, 'authenticated: analyses TRUNCATE 불가');

-- pets
select results_eq('select name from public.pets', $$values ('초코'::text)$$, 'pets: 내 아이만 보인다');
select throws_ok(
  $$insert into public.pets (user_id, name, species) values ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '가짜', 'DOG')$$,
  '42501', null, 'pets: 다른 사용자 소유로 추가 불가');
select lives_ok($$insert into public.pets (name, species) values ('보리', 'DOG')$$, 'pets: 내 아이 추가 가능');
select is((select user_id from public.pets where name = '보리'), :uid_a::uuid, 'pets: user_id 기본값은 auth.uid()');
select throws_ok(
  $$update public.pets set user_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb' where id = 'a0000000-0000-0000-0000-000000000001'$$,
  '42501', null, 'pets: 내 아이를 다른 사용자에게 넘길 수 없음');
update public.pets set name = '해킹' where id = 'b0000000-0000-0000-0000-000000000001';
delete from public.pets where id = 'b0000000-0000-0000-0000-000000000001';

-- analyses
select results_eq('select id::text from public.analyses',
  $$values ('a0000000-0000-0000-0000-0000000000a1')$$, 'analyses: 내 기록만 보인다');
select throws_ok(
  $$insert into public.analyses (pet_id, image_path, species, primary_emotion, confidence)
    values ('b0000000-0000-0000-0000-000000000001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/2.jpg', 'CAT', 'HAPPY', 0.5)$$,
  '42501', null, 'analyses: 남의 아이로 기록 추가 불가');
select lives_ok(
  $$insert into public.analyses (pet_id, image_path, species, primary_emotion, confidence)
    values ('a0000000-0000-0000-0000-000000000001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/2.jpg', 'DOG', 'HAPPY', 0.5)$$,
  'analyses: 내 아이로 기록 추가 가능');
select throws_ok(
  $$insert into public.analyses (image_path, species, primary_emotion, confidence)
    values ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb/1.jpg', 'DOG', 'HAPPY', 1)$$,
  '23514', null, 'analyses: 남의 폴더 사진 경로로 기록 추가 불가');
select throws_ok(
  $$update public.analyses set primary_emotion = 'ANNOYED', confidence = 1
    where id = 'a0000000-0000-0000-0000-0000000000a1'$$,
  '42501', null, 'analyses: 분석 결과는 수정 불가');
select lives_ok(
  $$update public.analyses set note = '산책 다녀옴' where id = 'a0000000-0000-0000-0000-0000000000a1'$$,
  'analyses: 오늘의 이야기(note) 수정 가능');
select throws_ok(
  $$update public.analyses set pet_id = 'b0000000-0000-0000-0000-000000000001'
    where id = 'a0000000-0000-0000-0000-0000000000a1'$$,
  '42501', null, 'analyses: 내 기록을 남의 아이로 옮길 수 없음');
select throws_ok(
  $$update public.analyses set user_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'
    where id = 'a0000000-0000-0000-0000-0000000000a1'$$,
  '42501', null, 'analyses: 내 기록을 다른 사용자에게 넘길 수 없음');
update public.analyses set note = '해킹' where id = 'b0000000-0000-0000-0000-0000000000b1';
delete from public.analyses where id = 'b0000000-0000-0000-0000-0000000000b1';

-- notifications (앱은 읽기·읽음 처리·삭제만, 생성은 서버)
select results_eq('select title from public.notifications', $$values ('A 알림'::text)$$, 'notifications: 내 알림만 보인다');
select throws_ok(
  $$insert into public.notifications (user_id, kind, title) values ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'NOTICE', 'x')$$,
  '42501', null, 'notifications: 앱에서 알림 생성 불가');
select throws_ok(
  $$update public.notifications set title = 'x' where id = 'a0000000-0000-0000-0000-0000000000c1'$$,
  '42501', null, 'notifications: read_at 외 컬럼 수정 불가');
select throws_ok(
  $$update public.notifications set pet_id = 'b0000000-0000-0000-0000-000000000001'
    where id = 'a0000000-0000-0000-0000-0000000000c1'$$,
  '42501', null, 'notifications: 남의 아이를 알림에 연결 불가');
select lives_ok(
  $$update public.notifications set read_at = now() where id = 'a0000000-0000-0000-0000-0000000000c1'$$,
  'notifications: 내 알림 읽음 처리 가능');
update public.notifications set read_at = now() where id = 'b0000000-0000-0000-0000-0000000000c1';
delete from public.notifications where id = 'b0000000-0000-0000-0000-0000000000c1';
select lives_ok(
  $$delete from public.notifications where id = 'a0000000-0000-0000-0000-0000000000c1'$$,
  'notifications: 내 알림 삭제 가능');

-- storage: pet-photos/{uid}/...
select results_eq(
  $$select name from storage.objects where bucket_id = 'pet-photos'$$,
  $$values ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/1.jpg'::text)$$, 'storage: 내 폴더 사진만 보인다');
select throws_ok(
  $$insert into storage.objects (bucket_id, name) values ('pet-photos', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb/x.jpg')$$,
  '42501', null, 'storage: 남의 폴더에 업로드 불가');
select throws_ok(
  $$insert into storage.objects (bucket_id, name) values ('pet-photos', 'x.jpg')$$,
  '42501', null, 'storage: 폴더 없는 경로에 업로드 불가');
select throws_ok(
  $$insert into storage.objects (bucket_id, name) values ('pet-photos', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/sub/x.jpg')$$,
  '42501', null, 'storage: 하위 폴더 업로드 불가 (탈퇴 시 정리 누락 방지)');
select lives_ok(
  $$insert into storage.objects (bucket_id, name) values ('pet-photos', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/x.jpg')$$,
  'storage: 내 폴더에 업로드 가능');
-- Storage 는 직접 DELETE 를 트리거로 막으므로, RLS delete 정책만 검증하도록 이 트랜잭션에서만 해제
select set_config('storage.allow_delete_query', 'true', true);
delete from storage.objects where name = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb/1.jpg';
select lives_ok(
  $$delete from storage.objects where name = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/x.jpg'$$,
  'storage: 내 사진 삭제 가능');

-- ── superuser 로 돌아와 B 의 데이터가 그대로인지 확인 ─────────────────────
reset role;
select is((select name from public.pets where id = 'b0000000-0000-0000-0000-000000000001'), '나비',
  'pets: A 의 수정·삭제 시도 후에도 B 의 아이는 그대로');
select is((select note from public.analyses where id = 'b0000000-0000-0000-0000-0000000000b1'), null,
  'analyses: B 의 기록은 수정되지 않음');
select is((select count(*)::int from public.analyses where id = 'b0000000-0000-0000-0000-0000000000b1'), 1,
  'analyses: B 의 기록은 삭제되지 않음');
select is((select read_at from public.notifications where id = 'b0000000-0000-0000-0000-0000000000c1'), null,
  'notifications: B 의 알림은 읽음 처리되지 않음');
select is((select count(*)::int from public.notifications where id = 'b0000000-0000-0000-0000-0000000000c1'), 1,
  'notifications: B 의 알림은 삭제되지 않음');
select is((select count(*)::int from storage.objects where name = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb/1.jpg'), 1,
  'storage: B 의 사진은 삭제되지 않음');

select * from finish();
rollback;
