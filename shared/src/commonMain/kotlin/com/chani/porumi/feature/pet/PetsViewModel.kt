package com.chani.porumi.feature.pet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.porumi.data.AnalysisRepository
import com.chani.porumi.data.PetRepository
import com.chani.porumi.domain.Pet
import com.chani.porumi.domain.Species
import com.chani.porumi.ui.components.latestPhotoByPet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PetsViewModel(
    private val petRepository: PetRepository,
    analysisRepository: AnalysisRepository,
) : ViewModel() {

    val pets: StateFlow<List<Pet>> = petRepository.pets

    val avatars: StateFlow<Map<String, String>> = analysisRepository.analyses
        .map(::latestPhotoByPet)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun addPet(name: String, species: Species) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { petRepository.add(name, species) }
                .onFailure { _error.value = "아이를 추가하지 못했어요." }
        }
    }

    fun updatePet(id: String, name: String, species: Species) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { petRepository.update(id, name, species) }
                .onFailure { _error.value = "수정하지 못했어요." }
        }
    }

    fun deletePet(id: String) {
        if (petRepository.pets.value.size <= 1) return // 최소 한 명 유지
        viewModelScope.launch {
            runCatching { petRepository.delete(id) }
                .onFailure { _error.value = "삭제하지 못했어요." }
        }
    }

    fun dismissError() {
        _error.value = null
    }
}
