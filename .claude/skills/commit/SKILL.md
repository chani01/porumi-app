---
name: commit
description: Porumi 컨벤션(한국어 <type>: 요약)으로 현재 브랜치에 커밋한다. 사용자가 커밋을 요청했을 때만 사용.
disable-model-invocation: true
---

# /commit [메모]

사용자가 요청했을 때만 실행한다. 브랜치 생성·병합·push 는 하지 않는다 (가드 훅이 막는다).

1. `git status --short`, `git diff --stat`, 필요하면 `git diff`로 변경 내용 파악. 이번 작업과 무관한 변경(사용자가 따로 고치던 파일)이 섞여 있으면 포함할지 묻는다.
2. 파일을 명시해서 `git add <경로...>` (`git add -A`/`.`은 무관한 파일이 섞일 때 쓰지 않는다). `local.properties`, `.env*`, `deno.lock`, `build/`는 절대 넣지 않는다.
3. 메시지 (`.githooks/commit-msg`가 검사):
   - 제목: `<type>[(scope)]: <한국어 요약>` — 50자 이내, 마침표 없음, 명사형/개조식
   - type: `feat` 기능, `fix` 버그, `refactor`, `style` 포맷·UI 스타일, `docs`, `test`, `chore` 빌드·설정·하네스, `db` 마이그레이션, `func` Edge Function
   - 성격이 다른 변경(예: 마이그레이션 + 앱 기능)은 커밋을 나누자고 제안
   - 본문: 빈 줄 후 무엇을/왜, 한국어, 72자 줄바꿈
   - 마지막 줄: 시스템이 지정한 `Co-Authored-By:` 트레일러
4. `git commit -F - <<'EOF' ... EOF` 로 커밋. pre-commit 훅(불변식 검사)이 실패하면 `--no-verify`로 우회하지 말고 원인을 고치거나 보고한다.
5. `git log --oneline -1`로 결과 보고.
