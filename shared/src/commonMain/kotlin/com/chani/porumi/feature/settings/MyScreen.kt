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
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import com.chani.porumi.ui.theme.LocalPorumiColors
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Add
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.chani.porumi.domain.Pet
import com.chani.porumi.ui.components.PetAvatar
import com.chani.porumi.ui.components.TabHeader
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.chani.porumi.ui.components.IconBubble

@Composable
fun MyScreen(
    onAccount: () -> Unit,
    onPets: () -> Unit,
    onAddPet: () -> Unit,
    onNotifications: () -> Unit,
    onSupport: () -> Unit,
    onLicenses: () -> Unit,
    onOpenLegal: (LegalDoc) -> Unit,
    viewModel: MyViewModel = koinViewModel(),
) {
    val pets by viewModel.pets.collectAsStateWithLifecycle()
    val avatars by viewModel.avatars.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
        TabHeader(
            eyebrow = "내 정보",
            title = "반가워요,\n${guardianOf(pets)}",
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
                    Text(viewModel.email ?: "내 계정", style = MaterialTheme.typography.titleMedium)
                    Text("내 계정 설정", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = LocalPorumiColors.current.tertiaryLabel,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        FriendsCard(
            pets = pets,
            avatars = avatars,
            onManage = onPets,
            onAdd = onAddPet,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        // 자주 쓰는 두 가지는 위 카드들과 같은 말랑한 타일로
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickTile(Icons.Rounded.Notifications, "알림 설정", "리마인더 · 리포트", onNotifications, Modifier.weight(1f))
            QuickTile(Icons.Rounded.SupportAgent, "고객센터", "자주 묻는 질문 · 문의", onSupport, Modifier.weight(1f))
        }

        // 약관·정보는 아이콘 없이 조용하게
        SettingsSection(null) {
            SettingsRow(LegalDoc.TERMS.title, icon = null, onClick = { onOpenLegal(LegalDoc.TERMS) })
            SettingsRow(LegalDoc.PRIVACY.title, icon = null, onClick = { onOpenLegal(LegalDoc.PRIVACY) })
            SettingsRow("오픈소스 라이선스", icon = null, onClick = onLicenses)
        }

        Text(
            "Porumi v${AppInfo.VERSION}\n우리 아이 표정으로 읽는 하루 기분",
            style = MaterialTheme.typography.labelSmall,
            color = LocalPorumiColors.current.tertiaryLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        )
    }
}

/** 바로가기 타일: 복숭아 아이콘 동그라미 + 제목 + 짧은 설명 */
@Composable
private fun QuickTile(icon: ImageVector, title: String, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ClickableCard(onClick = onClick, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBubble(icon, size = 44.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** "모모 보호자님" / "모모·딸기 보호자님" / "모모 외 2마리 보호자님" */
private fun guardianOf(pets: List<Pet>): String = when {
    pets.isEmpty() -> "보호자님"
    pets.size <= 2 -> pets.joinToString("·") { it.name } + " 보호자님"
    else -> "${pets.first().name} 외 ${pets.size - 1}마리 보호자님"
}

/** 함께하는 친구들: 등록한 아이들 프로필을 가로로 보여주고, 누르면 관리 화면으로 */
@Composable
private fun FriendsCard(
    pets: List<Pet>,
    avatars: Map<String, String>,
    onManage: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ClickableCard(onClick = onManage, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("함께하는 친구들", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (pets.isEmpty()) "아직 등록한 친구가 없어요" else "${pets.size}마리와 함께하고 있어요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = LocalPorumiColors.current.tertiaryLabel,
                    modifier = Modifier.size(14.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                pets.forEach { pet ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                        PetAvatar(pet, avatars[pet.id], 56.dp)
                        Text(
                            pet.name,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(60.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onAdd),
                ) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        "추가",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}
