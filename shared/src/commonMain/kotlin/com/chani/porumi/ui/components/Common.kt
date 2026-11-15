package com.chani.porumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.chani.porumi.data.PHOTO_BUCKET
import com.chani.porumi.domain.Analysis
import com.chani.porumi.domain.Emotion
import io.github.jan.supabase.storage.authenticatedStorageItem
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** 비공개 Storage 사진 (Coil3Integration 이 사용자 토큰으로 내려받음) */
@Composable
fun PetPhoto(path: String, modifier: Modifier = Modifier, contentDescription: String? = null) {
    AsyncImage(
        model = authenticatedStorageItem(PHOTO_BUCKET, path),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
fun EmotionBadge(emotion: Emotion, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(emotion.emoji, fontSize = 14.sp)
    }
}

/** 감정별 점수 막대 + 근거 + 한 줄 요약 (분석 결과 / 상세 화면 공용) */
@Composable
fun AnalysisResultContent(analysis: Analysis, petName: String?, modifier: Modifier = Modifier) {
    val colors = com.chani.porumi.ui.theme.LocalChartColors.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(analysis.primaryEmotion.emoji, fontSize = 44.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "${analysis.species.emoji} ${petName ?: analysis.species.label}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${analysis.primaryEmotion.label}  ${(analysis.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (analysis.summary.isNotBlank()) {
            Text(
                "“${analysis.summary}”",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            analysis.scores.entries
                .sortedByDescending { it.value }
                .filter { it.value >= 0.01f }
                .take(4)
                .forEach { (emotion, score) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${emotion.emoji} ${emotion.label}", modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodyMedium)
                        Box(
                            Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.track),
                        ) {
                            Box(
                                Modifier.fillMaxWidth(score.coerceIn(0f, 1f)).height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)).background(colors.bar),
                            )
                        }
                        Text(
                            "${(score * 100).toInt()}",
                            modifier = Modifier.width(36.dp).padding(start = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
        }

        if (analysis.cues.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("💬 이렇게 읽었어요", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                analysis.cues.forEach { cue ->
                    Text("• $cue", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Text(
            "표정 분석은 참고용이며, 건강 이상이 의심되면 수의사와 상담해 주세요.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ClickableCard(onClick: (() -> Unit)?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) { content() }
}

// ── 날짜 포맷 ──────────────────────────────

val DayOfWeek.korean: String
    get() = when (this) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }

fun LocalDate.monthDay(): String = "${month.ordinal + 1}월 ${day}일"

fun Analysis.localDateTimeLabel(): String {
    val dt = takenAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val minute = dt.minute.toString().padStart(2, '0')
    return "${dt.date.monthDay()} (${dt.dayOfWeek.korean}) ${dt.hour}:$minute"
}
