package com.chani.porumi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.chani.porumi.data.handleAuthDeeplink

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 시스템 스플래시 → 곧바로 Compose 스플래시(SplashScreen.kt)로 이어진다
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // SNS 로그인 후 앱이 딥링크로 새로 열린 경우
        handleAuthDeeplink(intent)

        setContent {
            App()
        }
    }

    // singleTask: 이미 떠 있는 앱으로 딥링크가 돌아온 경우
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthDeeplink(intent)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}