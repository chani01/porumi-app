package com.chani.porumi.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.porumi.data.AuthRepository
import com.chani.porumi.data.NotificationPrefs
import com.chani.porumi.data.NotificationSettingsRepository
import com.chani.porumi.data.PetRepository
import com.chani.porumi.domain.Pet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyViewModel(
    authRepository: AuthRepository,
    petRepository: PetRepository,
) : ViewModel() {
    val email: String? = authRepository.currentEmail
    val pets: StateFlow<List<Pet>> = petRepository.pets
}

data class AccountUiState(
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class AccountViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val email: String? = authRepository.currentEmail

    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    fun changePassword(newPassword: String) {
        if (newPassword.length < 6) {
            _state.update { it.copy(error = "비밀번호는 6자 이상이어야 해요.") }
            return
        }
        run(success = "비밀번호를 변경했어요.", failure = "비밀번호를 변경하지 못했어요.") {
            authRepository.changePassword(newPassword)
        }
    }

    /** 로그아웃 → 루트(MainViewModel)가 로그인 화면으로 전환 */
    fun signOut() = run(success = null, failure = "로그아웃하지 못했어요.") { authRepository.signOut() }

    /** 회원 탈퇴 → 서버에서 사진·기록·계정 삭제 후 로그인 화면으로 전환 */
    fun deleteAccount() = run(success = null, failure = "탈퇴 처리 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.") {
        authRepository.deleteAccount()
    }

    fun clearMessage() = _state.update { it.copy(message = null, error = null) }

    private fun run(success: String?, failure: String, block: suspend () -> Unit) {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, message = null, error = null) }
        viewModelScope.launch {
            try {
                block()
                _state.update { it.copy(loading = false, message = success) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = failure) }
            }
        }
    }
}

class NotificationSettingsViewModel(private val repository: NotificationSettingsRepository) : ViewModel() {
    val prefs: StateFlow<NotificationPrefs> = repository.prefs
    fun update(transform: (NotificationPrefs) -> NotificationPrefs) = repository.update(transform)
}
