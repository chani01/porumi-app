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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import com.chani.porumi.ui.components.PlainButton

class NotificationsViewModel(private val repository: NotificationRepository) : ViewModel() {

    val notifications: StateFlow<List<AppNotification>> = repository.notifications
    val loaded: StateFlow<Boolean> = repository.loaded

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** 이번에 화면을 열었을 때 안 읽은 상태였던 알림 (읽음 처리 후에도 이번엔 "새 알림"으로 표시) */
    private val _freshIds = MutableStateFlow<Set<String>>(emptySet())
    val freshIds: StateFlow<Set<String>> = _freshIds.asStateFlow()

    /** 화면을 열면 새로 불러오고, 본 알림은 서버에 바로 읽음 처리 */
    fun open() {
        _error.value = null
        viewModelScope.launch {
            runCatching {
                repository.refresh()
                _freshIds.value = repository.notifications.value.filter { !it.read }.map { it.id }.toSet()
                repository.markAllRead()
            }.onFailure { _error.value = "알림을 불러오지 못했어요. 네트워크를 확인해 주세요." }
        }
    }
}

/** 홈 > 알림 기록 */
@Composable
fun NotificationsScreen(onBack: () -> Unit, viewModel: NotificationsViewModel = koinViewModel()) {
    val items by viewModel.notifications.collectAsStateWithLifecycle()
    val freshIds by viewModel.freshIds.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.open() }
    var expandedKinds by remember { mutableStateOf(emptySet<NotificationKind>()) }

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
                // 종류별 묶음: 종류 순서 고정, 묶음마다 최신 PER_KIND 개만 보이고 나머지는 펼쳐 보기
                items.groupBy { it.kind }
                    .entries
                    .sortedBy { it.key.ordinal }
                    .forEach { (kind, list) ->
                        item(key = "header-${kind.name}") { KindHeader(kind, list.size, list.count { it.id in freshIds }) }
                        val expanded = kind in expandedKinds
                        val shown = if (expanded) list else list.take(PER_KIND)
                        items(shown, key = { it.id }) { NotificationItem(it, isNew = it.id in freshIds) }
                        if (list.size > PER_KIND) {
                            item(key = "more-${kind.name}") {
                                MoreToggle(
                                    expanded = expanded,
                                    hidden = list.size - PER_KIND,
                                    onClick = {
                                        expandedKinds = if (expanded) expandedKinds - kind else expandedKinds + kind
                                    },
                                )
                            }
                        }
                    }
            }
        }
    }
}

/** 묶음마다 처음에 보여줄 개수 */
private const val PER_KIND = 3

private val NotificationKind.label: String
    get() = when (this) {
        NotificationKind.REMINDER -> "기록 리마인더"
        NotificationKind.WEEKLY_REPORT -> "주간 리포트"
        NotificationKind.ANALYSIS -> "분석 결과"
        NotificationKind.NOTICE -> "공지"
        NotificationKind.EVENT -> "이벤트·혜택"
    }

@Composable
private fun KindHeader(kind: NotificationKind, total: Int, unread: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 12.dp),
    ) {
        Text(kind.label, style = MaterialTheme.typography.titleSmall)
        Text("$total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        if (unread > 0) {
            Text(
                "새 알림 $unread",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun MoreToggle(expanded: Boolean, hidden: Int, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        PlainButton(onClick = onClick, textStyle = MaterialTheme.typography.labelMedium) {
            Text(if (expanded) "접기" else "${hidden}개 더 보기")
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun NotificationItem(item: AppNotification, isNew: Boolean) {
    val colors = MaterialTheme.colorScheme
    ClickableCard(onClick = null) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBubble(item.kind.icon, size = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isNew) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    if (isNew) {
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
