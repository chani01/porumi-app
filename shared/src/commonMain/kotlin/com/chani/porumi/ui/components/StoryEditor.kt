package com.chani.porumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.chani.porumi.domain.NOTE_MAX_LENGTH
import com.chani.porumi.ui.theme.LocalPorumiColors

/** 오늘의 이야기에 한 번에 붙여 넣는 빠른 문구 */
val StoryPrompts = listOf(
    "산책했어요",
    "간식 먹었어요",
    "신나게 놀았어요",
    "푹 잤어요",
    "목욕했어요",
    "병원 다녀왔어요",
    "새 장난감이 생겼어요",
)

/** 여러 줄 이야기 입력 칸 + 글자 수 표시 */
@Composable
fun StoryEditor(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.background,
) {
    val colors = MaterialTheme.colorScheme
    val extra = LocalPorumiColors.current
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val shape = RoundedCornerShape(18.dp)
    Column(modifier) {
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.take(NOTE_MAX_LENGTH)) },
            enabled = enabled,
            interactionSource = source,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .clip(shape)
                        .background(containerColor)
                        .border(if (focused) 1.5.dp else 1.dp, if (focused) colors.primary else colors.outline, shape)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    if (value.isEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = extra.tertiaryLabel)
                    }
                    inner()
                }
            },
        )
        Text(
            "${value.length} / $NOTE_MAX_LENGTH",
            style = MaterialTheme.typography.labelSmall,
            color = if (value.length >= NOTE_MAX_LENGTH) colors.error else extra.tertiaryLabel,
            modifier = Modifier.align(androidx.compose.ui.Alignment.End).padding(top = 6.dp, end = 4.dp),
        )
    }
}

/** 빠른 문구 칩 (가로 스크롤) */
@Composable
fun StoryPromptChips(onPick: (String) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StoryPrompts.forEach { phrase ->
            Text(
                phrase,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onPrimaryContainer,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.primaryContainer)
                    .clickable(enabled = enabled) { onPick(phrase) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
