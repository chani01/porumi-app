package com.chani.porumi.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class DayMood(
    val date: LocalDate,
    /** 그날의 대표 감정 (신뢰도 가중 최빈값). 기록이 없으면 null */
    val emotion: Emotion?,
    /** 그날의 기분 지수 (-2 ~ +2). 기록이 없으면 null */
    val score: Float?,
    val count: Int,
)

data class WeeklyMood(
    val weekStart: LocalDate,
    val days: List<DayMood>,
    val averageScore: Float?,
    val previousAverageScore: Float?,
    val distribution: Map<Emotion, Int>,
    val happiest: Analysis?,
    val toughest: Analysis?,
    val totalCount: Int,
) {
    val level: MoodLevel? get() = averageScore?.let(MoodLevel::from)
}

enum class MoodLevel(val label: String, val emoji: String) {
    GREAT("아주 좋음", "🥰"),
    GOOD("좋음", "😊"),
    OKAY("보통", "🙂"),
    SENSITIVE("조금 예민", "😕"),
    TOUGH("힘든 한 주", "😢");

    companion object {
        fun from(score: Float): MoodLevel = when {
            score >= 1.2f -> GREAT
            score >= 0.4f -> GOOD
            score > -0.4f -> OKAY
            score > -1.2f -> SENSITIVE
            else -> TOUGH
        }
    }
}

object WeeklyMoodCalculator {

    /** date 가 속한 주의 월요일 */
    fun weekStartOf(date: LocalDate): LocalDate =
        date.minus(DatePeriod(days = date.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber))

    fun calculate(
        analyses: List<Analysis>,
        weekStart: LocalDate,
        timeZone: TimeZone,
    ): WeeklyMood {
        val byDate = analyses.groupBy { it.takenAt.toLocalDateTime(timeZone).date }
        val weekDates = (0 until 7).map { weekStart.plus(DatePeriod(days = it)) }
        val inWeek = weekDates.flatMap { byDate[it].orEmpty() }

        val days = weekDates.map { date ->
            val items = byDate[date].orEmpty()
            DayMood(
                date = date,
                emotion = representativeEmotion(items),
                score = moodScore(items),
                count = items.size,
            )
        }

        val prevDates = (1..7).map { weekStart.minus(DatePeriod(days = it)) }
        val prevItems = prevDates.flatMap { byDate[it].orEmpty() }

        return WeeklyMood(
            weekStart = weekStart,
            days = days,
            averageScore = average(days.mapNotNull { it.score }),
            previousAverageScore = average(
                prevDates.mapNotNull { moodScore(byDate[it].orEmpty()) }
            ).takeIf { prevItems.isNotEmpty() },
            distribution = inWeek.groupingBy { it.primaryEmotion }.eachCount(),
            happiest = inWeek.maxWithOrNull(compareBy({ it.primaryEmotion.valence }, { it.confidence })),
            toughest = inWeek
                .filter { it.primaryEmotion.valence < 0 }
                .minWithOrNull(compareBy({ it.primaryEmotion.valence }, { -it.confidence })),
            totalCount = inWeek.size,
        )
    }

    /** 신뢰도 가중 최빈 감정. 동점이면 더 최근 기록의 감정 */
    fun representativeEmotion(items: List<Analysis>): Emotion? {
        if (items.isEmpty()) return null
        val weights = mutableMapOf<Emotion, Float>()
        items.forEach { weights[it.primaryEmotion] = (weights[it.primaryEmotion] ?: 0f) + it.confidence }
        val max = weights.values.max()
        val tied = weights.filterValues { it == max }.keys
        return items.sortedByDescending { it.takenAt }.first { it.primaryEmotion in tied }.primaryEmotion
    }

    /** Σ(valence × confidence) / Σconfidence */
    fun moodScore(items: List<Analysis>): Float? {
        if (items.isEmpty()) return null
        val weightSum = items.sumOf { it.confidence.toDouble() }
        if (weightSum <= 0.0) return items.map { it.primaryEmotion.valence }.average().toFloat()
        return (items.sumOf { it.primaryEmotion.valence * it.confidence.toDouble() } / weightSum).toFloat()
    }

    /** today 를 마지막 날로 하는 최근 7일 구간의 시작일 */
    fun recentStartOf(today: LocalDate, periodsAgo: Int = 0): LocalDate =
        today.minus(DatePeriod(days = 6 + periodsAgo * 7))

    /** 리포트 화면의 한 줄 코멘트 (규칙 기반). periodLabel 예: "최근 7일" */
    fun comment(mood: WeeklyMood, petName: String?, periodLabel: String = "최근 7일"): String {
        val who = petName ?: "우리 아이"
        val avg = mood.averageScore ?: return "${periodLabel} 동안 기록이 없어요. ${who}의 표정을 남겨볼까요? 📸"
        val prev = mood.previousAverageScore
        // 동점이면 "가장 많이"라고 말할 수 없으므로 기분 등급으로 대신 표현
        val maxCount = mood.distribution.values.maxOrNull() ?: 0
        val top = mood.distribution.filterValues { it == maxCount }.keys.singleOrNull()
        val trend = when {
            prev == null -> null
            avg - prev >= 0.3f -> "그 전 7일보다 기분이 더 좋아졌어요"
            prev - avg >= 0.3f -> "그 전 7일보다 조금 지쳐 보여요"
            else -> "그 전 7일과 비슷한 기분이에요"
        }
        return buildString {
            append("$periodLabel ${who}${topicParticle(who)} ")
            append(if (top != null) "'${top.label}' 표정을 가장 많이 지었어요" else "전반적으로 기분이 '${MoodLevel.from(avg).label}'이었어요")
            if (trend != null) append(". $trend")
            append(if (avg < -0.4f) ". 조금 더 다정하게 챙겨주세요 💛" else " ✨")
        }
    }

    /** 받침 유무에 따라 은/는 */
    fun topicParticle(word: String): String {
        val last = word.lastOrNull() ?: return "는"
        if (last !in '가'..'힣') return "는"
        return if ((last - '가') % 28 != 0) "은" else "는"
    }

    private fun average(values: List<Float>): Float? =
        if (values.isEmpty()) null else values.average().toFloat()
}
