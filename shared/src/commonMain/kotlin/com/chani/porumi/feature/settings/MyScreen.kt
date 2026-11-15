package com.chani.porumi.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chani.porumi.feature.legal.LegalDoc
import com.chani.porumi.ui.components.ClickableCard
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MyScreen(
    onAccount: () -> Unit,
    onPets: () -> Unit,
    onNotifications: () -> Unit,
    onSupport: () -> Unit,
    onLicenses: () -> Unit,
    onOpenLegal: (LegalDoc) -> Unit,
    viewModel: MyViewModel = koinViewModel(),
) {
    val pets by viewModel.pets.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
        Text(
            "마이",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(12.dp))

        // 계정 카드
        ClickableCard(onClick = onAccount, modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(viewModel.email ?: "내 계정", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("내 계정 설정", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        SettingsSection("반려동물") {
            SettingsRow("반려동물 관리", Icons.Outlined.Pets, onPets, trailingText = "${pets.size}명")
        }
        SettingsSection("앱 설정") {
            SettingsRow("알림 설정", Icons.Outlined.Notifications, onNotifications)
        }
        SettingsSection("지원") {
            SettingsRow("고객센터", Icons.AutoMirrored.Outlined.HelpOutline, onSupport)
            SettingsRow(LegalDoc.TERMS.title, Icons.Outlined.Description, { onOpenLegal(LegalDoc.TERMS) })
            SettingsRow(LegalDoc.PRIVACY.title, Icons.Outlined.PrivacyTip, { onOpenLegal(LegalDoc.PRIVACY) })
            SettingsRow("오픈소스 라이선스", Icons.Outlined.Code, onLicenses)
            SettingsRow("앱 버전", Icons.Outlined.Info, onClick = null, trailingText = AppInfo.VERSION)
        }
    }

}
