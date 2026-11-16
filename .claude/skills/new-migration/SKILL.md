---
name: new-migration
description: Porumi Supabase 마이그레이션(테이블·컬럼·정책·Storage 정책 변경)을 새 파일로 만들고 RLS 테스트까지 추가한다. DB 스키마나 권한을 바꿀 때 사용.
---

# /new-migration <설명>

`.claude/rules/supabase.md`를 따른다.

1. **파일 생성** — `supabase/migrations/$(date -u +%Y%m%d%H%M%S)_<snake_case_설명>.sql`
   - 기존(커밋된) 마이그레이션은 수정하지 않는다. 마지막 파일보다 타임스탬프가 커야 한다.
   - 파일 맨 위에 무엇을/왜 하는지 한국어 주석.
2. **작성 체크리스트**
   - [ ] 새 테이블: `enable row level security` + select/insert/update/delete 소유자 정책 `(select auth.uid()) = user_id`
   - [ ] `user_id uuid not null default auth.uid() references auth.users (id) on delete cascade` + `user_id` 선두 인덱스
   - [ ] 다른 테이블 참조 컬럼은 `with check`에서 참조 행 소유권도 확인
   - [ ] 권한: Supabase 기본 권한이 anon/authenticated 에 전부 열려 있다 → 앱이 쓰지 않는 동작은 `revoke`, 컬럼 제한은 `revoke update` 후 `grant update (col)`
   - [ ] 값 제약은 `check` (길이, enum 목록). 감정 목록 변경이면 `/change-emotion`
   - [ ] 기존 행이 있는 테이블에 `not null` 컬럼 추가 시 기본값/백필
   - [ ] `comment on column` 으로 한국어 설명
3. **RLS 테스트** — `supabase/tests/rls_test.sql`에 격리 케이스 추가(다른 사용자 행 읽기·쓰기·삭제 불가, 허용 동작 가능), `plan(n)` 갱신.
4. **앱 반영** — `shared/.../data/Dto.kt`, `domain/Models.kt`, Repository 쿼리를 함께 수정.
5. **로컬 검증** — 로컬 DB에 적용: `supabase db reset --local` 은 확인을 받으므로 대신 `supabase migration up --local` 사용, 그다음 `/verify` (db lint + test db 포함).
6. **리뷰** — `rls-reviewer` 에이전트에 이번 diff 검토를 맡긴다.
7. **보고** — 원격 반영(`supabase db push`)은 사용자가 한다. 앱 배포 전에 먼저 push 해야 하는지(호환성) 함께 알린다.
