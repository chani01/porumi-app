---
name: verify
description: Porumi 변경 사항 전체 검증 — Gradle 테스트·빌드, Edge Function deno check/lint/test, RLS pgTAP, 불변식. 작업을 마무리하기 전, 커밋 전, 또는 "검증해/테스트 돌려" 요청 시 사용.
---

# /verify

1. 검사 범위 결정
   - 기본: `scripts/harness/verify.sh` (HEAD 대비 변경 + 미추적 파일만)
   - 인자에 `all`/`전체`가 있으면 `scripts/harness/verify.sh --all`
2. `supabase/migrations/` 또는 `supabase/tests/`가 바뀌었는데 로컬 DB가 꺼져 있으면(`supabase status` 실패) DB 검사가 경고로 건너뛰어진다. 이때는 먼저 로컬 Postgres를 띄운다:
   - `open -a Docker` (꺼져 있으면) 후
   - `supabase start -x gotrue,realtime,storage-api,imgproxy,kong,mailpit,postgrest,postgres-meta,studio,edge-runtime,logflare,vector,supavisor --ignore-health-check`
3. 스크립트 실행 (timeout 넉넉히, Gradle 첫 실행은 수 분).
4. 실패하면 출력의 핵심 줄로 원인을 찾고, 더 필요하면 `build/harness/<단계>.log` 전체를 읽는다.
   - 이번 작업이 원인이면 고치고 다시 3번.
   - 작업 범위 밖(기존 실패, 환경 문제)이면 고치지 말고 그대로 보고한다.
5. 결과 보고: 통과/실패한 단계, 경고(⚠), 건너뛴 검사와 이유. 검사를 건너뛰었다면 "통과"라고 하지 않는다.

검사가 코드 구조 변경 때문에 파싱에 실패하면 우회하지 말고 `scripts/harness/` 스크립트를 고친다.
