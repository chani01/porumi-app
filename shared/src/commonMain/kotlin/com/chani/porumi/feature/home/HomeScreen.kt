package com.chani.porumi.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chani.porumi.domain.Analysis
import com.chani.porumi.domain.WeeklyMood
import com.chani.porumi.ui.components.ActivityIndicator
import com.chani.porumi.ui.components.ClickableCard
import com.chani.porumi.ui.components.PrimaryButton
import com.chani.porumi.ui.components.EmotionBadge
import com.chani.porumi.ui.components.PetProfileHeader
import com.chani.porumi.ui.components.PetPhoto
import com.chani.porumi.ui.components.korean
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import com.chani.porumi.domain.profileLine
import com.chani.porumi.domain.breedOrSpecies
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import com.chani.porumi.data.NotificationRepository
import com.chani.porumi.ui.components.CircleIconButton
import org.koin.compose.koinInject
import androidx.compose.material.icons.rounded.Pets
import com.chani.porumi.ui.components.IconBubble
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.shadow
import com.chani.porumi.domain.WeeklyMoodCalculator
import com.chani.porumi.ui.components.Hairline
import com.chani.porumi.ui.theme.LocalChartColors
import com.chani.porumi.ui.theme.LocalPorumiColors

private const val GAP = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPhotoClick: (String) -> Unit,
    onWeekClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onAddPet: () -> Unit,
    onNotifications: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rows = buildFeedRows(state.photos)

    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
        state = pullState,
        // iOS UIRefreshControl 처럼 당기는 만큼 스피너가 나타난다
        indicator = {
            ActivityIndicator(
                animating = state.refreshing,
                size = 26.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .graphicsLayer { alpha = if (state.refreshing) 1f else pullState.distanceFraction.coerceIn(0f, 1f) },
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(GAP.dp),
        ) {
            item(key = "title") { HomeTitleBar(onNotifications) }
            item(key = "profile") {
                val pet = state.selectedPet
                PetProfileHeader(
                    pets = state.pets,
                    selected = pet,
                    avatars = state.avatars,
                    subtitle = if (pet != null) {
                        val profile = state.today?.let { pet.profileLine(it) } ?: pet.breedOrSpecies
                        "$profile · 기록 ${state.photos.size}장"
                    } else "",
                    onSelect = viewModel::selectPet,
                    onAddPet = onAddPet,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            item(key = "week") {
                val week = state.recent
                if (week != null && state.today != null) {
                    MoodSummaryCard(
                        week = week,
                        today = state.today!!,
                        petName = state.selectedPet?.name ?: "우리 아이",
                        onOpenReport = onWeekClick,
                        onRecord = onAnalyzeClick,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
            if (rows.isEmpty()) {
                item(key = "empty") { EmptyFeed(onAnalyzeClick) }
            } else {
                items(rows, key = { row -> row.items.first().id }) { row ->
                    Box(Modifier.padding(horizontal = 16.dp)) { FeedRowView(row, onPhotoClick) }
                }
            }
        }
    }
}

// ── 상단: porumi. 로고 + 알림 ──────────────────────────────

@Composable
private fun HomeTitleBar(onNotifications: () -> Unit, notifications: NotificationRepository = koinInject()) {
    val items by notifications.notifications.collectAsStateWithLifecycle()
    val hasUnread = items.any { !it.read }
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 8.dp),
    ) {
        Text(
            buildAnnotatedString {
                append("porumi")
                withStyle(SpanStyle(color = colors.primary)) { append(".") }
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,
            modifier = Modifier.weight(1f),
        )
        Box {
            CircleIconButton(
                if (hasUnread) Icons.Rounded.NotificationsActive else Icons.Rounded.Notifications,
                contentDescription = if (hasUnread) "알림 (새 알림 있음)" else "알림",
                onClick = onNotifications,
            )
            if (hasUnread) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 2.dp)
                        .size(10.dp)
                        .border(2.dp, colors.surfaceContainer, CircleShape)
                        .background(colors.primary, CircleShape),
                )
            }
        }
    }
}

// ── 최근 7일 기분 요약 ──────────────────────────────

/**
 * 홈 상단 요약: 한 줄 결론(기분 단계) + 가장 많이 지은 표정 + 기록 일수·추세 + 작은 7일 점.
 * 누르면 리포트로 이동. 오늘 기록이 없으면 아래에 기록하기 안내가 붙는다.
 */
