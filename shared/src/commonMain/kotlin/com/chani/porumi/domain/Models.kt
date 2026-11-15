package com.chani.porumi.domain

import kotlin.time.Instant

enum class Species(val label: String, val emoji: String) {
    DOG("강아지", "🐶"),
    CAT("고양이", "🐱"),
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
)

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
)
