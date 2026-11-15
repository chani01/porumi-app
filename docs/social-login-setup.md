# SNS 로그인 콘솔 설정

앱 코드는 준비되어 있고, 아래 콘솔 설정을 마치면 각 버튼이 동작한다.
각 서비스에서 받은 키는 **Supabase 대시보드에만** 넣는다 (앱 코드·git 에 넣지 않음).

공통 값

| 항목 | 값 |
|---|---|
| Supabase 콜백 URL | `https://mkdjohstimbhbvgavgnu.supabase.co/auth/v1/callback` |
| 앱 딥링크 | `porumi://login-callback` |
| Android 패키지 / iOS 번들 ID | `com.chani.porumi` / Xcode 에서 확인 |

## 0. Supabase 공통

1. Dashboard → Authentication → URL Configuration → **Redirect URLs** 에 `porumi://login-callback` 추가

## 1. 카카오

1. [Kakao Developers](https://developers.kakao.com) → 내 애플리케이션 → 앱 추가
2. 앱 설정 → 플랫폼 키 → **REST API 키** 복사
3. 카카오 로그인 → 활성화 ON, **Redirect URI** 에 Supabase 콜백 URL 등록
4. 카카오 로그인 → 동의항목: 닉네임, 프로필 사진, 카카오계정(이메일)
   - 이메일 동의항목은 **비즈 앱**이어야 켤 수 있다 (개인 개발자도 비즈 앱 전환 가능)
5. 보안 → **Client Secret** 생성, 활성화 ON
6. Supabase → Authentication → Providers → **Kakao** → REST API 키를 Client ID 에, Client Secret 입력 후 저장

## 2. 네이버 (사용자 지정 제공자)

1. [네이버 개발자센터](https://developers.naver.com) → Application → 애플리케이션 등록
   - 사용 API: **네이버 로그인**, 제공 정보: 이메일, 이름(또는 별명), 프로필 사진
   - 서비스 환경: **PC 웹** 추가, Callback URL 에 Supabase 콜백 URL
2. Client ID / Client Secret 복사
3. 변환 함수 배포 (운영 프로젝트에 바로 반영됨)
   ```bash
   supabase functions deploy naver-userinfo
   ```
4. Supabase → Authentication → Providers → **Custom providers** → 추가
   - 종류: OAuth2
   - 식별자: `custom:naver` (앱 코드 `NaverProvider` 와 같아야 함)
   - Client ID / Client Secret: 2번 값
   - Authorization URL: `https://nid.naver.com/oauth2.0/authorize`
   - Token URL: `https://nid.naver.com/oauth2.0/token`
   - Userinfo URL: `https://mkdjohstimbhbvgavgnu.supabase.co/functions/v1/naver-userinfo`
   - Scopes: 비워 둠 (네이버는 동의항목을 개발자센터에서 정함)
   - PKCE: 끔 (네이버는 PKCE 를 지원하지 않음)

## 3. 구글

1. [Google Cloud Console](https://console.cloud.google.com) → API 및 서비스 → **OAuth 동의 화면** 설정 (앱 이름, 지원 이메일)
2. 사용자 인증 정보 → OAuth 클라이언트 ID 만들기 → 유형 **웹 애플리케이션**
   - 승인된 리디렉션 URI: Supabase 콜백 URL
3. Supabase → Providers → **Google** → Client ID / Client Secret 입력

## 4. Apple

App Store 정책상 다른 SNS 로그인이 있으면 Apple 로그인이 필수다.

1. [Apple Developer](https://developer.apple.com/account) → Identifiers → 앱 ID 에서 **Sign in with Apple** 체크
2. Identifiers → **Services ID** 추가 (예: `com.chani.porumi.signin`)
   - Sign in with Apple 설정: 도메인 `mkdjohstimbhbvgavgnu.supabase.co`, Return URL 에 Supabase 콜백 URL
3. Keys → 새 키 → Sign in with Apple 체크 → `.p8` 다운로드 (Key ID, Team ID 기록)
4. Supabase → Providers → **Apple**
   - Client IDs: Services ID (+ 앱 번들 ID)
   - Secret Key: Supabase 대시보드의 생성 도구에 `.p8`·Key ID·Team ID 를 넣어 만든 값
   - 이 Secret 은 **6개월마다 만료**되므로 갱신 일정을 잡아 둔다

## 확인 순서

1. 앱에서 버튼 → 브라우저(또는 Custom Tabs)에서 로그인 → 앱으로 돌아와 홈이 보이면 성공
2. 실패하면 Supabase → Logs → Auth 에서 원인 확인
   - `redirect_uri mismatch`: 각 콘솔의 Redirect/Callback URL 확인
   - 앱으로 안 돌아옴: Redirect URLs 에 `porumi://login-callback` 이 있는지 확인
