#!/usr/bin/env bash
# iOS 시뮬레이터 스모크 실행: 빌드 → 설치 → 실행 → 로그 수집 → 스크린샷.
# 사용: scripts/harness/run-ios.sh [시뮬레이터 이름 (기본: 사용 가능한 첫 iPhone)] [대기 초 (기본 6)]
# 결과: build/harness/ios-screen.png, ios-run.log(에러 로그), ios-build.log
# 실패 시 exit 1: 빌드 실패 / 실행 후 프로세스 종료(크래시) / 치명 로그
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
OUT=build/harness
mkdir -p "$OUT"
BUNDLE_ID=com.chani.porumi.Porumi
WAIT="${2:-6}"

[ "$(uname)" = Darwin ] || { echo "✖ macOS 에서만 실행할 수 있어요" >&2; exit 1; }

# 시뮬레이터 선택
if [ -n "${1:-}" ]; then
  UDID=$(xcrun simctl list devices available | grep -F "$1 (" | head -n 1 | grep -oE '[0-9A-F-]{36}')
else
  UDID=$(xcrun simctl list devices available | grep -E '^\s+iPhone' | head -n 1 | grep -oE '[0-9A-F-]{36}')
fi
[ -n "$UDID" ] || { echo "✖ 사용할 iPhone 시뮬레이터를 찾지 못했어요 (xcrun simctl list devices available)" >&2; exit 1; }
NAME=$(xcrun simctl list devices | grep "$UDID" | sed -E 's/^\s+//; s/ \(.*//')
echo "▶ 시뮬레이터: $NAME ($UDID)"

# 빌드 (Kotlin 공유 프레임워크 포함). 서명 없이 시뮬레이터용
echo "▶ xcodebuild (로그: $OUT/ios-build.log)"
if ! xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
     -destination "id=$UDID" -derivedDataPath build/ios CODE_SIGNING_ALLOWED=NO build \
     > "$OUT/ios-build.log" 2>&1; then
  echo "✖ iOS 빌드 실패" >&2
  grep -E "error:|e: " "$OUT/ios-build.log" | sed "s|$ROOT/||g" | head -n 20 >&2
  exit 1
fi
APP=$(find build/ios/Build/Products/Debug-iphonesimulator -maxdepth 1 -name '*.app' | head -n 1)

# 부팅·설치·실행
xcrun simctl boot "$UDID" 2>/dev/null
xcrun simctl bootstatus "$UDID" -b >/dev/null 2>&1
open -a Simulator --args -CurrentDeviceUDID "$UDID" 2>/dev/null
xcrun simctl install "$UDID" "$APP"
xcrun simctl launch --terminate-running-process "$UDID" "$BUNDLE_ID" >/dev/null

# 렌더링을 기다리는 동안 앱 에러 로그 수집 (스플래시 최소 시간 포함)
timeout "$WAIT" xcrun simctl spawn "$UDID" log stream --level error \
  --predicate "process == \"Porumi\"" > "$OUT/ios-run.log" 2>&1

xcrun simctl io "$UDID" screenshot "$OUT/ios-screen.png" >/dev/null 2>&1

status=0
# pipefail 에서 grep -q 가 일찍 끝나면 SIGPIPE 로 오탐하므로 출력을 먼저 받는다
running=$(xcrun simctl spawn "$UDID" launchctl list 2>/dev/null)
if ! grep -q "$BUNDLE_ID" <<<"$running"; then
  echo "✖ 앱 프로세스가 종료됨 (크래시 의심) — 크래시 리포트: ~/Library/Logs/DiagnosticReports/Porumi-*.ips" >&2
  status=1
fi
fatal=$(grep -iE "fatal|uncaught|terminating|kotlin\..*Exception" "$OUT/ios-run.log" | head -n 10)
if [ -n "$fatal" ]; then
  echo "✖ 치명 로그:" >&2
  echo "$fatal" >&2
  status=1
fi
# 시스템 프레임워크(UIKit 등) 소음은 빼고 앱 바이너리(Kotlin 포함)에서 나온 에러만 센다
app_errors=$(grep -E 'Porumi: \((Porumi|Shared)\)' "$OUT/ios-run.log" | head -n 10)
if [ -n "$app_errors" ]; then
  echo "⚠ 앱 에러 로그:"
  echo "$app_errors"
fi
echo "✔ 스크린샷: $OUT/ios-screen.png  (전체 로그: $OUT/ios-run.log)"
exit $status
