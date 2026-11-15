package com.chani.porumi.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chani.porumi.ui.components.AnalysisResultContent
import com.chani.porumi.ui.components.PetPhoto
import com.chani.porumi.ui.components.localDateTimeLabel
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.Delete
import com.chani.porumi.ui.components.AlertAction
import com.chani.porumi.ui.components.AlertActionStyle
import com.chani.porumi.ui.components.PorumiAlertDialog
import com.chani.porumi.ui.components.PorumiTopBar
import com.chani.porumi.ui.components.CircleIconButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import com.chani.porumi.domain.NOTE_MAX_LENGTH
import com.chani.porumi.ui.components.ClickableCard
import com.chani.porumi.ui.components.StoryEditor
import com.chani.porumi.ui.components.StoryPromptChips
import com.chani.porumi.ui.components.TintedButton

@Composable
fun DetailScreen(
    analysisId: String,
    onBack: () -> Unit,
    viewModel: DetailViewModel = koinViewModel(),
) {
    val flow = remember(analysisId) { viewModel.observe(analysisId) }
    val (analysis, pet) = flow.collectAsStateWithLifecycle(null to null).value
    var confirmDelete by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf(false) }
    val noteError by viewModel.noteError.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PorumiTopBar(title = analysis?.localDateTimeLabel() ?: "", onBack = onBack) {
            if (analysis != null) {
                CircleIconButton(Icons.Outlined.Delete, contentDescription = "삭제", onClick = { confirmDelete = true }, tint = MaterialTheme.colorScheme.error)
            }
        }
        if (analysis != null) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PetPhoto(analysis.imagePath, Modifier.padding(horizontal = 16.dp).fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)))
                StorySection(
                    note = analysis.note,
                    error = noteError,
                    onEdit = { editingNote = true },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                AnalysisResultContent(analysis, pet?.name, Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp))
            }
        }
    }

    if (editingNote && analysis != null) {
        var draft by remember { mutableStateOf(analysis.note.orEmpty()) }
        PorumiAlertDialog(
            onDismissRequest = { editingNote = false },
            title = "오늘의 이야기",
            actions = listOf(
                AlertAction("취소", AlertActionStyle.Cancel) { editingNote = false },
                AlertAction("저장") {
                    viewModel.updateNote(analysis, draft)
                    editingNote = false
                },
            ),
        ) {
            StoryPromptChips(onPick = { phrase ->
                val current = draft.trimEnd()
                draft = (if (current.isEmpty()) phrase else "$current $phrase").take(NOTE_MAX_LENGTH)
            })
            StoryEditor(
                value = draft,
                onValueChange = { draft = it },
                placeholder = "이 날의 이야기를 남겨 보세요",
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            )
        }
    }

    if (confirmDelete && analysis != null) {
        PorumiAlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = "이 기록을 삭제할까요?",
            message = "사진과 분석 결과가 함께 삭제되고 되돌릴 수 없어요.",
            actions = listOf(
                AlertAction("취소", AlertActionStyle.Cancel) { confirmDelete = false },
                AlertAction("삭제", AlertActionStyle.Destructive) {
                    confirmDelete = false
                    viewModel.delete(analysis, onBack)
                },
            ),
        )
    }
}

/** 이야기가 있으면 카드로 보여주고, 없으면 남기기 버튼 */
@Composable
private fun StorySection(note: String?, error: String?, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (note == null) {
            TintedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("오늘의 이야기 남기기")
            }
        } else {
            ClickableCard(onClick = onEdit) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("오늘의 이야기", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "이야기 수정",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(note, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
