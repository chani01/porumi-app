package com.chani.porumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chani.porumi.domain.Analysis
import com.chani.porumi.domain.Pet
import com.chani.porumi.ui.theme.LocalPorumiColors
import com.chani.porumi.domain.breedOrSpecies

private val HEADER_HEIGHT = 80.dp

/** 아이별 가장 최근 사진 경로 (프로필 아바타로 사용) */
fun latestPhotoByPet(analyses: List<Analysis>): Map<String, String> =
    analyses.filter { it.petId != null }
        .groupBy { it.petId!! }
        .mapValues { (_, list) -> list.maxBy { it.takenAt }.imagePath }

@Composable
fun PetAvatar(pet: Pet, photoPath: String?, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (photoPath != null) {
            PetPhoto(photoPath, Modifier.fillMaxSize(), contentDescription = pet.name)
        } else {
            Text(pet.species.emoji, fontSize = (size.value * 0.5f).sp)
        }
    }
}

/**
 * 화면 상단의 아이 프로필 영역. 탭하면 드롭다운으로 다른 아이를 고르거나 새 아이를 추가할 수 있다.
 */
@Composable
fun PetProfileHeader(
    pets: List<Pet>,
    selected: Pet?,
    avatars: Map<String, String>,
    subtitle: String,
    onSelect: (String) -> Unit,
    onAddPet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected == null) {
        // 데이터 로딩 전에도 높이를 확보해야 리스트 스크롤 기준점이 아래 항목으로 밀리지 않음
        Spacer(modifier.height(HEADER_HEIGHT))
        return
    }
    var expanded by remember { mutableStateOf(false) }

    Box(modifier.padding(horizontal = 16.dp).heightIn(min = HEADER_HEIGHT)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { expanded = true }
                .padding(vertical = 8.dp, horizontal = 4.dp),
        ) {
            PetAvatar(
                pet = selected,
                photoPath = avatars[selected.id],
                size = 60.dp,
                modifier = Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape).padding(3.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(selected.name, style = MaterialTheme.typography.titleLarge)
                    Icon(
                        Icons.Rounded.ExpandMore,
                        contentDescription = "아이 선택",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            pets.forEach { pet ->
                DropdownMenuItem(
                    leadingIcon = { PetAvatar(pet, avatars[pet.id], 32.dp) },
                    text = {
                        Text(
                            "${pet.name}  ${pet.breedOrSpecies}",
                            fontWeight = if (pet.id == selected.id) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    trailingIcon = {
                        if (pet.id == selected.id) Icon(Icons.Rounded.Check, contentDescription = "선택됨", tint = MaterialTheme.colorScheme.primary)
                    },
                    onClick = {
                        expanded = false
                        onSelect(pet.id)
                    },
                )
            }
            HorizontalDivider(thickness = Hairline, color = LocalPorumiColors.current.separator)
            DropdownMenuItem(
                leadingIcon = {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                },
                text = { Text("아이 추가하기") },
                onClick = {
                    expanded = false
                    onAddPet()
                },
            )
        }
    }
}
