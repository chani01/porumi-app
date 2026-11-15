# Porumi 🐶🐱

강아지·고양이 사진의 표정을 분석해 기록하고, 한 주의 기분을 보여주는 앱 (Kotlin Multiplatform + Compose Multiplatform, Android / iOS)

## 구조

```
shared/src/commonMain/kotlin/com/chani/porumi
├── domain/     Models (Pet, Analysis, Emotion), WeeklyMood 계산
├── data/       Supabase 클라이언트, Repository (Auth / Pet / Analysis)
├── di/         Koin 모듈
├── ui/         테마, 공용 컴포넌트
└── feature/    auth · home(피드 그리드) · analyze · report(주간 무드) · pet · detail
supabase/
├── migrations/                 테이블 + RLS + Storage 버킷/정책
└── functions/analyze-photo/    사진 → Claude 표정 분석 → analyses 저장
```

분석 흐름: 앱에서 사진 압축(1280px JPEG) → Storage `pet-photos/{uid}/…` 업로드 → Edge Function `analyze-photo` 호출 → Claude 분석(구조화 JSON) → `analyses` insert → 피드 반영

## 셋업

1. Supabase 프로젝트 생성 후 연결 및 스키마 적용
   ```bash
   supabase link --project-ref <project-ref>
   supabase db push
   ```
2. Claude API 키를 secret 으로 등록하고 함수 배포
   ```bash
   supabase secrets set ANTHROPIC_API_KEY=sk-ant-...
   supabase functions deploy analyze-photo
   ```
3. `local.properties` 에 앱 설정 추가 (publishable/anon 키만. service_role 키 금지)
   ```properties
   supabase.url=https://<project-ref>.supabase.co
   supabase.key=<publishable 또는 anon key>
   ```
4. 이메일 인증을 끄고 빠르게 테스트하려면: Dashboard → Authentication → Sign In / Providers → Email → "Confirm email" 해제

## 실행 / 테스트

- Android: `./gradlew :androidApp:assembleDebug`
- iOS: `iosApp` 을 Xcode 로 열어 실행
- 단위 테스트: `./gradlew :shared:testAndroidHostTest`
