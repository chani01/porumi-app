package com.chani.porumi.data

import android.content.Intent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.AuthConfig
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.auth.handleDeeplinks
import org.koin.mp.KoinPlatform

/** 외부 브라우저 대신 앱 위에 뜨는 Custom Tabs 로 로그인 */
internal actual fun AuthConfig.platformAuthDefaults() {
    defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
}

/** MainActivity 에서 호출: porumi://login-callback 딥링크로 돌아온 로그인 결과를 세션으로 바꾼다 */
fun handleAuthDeeplink(intent: Intent) {
    val supabase = KoinPlatform.getKoinOrNull()?.getOrNull<SupabaseClient>() ?: return
    supabase.handleDeeplinks(intent)
}
