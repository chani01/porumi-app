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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import com.chani.porumi.domain.profileLine
import com.chani.porumi.ui.components.BirthDateField

/** 마이 > 함께하는 친구들: 추가 / 이름·종 수정 / 삭제 (최소 한 마리 유지) */
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
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    SettingsPage(title = "함께하는 친구들", onBack = onBack) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            pets.forEach { pet ->
                ClickableCard(onClick = { editing = pet }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PetAvatar(pet, avatars[pet.id], 48.dp)
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(pet.name, style = MaterialTheme.typography.titleMedium)
                            Text(pet.profileLine(today), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    "아이는 최소 한 마리 등록돼 있어야 해요.",
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
            onConfirm = { name, species, breed, birthDate ->
                viewModel.addPet(name, species, breed, birthDate)
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
            initialBreed = pet.breed.orEmpty(),
            initialBirthDate = pet.birthDate,
            onDismiss = { editing = null },
            onConfirm = { name, species, breed, birthDate ->
                viewModel.updatePet(pet.id, name, species, breed, birthDate)
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
    onConfirm: (name: String, species: Species, breed: String?, birthDate: LocalDate?) -> Unit,
    initialName: String = "",
    initialSpecies: Species = Species.DOG,
    initialBreed: String = "",
    initialBirthDate: LocalDate? = null,
) {
    var name by remember { mutableStateOf(initialName) }
    var species by remember { mutableStateOf(initialSpecies) }
    var breed by remember { mutableStateOf(initialBreed) }
    var birthDate by remember { mutableStateOf(initialBirthDate) }
    PorumiAlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        actions = listOf(
            AlertAction("취소", AlertActionStyle.Cancel, onClick = onDismiss),
            AlertAction(confirmLabel, enabled = name.isNotBlank()) { onConfirm(name, species, breed, birthDate) },
        ),
    ) {
        SegmentedControl(
            options = Species.entries.map { it.label },
            selectedIndex = Species.entries.indexOf(species),
            onSelect = { species = Species.entries[it] },
        )
        PorumiTextField(
            value = name,
            onValueChange = { if (it.length <= 30) name = it },
            placeholder = "이름",
        )
        PorumiTextField(
            value = breed,
            onValueChange = { if (it.length <= 30) breed = it },
            placeholder = if (species == Species.CAT) "품종 (예: 코리안 숏헤어)" else "품종 (예: 골든 리트리버)",
        )
        BirthDateField(value = birthDate, onValueChange = { birthDate = it })
    }
}
