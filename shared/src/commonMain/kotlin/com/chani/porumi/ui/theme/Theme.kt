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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chani.porumi.resources.Res
import com.chani.porumi.resources.pretendard_bold
import com.chani.porumi.resources.pretendard_medium
import com.chani.porumi.resources.pretendard_regular
import com.chani.porumi.resources.pretendard_semibold
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.Font

/**
 * Porumi "말랑 파스텔" 테마.
 * - 색: 크림 배경 + 흰 카드 + 복숭아색 포인트, 글자는 코코아 브라운 (다크 모드는 같은 톤의 코코아 나이트)
 * - 글자: Pretendard (Regular / Medium / SemiBold / Bold)
 * - 모양: 큰 둥근 모서리(카드 24), 캡슐 버튼, 복숭아빛 부드러운 그림자
 * - 터치 피드백: 물결(ripple) 대신 눌린 동안 살짝 어두워지는 하이라이트
 */

/** 아이콘 동그라미 등에 쓰는 파스텔 친화 색 (배경은 이 색의 옅은 버전, 아이콘은 이 색) */
object Tints {
    val Red = Color(0xFFF0646C)
    val Orange = Color(0xFFF59A45)
    val Yellow = Color(0xFFE9B53C)
    val Green = Color(0xFF4CB98A)
    val Teal = Color(0xFF3BB3B8)
    val Blue = Color(0xFF5B8DEF)
    val Indigo = Color(0xFF7C74E8)
    val Purple = Color(0xFFB07CE0)
    val Pink = Color(0xFFF07AA5)
    val Gray = Color(0xFFA28F84)
}

private val Peach = Color(0xFFF2754C)
private val PeachDark = Color(0xFFFF9470)

private val LightColors = lightColorScheme(
    primary = Peach,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE7D6),
    onPrimaryContainer = Color(0xFF8A3B1E),
    inversePrimary = PeachDark,
    secondary = Color(0xFFBFA697),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEDE1),
    onSecondaryContainer = Color(0xFF4A3428),
    tertiary = Tints.Blue,
    onTertiary = Color.White,
    background = Color(0xFFFFF6EF),
    onBackground = Color(0xFF4A3428),
    surface = Color(0xFFFFF6EF),
    onSurface = Color(0xFF4A3428),
    surfaceVariant = Color(0xFFFFEDE1),
    onSurfaceVariant = Color(0xFF8E7363),
    surfaceTint = Color.Transparent,
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFF6E4D8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFFFEDE1),
    inverseSurface = Color(0xFF4A3428),
    inverseOnSurface = Color(0xFFFFF6EF),
    outline = Color(0xFFF0DDD0),
    outlineVariant = Color(0xFFF5E6DB),
    error = Color(0xFFE5484D),
    onError = Color.White,
    errorContainer = Color(0xFFFFE3E3),
    onErrorContainer = Color(0xFF8A1F22),
    scrim = Color(0xFF2A1A12),
)

private val DarkColors = darkColorScheme(
    primary = PeachDark,
    onPrimary = Color(0xFF2A140B),
    primaryContainer = Color(0xFF4A2D21),
    onPrimaryContainer = Color(0xFFFFD9C7),
    inversePrimary = Peach,
    secondary = Color(0xFF8C776B),
    onSecondary = Color(0xFF1C1512),
    secondaryContainer = Color(0xFF33271F),
    onSecondaryContainer = Color(0xFFF7E9E0),
    tertiary = Color(0xFF8EA8F0),
    onTertiary = Color(0xFF1C1512),
    background = Color(0xFF1C1512),
    onBackground = Color(0xFFF7E9E0),
    surface = Color(0xFF1C1512),
    onSurface = Color(0xFFF7E9E0),
    surfaceVariant = Color(0xFF33271F),
    onSurfaceVariant = Color(0xFFBBA293),
    surfaceTint = Color.Transparent,
    surfaceBright = Color(0xFF33271F),
    surfaceDim = Color(0xFF1C1512),
    surfaceContainerLowest = Color(0xFF2A201B),
    surfaceContainerLow = Color(0xFF2A201B),
    surfaceContainer = Color(0xFF2A201B),
    surfaceContainerHigh = Color(0xFF33271F),
    surfaceContainerHighest = Color(0xFF3D2F27),
    inverseSurface = Color(0xFFF7E9E0),
    inverseOnSurface = Color(0xFF1C1512),
    outline = Color(0xFF45362D),
    outlineVariant = Color(0xFF3A2D25),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2A0D0D),
    errorContainer = Color(0xFF4A1A1A),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color.Black,
)

/** Material 색 체계에 없는 Porumi 전용 색 */
@Immutable
data class PorumiColors(
    /** 셰브런, placeholder, 비활성 글자 */
    val tertiaryLabel: Color,
    /** 리스트 구분선 */
    val separator: Color,
    /** 스위치 꺼짐 트랙, 세그먼트 컨트롤 배경, 비활성 버튼 */
    val fill: Color,
    /** 세그먼트 컨트롤에서 선택된 칸 */
    val segmentSelected: Color,
    /** 떠 있는 탭바 배경 */
    val barBackground: Color,
    /** 알림창 배경 */
    val alertBackground: Color,
    /** 스위치 켜짐 */
    val switchOn: Color,
    /** 눌렀을 때 덮는 하이라이트 */
    val pressedOverlay: Color,
    /** 카드·탭바 그림자 (다크 모드는 투명) */
    val cardShadow: Color,
)

