-- ─────────────────────────────────────────────
-- analyses: 오늘의 이야기 (선택 입력)
--   분석은 Edge Function 이 insert 하고, 이야기는 앱이 기존 "analyses: update own" 정책으로 따로 저장한다.
--   nullable 컬럼 추가라 테이블 재작성/긴 잠금 없음.
-- ─────────────────────────────────────────────
alter table public.analyses
  add column note text check (note is null or char_length(note) between 1 and 500);

comment on column public.analyses.note is '사진과 함께 남기는 오늘의 이야기 (선택)';