@Composable
fun MoodSummaryCard(
    week: WeeklyMood,
    today: LocalDate,
    petName: String,
    onOpenReport: () -> Unit,
    onRecord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val extra = LocalPorumiColors.current
    val level = week.level
    val recordedDays = week.days.count { it.count > 0 }
    val recordedToday = week.days.any { it.date == today && it.count > 0 }
    val shape = RoundedCornerShape(24.dp)

    Column(
        modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, ambientColor = extra.cardShadow, spotColor = extra.cardShadow)
            .clip(shape)
            .background(colors.surfaceContainer),
    ) {
        Column(
            Modifier.fillMaxWidth().clickable(onClick = onOpenReport).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (level != null) {
                    Box(
                        Modifier.size(56.dp).background(colors.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text(level.emoji, fontSize = 30.sp) }
                } else {
                    IconBubble(Icons.Rounded.Insights, size = 56.dp)
                }
                Column(Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (level != null) "최근 7일 $petName${WeeklyMoodCalculator.topicParticle(petName)} '${level.label}'"
                        else "최근 7일 기록이 없어요",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    summaryLine(week, petName)?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    if (recordedDays > 0) {
                        Text(
                            listOfNotNull("기록 ${recordedDays}일", trendLabel(week)).joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = "리포트 보기",
                    tint = extra.tertiaryLabel,
                    modifier = Modifier.size(14.dp),
                )
            }
            WeekDots(week, today)
        }
        if (!recordedToday) {
            Box(Modifier.fillMaxWidth().height(Hairline).background(extra.separator))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            ) {
                Text(
                    "오늘은 아직 기록이 없어요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "기록하기",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.primary)
                        .clickable(onClick = onRecord)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/** 작은 7일 점: 기록한 날은 기분 색(좋음/예민/보통), 없는 날은 빈 점, 오늘은 링 */
@Composable
private fun WeekDots(week: WeeklyMood, today: LocalDate) {
    val colors = MaterialTheme.colorScheme
    val chart = LocalChartColors.current
    Row(Modifier.fillMaxWidth()) {
        week.days.forEach { day ->
            val isToday = day.date == today
            val score = day.score
            val dot = when {
                score == null -> colors.outline
                score > 0.05f -> chart.positive
                score < -0.05f -> chart.negative
                else -> chart.neutral
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(18.dp)
                        .then(if (isToday) Modifier.border(2.dp, colors.primary, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(10.dp).background(dot, CircleShape))
                }
                Text(
                    day.date.dayOfWeek.korean,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) colors.primary else colors.secondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** 가장 많이 지은 표정 (동점이면 생략) */
private fun summaryLine(week: WeeklyMood, petName: String): String? {
    if (week.totalCount == 0) return "$petName${WeeklyMoodCalculator.topicParticle(petName)} 어떤 표정을 짓고 있을까요?"
    val max = week.distribution.values.maxOrNull() ?: return null
    val top = week.distribution.filterValues { it == max }.keys.singleOrNull() ?: return null
    return "'${top.label}' 표정을 가장 많이 지었어요"
}

/** 그 전 7일과 비교한 추세 */
private fun trendLabel(week: WeeklyMood): String? {
    val avg = week.averageScore ?: return null
    val prev = week.previousAverageScore ?: return null
    return when {
        avg - prev >= 0.3f -> "지난 7일보다 ↑"
        prev - avg >= 0.3f -> "지난 7일보다 ↓"
        else -> "지난 7일과 비슷"
    }
}

// ── 탐색 탭 스타일 그리드 ──────────────────────────────

enum class FeedRowType { Normal, BigLeft, BigRight }

data class FeedRow(val type: FeedRowType, val items: List<Analysis>)

/**
 * 3장씩 묶어 [Normal, BigLeft, Normal, Normal, BigRight, Normal] 패턴으로 배치.
 * Big 행은 2x2 큰 타일 1장 + 오른쪽(왼쪽)에 작은 타일 2장.
 */
fun buildFeedRows(photos: List<Analysis>): List<FeedRow> {
    val pattern = listOf(
        FeedRowType.Normal, FeedRowType.BigLeft, FeedRowType.Normal,
        FeedRowType.Normal, FeedRowType.BigRight, FeedRowType.Normal,
    )
    return photos.chunked(3).mapIndexed { i, chunk ->
        val type = if (chunk.size == 3) pattern[i % pattern.size] else FeedRowType.Normal
        FeedRow(type, chunk)
    }
}

@Composable
private fun FeedRowView(row: FeedRow, onClick: (String) -> Unit) {
    when (row.type) {
        FeedRowType.Normal -> Row(horizontalArrangement = Arrangement.spacedBy(GAP.dp)) {
            repeat(3) { i ->
                val item = row.items.getOrNull(i)
                if (item != null) {
                    FeedTile(item, onClick, Modifier.weight(1f).aspectRatio(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        FeedRowType.BigLeft, FeedRowType.BigRight -> {
            val big = row.items[0]
            val smalls = row.items.drop(1)
            Row(horizontalArrangement = Arrangement.spacedBy(GAP.dp)) {
                if (row.type == FeedRowType.BigLeft) {
                    FeedTile(big, onClick, Modifier.weight(2f).aspectRatio(1f))
                    SmallColumn(smalls, onClick, Modifier.weight(1f))
                } else {
                    SmallColumn(smalls, onClick, Modifier.weight(1f))
                    FeedTile(big, onClick, Modifier.weight(2f).aspectRatio(1f))
                }
            }
        }
    }
}

@Composable
private fun SmallColumn(items: List<Analysis>, onClick: (String) -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(GAP.dp)) {
        items.forEach { FeedTile(it, onClick, Modifier.fillMaxWidth().aspectRatio(1f)) }
    }
}

@Composable
private fun FeedTile(item: Analysis, onClick: (String) -> Unit, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(16.dp)).clickable { onClick(item.id) }) {
        PetPhoto(item.imagePath, Modifier.fillMaxSize(), contentDescription = item.primaryEmotion.label)
        EmotionBadge(item.primaryEmotion, Modifier.align(Alignment.TopEnd).padding(6.dp))
    }
}

@Composable
private fun EmptyFeed(onAnalyzeClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBubble(Icons.Rounded.Pets, size = 88.dp)
        Text("아직 기록이 없어요", style = MaterialTheme.typography.titleLarge)
        Text(
            "사진을 올리면 표정을 읽어서 기분을 기록해 드려요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(onClick = onAnalyzeClick) { Text("첫 사진 분석하기") }
    }
}
