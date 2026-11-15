package com.chani.porumi.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chani.porumi.feature.legal.LegalDoc
import com.chani.porumi.feature.legal.LegalScreen
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material3.LocalContentColor
import com.chani.porumi.ui.components.ActivityIndicator
import com.chani.porumi.ui.components.CheckCircle
import com.chani.porumi.ui.components.PorumiTextField
import com.chani.porumi.ui.components.PlainButton
import com.chani.porumi.ui.components.PrimaryButton

@Composable
fun AuthScreen(viewModel: AuthViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // 로그인 전이라 NavHost 밖 → 약관은 화면 전환 대신 상태로 띄운다
    var openDoc by rememberSaveable { mutableStateOf<LegalDoc?>(null) }
    openDoc?.let { doc ->
        LegalScreen(doc, onBack = { openDoc = null })
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🐶🐱", fontSize = 56.sp)
        Text("Porumi", style = MaterialTheme.typography.headlineLarge)
        Text(
            "우리 아이 표정으로 읽는 하루 기분",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        PorumiTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            placeholder = "이메일",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        PorumiTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            placeholder = "비밀번호 (6자 이상)",
            secure = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.isSignUp) {
            TermsAgreement(
                checked = state.agreedToTerms,
                onCheckedChange = viewModel::onAgreeChange,
                onOpen = { openDoc = it },
            )
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        state.info?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
        PrimaryButton(
            onClick = viewModel::submit,
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.loading) ActivityIndicator(color = LocalContentColor.current, size = 20.dp)
            else Text(if (state.isSignUp) "회원가입" else "로그인")
        }
        PlainButton(onClick = viewModel::toggleMode, textStyle = MaterialTheme.typography.bodyMedium) {
            Text(if (state.isSignUp) "이미 계정이 있어요 · 로그인" else "처음이에요 · 회원가입")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TermsAgreement(checked: Boolean, onCheckedChange: (Boolean) -> Unit, onOpen: (LegalDoc) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CheckCircle(checked = checked, onCheckedChange = onCheckedChange)
            Text(
                "[필수] 만 14세 이상이며, 이용약관과 개인정보 처리방침(국외 이전 포함)에 동의해요",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        FlowRow(Modifier.padding(start = 40.dp)) {
            LegalDoc.entries.forEach { doc ->
                PlainButton(onClick = { onOpen(doc) }, textStyle = MaterialTheme.typography.labelMedium) {
                    Text("${doc.title} 보기")
                }
            }
        }
    }
}
