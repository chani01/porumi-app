package com.chani.porumi.data

import com.chani.porumi.domain.Analysis
import com.chani.porumi.domain.Emotion
import com.chani.porumi.domain.Pet
import com.chani.porumi.domain.Species
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
internal data class PetDto(
    val id: String,
    val name: String,
    val species: String,
    val breed: String? = null,
    @SerialName("birth_date") val birthDate: LocalDate? = null,
)

/** insert / update 공용. breed·birthDate 는 null 도 그대로 보내 지울 수 있게 한다 */
@Serializable
internal data class NewPetDto(
    val name: String,
    val species: String,
    val breed: String?,
    @SerialName("birth_date") val birthDate: LocalDate?,
)

@Serializable
internal data class AnalysisDto(
    val id: String,
    @SerialName("pet_id") val petId: String? = null,
    @SerialName("image_path") val imagePath: String,
    val species: String,
    @SerialName("primary_emotion") val primaryEmotion: String,
    val confidence: Float,
    val scores: Map<String, Float> = emptyMap(),
    val cues: List<String> = emptyList(),
    val summary: String = "",
    @SerialName("taken_at") val takenAt: String,
)

@Serializable
internal data class AnalyzeRequest(
    @SerialName("image_path") val imagePath: String,
    @SerialName("pet_id") val petId: String?,
    @SerialName("taken_at") val takenAt: String?,
)

internal fun PetDto.toDomain(): Pet? {
    val species = Species.entries.firstOrNull { it.name == species } ?: return null
    return Pet(id = id, name = name, species = species, breed = breed, birthDate = birthDate)
}

internal fun AnalysisDto.toDomain(): Analysis? {
    val emotion = Emotion.fromKey(primaryEmotion) ?: return null
    val species = Species.entries.firstOrNull { it.name == species } ?: return null
    return Analysis(
        id = id,
        petId = petId,
        imagePath = imagePath,
        species = species,
        primaryEmotion = emotion,
        confidence = confidence,
        scores = scores.mapNotNull { (k, v) -> Emotion.fromKey(k)?.let { it to v } }.toMap(),
        cues = cues,
        summary = summary,
        takenAt = Instant.parse(takenAt),
    )
}
