package com.chani.porumi.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NotificationPrefs(
    /** 매일 기록 리마인더 */
    val dailyReminder: Boolean = true,
    /** 리마인더 시각 (0~23시) */
    val reminderHour: Int = 20,
    /** 최근 7일 리포트 도착 알림 */
    val weeklyReport: Boolean = true,
    /** 이벤트·혜택 알림 (마케팅 수신 동의) */
    val marketing: Boolean = false,
)

/**
 * 알림 설정을 기기에 저장한다 (Android SharedPreferences / iOS NSUserDefaults).
 * 실제 알림 발송(로컬 알림 예약 또는 푸시)은 아직 연결되지 않았다.
 */
class NotificationSettingsRepository(private val settings: Settings = Settings()) {

    private val _prefs = MutableStateFlow(load())
    val prefs: StateFlow<NotificationPrefs> = _prefs.asStateFlow()

    fun update(transform: (NotificationPrefs) -> NotificationPrefs) {
        _prefs.update(transform)
        val p = _prefs.value
        settings[KEY_DAILY] = p.dailyReminder
        settings[KEY_HOUR] = p.reminderHour
        settings[KEY_WEEKLY] = p.weeklyReport
        settings[KEY_MARKETING] = p.marketing
    }

    private fun load() = NotificationPrefs(
        dailyReminder = settings.getBoolean(KEY_DAILY, true),
        reminderHour = settings.getInt(KEY_HOUR, 20),
        weeklyReport = settings.getBoolean(KEY_WEEKLY, true),
        marketing = settings.getBoolean(KEY_MARKETING, false),
    )

    private companion object {
        const val KEY_DAILY = "noti_daily_reminder"
        const val KEY_HOUR = "noti_reminder_hour"
        const val KEY_WEEKLY = "noti_weekly_report"
        const val KEY_MARKETING = "noti_marketing"
    }
}
