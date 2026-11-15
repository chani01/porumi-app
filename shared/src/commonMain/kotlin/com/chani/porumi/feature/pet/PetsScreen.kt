package com.chani.porumi.feature.pet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chani.porumi.domain.Pet
import com.chani.porumi.domain.Species
import com.chani.porumi.feature.settings.SettingsPage
import com.chani.porumi.ui.components.ClickableCard
import com.chani.porumi.ui.components.PetAvatar
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.size
import com.chani.porumi.ui.components.AlertAction
import com.chani.porumi.ui.components.AlertActionStyle
import com.chani.porumi.ui.components.PorumiAlertDialog
import com.chani.porumi.ui.components.PorumiTextField
import com.chani.porumi.ui.components.PlainIconButton
import com.chani.porumi.ui.components.PrimaryButton
import com.chani.porumi.ui.components.SegmentedControl

/** 마이 > 반려동물 관리: 추가 / 이름·종 수정 / 삭제 (최소 한 명 유지) */
@Composable
fun PetManageScreen(
    onBack: () -> Unit,
    openAddDialog: Boolean = false,
    viewModel: PetsViewModel = koinViewModel(),
) {
    val pets by viewModel.pets.collectAsStateWithLifecycle()
    val avatars by viewModel.avatars.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(openAddDialog) }
    var editing by remember { mutableStateOf<Pet?>(null) }
    var deleting by remember { mutableStateOf<Pet?>(null) }

    SettingsPage(title = "반려동물 관리", onBack = onBack) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            pets.forEach { pet ->
                ClickableCard(onClick = { editing = pet }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PetAvatar(pet, avatars[pet.id], 48.dp)
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(pet.name, style = MaterialTheme.typography.titleMedium)
                            Text(pet.species.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        PlainIconButton(Icons.Outlined.Edit, contentDescription = "수정", onClick = { editing = pet })
                        if (pets.size > 1) {
                            PlainIconButton(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "삭제",
                                onClick = { deleting = pet },
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
            if (pets.size == 1) {
                Text(
                    "아이는 최소 한 명 등록돼 있어야 해요.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PrimaryButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("아이 추가")
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }

    if (showAdd) {
        PetEditDialog(
            title = "아이 추가",
            confirmLabel = "등록",
            onDismiss = { showAdd = false },
            onConfirm = { name, species ->
                viewModel.addPet(name, species)
                showAdd = false
            },
        )
    }

    editing?.let { pet ->
        PetEditDialog(
            title = "정보 수정",
            confirmLabel = "저장",
            initialName = pet.name,
            initialSpecies = pet.species,
            onDismiss = { editing = null },
            onConfirm = { name, species ->
                viewModel.updatePet(pet.id, name, species)
                editing = null
            },
        )
    }

    deleting?.let { pet ->
        PorumiAlertDialog(
            onDismissRequest = { deleting = null },
            title = "${pet.name}을(를) 삭제할까요?",
            message = "지금까지의 분석 기록은 남지만, 아이별 모아보기에서는 빠져요.",
            actions = listOf(
                AlertAction("취소", AlertActionStyle.Cancel) { deleting = null },
                AlertAction("삭제", AlertActionStyle.Destructive) {
                    viewModel.deletePet(pet.id)
                    deleting = null
                },
            ),
        )
    }
}

@Composable
fun PetEditDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Species) -> Unit,
    initialName: String = "",
    initialSpecies: Species = Species.DOG,
) {
    var name by remember { mutableStateOf(initialName) }
    var species by remember { mutableStateOf(initialSpecies) }
    PorumiAlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        actions = listOf(
            AlertAction("취소", AlertActionStyle.Cancel, onClick = onDismiss),
            AlertAction(confirmLabel, enabled = name.isNotBlank()) { onConfirm(name, species) },
        ),
    ) {
        SegmentedControl(
            options = Species.entries.map { "${it.emoji} ${it.label}" },
            selectedIndex = Species.entries.indexOf(species),
            onSelect = { species = Species.entries[it] },
        )
        PorumiTextField(
            value = name,
            onValueChange = { if (it.length <= 30) name = it },
            placeholder = "이름",
        )
    }
}
