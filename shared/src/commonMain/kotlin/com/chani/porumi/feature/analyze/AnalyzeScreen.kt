package com.chani.porumi.feature.analyze

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.chani.porumi.domain.Pet
import com.chani.porumi.ui.components.AnalysisResultContent
import com.chani.porumi.ui.components.ClickableCard
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberCameraPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AnalyzeScreen(
    onGoHome: () -> Unit,
    onAddPet: () -> Unit,
    viewModel: AnalyzeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val galleryLauncher = rememberFilePickerLauncher(type = FileKitType.Image) { viewModel.onFilePicked(it) }
    val cameraLauncher = rememberCameraPickerLauncher { viewModel.onFilePicked(it) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("기록하기", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        when (val step = state.step) {
            AnalyzeStep.Pick -> PickContent(
                onGallery = galleryLauncher::launch,
                onCamera = { cameraLauncher.launch() },
            )

            AnalyzeStep.Preparing -> Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            is AnalyzeStep.Ready -> {
                Preview(step.image, analyzing = false)
                PetSelector(state.pets, state.selectedPetId, viewModel::selectPet, onAddPet)
                state.error?.let { ErrorText(it) }
                Button(onClick = viewModel::analyze, enabled = state.selectedPetId != null, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("표정 분석하기")
                }
                OutlinedButton(onClick = viewModel::reset, modifier = Modifier.fillMaxWidth()) { Text("다른 사진 고르기") }
            }

            is AnalyzeStep.Analyzing -> {
                Preview(step.image, analyzing = true)
                Text(
                    "표정을 읽고 있어요… 귀, 눈, 입 모양을 살펴보는 중이에요 👀",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is AnalyzeStep.Done -> {
                Preview(step.image, analyzing = false)
                AnalysisResultContent(
                    analysis = step.analysis,
                    petName = state.pets.firstOrNull { it.id == step.analysis.petId }?.name,
                )
                Button(onClick = onGoHome, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("피드에서 보기") }
                OutlinedButton(onClick = viewModel::reset, modifier = Modifier.fillMaxWidth()) { Text("다른 사진 분석하기") }
            }
        }

        if (state.step == AnalyzeStep.Pick) state.error?.let { ErrorText(it) }
    }
}

@Composable
private fun PickContent(onGallery: () -> Unit, onCamera: () -> Unit) {
    ClickableCard(onClick = null) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                Icons.Rounded.AddAPhoto,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Text("사진 한 장으로 기분을 읽어볼게요", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "• 얼굴이 정면으로 크게 나온 사진이 가장 정확해요\n• 강아지와 고양이를 지원해요\n• 한 장에 한 마리만 나오게 찍어 주세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Button(onClick = onGallery, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("앨범에서 고르기")
    }
    OutlinedButton(onClick = onCamera, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("카메라로 찍기")
    }
}

@Composable
private fun Preview(image: ByteArray, analyzing: Boolean) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp))
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
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
private fun PetSelector(pets: List<Pet>, selectedId: String?, onSelect: (String) -> Unit, onAddPet: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("누구의 사진인가요?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            pets.forEach { pet ->
                FilterChip(
                    selected = selectedId == pet.id,
                    onClick = { onSelect(pet.id) },
                    label = { Text("${pet.species.emoji} ${pet.name}") },
                )
            }
            FilterChip(selected = false, onClick = onAddPet, label = { Text("+ 아이 추가") })
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun ErrorText(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
}
