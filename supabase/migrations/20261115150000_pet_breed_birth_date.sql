-- ─────────────────────────────────────────────
-- pets: 품종 + 생일 (둘 다 선택 입력)
--   nullable 컬럼 추가라 테이블 재작성/긴 잠금 없음. 기존 RLS 정책이 그대로 적용된다.
-- ─────────────────────────────────────────────
alter table public.pets
  add column breed text check (breed is null or char_length(breed) between 1 and 30),
  add column birth_date date check (birth_date is null or birth_date >= date '1980-01-01');

comment on column public.pets.breed is '품종 (예: 골든 리트리버). 없으면 앱이 종(강아지/고양이)을 대신 보여준다';
comment on column public.pets.birth_date is '생일. 앱에서 나이(개월/살)를 계산해 보여준다';
