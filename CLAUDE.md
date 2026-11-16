# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Porumi is a Kotlin Multiplatform + Compose Multiplatform app (Android / iOS) that analyzes the facial expression of a user's dog/cat photo with Claude and shows a weekly mood report. Backend is Supabase (Auth, Postgres + RLS, Storage, Edge Functions). UI strings, code comments, and the README are in Korean — keep new user-facing text in Korean.

## Commands

```bash
./gradlew :androidApp:assembleDebug          # Android debug build
./gradlew :shared:testAndroidHostTest        # unit tests (commonTest + androidHostTest) on JVM
./gradlew :shared:testAndroidHostTest --tests "com.chani.porumi.domain.WeeklyMoodCalculatorTest"   # single test class
./gradlew :shared:iosSimulatorArm64Test      # run commonTest on iOS simulator
(cd supabase/functions/analyze-photo && deno test --no-lock)   # Edge Function tests (each function dir)
supabase test db                             # pgTAP RLS tests in supabase/tests/ (needs local Supabase)
scripts/harness/verify.sh [--all]            # everything relevant to the current diff
```

- iOS: open `iosApp/` in Xcode and run (it links the static `Shared` framework built from `:shared`).
- Supabase (CLI, project already linked via `supabase/.temp`):
  - `supabase db push` — apply `supabase/migrations/`
  - `supabase functions deploy analyze-photo` / `supabase functions deploy delete-account`
  - `supabase secrets set ANTHROPIC_API_KEY=...` — required by `analyze-photo`
  - Local stack for tests (if port 54321 is taken, e.g. by CleanMyMac, start only Postgres — enough for `test db`/`db lint`):
    `supabase start -x gotrue,realtime,storage-api,imgproxy,kong,mailpit,postgrest,postgres-meta,studio,edge-runtime,logflare,vector,supavisor --ignore-health-check`

## Configuration

`shared/build.gradle.kts` has a `generateAppConfig` task that reads `supabase.url` / `supabase.key` from root `local.properties` and generates `com.chani.porumi.AppConfig` into `shared/build/generated/appconfig/` (added to commonMain). Only the publishable/anon key belongs there — never the service_role key. If either value is blank, `App()` shows `ConfigMissingScreen` instead of the app.

## Architecture

Almost all code lives in `shared/src/commonMain/kotlin/com/chani/porumi/`; `androidApp` (`MainActivity`, `PorumiApplication`) and `iosApp` (`MainViewController.kt` + Swift shell) only call `initKoin()` and host `App()`.

- **`App.kt`** — root composable and the entire navigation graph. Routes are `@Serializable` objects/data classes used with type-safe Navigation Compose. A `Crossfade` switches between top-level states (Splash / Auth / FirstPet / LoadFailed / MainScaffold) based on `MainViewModel.appState`; the splash is held for at least `SPLASH_MIN_MS`. The bottom bar (홈/리포트/기록/마이) shows only on the four tab routes. A singleton Coil `ImageLoader` is configured with the Supabase coil3 integration so `StorageItem`s load directly from the private bucket.
- **`feature/MainViewModel.kt`** — derives `AppState` from auth status + pet list. On sign-in it refreshes pets and analyses; on sign-out it clears `PetRepository`, `AnalysisRepository`, and `PetFilter`. Any new app-wide in-memory state must also be cleared here.
- **`data/Repositories.kt`** — `AuthRepository`, `PetRepository`, `AnalysisRepository` are Koin singletons that own in-memory `StateFlow` caches. ViewModels observe these flows rather than fetching independently; mutations update the server and then patch the local flow. `AnalysisRepository.analyses` (latest 500 by `taken_at`) is shared by the home feed and the report.
- **`data/PetFilter.kt`** — app-wide "currently selected pet"; always resolves to some pet (falls back to the first).
- **`domain/`** — models (`Pet`, `Analysis`, `Emotion`, `Species`) and `WeeklyMoodCalculator` (weeks start Monday; tested in `commonTest`).
- **`di/Koin.kt`** — single `appModule`; register new repositories/ViewModels here (`viewModelOf(::X)`), screens obtain them with `koinViewModel()`.
- **`feature/<name>/`** — one `Screen` + `ViewModel` per feature (auth, home, analyze, report, pet, detail, settings, legal, splash).

### Photo analysis flow

1. App compresses the photo (1280px JPEG) and uploads to Storage `pet-photos/{uid}/{uuid}.jpg`.
2. App invokes Edge Function `analyze-photo` with `{ image_path, pet_id, taken_at }`.
3. The function (`supabase/functions/analyze-photo/`: `analysis.ts` schema + pure logic, `handler.ts` HTTP/status-code contract with injected deps, `index.ts` wiring only) runs with the **user's JWT** (so RLS applies), downloads the image, calls Claude via `@anthropic-ai/sdk` with a Zod structured-output schema, normalizes scores, and inserts the row into `analyses`, returning it.
4. Non-pet / face-not-visible photos return `422 { code: "NO_PET_FACE" }` and the uploaded file is deleted; refusals return `422 REFUSED`. `AnalysisRepository.analyze` maps HTTP status codes to Korean error messages via `analysisErrorMessage` — keep it in sync with `analyze-photo/handler.ts` (both sides are tested).

