package com.chani.porumi.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.chani.porumi.ui.components.ActivityIndicator
import com.chani.porumi.ui.components.AlertAction
import com.chani.porumi.ui.components.AlertActionStyle
import com.chani.porumi.ui.components.CheckCircle
import com.chani.porumi.ui.components.PorumiAlertDialog
import com.chani.porumi.ui.components.PorumiTextField
import com.chani.porumi.ui.theme.Tints

private enum class AccountDialog { Password, SignOut, Delete }

/** 마이 > 내 계정 설정: 이메일, 비밀번호 변경, 로그아웃, 회원 탈퇴 */
@Composable
fun AccountScreen(onBack: () -> Unit, viewModel: AccountViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<AccountDialog?>(null) }

    SettingsPage(title = "내 계정 설정", onBack = onBack) {
        SettingsSection("계정 정보") {
            SettingsRow("이메일", Icons.Outlined.Email, onClick = null, subtitle = viewModel.email ?: "-", iconColor = Tints.Blue)
            SettingsRow("비밀번호 변경", Icons.Outlined.Lock, { dialog = AccountDialog.Password }, iconColor = Tints.Gray)
        }
        SettingsSection(null) {
            SettingsRow("로그아웃", Icons.AutoMirrored.Outlined.Logout, { dialog = AccountDialog.SignOut }, iconColor = Tints.Orange)
        }
        SettingsSection(null) {
            SettingsRow(
                "회원 탈퇴",
                Icons.Outlined.PersonRemove,
                { dialog = AccountDialog.Delete },
                titleColor = MaterialTheme.colorScheme.error,
                iconColor = Tints.Red,
            )
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.loading) ActivityIndicator(Modifier.align(Alignment.CenterHorizontally))
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }

    when (dialog) {
        AccountDialog.Password -> PasswordDialog(
            onDismiss = { dialog = null },
            onConfirm = {
                viewModel.changePassword(it)
                dialog = null
            },
        )
        AccountDialog.SignOut -> PorumiAlertDialog(
            onDismissRequest = { dialog = null },
            title = "로그아웃할까요?",
            actions = listOf(
                AlertAction("취소", AlertActionStyle.Cancel) { dialog = null },
                AlertAction("로그아웃", AlertActionStyle.Destructive) {
                    dialog = null
                    viewModel.signOut()
                },
            ),
        )
        AccountDialog.Delete -> DeleteAccountDialog(
            onDismiss = { dialog = null },
            onConfirm = {
                dialog = null
                viewModel.deleteAccount()
            },
        )
        null -> Unit
    }
}

@Composable
private fun PasswordDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val mismatch = confirm.isNotEmpty() && password != confirm
    PorumiAlertDialog(
        onDismissRequest = onDismiss,
        title = "비밀번호 변경",
        message = "새 비밀번호를 6자 이상 입력해 주세요.",
        actions = listOf(
            AlertAction("취소", AlertActionStyle.Cancel, onClick = onDismiss),
            AlertAction("변경", enabled = password.length >= 6 && password == confirm) { onConfirm(password) },
        ),
    ) {
        PorumiTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "새 비밀번호",
            secure = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        PorumiTextField(
            value = confirm,
            onValueChange = { confirm = it },
            placeholder = "새 비밀번호 확인",
            errorText = if (mismatch) "비밀번호가 일치하지 않아요" else null,
            secure = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
    }
}

@Composable
private fun DeleteAccountDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var agreed by remember { mutableStateOf(false) }
    PorumiAlertDialog(
        onDismissRequest = onDismiss,
        title = "정말 탈퇴할까요?",
        message = "탈퇴하면 아래 정보가 모두 삭제되고 되돌릴 수 없어요.",
        actions = listOf(
            AlertAction("취소", AlertActionStyle.Cancel, onClick = onDismiss),
            AlertAction("탈퇴하기", AlertActionStyle.Destructive, enabled = agreed, onClick = onConfirm),
        ),
    ) {
        Text(
            "• 등록한 반려동물 정보\n• 업로드한 모든 사진과 표정 분석 기록\n• 계정 정보",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            CheckCircle(checked = agreed, onCheckedChange = { agreed = it })
            Text("위 내용을 확인했어요", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
