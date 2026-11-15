package com.chani.porumi.feature.report

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chani.porumi.domain.Analysis
import com.chani.porumi.domain.DayMood
import com.chani.porumi.domain.WeeklyMood
import com.chani.porumi.ui.components.ClickableCard
import com.chani.porumi.ui.components.PetProfileHeader
import com.chani.porumi.ui.components.PetPhoto
import com.chani.porumi.ui.components.korean
import com.chani.porumi.ui.components.monthDay
import com.chani.porumi.ui.theme.LocalChartColors
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs
import kotlin.math.roundToInt
import com.chani.porumi.ui.components.Hairline
import com.chani.porumi.ui.components.LargeTitle
import com.chani.porumi.ui.components.PlainButton
import com.chani.porumi.ui.theme.LocalPorumiColors
import androidx.compose.material.icons.rounded.Insights
import com.chani.porumi.ui.components.IconBubble
import com.chani.porumi.domain.profileLine
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@Composable
fun ReportScreen(
    onPhotoClick: (String) -> Unit,
    onAddPet: () -> Unit,
    viewModel: ReportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mood = state.mood ?: return

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LargeTitle("무드 리포트", Modifier.padding(horizontal = 16.dp))
        PetProfileHeader(
            pets = state.pets,
            selected = state.selectedPet,
            avatars = state.avatars,
            subtitle = state.selectedPet?.let { pet -> pet.profileLine(Clock.System.todayIn(TimeZone.currentSystemDefault())) }.orEmpty(),
            onSelect = viewModel::selectPet,
            onAddPet = onAddPet,
        )

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PeriodNavigator(mood, state.periodsAgo, viewModel::previous, viewModel::next)
            SummaryCard(mood, state.comment)
            DailyMoodChart(mood)
            DistributionCard(mood)
            HighlightsCard(mood, onPhotoClick)
        }
    }
}

@Composable
private fun PeriodNavigator(mood: WeeklyMood, periodsAgo: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    val end = mood.weekStart.plus(DatePeriod(days = 6))
    val isRecent = periodsAgo == 0
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        PlainButton(onClick = onPrev) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = "이전 7일")
        }
        Text(
            "${mood.weekStart.monthDay()} – ${end.monthDay()}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        PlainButton(onClick = onNext, enabled = !isRecent) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = "다음 7일")
        }
    }
}

@Composable
private fun SummaryCard(mood: WeeklyMood, comment: String) {
    ClickableCard(onClick = null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val level = mood.level
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (level != null) Text(level.emoji, fontSize = 40.sp)
                else IconBubble(Icons.Rounded.Insights, size = 52.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(level?.label ?: "기록 없음", style = MaterialTheme.typography.titleLarge)
                    val avg = mood.averageScore
                    if (avg != null) {
                        val prev = mood.previousAverageScore
                        val change = prev?.let { avg - it }
                        Text(
                            buildString {
                                append("기분 지수 ${avg.signed()} · 기록 ${mood.totalCount}장")
                                if (change != null) append(" · 이전 7일 대비 ${change.signed()}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(comment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** 요일별 기분 지수: 0 기준선 위(좋음)/아래(안 좋음)로 뻗는 diverging 막대. 탭하면 값 표시 */
@Composable
private fun DailyMoodChart(mood: WeeklyMood) {
    val colors = LocalChartColors.current
    var selected by remember(mood.weekStart) { mutableStateOf<DayMood?>(null) }
    val halfHeight = 56.dp

    ClickableCard(onClick = null) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("요일별 기분", style = MaterialTheme.typography.titleMedium)
            Text(
                selected?.let { d ->
                    if (d.score == null) "${d.date.monthDay()} (${d.date.dayOfWeek.korean}) · 기록 없음"
                    else "${d.date.monthDay()} (${d.date.dayOfWeek.korean}) · ${d.emotion?.emoji} ${d.emotion?.label} · 기분 지수 ${d.score.signed()} · ${d.count}장"
                } ?: "막대를 누르면 자세히 볼 수 있어요",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth()) {
                mood.days.forEach { day ->
                    val isSelected = selected?.date == day.date
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { selected = if (isSelected) null else day }
                            .padding(vertical = 4.dp),
                    ) {
                        Text(day.emotion?.emoji ?: "·", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        // 위쪽 절반 (양수)
                        Box(Modifier.height(halfHeight).width(14.dp), contentAlignment = Alignment.BottomCenter) {
                            val s = day.score
                            if (s != null && s > 0.05f) {
                                Box(
                                    Modifier.fillMaxWidth().fillMaxHeight((s / 2f).coerceIn(0.06f, 1f))
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(colors.positive),
                                )
                            }
                        }
                        // 기준선 (0) — 중립 값이면 회색 점
                        Box(Modifier.height(2.dp).fillMaxWidth().background(
                            if (day.score != null && abs(day.score) <= 0.05f) colors.neutral else MaterialTheme.colorScheme.outlineVariant,
                        ))
                        // 아래쪽 절반 (음수)
                        Box(Modifier.height(halfHeight).width(14.dp), contentAlignment = Alignment.TopCenter) {
                            val s = day.score
                            if (s != null && s < -0.05f) {
                                Box(
                                    Modifier.fillMaxWidth().fillMaxHeight((-s / 2f).coerceIn(0.06f, 1f))
                                        .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                                        .background(colors.negative),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(day.date.dayOfWeek.korean, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem(colors.positive, "좋은 기분")
                LegendItem(colors.negative, "예민·불안")
            }
        }
    }
}

@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(10.dp).height(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 감정 분포: 단일 색 막대 + 이모지/이름/횟수 라벨 (색이 아니라 라벨로 구분) */
@Composable
private fun DistributionCard(mood: WeeklyMood) {
    if (mood.distribution.isEmpty()) return
    val colors = LocalChartColors.current
    val max = mood.distribution.values.max().toFloat()
    ClickableCard(onClick = null) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("자주 지은 표정", style = MaterialTheme.typography.titleMedium)
            mood.distribution.entries.sortedByDescending { it.value }.forEach { (emotion, count) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${emotion.emoji} ${emotion.label}", modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodyMedium)
                    Box(Modifier.weight(1f).height(10.dp)) {
                        Box(
                            Modifier.fillMaxWidth(count / max).fillMaxHeight()
                                .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                                .background(colors.bar),
                        )
                    }
                    Text(
                        "${count}회 · ${(count * 100f / mood.totalCount).roundToInt()}%",
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightsCard(mood: WeeklyMood, onPhotoClick: (String) -> Unit) {
    val happiest = mood.happiest ?: return
    ClickableCard(onClick = null) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("이 기간 하이라이트", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Highlight("가장 행복했던 순간", happiest, onPhotoClick, Modifier.weight(1f))
                val toughest = mood.toughest
                if (toughest != null) {
                    Highlight("조금 힘들었던 순간", toughest, onPhotoClick, Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
            HorizontalDivider(thickness = Hairline, color = LocalPorumiColors.current.separator)
            Text(
                "표정 분석은 참고용이에요. 평소와 다른 모습이 계속되면 수의사와 상담해 주세요.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Highlight(title: String, analysis: Analysis, onClick: (String) -> Unit, modifier: Modifier) {
    Column(modifier.clickable { onClick(analysis.id) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PetPhoto(analysis.imagePath, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)))
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${analysis.primaryEmotion.emoji} ${analysis.primaryEmotion.label}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun Float.signed(): String {
    val rounded = (this * 10).roundToInt() / 10f
    return if (rounded > 0) "+$rounded" else "$rounded"
}
