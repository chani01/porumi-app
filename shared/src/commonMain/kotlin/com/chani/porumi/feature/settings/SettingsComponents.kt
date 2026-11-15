package com.chani.porumi.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chani.porumi.ui.components.Hairline
import com.chani.porumi.ui.components.PorumiTopBar
import com.chani.porumi.ui.theme.LocalPorumiColors
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.CircleShape

/** 설정 하위 화면 공통 틀: 상단 바(뒤로 + 제목) + 스크롤 영역 */
@Composable
fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PorumiTopBar(title = title, onBack = onBack)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            content = content,
        )
    }
}

private val SectionShape = RoundedCornerShape(24.dp)
private val RowPadding = 16.dp
private val IconTile = 34.dp
private val IconGap = 14.dp

/** 아이콘이 있는 행의 구분선은 글자 시작점부터 그린다 */
private val IconRowInset = RowPadding + IconTile + IconGap

private data class SeparatorInset(val start: Dp)

/** 묶음 목록: 둥근 흰 카드 + 행 사이 들여쓴 구분선, 위에 작은 머리글 */
@Composable
fun SettingsSection(title: String?, footer: String? = null, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = RowPadding, bottom = 8.dp),
            )
        }
        GroupedColumn(
            separatorColor = LocalPorumiColors.current.separator,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, SectionShape, ambientColor = LocalPorumiColors.current.cardShadow, spotColor = LocalPorumiColors.current.cardShadow)
                .clip(SectionShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            content = content,
        )
        if (footer != null) {
            Text(
                footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = RowPadding, end = RowPadding, top = 6.dp),
            )
        }
    }
}

/** 자식을 세로로 쌓고 자식 사이에만 구분선을 그린다. 구분선 시작 위치는 각 행의 [SeparatorInset] */
@Composable
private fun GroupedColumn(separatorColor: Color, modifier: Modifier, content: @Composable () -> Unit) {
    // (y, startX) — 배치 단계에서 계산해 그리기 단계에서 사용
    var separators by remember { mutableStateOf(emptyList<Pair<Float, Float>>()) }
    Layout(
        content = content,
        modifier = modifier.drawWithContent {
            drawContent()
            val stroke = Hairline.toPx().coerceAtLeast(1f)
            separators.forEach { (y, x) ->
                drawLine(separatorColor, Offset(x, y), Offset(size.width, y), strokeWidth = stroke)
            }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minHeight = 0)
        val placeables = measurables.map { it.measure(loose) }
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else placeables.maxOfOrNull { it.width } ?: 0
        val lines = ArrayList<Pair<Float, Float>>()
        var y = 0
        placeables.forEachIndexed { i, p ->
            if (i > 0 && p.height > 0) {
                val inset = (measurables[i].layoutId as? SeparatorInset)?.start ?: IconRowInset
                lines += y.toFloat() to inset.toPx()
            }
            y += p.height
        }
        layout(width, y) {
            if (lines != separators) separators = lines
            var top = 0
            placeables.forEach {
                it.place(0, top)
                top += it.height
            }
        }
    }
}

/**
 * 설정 행: 파스텔 동그라미 아이콘 + 제목(+부제목) + 오른쪽 값/셰브런/스위치.
 * [iconColor] 는 아이콘 색 (배경은 이 색의 옅은 버전, Tints 사용 권장).
 */
@Composable
fun SettingsRow(
    title: String,
    icon: ImageVector?,
    onClick: (() -> Unit)?,
    subtitle: String? = null,
    trailingText: String? = null,
    titleColor: Color = Color.Unspecified,
    showChevron: Boolean = onClick != null,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    trailing: (@Composable () -> Unit)? = null,
) {
    val ios = LocalPorumiColors.current
    Row(
        modifier = Modifier
            .layoutId(SeparatorInset(if (icon != null) IconRowInset else RowPadding))
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = RowPadding, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                Modifier.size(IconTile).clip(CircleShape).background(iconColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(IconGap))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when {
            trailing != null -> Box(Modifier.padding(start = 12.dp)) { trailing() }
            trailingText != null || showChevron -> Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp),
            ) {
                if (trailingText != null) {
                    Text(trailingText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (showChevron) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = ios.tertiaryLabel,
                        modifier = Modifier.padding(start = 6.dp).size(14.dp),
                    )
                }
            }
        }
    }
}
