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
    val selectedPetId: String? = null,
    val error: String? = null,
)

class AnalyzeViewModel(
    private val analysisRepository: AnalysisRepository,
    petRepository: PetRepository,
    private val petFilter: PetFilter,
) : ViewModel() {

    private val step = MutableStateFlow<AnalyzeStep>(AnalyzeStep.Pick)
    // 직접 고르지 않았으면 홈에서 선택돼 있던 아이가 기본값
    private val selectedPetId = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<AnalyzeUiState> = combine(
        step, petRepository.pets, selectedPetId, petFilter.selectedPet, error,
    ) { step, pets, selected, current, error ->
        AnalyzeUiState(
            step = step,
            pets = pets,
            selectedPetId = selected?.takeIf { id -> pets.any { it.id == id } } ?: current?.id,
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

    fun analyze() {
        val ready = step.value as? AnalyzeStep.Ready ?: return
        error.value = null
        step.value = AnalyzeStep.Analyzing(ready.image)
        viewModelScope.launch {
            try {
                val petId = state.value.selectedPetId
                val result = analysisRepository.analyze(ready.image, petId)
                // 분석한 아이를 홈/리포트에서도 선택 상태로
                petId?.let(petFilter::select)
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
        error.update { null }
    }

    private companion object {
        const val MAX_SIDE = 1280
    }
}
