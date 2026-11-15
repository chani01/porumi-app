package com.chani.porumi.feature.analyze

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.porumi.data.AnalysisException
import com.chani.porumi.data.AnalysisRepository
import com.chani.porumi.data.PetFilter
import com.chani.porumi.data.PetRepository
import com.chani.porumi.domain.Analysis
import com.chani.porumi.domain.Pet
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.compressImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import com.chani.porumi.domain.NOTE_MAX_LENGTH
import com.chani.porumi.ui.components.latestPhotoByPet

sealed interface AnalyzeStep {
    data object Pick : AnalyzeStep
    data object Preparing : AnalyzeStep
    data class Ready(val image: ByteArray) : AnalyzeStep
    data class Analyzing(val image: ByteArray) : AnalyzeStep
    data class Done(val image: ByteArray, val analysis: Analysis) : AnalyzeStep
}

data class AnalyzeUiState(
    val step: AnalyzeStep = AnalyzeStep.Pick,
    val pets: List<Pet> = emptyList(),
    val avatars: Map<String, String> = emptyMap(),
    val selectedPetId: String? = null,
    val note: String = "",
    val error: String? = null,
) {
    val selectedPet: Pet? get() = pets.firstOrNull { it.id == selectedPetId }
}

class AnalyzeViewModel(
    private val analysisRepository: AnalysisRepository,
    petRepository: PetRepository,
    private val petFilter: PetFilter,
) : ViewModel() {

    private val step = MutableStateFlow<AnalyzeStep>(AnalyzeStep.Pick)
    // 직접 고르지 않았으면 홈에서 선택돼 있던 아이가 기본값
    private val selectedPetId = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)
    private val note = MutableStateFlow("")
    private val avatars = analysisRepository.analyses.map(::latestPhotoByPet)

    val state: StateFlow<AnalyzeUiState> = combine(
        combine(step, note, error, ::Triple),
        petRepository.pets,
        avatars,
        selectedPetId,
        petFilter.selectedPet,
    ) { (step, note, error), pets, avatars, selected, current ->
        AnalyzeUiState(
            step = step,
            pets = pets,
            avatars = avatars,
            selectedPetId = selected?.takeIf { id -> pets.any { it.id == id } } ?: current?.id,
            note = note,
            error = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyzeUiState())

    fun onFilePicked(file: PlatformFile?) {
        if (file == null) return
        error.value = null
        step.value = AnalyzeStep.Preparing
        viewModelScope.launch {
            try {
                // 업로드 전 리사이즈 + JPEG 압축 → 비용·속도 절감
                val bytes = FileKit.compressImage(
                    file = file,
                    imageFormat = ImageFormat.JPEG,
                    quality = 85,
                    maxWidth = MAX_SIDE,
                    maxHeight = MAX_SIDE,
                )
                step.value = AnalyzeStep.Ready(bytes)
            } catch (e: Exception) {
                step.value = AnalyzeStep.Pick
                error.value = "사진을 불러오지 못했어요."
            }
        }
    }

    fun selectPet(petId: String) {
        selectedPetId.value = petId
    }

    fun onNoteChange(text: String) {
        note.value = text.take(NOTE_MAX_LENGTH)
    }

    /** 빠른 문구 칩: 이야기 끝에 이어 붙인다 */
    fun appendToNote(phrase: String) {
        val current = note.value.trimEnd()
        onNoteChange(if (current.isEmpty()) phrase else "$current $phrase")
    }

    fun analyze() {
        val ready = step.value as? AnalyzeStep.Ready ?: return
        error.value = null
        step.value = AnalyzeStep.Analyzing(ready.image)
        viewModelScope.launch {
            try {
                val petId = state.value.selectedPetId
                var result = analysisRepository.analyze(ready.image, petId)
                // 분석한 아이를 홈/리포트에서도 선택 상태로
                petId?.let(petFilter::select)
                // 오늘의 이야기는 선택 사항: 저장에 실패해도 분석 결과는 보여주고 상세 화면에서 다시 남길 수 있다
                if (note.value.isNotBlank()) {
                    runCatching { analysisRepository.updateNote(result.id, note.value) }
                        .onSuccess { saved -> result = result.copy(note = saved) }
                        .onFailure { error.value = "이야기를 저장하지 못했어요. 기록 상세에서 다시 남겨 주세요." }
                }
                step.value = AnalyzeStep.Done(ready.image, result)
            } catch (e: AnalysisException) {
                step.value = ready
                error.value = e.message
            } catch (e: Exception) {
                step.value = ready
                error.value = "네트워크 상태를 확인하고 다시 시도해 주세요."
            }
        }
    }

    fun reset() {
        step.value = AnalyzeStep.Pick
        note.value = ""
        error.update { null }
    }

    private companion object {
        const val MAX_SIDE = 1280
    }
}
