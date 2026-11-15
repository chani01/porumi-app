package com.chani.porumi.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.periodUntil
import kotlin.time.Instant

enum class Species(val label: String) {
    DOG("강아지"),
    CAT("고양이"),
}

/**
 * 강아지·고양이 공통 감정 세트.
 * valence: 기분 점수(-2 ~ +2). 주간 "기분 지수" 계산에 사용한다.
 */
enum class Emotion(val label: String, val emoji: String, val valence: Int) {
    HAPPY("행복해요", "😊", 2),
    RELAXED("편안해요", "😌", 1),
    EXCITED("신나요", "🤩", 2),
    CURIOUS("궁금해요", "🧐", 1),
    SLEEPY("졸려요", "😴", 0),
    ANXIOUS("불안해요", "😟", -1),
    ANNOYED("예민해요", "😾", -2);

    companion object {
        fun fromKey(key: String): Emotion? = entries.firstOrNull { it.name == key }
    }
}

data class Pet(
    val id: String,
    val name: String,
    val species: Species,
    /** 품종 (예: 골든 리트리버). 선택 입력 */
    val breed: String? = null,
    /** 생일. 선택 입력 */
    val birthDate: LocalDate? = null,
)

/** 품종이 있으면 품종, 없으면 종 이름 (강아지/고양이) */
val Pet.breedOrSpecies: String
    get() = breed?.takeIf { it.isNotBlank() } ?: species.label

/** 프로필 한 줄: "골든 리트리버 · 3살" (생일이 없으면 품종만) */
fun Pet.profileLine(today: LocalDate): String =
    listOfNotNull(breedOrSpecies, birthDate?.let { ageLabel(it, today) }).joinToString(" · ")

/** 나이: 1개월 미만 → "1개월 미만", 1살 미만 → "N개월", 그 이상 → "N살". 생일이 미래면 null */
fun ageLabel(birthDate: LocalDate, today: LocalDate): String? {
    if (birthDate > today) return null
    val period = birthDate.periodUntil(today)
    return when {
        period.years >= 1 -> "${period.years}살"
        period.months >= 1 -> "${period.months}개월"
        else -> "1개월 미만"
    }
}

data class Analysis(
    val id: String,
    val petId: String?,
    val imagePath: String,
    val species: Species,
    val primaryEmotion: Emotion,
    val confidence: Float,
    val scores: Map<Emotion, Float>,
    val cues: List<String>,
    val summary: String,
    val takenAt: Instant,
    /** 사진과 함께 남긴 오늘의 이야기 (선택) */
    val note: String? = null,
)

/** 오늘의 이야기 최대 글자 수 (DB 제약은 500자, 앱에서는 300자로 제한) */
const val NOTE_MAX_LENGTH = 300
