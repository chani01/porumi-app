package com.chani.porumi.feature.analyze

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.chani.porumi.domain.Pet
import com.chani.porumi.ui.components.ActivityIndicator
import com.chani.porumi.ui.components.AnalysisResultContent
import com.chani.porumi.ui.components.ClickableCard
import com.chani.porumi.ui.components.PetAvatar
import com.chani.porumi.ui.components.PrimaryButton
import com.chani.porumi.ui.components.StoryEditor
import com.chani.porumi.ui.components.StoryPromptChips
import com.chani.porumi.ui.components.TintedButton
import com.chani.porumi.ui.components.korean
import com.chani.porumi.ui.components.monthDay
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberCameraPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Pets
import com.chani.porumi.ui.components.IconBubble

/**
 * 기록 탭: 아이 고르기 → 사진 담기 → (선택) 오늘의 이야기 → 표정 분석.
 * 이야기는 분석이 끝난 뒤 같은 기록에 저장된다.
 */
@Composable
fun AnalyzeScreen(
    onGoHome: () -> Unit,
    onAddPet: () -> Unit,
    viewModel: AnalyzeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val galleryLauncher = rememberFilePickerLauncher(type = FileKitType.Image) { viewModel.onFilePicked(it) }
    val cameraLauncher = rememberCameraPickerLauncher { viewModel.onFilePicked(it) }
    val step = state.step
    val petName = state.selectedPet?.name

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Header()

        if (step !is AnalyzeStep.Done) {
            Section("누구의 순간인가요?") {
                PetPicker(
                    pets = state.pets,
                    avatars = state.avatars,
                    selectedId = state.selectedPetId,
                    enabled = step !is AnalyzeStep.Analyzing,
                    onSelect = viewModel::selectPet,
                    onAddPet = onAddPet,
                )
            }
        }

        when (step) {
            AnalyzeStep.Pick -> PhotoDropCard(
                loading = false,
                onGallery = galleryLauncher::launch,
                onCamera = { cameraLauncher.launch() },
            )
            AnalyzeStep.Preparing -> PhotoDropCard(loading = true, onGallery = {}, onCamera = {})
            is AnalyzeStep.Ready -> Preview(step.image, analyzing = false, onRepick = viewModel::reset)
            is AnalyzeStep.Analyzing -> Preview(step.image, analyzing = true, onRepick = null)
            is AnalyzeStep.Done -> Preview(step.image, analyzing = false, onRepick = null)
        }

        if (step is AnalyzeStep.Done) {
            AnalysisResultContent(
                analysis = step.analysis,
                petName = state.pets.firstOrNull { it.id == step.analysis.petId }?.name,
            )
            step.analysis.note?.let { SavedStoryCard(it) }
            state.error?.let { ErrorText(it) }
            PrimaryButton(onClick = onGoHome, modifier = Modifier.fillMaxWidth()) { Text("피드에서 보기") }
            TintedButton(onClick = viewModel::reset, modifier = Modifier.fillMaxWidth()) { Text("다른 순간 기록하기") }
        } else {
            StoryCard(
                note = state.note,
                petName = petName,
                enabled = step !is AnalyzeStep.Analyzing,
                onChange = viewModel::onNoteChange,
                onPrompt = viewModel::appendToNote,
            )
            state.error?.let { ErrorText(it) }
            when (step) {
                is AnalyzeStep.Ready -> PrimaryButton(
                    onClick = viewModel::analyze,
                    enabled = state.selectedPetId != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.note.isBlank()) "표정 분석하기" else "표정 분석하고 이야기 저장하기") }
                is AnalyzeStep.Analyzing -> Text(
                    "표정을 읽고 있어요… 귀, 눈, 입 모양을 살펴보는 중이에요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                )
                else -> TipsCard()
            }
        }
    }
}

