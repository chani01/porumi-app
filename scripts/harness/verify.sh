#!/usr/bin/env bash
# Porumi 변경 검증 (L3). HEAD 대비 바뀐 파일(미추적 포함)을 보고 필요한 검사만 돌린다.
# 사용: scripts/harness/verify.sh          # 변경 영역만
#       scripts/harness/verify.sh --all    # 전체
# 실패 시 exit 1 과 함께 실패 단계의 핵심 로그를 출력한다. 로그 원본: build/harness/*.log
set -uo pipefail
export NO_COLOR=1

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
LOG_DIR=build/harness
mkdir -p "$LOG_DIR"
touch "$LOG_DIR/.start"

ALL=0
[ "${1:-}" = "--all" ] && ALL=1

if [ "$ALL" = 1 ]; then
  changed=$(git ls-files)
else
  changed=$( { git diff --name-only HEAD; git ls-files --others --exclude-standard; } | sort -u)
fi
has() { grep -qE "$1" <<<"$changed"; }

failed=()
warnings=()
passed=()

# run <이름> <로그파일> <명령...>
run() {
  local name="$1" log="$2"; shift 2
  echo "▶ $name" >&2
  if "$@" >"$log" 2>&1; then
    passed+=("$name")
  else
    failed+=("$name|$log")
  fi
}

# ── Gradle (한 번의 호출로 묶어서 데몬·구성 비용 절약) ─────────────────────────
gradle_tasks=()
if has '^(shared/src/(commonMain|commonTest|androidMain|androidHostTest)/|shared/build\.gradle\.kts|gradle/|build\.gradle\.kts|settings\.gradle\.kts)'; then
  gradle_tasks+=(":shared:testAndroidHostTest")
fi
if has '^androidApp/'; then
  gradle_tasks+=(":androidApp:assembleDebug")
fi
if [ "$(uname)" = Darwin ] && has '^shared/src/(iosMain|iosTest|commonMain)/'; then
  gradle_tasks+=(":shared:compileKotlinIosSimulatorArm64")
fi
if [ ${#gradle_tasks[@]} -gt 0 ]; then
  run "gradle ${gradle_tasks[*]}" "$LOG_DIR/gradle.log" ./gradlew --console=plain --continue ${gradle_tasks[@]+"${gradle_tasks[@]}"}
fi

# ── Edge Functions (함수별 deno check / lint / test) ──────────────────────────
if command -v deno >/dev/null; then
  for fn in $(grep -oE '^supabase/functions/[^/]+/' <<<"$changed" | sort -u); do
    [ -f "$fn/index.ts" ] || continue
    name=$(basename "$fn")
    run "deno check $name" "$LOG_DIR/deno-check-$name.log" bash -c "cd '$fn' && deno check --no-lock index.ts"
    run "deno lint $name"  "$LOG_DIR/deno-lint-$name.log"  bash -c "cd '$fn' && deno lint index.ts"
    if ls "$fn"/*_test.ts >/dev/null 2>&1; then
      run "deno test $name" "$LOG_DIR/deno-test-$name.log" bash -c "cd '$fn' && deno test --no-lock --allow-env --allow-read"
    fi
  done
elif has '^supabase/functions/'; then
  warnings+=("deno 가 없어 Edge Function 검사를 건너뜀 (brew install deno)")
fi

# ── 마이그레이션 (로컬 Supabase 가 떠 있을 때만) ─────────────────────────────
if has '^supabase/(migrations|tests)/'; then
  if docker info >/dev/null 2>&1 && supabase status >/dev/null 2>&1; then
    run "supabase db lint" "$LOG_DIR/db-lint.log" supabase db lint --local --fail-on error
    if ls supabase/tests/*.sql >/dev/null 2>&1; then
      run "supabase test db" "$LOG_DIR/db-test.log" supabase test db
    fi
  else
    warnings+=("마이그레이션 변경됨: 로컬 Supabase(docker) 미실행으로 db lint/test 생략 — 'supabase start' 후 다시 검증 필요")
  fi
fi

# ── 불변식 (항상) ────────────────────────────────────────────────────────────
run "불변식 검사" "$LOG_DIR/invariants.log" scripts/harness/check-invariants.sh

# ── 새 UI 문구가 한국어인지 (경고) ─────────────────────────────────────────────
added_ui=$( { git diff -U0 HEAD -- shared/src/commonMain | grep -E '^\+[^+]';
              git ls-files --others --exclude-standard -- shared/src/commonMain | xargs cat 2>/dev/null; } \
  | perl -CSD -ne 'while (/\b(?:Text|text\s*=|label\s*=|title\s*=|placeholder\s*=)\s*\(?\s*"([^"\$]*[A-Za-z][^"\$]*)"/g) { my $s=$1; print "$s\n" unless $s =~ /\p{Hangul}/ }' \
  | sort -u | head -n 10)
if [ -n "$added_ui" ]; then
  warnings+=("한글이 없는 새 UI 문구 (사용자 노출 문구는 한국어): $(tr '\n' ',' <<<"$added_ui" | sed 's/,$//')")
fi

# ── 결과 ───────────────────────────────────────────────────────────────────
echo
for p in ${passed[@]+"${passed[@]}"}; do echo "✔ $p"; done
for w in ${warnings[@]+"${warnings[@]}"}; do echo "⚠ $w"; done
if [ ${#failed[@]} -eq 0 ]; then
  echo "✔ 검증 통과"
  exit 0
fi

for f in ${failed[@]+"${failed[@]}"}; do
  name="${f%%|*}"; log="${f#*|}"
  echo
  echo "✖ $name  (전체 로그: $log)"
  # 핵심 줄 우선: Kotlin 컴파일 오류, 실패한 테스트, deno 오류, 불변식 오류
  key=$(grep -E '^e: |FAILED|AssertionError|Exception|^error|^TS[0-9]+|^\s*at file|✖|\[ERROR\]|ERROR' "$log" | head -n 30)
  # 테스트 실패 메시지는 콘솔에 안 나오므로 JUnit XML 에서 보충
  if [[ "$name" == gradle* ]]; then
    msgs=$(grep -ho '<testcase name="[^"]*" classname="[^"]*"\|<failure message="[^"]*"' \
      $(find . -path '*/build/test-results/*' -name '*.xml' -newer "$LOG_DIR/.start" 2>/dev/null) 2>/dev/null \
      | grep -B1 '<failure' | grep -v '^--$' \
      | sed -E 's/<testcase name="([^"]*)" classname="([^"]*)"/  ▸ \2.\1/; s/<failure message="([^"]*)"/      \1/' \
      | perl -MHTML::Entities -pe 'decode_entities($_)' 2>/dev/null | head -n 20)
    [ -n "$msgs" ] && key="$key"$'\n'"실패한 테스트:"$'\n'"$msgs"
  fi
  if [ -z "$key" ]; then key=$(tail -n 30 "$log"); fi
  sed "s|file://$ROOT/||g; s|$ROOT/||g" <<<"$key"
done
echo
echo "✖ 검증 실패: ${#failed[@]}개 단계"
exit 1
