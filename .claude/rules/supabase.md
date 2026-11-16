---
paths:
  - "supabase/**"
---

# Supabase backend rules

## Migrations (`supabase/migrations/`)

- Never edit a migration that is already committed/applied — add a new `YYYYMMDDHHMMSS_<snake_name>.sql`. (The guard hook asks before editing a committed one.)
- Every new `public` table: `enable row level security`, owner-only policies using `(select auth.uid()) = user_id` (the `select` wrapper is for plan caching), `user_id ... default auth.uid() references auth.users (id) on delete cascade`, and an index leading with `user_id`.
- Supabase's default privileges grant `anon` and `authenticated` **all** table privileges. Column-level `grant update (col)` restricts nothing unless you first `revoke update on <table> from authenticated` (and `revoke all ... from anon` where anon has no use). Server-only writes (e.g. notifications insert) need an explicit `revoke insert`.
- Cross-table references in policies (e.g. `analyses.pet_id`) must also check ownership of the referenced row in `with check`.
- Storage paths are `{uid}/...`; bucket policies compare `(storage.foldername(name))[1]` with `auth.uid()::text`.
- Add isolation cases to `supabase/tests/rls_test.sql` for every new table/policy (and bump `plan(n)`). Run `supabase db lint --local` and `supabase test db`; the user runs `supabase db push`.
- Emotion set changes go through `/change-emotion`.

## Edge Functions (`supabase/functions/<fn>/`)

- Structure: `handler.ts` holds all logic and the HTTP status-code contract with Supabase/Claude/fetch injected as `Deps`; `index.ts` only wires real clients and calls `Deno.serve(createHandler(...))`. Pure helpers may live in a separate module (e.g. `analysis.ts`).
- Every behavior change needs a `*_test.ts` case (fake deps, no network). Shared fixtures go in `test_fixtures.ts`, never import one test file from another (tests would register twice).
- Functions that act as a user create the Supabase client with the caller's `Authorization` header so RLS applies; only `delete-account` uses the service-role key, and only server-side.
- `verify_jwt = false` in `config.toml` for all functions — they verify tokens themselves (`auth.getUser`), except `naver-userinfo`, which receives a Naver token.
- Pin dependencies exactly in each function's `deno.json` (test-only `@std/assert` included). Run deno with `--no-lock`; do not commit `deno.lock`.
- analyze-photo status codes are mirrored by `analysisErrorMessage` in `shared/.../data/Repositories.kt` (+ `AnalysisErrorMessageTest`) — change both together.
- User-facing error strings in responses are Korean.
