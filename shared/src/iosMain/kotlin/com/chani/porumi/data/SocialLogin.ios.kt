package com.chani.porumi.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.AuthConfig
import io.github.jan.supabase.auth.handleDeeplinks
import org.koin.mp.KoinPlatform
import platform.Foundation.NSURL

internal actual fun AuthConfig.platformAuthDefaults() = Unit

/** ContentView.swift 의 onOpenURL 에서 호출: 로그인 후 돌아온 딥링크를 세션으로 바꾼다 */
fun handleAuthDeeplink(url: NSURL) {
    val supabase = KoinPlatform.getKoinOrNull()?.getOrNull<SupabaseClient>() ?: return
    supabase.handleDeeplinks(url)
}
