package com.chani.porumi.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.porumi.data.AnalysisRepository
import com.chani.porumi.data.PetFilter
import com.chani.porumi.data.PetRepository
import com.chani.porumi.domain.Analysis
import com.chani.porumi.domain.Pet
import com.chani.porumi.domain.WeeklyMood
import com.chani.porumi.domain.WeeklyMoodCalculator
import com.chani.porumi.ui.components.latestPhotoByPet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class HomeUiState(
    val pets: List<Pet> = emptyList(),
    val selectedPet: Pet? = null,
    val avatars: Map<String, String> = emptyMap(),
    val photos: List<Analysis> = emptyList(),
    /** 오늘을 마지막 날로 하는 최근 7일 */
    val recent: WeeklyMood? = null,
    val today: LocalDate? = null,
    val refreshing: Boolean = false,
)

class HomeViewModel(
    private val analysisRepository: AnalysisRepository,
    private val petRepository: PetRepository,
    private val petFilter: PetFilter,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)

    val state: StateFlow<HomeUiState> = combine(
        analysisRepository.analyses,
        petRepository.pets,
        petFilter.selectedPet,
        refreshing,
    ) { analyses, pets, selected, isRefreshing ->
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(tz)
        val photos = analyses.filter { it.petId == selected?.id }
        HomeUiState(
            pets = pets,
            selectedPet = selected,
            avatars = latestPhotoByPet(analyses),
            photos = photos,
            recent = WeeklyMoodCalculator.calculate(photos, WeeklyMoodCalculator.recentStartOf(today), tz),
            today = today,
            refreshing = isRefreshing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectPet(petId: String) = petFilter.select(petId)

    fun refresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            runCatching { petRepository.refresh() }
            runCatching { analysisRepository.refresh() }
            refreshing.value = false
        }
    }
}
