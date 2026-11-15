package com.chani.porumi.data

import io.github.jan.supabase.auth.AuthConfig
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.Kakao
import io.github.jan.supabase.auth.providers.OAuthProvider

/** 로그인 후 브라우저가 앱으로 돌려보내는 딥링크: porumi://login-callback (Supabase 대시보드 Redirect URLs 에 등록 필요) */
const val AUTH_SCHEME = "porumi"
const val AUTH_HOST = "login-callback"

/** 로그인 화면에 노출하는 SNS 순서 그대로 */
enum class SocialLogin(val label: String) {
    KAKAO("카카오로 시작하기"),
    NAVER("네이버로 시작하기"),
    APPLE("Apple로 시작하기"),
    GOOGLE("Google로 시작하기"),
}

/**
 * 네이버는 Supabase 기본 제공자가 아니라 "사용자 지정 OAuth 제공자"로 등록한다 (식별자 custom:naver).
 * 네이버 사용자 정보 응답이 표준과 달라 Edge Function naver-userinfo 가 변환해 준다.
 */
internal data object NaverProvider : OAuthProvider() {
    override val name = "custom:naver"
}

internal val SocialLogin.provider: OAuthProvider
    get() = when (this) {
        SocialLogin.KAKAO -> Kakao
        SocialLogin.NAVER -> NaverProvider
        SocialLogin.APPLE -> Apple
        SocialLogin.GOOGLE -> Google
    }

/** 플랫폼별 외부 로그인 화면 설정 (Android: Custom Tabs) */
internal expect fun AuthConfig.platformAuthDefaults()
