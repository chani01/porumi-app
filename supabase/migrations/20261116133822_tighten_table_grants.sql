-- 테이블 권한 정리: Supabase 기본 권한(anon·authenticated 에 모든 테이블 권한)을 앱이 실제로 쓰는 만큼만 남긴다.
--
-- 1) notifications: 이전 마이그레이션의 grant update (read_at) 는 테이블 단위 UPDATE 가 남아 있어
--    제한 효과가 없었다 (사용자가 자기 알림의 제목·종류·작성일·pet_id 를 바꿀 수 있었음).
--    알림 생성과 내용은 서버만 정하고, 앱은 조회·삭제·읽음 처리(read_at)만 한다.
-- 2) anon: 앱은 로그인 후에만 테이블에 접근한다. 지금까지는 RLS 만 막고 있었으므로 권한 단계에서도 막는다.
-- 3) authenticated: RLS 를 우회하는 TRUNCATE 와 쓰지 않는 TRIGGER·REFERENCES 를 회수한다.

revoke all on public.pets, public.analyses, public.notifications from anon;

revoke truncate, trigger, references on public.pets, public.analyses from authenticated;

revoke all on public.notifications from authenticated;
grant select, delete on public.notifications to authenticated;
grant update (read_at) on public.notifications to authenticated;
