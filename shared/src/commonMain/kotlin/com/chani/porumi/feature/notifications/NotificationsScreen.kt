package com.chani.porumi.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.chani.porumi.data.AppNotification
import com.chani.porumi.data.NotificationRepository
import com.chani.porumi.ui.components.ActivityIndicator
import com.chani.porumi.ui.components.ClickableCard
import com.chani.porumi.ui.components.PorumiTopBar
import com.chani.porumi.ui.components.monthDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.ui.graphics.vector.ImageVector
import com.chani.porumi.data.NotificationKind
import com.chani.porumi.ui.components.IconBubble

class NotificationsViewModel(private val repository: NotificationRepository) : ViewModel() {

    val notifications: StateFlow<List<AppNotification>> = repository.notifications
    val loaded: StateFlow<Boolean> = repository.loaded

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** 화면을 열면 새로 불러오고, 본 알림은 읽음 처리 */
    fun open() {
        _error.value = null
        viewModelScope.launch {
            runCatching {
                repository.refresh()
                repository.markAllRead()
            }.onFailure { _error.value = "알림을 불러오지 못했어요. 네트워크를 확인해 주세요." }
        }
    }
}

/** 홈 > 알림 기록 */
@Composable
fun NotificationsScreen(onBack: () -> Unit, viewModel: NotificationsViewModel = koinViewModel()) {
    val items by viewModel.notifications.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.open() }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PorumiTopBar(title = "알림", onBack = onBack)
        when {
            items.isEmpty() && !loaded && error == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ActivityIndicator(size = 28.dp)
            }
            items.isEmpty() -> EmptyNotifications(error)
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (error != null) {
                    item { Text(error!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }
                items(items, key = { it.id }) { NotificationItem(it) }
            }
        }
    }
}

@Composable
private fun NotificationItem(item: AppNotification) {
    val colors = MaterialTheme.colorScheme
    ClickableCard(onClick = null) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBubble(item.kind.icon, size = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (item.read) FontWeight.Medium else FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    if (!item.read) {
                        Box(Modifier.padding(start = 6.dp).size(8.dp).background(colors.primary, CircleShape))
                    }
                }
                item.body?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                Text(relativeTime(item.createdAt), style = MaterialTheme.typography.labelSmall, color = colors.secondary)
            }
        }
    }
}

@Composable
private fun EmptyNotifications(error: String?) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        IconBubble(Icons.Rounded.Notifications, size = 96.dp)
        Text("아직 받은 알림이 없어요", style = MaterialTheme.typography.titleLarge)
        Text(
            error ?: "기록 리마인더나 최근 7일 리포트 소식이\n오면 여기에 모아 둘게요.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val NotificationKind.icon: ImageVector
    get() = when (this) {
        NotificationKind.REMINDER -> Icons.Rounded.PhotoCamera
        NotificationKind.WEEKLY_REPORT -> Icons.Rounded.Insights
        NotificationKind.ANALYSIS -> Icons.Rounded.AutoAwesome
        NotificationKind.NOTICE -> Icons.Rounded.Campaign
        NotificationKind.EVENT -> Icons.Rounded.CardGiftcard
    }

/** 방금 / N분 전 / N시간 전 / N일 전 / M월 D일 */
private fun relativeTime(at: Instant): String {
    val diff = Clock.System.now() - at
    return when {
        diff < 1.minutes -> "방금"
        diff < 1.hours -> "${diff.inWholeMinutes}분 전"
        diff < 1.days -> "${diff.inWholeHours}시간 전"
        diff < 7.days -> "${diff.inWholeDays}일 전"
        else -> at.toLocalDateTime(TimeZone.currentSystemDefault()).date.monthDay()
    }
}