private val LightExtra = PorumiColors(
    tertiaryLabel = Color(0xFFCDB8AA),
    separator = Color(0xFFF3E3D8),
    fill = Color(0xFFF6E4D8),
    segmentSelected = Color.White,
    barBackground = Color.White,
    alertBackground = Color.White,
    switchOn = Peach,
    pressedOverlay = Color(0xFF4A3428).copy(alpha = 0.06f),
    cardShadow = Color(0xFFFF8A65).copy(alpha = 0.35f),
)

private val DarkExtra = PorumiColors(
    tertiaryLabel = Color(0xFF6E5A4F),
    separator = Color(0xFF3A2D25),
    fill = Color(0xFF3D2F27),
    segmentSelected = Color(0xFF5A463B),
    barBackground = Color(0xFF2A201B),
    alertBackground = Color(0xFF2A201B),
    switchOn = PeachDark,
    pressedOverlay = Color.White.copy(alpha = 0.08f),
    cardShadow = Color.Transparent,
)

val LocalPorumiColors = staticCompositionLocalOf { LightExtra }

/** 차트 전용 색 (diverging: 좋은 기분 = 복숭아, 안 좋은 기분 = 파스텔 블루, 0 = 중립) */
@Immutable
data class ChartColors(
    val positive: Color,
    val negative: Color,
    val neutral: Color,
    val bar: Color,
    val track: Color,
)

private val LightChart = ChartColors(
    positive = Color(0xFFFF9A76),
    negative = Color(0xFF8EAAF2),
    neutral = Color(0xFFE6D3C6),
    bar = Color(0xFFFFA683),
    track = Color(0xFFFFEDE1),
)

private val DarkChart = ChartColors(
    positive = Color(0xFFFF9A76),
    negative = Color(0xFF8EA8F0),
    neutral = Color(0xFF5A4A40),
    bar = Color(0xFFFF9A76),
    track = Color(0xFF3A2D25),
)

val LocalChartColors = staticCompositionLocalOf { LightChart }

// ── 글자: Pretendard ──────────────────────────────

@Composable
private fun pretendard() = FontFamily(
    Font(Res.font.pretendard_regular, FontWeight.Normal),
    Font(Res.font.pretendard_medium, FontWeight.Medium),
    Font(Res.font.pretendard_semibold, FontWeight.SemiBold),
    Font(Res.font.pretendard_bold, FontWeight.Bold),
)

@Composable
private fun porumiTypography(): Typography {
    val family = pretendard()
    fun style(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f) = TextStyle(
        fontFamily = family,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        fontWeight = weight,
        letterSpacing = tracking.sp,
    )
    return Typography(
        displayLarge = style(34, 42, FontWeight.Bold, -0.8f),
        displayMedium = style(30, 38, FontWeight.Bold, -0.6f),
        displaySmall = style(26, 34, FontWeight.Bold, -0.5f),
        headlineLarge = style(30, 38, FontWeight.Bold, -0.6f), // 탭 화면 큰 제목
        headlineMedium = style(26, 34, FontWeight.Bold, -0.5f),
        headlineSmall = style(24, 32, FontWeight.Bold, -0.5f),
        titleLarge = style(21, 28, FontWeight.Bold, -0.4f),
        titleMedium = style(17, 24, FontWeight.SemiBold, -0.3f),
        titleSmall = style(15, 22, FontWeight.SemiBold, -0.2f),
        bodyLarge = style(16, 24, tracking = -0.2f),
        bodyMedium = style(15, 22, tracking = -0.15f),
        bodySmall = style(13, 19, tracking = -0.1f),
        labelLarge = style(16, 22, FontWeight.SemiBold, -0.2f), // 버튼
        labelMedium = style(13, 18, FontWeight.Medium, -0.1f),
        labelSmall = style(12, 16, FontWeight.Medium),
    )
}

/** 말랑한 큰 모서리: 메뉴 16, 카드 24, 다이얼로그 28 */
private val PorumiShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

// ── 터치 피드백: 하이라이트 ──────────────────────────────

/** 눌린 동안 반투명 오버레이를 덮고, 손을 떼면 서서히 사라진다 */
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
    val extra = if (darkTheme) DarkExtra else LightExtra
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = porumiTypography(),
        shapes = PorumiShapes,
    ) {
        // MaterialTheme 안에서 다시 제공해야 기본 ripple 을 덮어쓴다
        CompositionLocalProvider(
            LocalPorumiColors provides extra,
            LocalChartColors provides if (darkTheme) DarkChart else LightChart,
            LocalIndication provides HighlightIndication(extra.pressedOverlay),
            content = content,
        )
    }
}
