package com.chani.porumi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFE8704F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE2D8),
    onPrimaryContainer = Color(0xFF5A1F0E),
    secondary = Color(0xFF8A6A58),
    secondaryContainer = Color(0xFFF5E6DC),
    onSecondaryContainer = Color(0xFF3A2A20),
    background = Color(0xFFFFFAF6),
    onBackground = Color(0xFF2A2420),
    surface = Color(0xFFFFFAF6),
    onSurface = Color(0xFF2A2420),
    surfaceVariant = Color(0xFFF3EAE3),
    onSurfaceVariant = Color(0xFF6B5E55),
    surfaceContainer = Color(0xFFFFFFFF),
    outline = Color(0xFFD9CEC5),
    outlineVariant = Color(0xFFEDE3DB),
    error = Color(0xFFC0392B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF08A6C),
    onPrimary = Color(0xFF4A1A0B),
    primaryContainer = Color(0xFF6B2E1C),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFFD9BFAE),
    secondaryContainer = Color(0xFF4A3B32),
    onSecondaryContainer = Color(0xFFF5E6DC),
    background = Color(0xFF1E1916),
    onBackground = Color(0xFFF2E9E3),
    surface = Color(0xFF1E1916),
    onSurface = Color(0xFFF2E9E3),
    surfaceVariant = Color(0xFF2F2824),
    onSurfaceVariant = Color(0xFFC9BBB1),
    surfaceContainer = Color(0xFF2A2420),
    outline = Color(0xFF5C514A),
    outlineVariant = Color(0xFF3D3530),
    error = Color(0xFFFF8A7A),
)

/** 차트 전용 색 (diverging: 좋은 기분 = 따뜻한 색, 안 좋은 기분 = 차가운 색, 0 = 중립 회색). 검증 완료 */
@Immutable
data class ChartColors(
    val positive: Color,
    val negative: Color,
    val neutral: Color,
    val bar: Color,
    val track: Color,
)

private val LightChart = ChartColors(
    positive = Color(0xFFE8704F),
    negative = Color(0xFF3D7DD8),
    neutral = Color(0xFFCFC6BE),
    bar = Color(0xFFE8704F),
    track = Color(0xFFF1E8E1),
)

private val DarkChart = ChartColors(
    positive = Color(0xFFDE6F50),
    negative = Color(0xFF5A8FDC),
    neutral = Color(0xFF5A514B),
    bar = Color(0xFFDE6F50),
    track = Color(0xFF3A322D),
)

val LocalChartColors = staticCompositionLocalOf { LightChart }

@Composable
fun PorumiTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalChartColors provides if (darkTheme) DarkChart else LightChart,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content,
        )
    }
}
