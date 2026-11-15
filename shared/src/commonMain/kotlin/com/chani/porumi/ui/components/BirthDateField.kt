package com.chani.porumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.chani.porumi.domain.ageLabel
import com.chani.porumi.ui.theme.LocalPorumiColors
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * 생일 입력 칸 (선택). 누르면 날짜 선택 창이 열리고, 고른 날짜와 나이를 보여준다.
 * 오른쪽 X 로 지울 수 있다. 오늘 이후 날짜는 고를 수 없다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthDateField(value: LocalDate?, onValueChange: (LocalDate?) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val extra = LocalPorumiColors.current
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    var picking by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(shape)
            .background(colors.surfaceContainer)
            .border(1.dp, colors.outline, shape)
            .clickable { picking = true }
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Cake, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
        if (value == null) {
            Text(
                "생일 (선택)",
                style = MaterialTheme.typography.bodyLarge,
                color = extra.tertiaryLabel,
                modifier = Modifier.weight(1f).padding(start = 10.dp),
            )
        } else {
            val age = ageLabel(value, today)
            Text(
                "${value.year}년 ${value.month.ordinal + 1}월 ${value.day}일" + (age?.let { " ($it)" } ?: ""),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(start = 10.dp),
            )
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).clickable { onValueChange(null) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "생일 지우기", tint = extra.tertiaryLabel, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (picking) {
        val todayMillis = today.toUtcMillis()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = value?.toUtcMillis(),
            yearRange = 1990..today.year,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
                override fun isSelectableYear(year: Int) = year <= today.year
            },
        )
        val pickerColors = DatePickerDefaults.colors(containerColor = extra.alertBackground)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                PlainButton(
                    onClick = {
                        state.selectedDateMillis?.let { onValueChange(it.toUtcLocalDate()) }
                        picking = false
                    },
                    enabled = state.selectedDateMillis != null,
                    modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                ) { Text("선택") }
            },
            dismissButton = {
                PlainButton(
                    onClick = { picking = false },
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                ) { Text("취소") }
            },
            shape = RoundedCornerShape(28.dp),
            colors = pickerColors,
        ) {
            DatePicker(
                state = state,
                colors = pickerColors,
                title = {
                    Text(
                        "생일 선택",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                    )
                },
            )
        }
    }
}

// DatePicker 는 UTC 자정 기준 millis 를 쓴다
private fun LocalDate.toUtcMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun Long.toUtcLocalDate(): LocalDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date
