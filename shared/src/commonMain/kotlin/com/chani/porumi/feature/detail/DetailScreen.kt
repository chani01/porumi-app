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
import com.chani.porumi.ui.components.IosAlertDialog
import com.chani.porumi.ui.components.IosTopBar
import com.chani.porumi.ui.components.PlainIconButton

@Composable
fun DetailScreen(
    analysisId: String,
    onBack: () -> Unit,
    viewModel: DetailViewModel = koinViewModel(),
) {
    val flow = remember(analysisId) { viewModel.observe(analysisId) }
    val (analysis, pet) = flow.collectAsStateWithLifecycle(null to null).value
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        IosTopBar(title = analysis?.localDateTimeLabel() ?: "", onBack = onBack) {
            if (analysis != null) {
                PlainIconButton(Icons.Outlined.Delete, contentDescription = "삭제", onClick = { confirmDelete = true })
            }
        }
        if (analysis != null) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PetPhoto(analysis.imagePath, Modifier.fillMaxWidth().aspectRatio(1f))
                AnalysisResultContent(analysis, pet?.name, Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp))
            }
        }
    }

    if (confirmDelete && analysis != null) {
        IosAlertDialog(
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
