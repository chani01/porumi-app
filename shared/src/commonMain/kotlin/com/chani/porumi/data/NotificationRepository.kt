package com.chani.porumi.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

/** 알림 종류. DB 의 notifications.kind 체크 제약과 같은 값 */
enum class NotificationKind(val emoji: String) {
    REMINDER("📸"),
    WEEKLY_REPORT("📊"),
    ANALYSIS("✨"),
    NOTICE("📢"),
    EVENT("🎁"),
}

data class AppNotification(
    val id: String,
    val kind: NotificationKind,
    val title: String,
    val body: String?,
    val createdAt: Instant,
    val read: Boolean,
)

@Serializable
internal data class NotificationDto(
    val id: String,
    val kind: String,
    val title: String,
    val body: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("read_at") val readAt: String? = null,
)

@Serializable
private data class ReadAtDto(@SerialName("read_at") val readAt: String)

private fun NotificationDto.toDomain(): AppNotification? {
    val kind = NotificationKind.entries.firstOrNull { it.name == kind } ?: return null
    return AppNotification(id, kind, title, body, Instant.parse(createdAt), read = readAt != null)
}

/**
 * 알림 기록 (최신 100개). 알림은 서버가 만들고, 앱은 조회·읽음 처리·삭제만 한다.
 * 로그아웃 시 [clear] 로 비운다 (MainViewModel).
 */
class NotificationRepository internal constructor(private val supabase: SupabaseClient) {

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    suspend fun refresh() {
        _notifications.value = supabase.from("notifications")
            .select {
                order("created_at", Order.DESCENDING)
                limit(100)
            }
            .decodeList<NotificationDto>()
            .mapNotNull { it.toDomain() }
        _loaded.value = true
    }

    /** 안 읽은 알림을 모두 읽음으로 */
    suspend fun markAllRead() {
        if (_notifications.value.none { !it.read }) return
        supabase.from("notifications").update(ReadAtDto(Clock.System.now().toString())) {
            filter { exact("read_at", null) }
        }
        _notifications.update { list -> list.map { it.copy(read = true) } }
    }

    suspend fun delete(id: String) {
        supabase.from("notifications").delete { filter { eq("id", id) } }
        _notifications.update { list -> list.filterNot { it.id == id } }
    }

    fun clear() {
        _notifications.value = emptyList()
        _loaded.value = false
    }
}
