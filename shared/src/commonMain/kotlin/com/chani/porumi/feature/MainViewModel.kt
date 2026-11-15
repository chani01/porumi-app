package com.chani.porumi.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.porumi.data.AnalysisRepository
import com.chani.porumi.data.AuthRepository
import com.chani.porumi.data.AuthState
import com.chani.porumi.data.PetFilter
import com.chani.porumi.data.PetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.chani.porumi.data.NotificationRepository

enum class AppState {
    Loading,
    SignedOut,
    /** 로그인은 됐지만 아이가 한 마리도 없음 → 첫 아이 등록 화면 */
    NeedsFirstPet,
    /** 펫 목록을 불러오지 못함 (네트워크 등) */
    LoadFailed,
    Ready,
}

/** 로그인 상태와 아이 등록 여부에 따라 루트 화면을 결정하는 ViewModel */
class MainViewModel(
    authRepository: AuthRepository,
    private val petRepository: PetRepository,
    private val analysisRepository: AnalysisRepository,
    private val notificationRepository: NotificationRepository,
    private val petFilter: PetFilter,
) : ViewModel() {

    private val loadFailed = MutableStateFlow(false)

    private val authState = authRepository.authState
        .distinctUntilChanged()
        .onEach { state ->
            when (state) {
                AuthState.SignedIn -> load()
                AuthState.SignedOut -> {
                    petRepository.clear()
                    analysisRepository.clear()
                    notificationRepository.clear()
                    petFilter.clear()
                }
                AuthState.Loading -> Unit
            }
        }

    val appState: StateFlow<AppState> = combine(
        authState, petRepository.pets, petRepository.loaded, loadFailed,
    ) { auth, pets, loaded, failed ->
        when (auth) {
            AuthState.Loading -> AppState.Loading
            AuthState.SignedOut -> AppState.SignedOut
            AuthState.SignedIn -> when {
                pets.isNotEmpty() -> AppState.Ready
                failed -> AppState.LoadFailed
                !loaded -> AppState.Loading
                else -> AppState.NeedsFirstPet
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppState.Loading)

    fun load() {
        loadFailed.value = false
        viewModelScope.launch {
            runCatching { petRepository.refresh() }.onFailure { loadFailed.value = true }
            runCatching { analysisRepository.refresh() }
            runCatching { notificationRepository.refresh() }
        }
    }
}
