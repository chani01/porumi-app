-- analyses·Storage 정책 보강 (RLS 리뷰 지적 사항)
--
-- 1) analyses.image_path 는 항상 소유자 폴더({user_id}/...) 아래여야 한다.
--    Edge Function 은 이미 검사하지만, 앱이 PostgREST 로 직접 넣는 행도 DB 에서 막는다.
-- 2) 앱이 수정하는 analyses 컬럼은 "오늘의 이야기"(note) 하나뿐이다.
--    분석 결과(감정·점수·요약·경로 등)를 나중에 바꿀 수 없게 UPDATE 를 note 로 제한한다.
-- 3) pet-photos 업로드 경로를 {uid}/{파일} 한 단계로 제한한다.
--    하위 폴더 파일은 delete-account 의 한 단계 list 로 지워지지 않아 탈퇴 후 남을 수 있었다.
--
-- push 전 원격에서 확인 (읽기 전용). 결과가 있으면 1) 의 제약 추가가 실패하므로 먼저 정리한다:
--   select id, user_id, image_path from public.analyses where split_part(image_path, '/', 1) <> user_id::text;
--   select name from storage.objects where bucket_id = 'pet-photos' and array_length(storage.foldername(name), 1) > 1;

alter table public.analyses
  add constraint analyses_image_path_owner check (split_part(image_path, '/', 1) = user_id::text);

revoke update on public.analyses from authenticated;
grant update (note) on public.analyses to authenticated;

drop policy "pet-photos: insert own" on storage.objects;
create policy "pet-photos: insert own" on storage.objects
  for insert to authenticated
  with check (
    bucket_id = 'pet-photos'
    and (storage.foldername(name))[1] = (select auth.uid())::text
    and array_length(storage.foldername(name), 1) = 1
  );
