#!/usr/bin/env bash
# Stop 훅 완료 게이트 (L3). Claude 가 턴을 끝내기 전에 작업 트리를 검증한다.
# - HEAD 대비 변경이 없으면 통과
# - 마지막으로 검사한 작업 트리와 같으면 다시 돌리지 않음 (이미 결과를 전달했으므로)
# - 실패하면 decision=block 으로 사유를 돌려줘 Claude 가 고치게 한다
# - 연속 차단은 MAX_BLOCKS 회까지만 (무한 루프 방지)
set -uo pipefail

MAX_BLOCKS=3
input=$(cat)
active=$(jq -r '.stop_hook_active // false' <<<"$input")
ROOT="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel)}"
cd "$ROOT" || exit 0

STATE=build/harness
mkdir -p "$STATE"

# 작업 트리 지문: 추적 파일 diff + 미추적 파일 내용
fingerprint() {
  {
    git diff HEAD --binary
    git ls-files --others --exclude-standard -z | xargs -0 shasum 2>/dev/null
  } | shasum | cut -d' ' -f1
}

if git diff --quiet HEAD && [ -z "$(git ls-files --others --exclude-standard)" ]; then
  rm -f "$STATE/blocks"
  exit 0
fi

fp=$(fingerprint)
if [ "$fp" = "$(cat "$STATE/last-fp" 2>/dev/null)" ]; then
  exit 0
fi

blocks=$(cat "$STATE/blocks" 2>/dev/null || echo 0)
[ "$active" != true ] && blocks=0

out=$(scripts/harness/verify.sh 2>/dev/null)
status=$?
echo "$fp" > "$STATE/last-fp"

if [ "$status" -eq 0 ]; then
  rm -f "$STATE/blocks"
  # 경고가 있으면 사용자에게 보이도록 systemMessage 로 전달 (차단 안 함)
  warn=$(grep '^⚠' <<<"$out")
  if [ -n "$warn" ]; then
    jq -n --arg m "하네스 검증 통과 (경고)
$warn" '{systemMessage: $m}'
  fi
  exit 0
fi

blocks=$((blocks + 1))
echo "$blocks" > "$STATE/blocks"
if [ "$blocks" -gt "$MAX_BLOCKS" ]; then
  rm -f "$STATE/blocks"
  jq -n --arg m "하네스 검증이 ${MAX_BLOCKS}회 연속 실패해 게이트를 통과시켰습니다. 실패 내용을 확인하세요:
$(tail -n 40 <<<"$out")" '{systemMessage: $m}'
  exit 0
fi

reason="하네스 검증 실패 (${blocks}/${MAX_BLOCKS}). 아래 문제를 고친 뒤 마무리하세요. 원인이 이번 작업 밖에 있거나 고칠 수 없다면 고치지 말고 실패 내용을 그대로 사용자에게 보고하세요.

$(head -c 6000 <<<"$out")"
jq -n --arg r "$reason" '{decision: "block", reason: $r}'
exit 0
