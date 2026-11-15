package com.chani.porumi.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.porumi.data.AnalysisRepository
import com.chani.porumi.data.PetRepository
import com.chani.porumi.domain.Analysis
import com.chani.porumi.domain.Pet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DetailViewModel(
    private val analysisRepository: AnalysisRepository,
    private val petRepository: PetRepository,
) : ViewModel() {

    fun observe(id: String): Flow<Pair<Analysis?, Pet?>> =
        combine(analysisRepository.analyses, petRepository.pets) { analyses, pets ->
            val analysis = analyses.firstOrNull { it.id == id }
            analysis to pets.firstOrNull { it.id == analysis?.petId }
        }

    private val _noteError = MutableStateFlow<String?>(null)
    val noteError: StateFlow<String?> = _noteError.asStateFlow()

    fun updateNote(analysis: Analysis, note: String) {
        _noteError.value = null
        viewModelScope.launch {
            runCatching { analysisRepository.updateNote(analysis.id, note) }
                .onFailure { _noteError.value = "이야기를 저장하지 못했어요. 다시 시도해 주세요." }
        }
    }

    fun delete(analysis: Analysis, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { analysisRepository.delete(analysis) }
            onDone()
        }
    }
}
