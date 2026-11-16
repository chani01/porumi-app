#!/usr/bin/env bash
# Porumi 불변식 검사: CLAUDE.md 에 "함께 바꿔야 한다"고 적힌 규칙을 기계적으로 확인한다.
# 사용: scripts/harness/check-invariants.sh   (실패 시 exit 1, 경고만 있으면 exit 0)
# 호출처: .githooks/pre-commit, (P2) Claude Stop 게이트
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

SRC=shared/src/commonMain/kotlin/com/chani/porumi
MODELS=$SRC/domain/Models.kt
KOIN=$SRC/di/Koin.kt
MAIN_VM=$SRC/feature/MainViewModel.kt
SOCIAL=$SRC/data/SocialLogin.kt
ANALYZE_FN=supabase/functions/analyze-photo/analysis.ts
MANIFEST=androidApp/src/main/AndroidManifest.xml
PLIST=iosApp/iosApp/Info.plist

errors=0
warnings=0
fail() { echo "✖ $*" >&2; errors=$((errors + 1)); }
warn() { echo "⚠ $*" >&2; warnings=$((warnings + 1)); }
join() { tr '\n' ' ' | sed 's/ $//'; }

# ── 1. 감정 7종: Kotlin enum = Edge Function(EMOTIONS, scores) = DB CHECK ──────────
kt_emotions=$(awk '/enum class Emotion/{f=1;next} f{print} f&&/;/{exit}' "$MODELS" \
  | grep -oE '^[[:space:]]*[A-Z_]+\(' | tr -d ' (\t' | join)
fn_emotions=$(grep -E '^(export )?const EMOTIONS' "$ANALYZE_FN" | grep -oE '"[A-Z_]+"' | tr -d '"' | join)
fn_scores=$(awk '/scores: z\.object\(\{/{f=1;next} f&&/\}\)/{exit} f' "$ANALYZE_FN" \
  | grep -oE '^[[:space:]]*[A-Z_]+:' | tr -d ' :\t' | join)
