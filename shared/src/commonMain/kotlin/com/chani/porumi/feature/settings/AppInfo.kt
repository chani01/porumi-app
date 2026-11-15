package com.chani.porumi.feature.settings

/**
 * 앱 정보 / 고객센터 연락처.
 * TODO(출시 전): 실제 고객센터 이메일로 교체. (이용약관·개인정보 처리방침은 feature/legal 에서 앱 내 표시)
 */
object AppInfo {
    const val VERSION = "1.0.0"
    const val SUPPORT_EMAIL = "support@porumi.app"
}

data class OpenSourceLicense(val name: String, val author: String, val license: String, val url: String)

/** 앱에서 사용하는 오픈소스 라이브러리 (gradle/libs.versions.toml 기준) */
val openSourceLicenses = listOf(
    OpenSourceLicense("Kotlin / kotlinx.coroutines / serialization / datetime", "JetBrains", "Apache License 2.0", "https://github.com/JetBrains/kotlin"),
    OpenSourceLicense("Compose Multiplatform", "JetBrains", "Apache License 2.0", "https://github.com/JetBrains/compose-multiplatform"),
    OpenSourceLicense("Material Icons", "Google", "Apache License 2.0", "https://github.com/google/material-design-icons"),
    OpenSourceLicense("AndroidX (Activity, Lifecycle, Navigation, SplashScreen)", "Google / AOSP", "Apache License 2.0", "https://github.com/androidx/androidx"),
    OpenSourceLicense("supabase-kt", "Jan Tennert", "MIT License", "https://github.com/supabase-community/supabase-kt"),
    OpenSourceLicense("Ktor", "JetBrains", "Apache License 2.0", "https://github.com/ktorio/ktor"),
    OpenSourceLicense("Koin", "Kotzilla / Arnaud Giuliani", "Apache License 2.0", "https://github.com/InsertKoinIO/koin"),
    OpenSourceLicense("Coil", "Coil Contributors", "Apache License 2.0", "https://github.com/coil-kt/coil"),
    OpenSourceLicense("FileKit", "Vincent Guillebaud", "MIT License", "https://github.com/vinceglb/FileKit"),
    OpenSourceLicense("Pretendard (글꼴)", "Kil Hyung-jin", "SIL Open Font License 1.1", "https://github.com/orioncactus/pretendard"),
    OpenSourceLicense("Multiplatform Settings", "Russell Wolf", "Apache License 2.0", "https://github.com/russhwolf/multiplatform-settings"),
)
