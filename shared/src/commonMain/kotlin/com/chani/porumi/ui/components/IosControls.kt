package com.chani.porumi.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chani.porumi.ui.theme.LocalIosColors
import androidx.compose.animation.core.animateFloat

/** iOS 구분선 두께 (1px 에 가깝게) */
val Hairline = 0.5.dp

private const val PRESSED_ALPHA = 0.4f

// ── 버튼 ──────────────────────────────

/** 채워진 강조 버튼 (UIButton.Configuration.filled, large) */
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ios = LocalIosColors.current
    FilledButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        container = if (enabled) colors.primary else ios.fill,
        content = if (enabled) colors.onPrimary else ios.tertiaryLabel,
        pressedAlpha = 0.75f,
        body = content,
    )
}

/** 옅게 물든 보조 버튼 (UIButton.Configuration.tinted) */
@Composable
fun TintedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ios = LocalIosColors.current
    FilledButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        container = if (enabled) colors.primary.copy(alpha = 0.14f) else ios.fill,
        content = if (enabled) colors.primary else ios.tertiaryLabel,
        pressedAlpha = 0.6f,
        body = content,
    )
}

@Composable
private fun FilledButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    container: Color,
    content: Color,
    pressedAlpha: Float,
    body: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    CompositionLocalProvider(LocalContentColor provides content) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            Row(
                modifier = modifier
                    .heightIn(min = 50.dp)
                    .alpha(if (pressed) pressedAlpha else 1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(container)
                    .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                content = body,
            )
        }
    }
}

/** 배경 없는 글자 버튼 (UIButton.Configuration.plain). 누르면 흐려진다 */
@Composable
fun PlainButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    CompositionLocalProvider(LocalContentColor provides if (enabled) color else LocalIosColors.current.tertiaryLabel) {
        ProvideTextStyle(textStyle) {
            Row(
                modifier = modifier
                    .heightIn(min = 44.dp)
                    .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
                    .alpha(if (pressed) PRESSED_ALPHA else 1f)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/** 아이콘만 있는 글자 버튼 (내비게이션 바 버튼 등) */
@Composable
fun PlainIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    iconSize: Dp = 22.dp,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = modifier
            .size(44.dp)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .alpha(if (pressed) PRESSED_ALPHA else 1f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

// ── 내비게이션 바 / 탭바 ──────────────────────────────

/** iOS 내비게이션 바: 가운데 제목(17pt semibold) + 왼쪽 ‹ 뒤로 + 오른쪽 액션 */
@Composable
fun IosTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(44.dp),
    ) {
        if (onBack != null) {
            PlainIconButton(
                Icons.Rounded.ArrowBackIosNew,
                contentDescription = "뒤로",
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 96.dp),
        )
        Row(Modifier.align(Alignment.CenterEnd).padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

data class TabItem(val label: String, val icon: ImageVector)

/** iOS 탭바: 위쪽 구분선 + 아이콘·작은 라벨, 선택된 탭만 tint 색 */
@Composable
fun IosTabBar(items: List<TabItem>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val ios = LocalIosColors.current
    Column(modifier.fillMaxWidth().background(ios.barBackground)) {
        Box(Modifier.fillMaxWidth().height(Hairline).background(ios.separator))
        Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).height(50.dp)) {
            items.forEachIndexed { i, item ->
                val color by animateColorAsState(
                    if (i == selectedIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    tween(150),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = null, indication = null, role = Role.Tab) { onSelect(i) }
                        .padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(item.icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
                    Text(item.label, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium, color = color)
                }
            }
        }
    }
}

/** 탭 화면 상단의 Large Title (34pt bold) */
@Composable
fun LargeTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.headlineLarge, modifier = modifier)
}

// ── 입력 컨트롤 ──────────────────────────────

/** iOS 스위치 (UISwitch, 51×31) */
@Composable
fun IosSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val ios = LocalIosColors.current
    val progress by animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = 0.75f, stiffness = 700f))
    Box(
        modifier = modifier
            .size(width = 51.dp, height = 31.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(CircleShape)
            .background(lerp(ios.fill, ios.switchOn, progress))
            .then(
                if (onCheckedChange != null) {
                    Modifier.toggleable(
                        value = checked,
                        onValueChange = onCheckedChange,
                        enabled = enabled,
                        role = Role.Switch,
                        interactionSource = null,
                        indication = null,
                    )
                } else Modifier,
            )
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .offset(x = 20.dp * progress)
                .size(27.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

/** 체크박스 대신 쓰는 원형 체크 (미리알림 앱 스타일) */
@Composable
fun CheckCircle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val tint = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(44.dp)
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Checkbox, interactionSource = null, indication = null),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .then(
                    if (checked) Modifier.background(tint)
                    else Modifier.border(1.5.dp, LocalIosColors.current.tertiaryLabel, CircleShape),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        }
    }
}