@Composable
private fun Header() {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "${today.monthDay()} ${today.dayOfWeek.korean}요일",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text("오늘의 순간을\n남겨볼까요?", style = MaterialTheme.typography.headlineLarge)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

/** 아이 프로필 사진으로 고르기. 선택된 아이는 복숭아색 링 */
@Composable
private fun PetPicker(
    pets: List<Pet>,
    avatars: Map<String, String>,
    selectedId: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    onAddPet: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        pets.forEach { pet ->
            val selected = pet.id == selectedId
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = enabled) { onSelect(pet.id) }
                    .padding(vertical = 4.dp),
            ) {
                PetAvatar(
                    pet,
                    avatars[pet.id],
                    56.dp,
                    modifier = Modifier
                        .border(2.5.dp, if (selected) colors.primary else Color.Transparent, CircleShape)
                        .padding(4.dp),
                )
                Text(
                    pet.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) colors.primary else colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onAddPet)
                .padding(vertical = 4.dp),
        ) {
            Box(
                Modifier
                    .padding(4.dp)
                    .size(56.dp)
                    .border(1.5.dp, colors.outline, CircleShape)
                    .background(colors.surfaceContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.primary)
            }
            Text("추가", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** 사진을 담기 전: 누르면 앨범이 열리는 큰 카드 + 앨범/카메라 버튼 */
@Composable
private fun PhotoDropCard(loading: Boolean, onGallery: () -> Unit, onCamera: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(28.dp))
                .background(colors.primaryContainer)
                .clickable(enabled = !loading, onClick = onGallery),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                ActivityIndicator(size = 32.dp)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconBubble(Icons.Rounded.AddAPhoto, size = 84.dp, background = colors.surfaceContainer)
                    Text("오늘의 표정을 담아 주세요", style = MaterialTheme.typography.titleMedium, color = colors.onPrimaryContainer)
                    Text(
                        "눌러서 앨범에서 고를 수 있어요",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(onClick = onGallery, enabled = !loading, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("앨범")
            }
            TintedButton(onClick = onCamera, enabled = !loading, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("카메라")
            }
        }
    }
}

@Composable
private fun Preview(image: ByteArray, analyzing: Boolean, onRepick: (() -> Unit)?) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AsyncImage(
            model = image,
            contentDescription = "선택한 사진",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (analyzing) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                ActivityIndicator(color = Color.White, size = 32.dp)
            }
        }
        if (onRepick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f))
                    .clickable(onClick = onRepick)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color(0xFF4A3428), modifier = Modifier.size(16.dp))
                Text("다시 고르기", style = MaterialTheme.typography.labelMedium, color = Color(0xFF4A3428))
            }
        }
    }
}

/** 오늘의 이야기 (선택) */
@Composable
private fun StoryCard(note: String, petName: String?, enabled: Boolean, onChange: (String) -> Unit, onPrompt: (String) -> Unit) {
    ClickableCard(onClick = null) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("오늘의 이야기", style = MaterialTheme.typography.titleMedium)
                Text(
                    "선택",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            StoryPromptChips(onPick = onPrompt, enabled = enabled)
            StoryEditor(
                value = note,
                onValueChange = onChange,
                placeholder = "오늘 ${petName ?: "우리 아이"}의 하루는 어땠나요?\n산책, 간식, 놀이… 무엇이든 좋아요",
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun SavedStoryCard(note: String) {
    ClickableCard(onClick = null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("오늘의 이야기", style = MaterialTheme.typography.titleMedium)
            Text(note, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** 사진 팁: 정면 얼굴 · 밝은 곳 · 한 마리만 */
@Composable
private fun TipsCard() {
    ClickableCard(onClick = null) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("이런 사진이 잘 읽혀요", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth()) {
                listOf(
                    Triple(Icons.Rounded.Face, "정면 얼굴", "눈·귀·입이 보이게"),
                    Triple(Icons.Rounded.WbSunny, "밝은 곳", "그림자 없이 환하게"),
                    Triple(Icons.Rounded.Pets, "한 마리만", "한 장에 한 아이"),
                ).forEach { (icon, title, desc) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        IconBubble(icon, size = 52.dp)
                        Spacer(Modifier.height(2.dp))
                        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
}
