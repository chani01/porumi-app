package com.chani.porumi.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Apple Human Interface Guidelines 를 따르는 테마.
 * - 색: iOS 시스템 색 (grouped background / secondary grouped background / label 계열 / separator)
 * - 글자: iOS Dynamic Type 기본 크기 (Large Title 34 · Title1 28 · Title2 22 · Headline 17 · Body 17 · Subheadline 15 · Footnote 13 · Caption 12)
 *   글꼴은 FontFamily.Default → iOS 에서는 SF Pro / Apple SD Gothic Neo 가 쓰인다.
 * - 터치 피드백: 물결(ripple) 대신 iOS 처럼 눌린 동안 살짝 어두워지는 하이라이트
 * 브랜드 코랄은 iOS 앱의 tint(강조) 색으로만 쓴다.
 */

/** iOS 시스템 색 (Light 기준). 설정 아이콘 타일 등에 사용 */
object SystemColors {
    val Red = Color(0xFFFF3B30)
    val Orange = Color(0xFFFF9500)
    val Yellow = Color(0xFFFFCC00)
    val Green = Color(0xFF34C759)
    val Teal = Color(0xFF30B0C7)
    val Blue = Color(0xFF007AFF)
    val Indigo = Color(0xFF5856D6)
    val Purple = Color(0xFFAF52DE)
    val Pink = Color(0xFFFF2D55)
    val Gray = Color(0xFF8E8E93)
}

private val BrandTint = Color(0xFFE8704F)
private val BrandTintDark = Color(0xFFF2825F)

private val LightColors = lightColorScheme(
    primary = BrandTint,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFDE9E2),
    onPrimaryContainer = Color(0xFF7A2E17),
    inversePrimary = BrandTintDark,
    secondary = SystemColors.Gray,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E9EB),
    onSecondaryContainer = Color.Black,
    tertiary = SystemColors.Blue,
    onTertiary = Color.White,
    // systemGroupedBackground / secondarySystemGroupedBackground
    background = Color(0xFFF2F2F7),
    onBackground = Color.Black,
    surface = Color(0xFFF2F2F7),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFEBEBF0),
    onSurfaceVariant = Color(0xFF6E6E73), // secondaryLabel (불투명 근사)
    surfaceTint = Color.Transparent,
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFE5E5EA),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF2F2F2),
    surfaceContainerHighest = Color(0xFFE5E5EA),
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color.White,
    outline = Color(0xFFC6C6C8), // opaqueSeparator
    outlineVariant = Color(0xFFD8D8DC),
    error = SystemColors.Red,
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E3),
    onErrorContainer = Color(0xFF8A1A12),
    scrim = Color.Black,
)

private val DarkColors = darkColorScheme(
    primary = BrandTintDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3F2219),
    onPrimaryContainer = Color(0xFFFFD6C8),
    inversePrimary = BrandTint,
    secondary = Color(0xFF8E8E93),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF39393D),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF0A84FF),
    onTertiary = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF98989F),
    surfaceTint = Color.Transparent,
    surfaceBright = Color(0xFF2C2C2E),
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color(0xFF1C1C1E),
    surfaceContainerLow = Color(0xFF1C1C1E),
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF3A3A3C),
    inverseSurface = Color(0xFFF2F2F7),
    inverseOnSurface = Color.Black,
    outline = Color(0xFF38383A),
    outlineVariant = Color(0xFF38383A),
    error = Color(0xFFFF453A),
    onError = Color.White,
    errorContainer = Color(0xFF4A1512),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color.Black,
)

/** Material 색 체계에 없는 iOS 전용 색 */
@Immutable
data class IosColors(
    /** tertiaryLabel: 셰브런, placeholder, 비활성 글자 */
    val tertiaryLabel: Color,
    /** 리스트 구분선 */
    val separator: Color,
    /** 스위치 꺼짐 트랙, 세그먼트 컨트롤 배경 */
    val fill: Color,
    /** 세그먼트 컨트롤에서 선택된 칸 */
    val segmentSelected: Color,
    /** 탭바 배경 */
    val barBackground: Color,
    /** 알림창 배경 */
    val alertBackground: Color,
    /** 스위치 켜짐 (systemGreen) */
    val switchOn: Color,
    /** 눌렀을 때 덮는 하이라이트 */
    val pressedOverlay: Color,
)