/** 세그먼트 컨트롤 (UISegmentedControl) */
@Composable
fun SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val ios = LocalIosColors.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(ios.fill)
            .padding(2.dp),
    ) {
        val segment = maxWidth / options.size
        val offset by animateDpAsState(segment * selectedIndex, spring(dampingRatio = 0.85f, stiffness = 600f))
        Box(
            Modifier
                .offset(x = offset)
                .width(segment)
                .fillMaxHeight()
                .shadow(1.dp, RoundedCornerShape(7.dp))
                .background(ios.segmentSelected, RoundedCornerShape(7.dp)),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = null, indication = null, role = Role.Tab) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (i == selectedIndex) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** 둥근 캡슐 선택 칩 (가로 스크롤 목록용) */
@Composable
fun CapsuleChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Text(
        label,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (selected) colors.onPrimary else colors.onSurface,
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) colors.primary else colors.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}

/** 테두리 없는 둥근 텍스트 필드 (iOS 는 라벨 대신 placeholder 를 쓴다) */
@Composable
fun IosTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    isError: Boolean = errorText != null,
    secure: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Column(modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            visualTransformation = if (secure) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .clip(shape)
                        .background(containerColor)
                        .then(if (isError) Modifier.border(1.dp, colors.error, shape) else Modifier)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = LocalIosColors.current.tertiaryLabel, maxLines = 1)
                    }
                    inner()
                }
            },
        )
        if (errorText != null) {
            Text(
                errorText,
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp),
            )
        }
    }
}

// ── 진행 표시 ──────────────────────────────

/** iOS 활동 표시기 (UIActivityIndicatorView): 8개 살이 돌아가며 흐려진다 */
@Composable
fun ActivityIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 22.dp,
    animating: Boolean = true,
) {
    val head = if (animating) {
        val step by rememberInfiniteTransition().animateFloat(
            initialValue = 0f,
            targetValue = SPOKES.toFloat(),
            animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
        )
        step.toInt() % SPOKES
    } else 0
    Canvas(modifier.size(size)) {
        val stroke = this.size.minDimension * 0.095f
        val outer = this.size.minDimension / 2f
        for (i in 0 until SPOKES) {
            val distance = (head - i + SPOKES) % SPOKES
            val alpha = if (animating) 1f - distance * 0.1f else 0.55f
            rotate(degrees = i * 360f / SPOKES) {
                drawLine(
                    color = color.copy(alpha = color.alpha * alpha),
                    start = Offset(center.x, center.y - outer * 0.48f),
                    end = Offset(center.x, center.y - outer + stroke / 2),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private const val SPOKES = 8

// ── 알림창 ──────────────────────────────

enum class AlertActionStyle { Default, Cancel, Destructive }

data class AlertAction(
    val label: String,
    val style: AlertActionStyle = AlertActionStyle.Default,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * iOS 알림창 (UIAlertController .alert): 가운데 정렬 제목/메시지, 구분선으로 나뉜 버튼.
 * 버튼이 2개면 가로(취소는 왼쪽에 두기), 그 외에는 세로로 쌓는다. 바깥을 눌러도 닫히지 않는다.
 */
@Composable
fun IosAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    actions: List<AlertAction>,
    message: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val ios = LocalIosColors.current
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Column(
            Modifier
                .width(286.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ios.alertBackground),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                if (message != null) {
                    Text(message, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
                if (content != null) {
                    Spacer(Modifier.height(10.dp))
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
                }
            }
            Box(Modifier.fillMaxWidth().height(Hairline).background(ios.separator))
            if (actions.size == 2) {
                Row(Modifier.fillMaxWidth().height(44.dp)) {
                    AlertButton(actions[0], Modifier.weight(1f).fillMaxHeight())
                    Box(Modifier.width(Hairline).fillMaxHeight().background(ios.separator))
                    AlertButton(actions[1], Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                actions.forEachIndexed { i, action ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(Hairline).background(ios.separator))
                    AlertButton(action, Modifier.fillMaxWidth().height(44.dp))
                }
            }
        }
    }
}

@Composable
private fun AlertButton(action: AlertAction, modifier: Modifier) {
    val color = when {
        !action.enabled -> LocalIosColors.current.tertiaryLabel
        action.style == AlertActionStyle.Destructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Box(
        modifier.clickable(enabled = action.enabled, role = Role.Button, onClick = action.onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            action.label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (action.style == AlertActionStyle.Default) FontWeight.SemiBold else FontWeight.Normal,
            color = color,
        )
    }
}