# 가장 최근에 primary_emotion 제약을 정의한 마이그레이션 기준
mig_file=$(grep -lE 'primary_emotion[[:space:]]+in' supabase/migrations/*.sql | sort | tail -n 1)
db_emotions=$(awk '/primary_emotion[[:space:]]+in/{f=1} f{print} f&&/\)\)/{exit}' "$mig_file" \
  | grep -oE "'[A-Z_]+'" | tr -d "'" | join)

if [ -z "$kt_emotions" ] || [ -z "$fn_emotions" ] || [ -z "$fn_scores" ] || [ -z "$db_emotions" ]; then
  fail "감정 목록 추출 실패 (파싱 패턴이 코드 구조와 맞지 않음 — check-invariants.sh 갱신 필요)
    Models.kt=[$kt_emotions] EMOTIONS=[$fn_emotions] scores=[$fn_scores] DB=[$db_emotions]"
elif [ "$kt_emotions" != "$fn_emotions" ] || [ "$kt_emotions" != "$fn_scores" ] || [ "$kt_emotions" != "$db_emotions" ]; then
  fail "감정 목록 불일치 — 세 곳을 함께 바꿔야 합니다
    $MODELS        : $kt_emotions
    $ANALYZE_FN EMOTIONS: $fn_emotions
    $ANALYZE_FN scores  : $fn_scores
    $mig_file : $db_emotions"
fi

# ── 2. 딥링크 scheme/host: SocialLogin.kt = AndroidManifest = Info.plist ───────────
kt_scheme=$(grep -E 'const val AUTH_SCHEME' "$SOCIAL" | grep -oE '"[^"]+"' | tr -d '"')
kt_host=$(grep -E 'const val AUTH_HOST' "$SOCIAL" | grep -oE '"[^"]+"' | tr -d '"')
if [ -z "$kt_scheme" ] || [ -z "$kt_host" ]; then
  fail "SocialLogin.kt 에서 AUTH_SCHEME/AUTH_HOST 를 찾지 못함"
else
  grep -qE "android:scheme=\"$kt_scheme\"" "$MANIFEST" \
    || fail "AndroidManifest.xml 에 android:scheme=\"$kt_scheme\" 없음 (AUTH_SCHEME 과 불일치)"
  grep -qE "android:host=\"$kt_host\"" "$MANIFEST" \
    || fail "AndroidManifest.xml 에 android:host=\"$kt_host\" 없음 (AUTH_HOST 와 불일치)"
  grep -qE "<string>$kt_scheme</string>" <<<"$(awk '/CFBundleURLSchemes/{f=1} f' "$PLIST")" \
    || fail "Info.plist CFBundleURLSchemes 에 \"$kt_scheme\" 없음 (AUTH_SCHEME 과 불일치)"
fi

# ── 3. 모든 ViewModel 이 Koin appModule 에 등록돼 있는지 ─────────────────────────
while IFS=: read -r file _ name; do
  grep -qE "viewModelOf\(::$name\)|viewModel[[:space:]]*\{[[:space:]]*$name\(" "$KOIN" \
    || fail "$name ($file) 이 $KOIN 에 등록되지 않음 → viewModelOf(::$name) 추가"
done < <(grep -rnoE '^[[:space:]]*(internal |open )?class [A-Za-z0-9_]+ViewModel\b' "$SRC" \
  | sed -E 's/:[[:space:]]*(internal |open )?class /:/')

# ── 4. 로그아웃 시 앱 전역 상태 초기화 ─────────────────────────────────────────
# data/ 의 클래스가 MutableStateFlow 를 가지면 fun clear() 가 있고 MainViewModel 의
# SignedOut 분기에서 호출돼야 한다. 기기 설정처럼 유지가 의도된 클래스는
# 클래스 본문(또는 바로 위 KDoc)에 `harness:keep-on-signout` 주석을 둔다.
for f in "$SRC"/data/*.kt; do
  awk -v file="$f" '
    function flush() { if (cls != "" && state && !keep) print cls "|" (clr ? 1 : 0) "|" file }
    /harness:keep-on-signout/ { if ($0 ~ /^[[:space:]]*(\/\*\*|\*)/) pendingKeep = 1; else keep = 1 }
    /^(internal |open )?class [A-Za-z0-9_]+/ {
      flush()
      match($0, /class [A-Za-z0-9_]+/); cls = substr($0, RSTART + 6, RLENGTH - 6)
      state = 0; clr = 0; keep = pendingKeep; pendingKeep = 0
    }
    cls != "" && /MutableStateFlow[<(]/ { state = 1 }
    cls != "" && /fun clear\(\)/ { clr = 1 }
    END { flush() }
  ' "$f"
done | while IFS='|' read -r cls has_clear file; do
  if [ "$has_clear" != 1 ]; then
    echo "✖ $cls ($file) 은 인메모리 상태가 있지만 fun clear() 가 없음 — 로그아웃 시 이전 사용자 데이터가 남음 (유지가 의도라면 harness:keep-on-signout 주석)" >&2
    echo FAIL
    continue
  fi
  var=$(grep -oE "[a-zA-Z0-9_]+:[[:space:]]*$cls\b" "$MAIN_VM" | head -n 1 | cut -d: -f1)
  if [ -z "$var" ] || ! grep -qE "\b$var\.clear\(\)" "$MAIN_VM"; then
    echo "✖ $cls.clear() 가 $MAIN_VM 의 SignedOut 분기에서 호출되지 않음" >&2
    echo FAIL
  fi
done | grep -c FAIL >/dev/null && errors=$((errors + 1))

# ── 5. 클라이언트 코드에 service_role 키 금지 ──────────────────────────────────
leaks=$(grep -rnIiE 'service_role|SERVICE_ROLE_KEY' shared/src androidApp/src iosApp \
  --exclude-dir=build --exclude-dir=.gradle 2>/dev/null)
if grep -qiE '^supabase\.key[[:space:]]*=.*service_role' local.properties 2>/dev/null; then
  leaks="$leaks"$'\n'"local.properties: supabase.key 에 service_role 키로 보이는 값"
fi
[ -n "${leaks//[$'\n']/}" ] && fail "클라이언트에 service_role 참조 — anon/publishable 키만 허용
$leaks"

# ── 결과 ───────────────────────────────────────────────────────────────────
if [ "$errors" -gt 0 ]; then
  echo "불변식 검사 실패: 오류 ${errors}건, 경고 ${warnings}건" >&2
  exit 1
fi
echo "✔ 불변식 검사 통과 (경고 ${warnings}건)"
