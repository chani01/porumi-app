package com.chani.porumi.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.porumi.data.AnalysisRepository
import com.chani.porumi.data.PetFilter
import com.chani.porumi.data.PetRepository
import com.chani.porumi.domain.Pet
import com.chani.porumi.domain.WeeklyMood
import com.chani.porumi.domain.WeeklyMoodCalculator
import com.chani.porumi.ui.components.latestPhotoByPet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class ReportUiState(
    val pets: List<Pet> = emptyList(),
    val selectedPet: Pet? = null,
    val avatars: Map<String, String> = emptyMap(),
    val mood: WeeklyMood? = null,
    val comment: String = "",
    /** 0 = 최근 7일, 1 = 그 전 7일 … */
    val periodsAgo: Int = 0,
)

class ReportViewModel(
    analysisRepository: AnalysisRepository,
    petRepository: PetRepository,
    private val petFilter: PetFilter,
) : ViewModel() {

    private val tz = TimeZone.currentSystemDefault()
    private val periodsAgo = MutableStateFlow(0)

    val state: StateFlow<ReportUiState> = combine(
        analysisRepository.analyses,
        petRepository.pets,
        petFilter.selectedPet,
        periodsAgo,
    ) { analyses, pets, selected, ago ->
        val today = Clock.System.todayIn(tz)
        val filtered = analyses.filter { it.petId == selected?.id }
        val mood = WeeklyMoodCalculator.calculate(filtered, WeeklyMoodCalculator.recentStartOf(today, ago), tz)
        ReportUiState(
            pets = pets,
            selectedPet = selected,
            avatars = latestPhotoByPet(analyses),
            mood = mood,
            comment = WeeklyMoodCalculator.comment(mood, selected?.name, if (ago == 0) "최근 7일" else "이 기간"),
            periodsAgo = ago,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())

    fun selectPet(petId: String) = petFilter.select(petId)

    fun previous() = periodsAgo.update { it + 1 }

    fun next() = periodsAgo.update { (it - 1).coerceAtLeast(0) }
}
