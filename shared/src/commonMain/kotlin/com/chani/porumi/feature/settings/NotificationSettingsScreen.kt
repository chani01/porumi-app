package com.chani.porumi.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.chani.porumi.ui.components.PorumiSwitch
import com.chani.porumi.ui.theme.Tints

/** 마이 > 알림 설정 */
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit, viewModel: NotificationSettingsViewModel = koinViewModel()) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    var hourMenu by remember { mutableStateOf(false) }

    SettingsPage(title = "알림 설정", onBack = onBack) {
        SettingsSection("기록") {
            SettingsRow(
                "기록 리마인더",
                Icons.Outlined.PhotoCamera,
                iconColor = Tints.Orange,
                onClick = { viewModel.update { it.copy(dailyReminder = !it.dailyReminder) } },
                subtitle = "매일 정해진 시간에 오늘의 표정을 남기도록 알려드려요",
                trailing = {
                    PorumiSwitch(checked = prefs.dailyReminder, onCheckedChange = { on -> viewModel.update { it.copy(dailyReminder = on) } })
                },
            )
            if (prefs.dailyReminder) {
                Box {
                    SettingsRow(
                        "알림 시간",
                        Icons.Outlined.Schedule,
                        iconColor = Tints.Blue,
                        onClick = { hourMenu = true },
                        trailingText = hourLabel(prefs.reminderHour),
                    )
                    DropdownMenu(expanded = hourMenu, onDismissRequest = { hourMenu = false }) {
                        (6..23).forEach { hour ->
                            DropdownMenuItem(
                                text = { Text(hourLabel(hour)) },
                                onClick = {
                                    viewModel.update { it.copy(reminderHour = hour) }
                                    hourMenu = false
                                },
                            )
                        }
                    }
                }
            }
        }
        SettingsSection("리포트") {
            SettingsRow(
                "주간 리포트 알림",
                Icons.Outlined.Insights,
                iconColor = Tints.Indigo,
                onClick = { viewModel.update { it.copy(weeklyReport = !it.weeklyReport) } },
                subtitle = "최근 7일 기분 리포트가 준비되면 알려드려요",
                trailing = {
                    PorumiSwitch(checked = prefs.weeklyReport, onCheckedChange = { on -> viewModel.update { it.copy(weeklyReport = on) } })
                },
            )
        }
        SettingsSection("혜택", footer = "기기 설정에서 Porumi 알림이 꺼져 있으면 알림을 받을 수 없어요.") {
            SettingsRow(
                "이벤트·혜택 알림",
                Icons.Outlined.Campaign,
                iconColor = Tints.Pink,
                onClick = { viewModel.update { it.copy(marketing = !it.marketing) } },
                subtitle = "새 기능과 이벤트 소식을 받아요 (마케팅 정보 수신 동의)",
                trailing = {
                    PorumiSwitch(checked = prefs.marketing, onCheckedChange = { on -> viewModel.update { it.copy(marketing = on) } })
                },
            )
        }

    }
}

private fun hourLabel(hour: Int): String = when {
    hour == 0 -> "오전 12시"
    hour < 12 -> "오전 ${hour}시"
    hour == 12 -> "오후 12시"
    else -> "오후 ${hour - 12}시"
}
