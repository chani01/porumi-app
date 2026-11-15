package com.chani.porumi.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chani.porumi.ui.components.ClickableCard

private val faqs = listOf(
    "표정 분석은 얼마나 정확한가요?" to
        "AI가 귀·눈·입·수염 같은 얼굴 신호를 보고 감정을 추정해요. 참고용이며, 건강 이상이 의심되면 꼭 수의사와 상담해 주세요.",
    "어떤 사진이 잘 분석되나요?" to
        "얼굴이 정면으로 크게 나오고, 한 장에 한 마리만 있는 밝은 사진이 가장 정확해요. 강아지와 고양이를 지원해요.",
    "분석이 실패했다고 나와요." to
        "얼굴이 잘 보이지 않거나 강아지·고양이가 아닌 것으로 판단되면 저장하지 않아요. 다른 각도의 사진으로 다시 시도해 주세요.",
    "제 사진은 다른 사람도 볼 수 있나요?" to
        "아니요. 사진과 기록은 본인 계정에서만 볼 수 있도록 비공개로 저장돼요.",
    "기분 지수는 어떻게 계산되나요?" to
        "감정마다 점수(-2 ~ +2)가 있고, 분석 신뢰도를 반영해 하루 평균을 내요. 최근 7일 평균이 리포트의 기분 지수예요.",
    "탈퇴하면 데이터는 어떻게 되나요?" to
        "마이 > 내 계정 설정 > 회원 탈퇴를 하면 반려동물 정보, 사진, 분석 기록, 계정이 모두 즉시 삭제돼요.",
)

/** 마이 > 고객센터: 자주 묻는 질문 + 이메일 문의 */
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    SettingsPage(title = "고객센터", onBack = onBack) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ClickableCard(onClick = null) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("무엇을 도와드릴까요?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "궁금한 점이나 불편한 점을 보내주시면 영업일 기준 1~2일 안에 답변드릴게요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            runCatching {
                                uriHandler.openUri("mailto:${AppInfo.SUPPORT_EMAIL}?subject=%5BPorumi%5D%20%EB%AC%B8%EC%9D%98%20(v${AppInfo.VERSION})")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Icon(Icons.Outlined.Email, contentDescription = null)
                        Text("이메일로 문의하기", modifier = Modifier.padding(start = 8.dp))
                    }
                    Text(AppInfo.SUPPORT_EMAIL, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text("자주 묻는 질문", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            ClickableCard(onClick = null) {
                Column {
                    faqs.forEachIndexed { i, (q, a) ->
                        FaqItem(q, a)
                        if (i != faqs.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Q. $question", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Icon(
                if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(open) {
            Text(
                answer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** 마이 > 오픈소스 라이선스 */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    SettingsPage(title = "오픈소스 라이선스", onBack = onBack) {
        Text(
            "Porumi는 아래 오픈소스 소프트웨어를 사용해요. 항목을 누르면 프로젝트 페이지로 이동해요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        SettingsSection(null) {
            openSourceLicenses.forEach { lib ->
                SettingsRow(
                    title = lib.name,
                    icon = null,
                    onClick = { runCatching { uriHandler.openUri(lib.url) } },
                    subtitle = "${lib.author} · ${lib.license}",
                )
            }
        }
    }
}
