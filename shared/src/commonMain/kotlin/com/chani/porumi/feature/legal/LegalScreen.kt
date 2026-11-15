package com.chani.porumi.feature.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.chani.porumi.ui.components.PorumiTopBar
import com.chani.porumi.ui.components.AdaptiveWidth
import com.chani.porumi.ui.components.centeredMaxSize

/** 이용약관 / 개인정보 처리방침 전문 화면 (로그인 전·후 공용) */
@Composable
fun LegalScreen(doc: LegalDoc, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PorumiTopBar(title = doc.title, onBack = onBack)
        Column(
            // 로그인 전(약관 보기)에는 NavHost 밖이라 여기서도 폭을 제한한다
            modifier = Modifier
                .centeredMaxSize(AdaptiveWidth.Content)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                "시행일: $LEGAL_EFFECTIVE_DATE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            doc.sections.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (section.title.isNotEmpty()) {
                        Text(section.title, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        section.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
