// naver-userinfo: Supabase "사용자 지정 OAuth 제공자(custom:naver)"의 userinfo_url 로 쓰는 변환 프록시.
//
// 네이버 회원 프로필 API(https://openapi.naver.com/v1/nid/me)는
//   { resultcode, message, response: { id, email, name, nickname, profile_image, ... } }
// 처럼 한 단계 감싼 응답을 주기 때문에, Supabase Auth 가 읽는 표준 OIDC userinfo 형태로 펼쳐서 돌려준다.
// 처리 로직은 handler.ts (handler_test.ts 로 검증).
//
// 사용자 JWT 가 아니라 네이버 토큰으로 호출되므로 config.toml 에서 verify_jwt = false.
// 받은 토큰을 네이버에 그대로 전달할 뿐 저장하거나 기록하지 않는다.

import "@supabase/functions-js/edge-runtime.d.ts";
import { createHandler } from "./handler.ts";

Deno.serve(createHandler());