private val LightIos = IosColors(
    tertiaryLabel = Color(0xFFC4C4C7),
    separator = Color(0xFFC6C6C8),
    fill = Color(0xFFE9E9EB),
    segmentSelected = Color.White,
    barBackground = Color(0xFFF9F9F9),
    alertBackground = Color(0xFFF2F2F2),
    switchOn = SystemColors.Green,
    pressedOverlay = Color.Black.copy(alpha = 0.08f),
)

private val DarkIos = IosColors(
    tertiaryLabel = Color(0xFF5C5C61),
    separator = Color(0xFF38383A),
    fill = Color(0xFF39393D),
    segmentSelected = Color(0xFF636366),
    barBackground = Color(0xFF161618),
    alertBackground = Color(0xFF2C2C2E),
    switchOn = Color(0xFF30D158),
    pressedOverlay = Color.White.copy(alpha = 0.10f),
)

val LocalIosColors = staticCompositionLocalOf { LightIos }

/** 차트 전용 색 (diverging: 좋은 기분 = 브랜드 코랄, 안 좋은 기분 = systemBlue, 0 = systemGray) */
@Immutable
data class ChartColors(
    val positive: Color,
    val negative: Color,
    val neutral: Color,
    val bar: Color,
    val track: Color,
)

private val LightChart = ChartColors(
    positive = BrandTint,
    negative = SystemColors.Blue,
    neutral = Color(0xFFC7C7CC),
    bar = BrandTint,
    track = Color(0xFFE5E5EA),
)

private val DarkChart = ChartColors(
    positive = BrandTintDark,
    negative = Color(0xFF0A84FF),
    neutral = Color(0xFF48484A),
    bar = BrandTintDark,
    track = Color(0xFF2C2C2E),
)

val LocalChartColors = staticCompositionLocalOf { LightChart }

// ── 글자: iOS Dynamic Type (Large 기본값) ──────────────────────────────

private fun ios(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f) = TextStyle(
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
)

private val IosTypography = Typography(
    displayLarge = ios(34, 41, FontWeight.Bold),
    displayMedium = ios(34, 41, FontWeight.Bold),
    displaySmall = ios(28, 34, FontWeight.Bold),
    headlineLarge = ios(34, 41, FontWeight.Bold), // Large Title
    headlineMedium = ios(28, 34, FontWeight.Bold), // Title 1
    headlineSmall = ios(28, 34, FontWeight.Bold), // Title 1
    titleLarge = ios(22, 28, FontWeight.Bold), // Title 2
    titleMedium = ios(17, 22, FontWeight.SemiBold, -0.4f), // Headline
    titleSmall = ios(15, 20, FontWeight.SemiBold, -0.2f), // Subheadline (강조)
    bodyLarge = ios(17, 22, tracking = -0.4f), // Body
    bodyMedium = ios(15, 20, tracking = -0.2f), // Subheadline
    bodySmall = ios(13, 18, tracking = -0.1f), // Footnote
    labelLarge = ios(17, 22, FontWeight.SemiBold, -0.4f), // 버튼
    labelMedium = ios(13, 18, FontWeight.Medium, -0.1f), // Footnote (강조)
    labelSmall = ios(12, 16), // Caption 1
)

/** iOS 는 모서리가 작고 일정하다: 메뉴·텍스트필드 10, 카드 16, 알림창 14 */
private val IosShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(14.dp),
)

// ── 터치 피드백: iOS 하이라이트 ──────────────────────────────

/** 눌린 동안 반투명 오버레이를 덮고, 손을 떼면 서서히 사라진다 (UITableViewCell 하이라이트와 같은 느낌) */
private data class HighlightIndication(val color: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = HighlightNode(interactionSource, color)
}

private class HighlightNode(
    private val interactionSource: InteractionSource,
    private val color: Color,
) : Modifier.Node(), DrawModifierNode {
    private val alpha = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            var pressed = 0
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        pressed++
                        launch { alpha.snapTo(1f) }
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        pressed = (pressed - 1).coerceAtLeast(0)
                        if (pressed == 0) launch { alpha.animateTo(0f, tween(durationMillis = 300)) }
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val a = alpha.value
        if (a > 0f) drawRect(color.copy(alpha = color.alpha * a))
    }
}

@Composable
fun PorumiTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val ios = if (darkTheme) DarkIos else LightIos
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = IosTypography,
        shapes = IosShapes,
    ) {
        // MaterialTheme 안에서 다시 제공해야 기본 ripple 을 덮어쓴다
        CompositionLocalProvider(
            LocalIosColors provides ios,
            LocalChartColors provides if (darkTheme) DarkChart else LightChart,
            LocalIndication provides HighlightIndication(ios.pressedOverlay),
            content = content,
        )
    }
}
