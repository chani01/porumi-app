#!/usr/bin/env bash
# PostToolUse 즉시 피드백 (L2). 방금 수정한 파일 하나만 빠르게 검사한다.
#   *.kt / *.kts                  → ktlint, 이번에 바뀐 줄의 위반만 보고 (차단 안 함)
#   supabase/functions/<fn>/*.ts  → deno check + lint, 오류면 exit 2 로 Claude 에게 전달
set -uo pipefail
export NO_COLOR=1

input=$(cat)
path=$(jq -r '.tool_input.file_path // empty' <<<"$input")
ROOT="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel)}"
[ -z "$path" ] || [ ! -f "$path" ] && exit 0
rel="${path#"$ROOT"/}"
cd "$ROOT" || exit 0

case "$rel" in
*.kt|*.kts)
  command -v ktlint >/dev/null || exit 0
  # HEAD 대비 바뀐 줄 번호 (미추적 파일은 전체)
  if git ls-files --error-unmatch -- "$rel" >/dev/null 2>&1; then
    lines=$(git diff -U0 HEAD -- "$rel" | perl -ne 'if (/^@@ .* \+(\d+)(?:,(\d+))? @@/) { my $c = defined $2 ? $2 : 1; print "$_\n" for $1 .. $1 + $c - 1 }')
    [ -z "$lines" ] && exit 0
  else
    lines=ALL
  fi
  report=$(ktlint --relative "$rel" 2>/dev/null | grep -E "^$rel:[0-9]+:" \
    | awk -F: -v want="$lines" 'BEGIN { n = split(want, a, "\n"); for (i = 1; i <= n; i++) ok[a[i]] = 1 }
                                want == "ALL" || ($2 in ok)' | head -n 20)
  if [ -n "$report" ]; then
    jq -n --arg c "ktlint (보고 전용 — 이번에 바꾼 줄의 스타일 위반, 가능하면 함께 정리):
$report" '{hookSpecificOutput: {hookEventName: "PostToolUse", additionalContext: $c}}'
  fi
  ;;

supabase/functions/*/*.ts)
  command -v deno >/dev/null || exit 0
  fn=$(cut -d/ -f1-3 <<<"$rel")
  file=$(cut -d/ -f4- <<<"$rel")
  out=$(cd "$fn" && { deno check --no-lock index.ts "$file" 2>&1 && deno lint "$file" 2>&1; })
  if [ $? -ne 0 ]; then
    echo "deno 검사 실패 ($rel):" >&2
    sed -E 's/\x1b\[[0-9;]*m//g' <<<"$out" | tail -n 30 >&2
    exit 2
  fi
  ;;
esac
exit 0
