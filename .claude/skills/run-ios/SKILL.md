---
name: run-ios
description: Porumi 를 iOS 시뮬레이터에서 실제로 빌드·실행하고 스크린샷으로 화면을 확인한다. UI 변경 후 화면이 의도대로 그려지는지, 앱이 크래시 없이 뜨는지 확인할 때, 또는 "앱 실행해/화면 확인해" 요청 시 사용.
---

# /run-ios [시뮬레이터 이름]

테스트 통과는 화면이 맞다는 뜻이 아니다. UI(Compose) 변경을 마무리하기 전에 실행해 직접 본다.

1. `scripts/harness/run-ios.sh [이름] [대기초]` 실행 (timeout 10분; 첫 빌드는 Kotlin 프레임워크 때문에 수 분).
   - 빌드 실패면 출력된 `error:`/`e:` 줄로 원인 수정 후 재실행.
2. `build/harness/ios-screen.png`를 Read 로 열어 **직접 본다**.
   - 비로그인 상태의 첫 화면은 로그인 화면(로고, 카카오·네이버·Apple·Google 버튼, "이메일로 계속하기")이 정상이다.
   - 확인할 것: 의도한 변경이 보이는지, 글자 잘림·겹침, 한국어 문구, 다크 모드/넓은 화면이 관련 변경이면 그 조건.
3. 크래시(프로세스 종료)나 치명 로그로 exit 1 이면 `build/harness/ios-run.log`와 `~/Library/Logs/DiagnosticReports/Porumi-*.ips` 최신 파일을 읽고 원인을 찾는다.

## 다른 조건으로 보기

- 넓은 화면: `scripts/harness/run-ios.sh "iPad Pro 13-inch (M5)"` 처럼 이름 지정 (`xcrun simctl list devices available`로 확인).
- 다크 모드: `xcrun simctl ui <UDID> appearance dark` 후 앱 재실행 → 스크린샷 (`xcrun simctl io <UDID> screenshot <경로>`), 확인 뒤 `light`로 되돌린다.
- 로그인 이후 화면: 실제 계정 세션이 필요하다. 시뮬레이터에서 사용자가 직접 로그인해 둔 상태면 재실행 후에도 세션이 유지되므로 그대로 스크린샷을 찍는다. 테스트 계정 비밀번호를 코드·로그·명령에 쓰지 않는다.

## 한계

- 탭·입력 같은 UI 조작은 자동화하지 않는다. 특정 화면 확인이 필요하면 사용자에게 그 화면으로 이동해 달라고 하고 스크린샷만 찍는다.
- Android 는 adb/에뮬레이터가 없어 아직 대상이 아니다.
- 끝나면 시뮬레이터는 켜 둬도 된다 (다음 실행이 빨라짐). 끄려면 `xcrun simctl shutdown all`.
