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

class DetailViewModel(
    private val analysisRepository: AnalysisRepository,
    private val petRepository: PetRepository,
) : ViewModel() {

    fun observe(id: String): Flow<Pair<Analysis?, Pet?>> =
        combine(analysisRepository.analyses, petRepository.pets) { analyses, pets ->
            val analysis = analyses.firstOrNull { it.id == id }
            analysis to pets.firstOrNull { it.id == analysis?.petId }
        }

    fun delete(analysis: Analysis, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { analysisRepository.delete(analysis) }
            onDone()
        }
    }
}
