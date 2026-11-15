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
```
