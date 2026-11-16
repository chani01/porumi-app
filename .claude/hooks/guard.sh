#!/usr/bin/env bash
# PreToolUse 가드레일 (L1). stdin 으로 훅 입력 JSON 을 받는다.
#   exit 2 + stderr  → 도구 호출 차단, 사유가 Claude 에게 전달됨
#   JSON permissionDecision "ask" → 사용자에게 확인 요청
#   exit 0           → 통과
set -uo pipefail

input=$(cat)
tool=$(jq -r '.tool_name // empty' <<<"$input")
ROOT="${CLAUDE_PROJECT_DIR:-$(pwd)}"

deny() { echo "⛔ 하네스 가드: $*" >&2; exit 2; }
ask() {
  jq -n --arg r "$1" '{hookSpecificOutput: {hookEventName: "PreToolUse",
    permissionDecision: "ask", permissionDecisionReason: $r}}'
  exit 0
}

case "$tool" in
Bash)
  cmd=$(jq -r '.tool_input.command // empty' <<<"$input")
  # 줄바꿈·연결 연산자로 나뉜 각 명령을 따로 검사 (`cd x && git push` 우회 방지)
  while IFS= read -r part; do
    part=$(sed -E 's/^[[:space:]]+//' <<<"$part")
    # git -C <dir> 같은 전역 옵션 제거
    g=$(sed -E 's/^git([[:space:]]+-C[[:space:]]+[^[:space:]]+|[[:space:]]+-c[[:space:]]+[^[:space:]]+)*/git/' <<<"$part")
    case "$g" in
      "git push"*)              deny "push 는 사용자가 직접 합니다." ;;
      "git merge"*|"git rebase"*|"git cherry-pick"*)
                                deny "브랜치 병합·재배치는 사용자가 직접 합니다 (커밋은 요청 시 현재 브랜치에만)." ;;
      "git checkout -b"*|"git checkout -B"*|"git switch -c"*|"git switch -C"*|"git switch --create"*)
                                deny "브랜치는 사용자가 만듭니다. 현재 브랜치에서 작업하세요." ;;
      "git worktree add"*)      deny "worktree/브랜치 생성은 사용자가 합니다." ;;
      "git reset --hard"*|"git clean -f"*|"git clean -d"*|"git checkout -- ."*|"git restore ."*)
                                deny "작업 트리를 되돌리는 파괴적 명령입니다. 필요하면 사용자에게 요청하세요." ;;
      "git branch"*)
        # 조회(--show-current, -a, -r, -v, --list)는 허용, 생성·삭제·이름변경은 차단
        rest=$(sed -E 's/^git branch//' <<<"$g")
        if grep -qE '(^|[[:space:]])-(d|D|m|M|c|C|f)\b|--(delete|move|copy|force)|(^|[[:space:]])[^-[:space:]][^[:space:]]*' <<<"$rest" \
           && ! grep -qE -- '--(list|show-current|contains|merged|no-merged)' <<<"$rest"; then
          deny "브랜치 생성·삭제·이름변경은 사용자가 합니다."
        fi ;;
    esac
  done < <(sed -E 's/(&&|\|\||;|\|)/\n/g' <<<"$cmd")
  ;;

Edit|Write|MultiEdit|NotebookEdit)
  path=$(jq -r '.tool_input.file_path // .tool_input.notebook_path // empty' <<<"$input")
  [ -z "$path" ] && exit 0
  rel="${path#"$ROOT"/}"

  case "$rel" in
    local.properties)
      deny "local.properties 는 사용자가 관리합니다 (supabase.url / anon key). 필요한 값을 사용자에게 안내하세요." ;;
  esac

  # 새로 쓰는 내용 (Write content / Edit new_string / MultiEdit edits[].new_string)
  new=$(jq -r '[.tool_input.content, .tool_input.new_string, (.tool_input.edits // [] | .[].new_string), .tool_input.new_source]
               | map(select(. != null)) | join("\n")' <<<"$input")

  case "$rel" in
    shared/*|androidApp/*|iosApp/*)
      if grep -qiE 'service_role|SERVICE_ROLE_KEY' <<<"$new"; then
        deny "클라이언트 코드($rel)에 service_role 참조를 넣을 수 없습니다. 권한이 필요한 작업은 Edge Function 에서 처리하세요."
      fi ;;
  esac

  case "$rel" in
    supabase/migrations/*.sql)
      if git -C "$ROOT" ls-files --error-unmatch -- "$rel" >/dev/null 2>&1; then
        ask "이미 커밋된 마이그레이션($rel) 수정입니다. 원격 DB 에 반영됐다면 새 마이그레이션 파일을 추가해야 합니다. 아직 반영 전이라 수정해도 되나요?"
      fi ;;
  esac
  ;;
esac

exit 0
