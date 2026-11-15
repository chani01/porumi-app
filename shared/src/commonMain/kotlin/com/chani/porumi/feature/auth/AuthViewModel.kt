package com.chani.porumi.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.porumi.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.chani.porumi.data.SocialLogin

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    /** 회원가입 시 비밀번호 확인 */
    val passwordConfirm: String = "",
    val isSignUp: Boolean = false,
    /** 회원가입 시 이용약관·개인정보 처리방침 필수 동의 */
    val agreedToTerms: Boolean = false,
    val loading: Boolean = false,
    /** 진행 중인 SNS 로그인 (해당 버튼에 로딩 표시) */
    val socialLoading: SocialLogin? = null,
    /** "이메일로 계속하기"를 눌러 이메일 입력 화면을 보는 중 */
    val showEmail: Boolean = false,
    val error: String? = null,
    val info: String? = null,
) {
    val passwordMismatch: Boolean get() = isSignUp && passwordConfirm.isNotEmpty() && passwordConfirm != password

    val canSubmit: Boolean get() = email.contains("@") && password.length >= 6 && !loading &&
        (!isSignUp || (agreedToTerms && passwordConfirm == password))
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onEmailChange(v: String) = _state.update { it.copy(email = v.trim(), error = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onPasswordConfirmChange(v: String) = _state.update { it.copy(passwordConfirm = v, error = null) }
    fun onAgreeChange(v: Boolean) = _state.update { it.copy(agreedToTerms = v) }
    fun setSignUp(signUp: Boolean) = _state.update {
        if (it.isSignUp == signUp) it else it.copy(isSignUp = signUp, passwordConfirm = "", error = null, info = null)
    }
    fun showEmail(show: Boolean) = _state.update { it.copy(showEmail = show, error = null, info = null) }

    fun signInWithSocial(social: SocialLogin) {
        if (_state.value.socialLoading != null) return
        _state.update { it.copy(socialLoading = social, error = null, info = null) }
        viewModelScope.launch {
            try {
                authRepository.signInWithSocial(social)
            } catch (e: Exception) {
                _state.update { it.copy(error = "${social.label.removeSuffix("로 시작하기")} 로그인을 시작하지 못했어요. 잠시 후 다시 시도해 주세요.") }
            } finally {
                _state.update { it.copy(socialLoading = null) }
            }
        }
    }

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
