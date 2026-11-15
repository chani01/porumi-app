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

private const val GAP = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPhotoClick: (String) -> Unit,
    onWeekClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onAddPet: () -> Unit,
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
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            item(key = "week") {
                val week = state.recent
                if (week != null && state.today != null) {
                    WeekStripCard(
                        week = week,
                        today = state.today!!,
                        title = "최근 7일 ${state.selectedPet?.name ?: "우리 아이"}의 기분",
                        onClick = onWeekClick,
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

// ── 이번 주 기분 스트립 ──────────────────────────────

@Composable
fun WeekStripCard(
    week: WeeklyMood,
    today: LocalDate,
    title: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    ClickableCard(onClick = onClick, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                val level = week.level
                Text(
                    if (level != null) "${level.emoji} ${level.label}" else "기록 없음",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.days.forEach { day ->
                    val isToday = day.date == today
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            day.date.dayOfWeek.korean,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isToday) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .then(if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
                        ) {
                            if (day.emotion != null) {
                                Text(day.emotion.emoji, fontSize = 18.sp)
                            } else {
                                Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
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
        Text("🐶🐱", fontSize = 48.sp)
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
