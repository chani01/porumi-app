package com.chani.porumi.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

private enum class AccountDialog { Password, SignOut, Delete }

/** 마이 > 내 계정 설정: 이메일, 비밀번호 변경, 로그아웃, 회원 탈퇴 */
@Composable
fun AccountScreen(onBack: () -> Unit, viewModel: AccountViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<AccountDialog?>(null) }

    SettingsPage(title = "내 계정 설정", onBack = onBack) {
        SettingsSection("계정 정보") {
            SettingsRow("이메일", Icons.Outlined.Email, onClick = null, subtitle = viewModel.email ?: "-")
            SettingsRow("비밀번호 변경", Icons.Outlined.Lock, { dialog = AccountDialog.Password })
        }
        SettingsSection(null) {
            SettingsRow("로그아웃", Icons.AutoMirrored.Outlined.Logout, { dialog = AccountDialog.SignOut })
        }
        SettingsSection(null) {
            SettingsRow(
                "회원 탈퇴",
                Icons.Outlined.PersonRemove,
                { dialog = AccountDialog.Delete },
                titleColor = MaterialTheme.colorScheme.error,
            )
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
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
        AccountDialog.SignOut -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("로그아웃할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    viewModel.signOut()
                }) { Text("로그아웃") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("취소") } },
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("비밀번호 변경") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("새 비밀번호 (6자 이상)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text("새 비밀번호 확인") },
                    singleLine = true,
                    isError = mismatch,
                    supportingText = if (mismatch) ({ Text("비밀번호가 일치하지 않아요") }) else null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                enabled = password.length >= 6 && password == confirm,
            ) { Text("변경") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun DeleteAccountDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var agreed by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("정말 탈퇴할까요?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("탈퇴하면 아래 정보가 모두 삭제되고 되돌릴 수 없어요.")
                Text(
                    "• 등록한 반려동물 정보\n• 업로드한 모든 사진과 표정 분석 기록\n• 계정 정보",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = agreed, onCheckedChange = { agreed = it })
                    Text("위 내용을 확인했어요")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = agreed) {
                Text("탈퇴하기", color = if (agreed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
