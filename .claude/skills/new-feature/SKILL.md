---
name: new-feature
description: Porumi 에 새 화면(Screen + ViewModel + Koin 등록 + 내비게이션 route)을 추가한다. "새 화면/기능 페이지 만들어" 같은 요청에 사용.
---

# /new-feature <이름> [설명]

먼저 `.claude/rules/compose-ui.md` 규칙을 따른다. 비슷한 기존 화면 하나(예: 하위 화면이면 `feature/notifications/NotificationsScreen.kt`, 설정형이면 `feature/settings/NotificationSettingsScreen.kt`)를 읽고 구조를 맞춘다.

기준 경로: `S=shared/src/commonMain/kotlin/com/chani/porumi`

## 체크리스트 (모두 해야 끝)

1. **ViewModel** — `$S/feature/<name>/<Name>ViewModel.kt`
   - 생성자로 필요한 Repository 주입, 저장소의 `StateFlow`를 관찰
   - 상태는 `<Name>UiState` data class, 오류 메시지는 한국어
   - 새 **앱 전역** 인메모리 상태(Repository 등)를 만들었다면 `fun clear()`를 두고 `MainViewModel`의 SignedOut 분기에서 호출 (기기 설정처럼 유지가 의도면 `harness:keep-on-signout` 주석)
2. **Screen** — `$S/feature/<name>/<Name>Screen.kt`
   - `fun <Name>Screen(onBack: () -> Unit, ..., viewModel: <Name>ViewModel = koinViewModel())`
   - 하위 화면이면 `PorumiTopBar(title = "...", onBack = onBack)`, 넓은 화면은 `centeredMaxWidth`
3. **Koin** — `$S/di/Koin.kt`에 `viewModelOf(::<Name>ViewModel)` (새 Repository면 `single { ... }`)
4. **Route** — `$S/App.kt`
   - `@Serializable data object <Name>Route` (인자가 있으면 `data class`, 원시 타입만)
   - `NavHost`에 `composable<<Name>Route> { <Name>Screen(onBack = { navController.popBackStack() }) }`
   - 진입점(버튼/메뉴)에서 `navController.navigate(<Name>Route)` 연결
   - 하단 탭은 4개(홈/리포트/기록/마이) 고정 — 탭 추가는 사용자 확인 후
5. **테스트** — 화면 로직 중 순수 함수로 뺄 수 있는 것(계산, 매핑, 필터)은 `internal fun`으로 빼서 `shared/src/commonTest/`에 테스트 추가
6. **검증** — `/verify` (불변식 검사가 Koin 등록·로그아웃 초기화 누락을 잡는다)

서버 데이터가 새로 필요하면 테이블/정책은 `/new-migration`으로 먼저 만든다.