The emotion set (`HAPPY, RELAXED, EXCITED, CURIOUS, SLEEPY, ANXIOUS, ANNOYED`) is duplicated in the Edge Function schema, the DB, and `domain/Models.kt` — change all of them together.

### Backend

- `supabase/migrations/` — `pets`, `analyses`, `notifications` with owner-only RLS; default Supabase grants are trimmed (`anon` has no table access, app may update only `analyses.note` and `notifications.read_at`, `analyses.image_path` must start with the owner's uid, uploads are one level deep `{uid}/{file}`), and the private `pet-photos` bucket whose policies require the first path segment to equal `auth.uid()` (Storage paths keep the `{uid}/...` prefix). `pets`/`analyses`/`notifications` cascade from `auth.users`.
- Edge Functions: `analyze-photo` (user JWT, RLS applies), `delete-account` (verifies the user, then service-role server-side to delete Storage folder + auth user), `naver-userinfo` (OIDC userinfo proxy for `custom:naver`). Each is `handler.ts` (logic, deps injected, `*_test.ts`) + thin `index.ts`.
- Detailed migration/RLS/function rules: `.claude/rules/supabase.md` (loaded when working under `supabase/`).

### Sign-in

- Kakao / Naver / Apple / Google via `AuthRepository.signInWithSocial` (`data/SocialLogin.kt`) plus email+password ("이메일로 계속하기", also for App Store review). OAuth (PKCE) returns through `porumi://login-callback`. Details: `.claude/rules/auth-deeplink.md`; console setup: `docs/social-login-setup.md`.

## Harness

- `scripts/harness/check-invariants.sh` — machine-checks the "change together" rules above: emotion set (Models.kt = analyze-photo `EMOTIONS`/`scores` = latest DB CHECK), deep-link scheme/host (SocialLogin.kt = AndroidManifest = Info.plist), every `*ViewModel` registered in Koin, every stateful `data/` class has `clear()` called on sign-out in `MainViewModel` (opt out with a `harness:keep-on-signout` comment), and no `service_role` in client code. Runs from `.githooks/pre-commit`; run it after touching any of these.
- `.claude/hooks/guard.sh` (PreToolUse) blocks git push/merge/rebase/cherry-pick, branch/worktree creation, `reset --hard`, edits to `local.properties`, and `service_role` in client code; editing an already-committed migration asks the user. Supabase deploy/push/secrets commands require confirmation (`.claude/settings.json`).
- `scripts/harness/verify.sh [--all]` — verifies what changed vs HEAD (incl. untracked): `shared/` → `:shared:testAndroidHostTest` (+ iOS compile when common/iOS code changed), `androidApp/` → `assembleDebug`, `supabase/functions/<fn>/` → `deno check`/`lint`/`test`, migrations → `supabase db lint`/`test db` (only if local Supabase is running), always the invariant check, plus a warning for new non-Korean UI strings. Logs in `build/harness/`.
- `.claude/hooks/stop-gate.sh` (Stop) runs `verify.sh` before a turn ends and blocks with the failures; it skips unchanged trees and gives up after 3 consecutive blocks. When it blocks, fix the cause — or, if the failure is outside the task, report it instead of working around it.
- `.claude/hooks/post-edit.sh` (PostToolUse) gives instant feedback on the edited file: `deno check`/`lint` for Edge Functions (blocking), ktlint for Kotlin limited to the lines changed vs HEAD (report-only; existing code is not yet ktlint-clean, config in `.editorconfig`).
- If a check's parsing breaks because code structure changed, update the script rather than bypassing it.
- Workflows: `/verify`, `/new-feature`, `/new-migration`, `/change-emotion`, `/commit` (user-invoked only) in `.claude/skills/`; the `rls-reviewer` agent (`.claude/agents/`) independently reviews migration/policy/grant changes. UI rules: `.claude/rules/compose-ui.md`.

## Commit convention

Commit messages are written in **Korean**. Format the subject as `<type>: <한국어 요약>` (optional scope: `feat(report): ...`).

- Types: `feat` 기능, `fix` 버그 수정, `refactor` 리팩터링, `style` 포맷/UI 스타일, `docs` 문서, `test` 테스트, `chore` 빌드·설정·의존성, `db` 마이그레이션, `func` Edge Function
- Subject: Korean, ≤ 50 chars, no trailing period, 명사형/개조식 (e.g. `feat: 주간 리포트에 감정 비율 차트 추가`, `fix: 로그아웃 시 펫 필터 초기화 누락 수정`).
- Body (optional): blank line after the subject, then what/why in Korean, wrapped at 72 chars. Trailers like `Co-Authored-By:` stay as-is.
- Enforced by `.githooks/commit-msg` (template: `.gitmessage`). After cloning, run once:
  `git config core.hooksPath .githooks && git config commit.template .gitmessage`
