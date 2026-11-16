---
name: change-emotion
description: Porumi 감정 목록(HAPPY, RELAXED, ...)을 추가·삭제·이름 변경한다. 감정 종류를 바꾸는 모든 요청에 사용 — 앱·Edge Function·DB 세 곳을 함께 바꿔야 한다.
---

# /change-emotion <변경 내용>

감정 목록은 세 곳에 중복돼 있고 `check-invariants.sh`가 일치 여부를 검사한다. 한 곳만 바꾸면 게이트에서 막힌다.

1. **앱** — `shared/src/commonMain/kotlin/com/chani/porumi/domain/Models.kt` `enum class Emotion`
   - 항목마다 `label`(한국어 해요체 "행복해요"), `emoji`, `valence`(-2~+2, 주간 기분 지수에 쓰임)
   - `when (emotion)` 분기는 컴파일러가 누락을 잡는다. 감정별 색상 등 다른 매핑도 grep: `grep -rn "Emotion\." shared/src/commonMain`
2. **Edge Function** — `supabase/functions/analyze-photo/analysis.ts`
   - `EMOTIONS` 배열, `AnalysisSchema.scores` 키(같은 순서), `SYSTEM_PROMPT`의 감정 정의(강아지/고양이 얼굴 신호)
3. **DB** — `/new-migration` 절차로 새 마이그레이션:
   - `alter table public.analyses drop constraint <기존 제약 이름>, add constraint analyses_primary_emotion_check check (primary_emotion in (...));`
   - 기존 제약 이름은 로컬 DB에서 확인: `select conname from pg_constraint where conrelid = 'public.analyses'::regclass and contype = 'c';`
   - 삭제·이름 변경이면 기존 행을 먼저 `update`로 옮긴다 (안 그러면 제약 추가가 실패). `scores` jsonb 키도 고려.
   - 불변식 검사는 `primary_emotion in` 을 포함한 **가장 최근** 마이그레이션을 기준으로 읽으니 목록 전체를 그 형태로 쓴다.
4. **배포 순서 안내** — 추가: DB → 함수 → 앱. 삭제/이름 변경: 앱이 모르는 값을 받지 않도록 함수와 DB 이행 계획을 사용자와 정한다 (구버전 앱은 `Emotion.fromKey`가 null → 해당 기록 숨김).
5. **검증** — `/verify` (불변식, deno 테스트의 schema/EMOTIONS 일치 테스트, Kotlin 테스트, RLS 테스트).
