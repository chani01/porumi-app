package com.chani.porumi.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.porumi.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isSignUp: Boolean = false,
    /** 회원가입 시 이용약관·개인정보 처리방침 필수 동의 */
    val agreedToTerms: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
) {
    val canSubmit: Boolean get() = email.contains("@") && password.length >= 6 && !loading &&
        (!isSignUp || agreedToTerms)
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onEmailChange(v: String) = _state.update { it.copy(email = v.trim(), error = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onAgreeChange(v: Boolean) = _state.update { it.copy(agreedToTerms = v) }
    fun toggleMode() = _state.update { it.copy(isSignUp = !it.isSignUp, error = null, info = null) }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { it.copy(loading = true, error = null, info = null) }
        viewModelScope.launch {
            try {
                if (s.isSignUp) {
                    val signedIn = authRepository.signUp(s.email, s.password)
                    if (!signedIn) {
                        _state.update {
                            it.copy(isSignUp = false, info = "인증 메일을 보냈어요. 메일의 링크를 누른 뒤 로그인해 주세요.")
                        }
                    }
                } else {
                    authRepository.signIn(s.email, s.password)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = if (s.isSignUp) "회원가입에 실패했어요. 이미 가입된 이메일인지 확인해 주세요." else "이메일 또는 비밀번호를 확인해 주세요.")
                }
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }
}
