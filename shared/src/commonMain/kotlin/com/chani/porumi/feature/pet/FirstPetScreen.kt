package com.chani.porumi.feature.pet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chani.porumi.domain.Species
import org.koin.compose.viewmodel.koinViewModel
import com.chani.porumi.ui.components.PorumiTextField
import com.chani.porumi.ui.components.PrimaryButton
import com.chani.porumi.ui.components.SegmentedControl
import kotlinx.datetime.LocalDate
import com.chani.porumi.ui.components.BirthDateField

/** 아이가 한 마리도 없을 때 보여주는 첫 등록 화면 */
@Composable
fun FirstPetScreen(viewModel: PetsViewModel = koinViewModel()) {
    var name by remember { mutableStateOf("") }
    var species by remember { mutableStateOf(Species.DOG) }
    var breed by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf<LocalDate?>(null) }
    val error by viewModel.error.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(species.emoji, fontSize = 56.sp)
        Text("우리 아이를 소개해 주세요", style = MaterialTheme.typography.headlineSmall)
        Text(
            "아이별로 표정을 기록하고 최근 7일 기분을 보여드려요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SegmentedControl(
            options = Species.entries.map { "${it.emoji} ${it.label}" },
            selectedIndex = Species.entries.indexOf(species),
            onSelect = { species = Species.entries[it] },
        )
        PorumiTextField(
            value = name,
            onValueChange = { if (it.length <= 30) name = it },
            placeholder = "이름",
            modifier = Modifier.fillMaxWidth(),
        )
        PorumiTextField(
            value = breed,
            onValueChange = { if (it.length <= 30) breed = it },
            placeholder = if (species == Species.CAT) "품종 (선택, 예: 코리안 숏헤어)" else "품종 (선택, 예: 골든 리트리버)",
            modifier = Modifier.fillMaxWidth(),
        )
        BirthDateField(value = birthDate, onValueChange = { birthDate = it }, modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        PrimaryButton(
            onClick = { viewModel.addPet(name, species, breed, birthDate) },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("시작하기") }
    }
}
