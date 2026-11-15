package com.chani.porumi.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeeklyMoodCalculatorTest {

    private val tz = TimeZone.of("Asia/Seoul")

    private fun analysis(
        id: String,
        emotion: Emotion,
        confidence: Float,
        at: LocalDateTime,
    ) = Analysis(
        id = id, petId = null, imagePath = "$id.jpg", species = Species.DOG,
        primaryEmotion = emotion, confidence = confidence, scores = emptyMap(),
        cues = emptyList(), summary = "", takenAt = at.toInstant(tz),
    )

    @Test
    fun weekStartIsMonday() {
        // 2026-11-15 은 일요일
        assertEquals(LocalDate(2026, 11, 9), WeeklyMoodCalculator.weekStartOf(LocalDate(2026, 11, 15)))
        assertEquals(LocalDate(2026, 11, 9), WeeklyMoodCalculator.weekStartOf(LocalDate(2026, 11, 9)))
    }

    @Test
    fun representativeEmotionIsConfidenceWeighted() {
        val day = LocalDateTime(2026, 11, 10, 9, 0)
        val items = listOf(
            analysis("a", Emotion.SLEEPY, 0.4f, day),
            analysis("b", Emotion.SLEEPY, 0.3f, day),
            analysis("c", Emotion.HAPPY, 0.9f, day),
        )
        assertEquals(Emotion.HAPPY, WeeklyMoodCalculator.representativeEmotion(items))
    }

    @Test
    fun moodScoreIsWeightedValence() {
        val day = LocalDateTime(2026, 11, 10, 9, 0)
        val items = listOf(
            analysis("a", Emotion.HAPPY, 1.0f, day),   // +2
            analysis("b", Emotion.ANXIOUS, 1.0f, day), // -1
        )
        assertEquals(0.5f, WeeklyMoodCalculator.moodScore(items)!!, 0.001f)
        assertNull(WeeklyMoodCalculator.moodScore(emptyList()))
    }

    @Test
    fun calculateGroupsByLocalDateAndComparesPreviousWeek() {
        val weekStart = LocalDate(2026, 11, 9)
        val items = listOf(
            // 한국 시간 월요일 00:30 → 이번 주 월요일에 포함돼야 함 (UTC 로는 일요일)
            analysis("mon", Emotion.HAPPY, 0.8f, LocalDateTime(2026, 11, 9, 0, 30)),
            analysis("wed", Emotion.ANNOYED, 0.7f, LocalDateTime(2026, 11, 11, 18, 0)),
            // 지난주 일요일
            analysis("prev", Emotion.RELAXED, 0.9f, LocalDateTime(2026, 11, 8, 23, 0)),
        )
        val mood = WeeklyMoodCalculator.calculate(items, weekStart, tz)

        assertEquals(7, mood.days.size)
        assertEquals(Emotion.HAPPY, mood.days[0].emotion)
        assertEquals(Emotion.ANNOYED, mood.days[2].emotion)
        assertNull(mood.days[1].emotion)
        assertEquals(2, mood.totalCount)
        assertEquals(0f, mood.averageScore!!, 0.001f)          // (+2 + -2) / 2
        assertEquals(1f, mood.previousAverageScore!!, 0.001f)  // RELAXED
        assertEquals("mon", mood.happiest?.id)
        assertEquals("wed", mood.toughest?.id)
        assertEquals(MoodLevel.OKAY, mood.level)
    }

    @Test
    fun emptyWeekHasNoScoreAndFriendlyComment() {
        val mood = WeeklyMoodCalculator.calculate(emptyList(), LocalDate(2026, 11, 9), tz)
        assertNull(mood.averageScore)
        assertNull(mood.level)
        assertTrue(WeeklyMoodCalculator.comment(mood, "코코").contains("코코"))
    }

    @Test
    fun recentWindowEndsToday() {
        val today = LocalDate(2026, 11, 15)
        assertEquals(LocalDate(2026, 11, 9), WeeklyMoodCalculator.recentStartOf(today))
        assertEquals(LocalDate(2026, 11, 2), WeeklyMoodCalculator.recentStartOf(today, periodsAgo = 1))
    }

    @Test
    fun topicParticleFollowsFinalConsonant() {
        assertEquals("는", WeeklyMoodCalculator.topicParticle("모모"))
        assertEquals("은", WeeklyMoodCalculator.topicParticle("딸깅"))
        assertEquals("는", WeeklyMoodCalculator.topicParticle("Coco"))
        assertTrue(WeeklyMoodCalculator.comment(
            WeeklyMoodCalculator.calculate(
                listOf(analysis("x", Emotion.HAPPY, 0.9f, LocalDateTime(2026, 11, 15, 10, 0))),
                LocalDate(2026, 11, 9), tz,
            ),
            "모모",
        ).startsWith("최근 7일 모모는"))
    }
}
