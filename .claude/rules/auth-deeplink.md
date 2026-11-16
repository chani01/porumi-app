---
paths:
  - "shared/src/commonMain/kotlin/com/chani/porumi/data/SocialLogin.kt"
  - "shared/src/commonMain/kotlin/com/chani/porumi/data/Supabase.kt"
  - "shared/src/commonMain/kotlin/com/chani/porumi/feature/auth/**"
  - "androidApp/src/main/AndroidManifest.xml"
  - "androidApp/src/main/kotlin/**/MainActivity.kt"
  - "iosApp/iosApp/Info.plist"
  - "iosApp/iosApp/ContentView.swift"
  - "supabase/functions/naver-userinfo/**"
  - "docs/social-login-setup.md"
---

# Sign-in / deep link rules

- Providers: Kakao / Naver (custom OIDC provider `custom:naver` whose `userinfo_url` is the `naver-userinfo` function) / Apple / Google via `AuthRepository.signInWithSocial`; email+password stays as the secondary "이메일로 계속하기" path (App Store review accounts use it — do not remove).
- OAuth uses PKCE and returns via `porumi://login-callback` (`AUTH_SCHEME` / `AUTH_HOST` in `SocialLogin.kt`). Android: `MainActivity` (`singleTask`, `onNewIntent`) → `handleAuthDeeplink`. iOS: `ContentView.swift` `onOpenURL` → `handleAuthDeeplink`.
- Scheme/host must match across `SocialLogin.kt`, `AndroidManifest.xml`, `Info.plist` (checked by `check-invariants.sh`) **and** the Supabase dashboard Redirect URLs (not checkable — tell the user when it changes).
- Provider console steps live in `docs/social-login-setup.md`; update it when the setup changes.
- Never log or persist provider access tokens.
