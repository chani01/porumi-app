package com.chani.porumi.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** 시스템 스플래시와 같은 크기: 지름 160dp 원 + 93dp 발바닥 (splash_icon.xml 참고) */
private val CIRCLE = 160.dp
private val PAW = 93.dp
private val LIFT = 56.dp

/**
 * 시스템 스플래시 직후 보여주는 브랜드 스플래시.
 * 화면 정중앙의 아이콘이 시스템 스플래시와 정확히 겹친 상태에서 시작해 → 위로 살짝 올라가며 작아지고
 * → 앱 이름과 문구가 나타난다. 로딩이 길어지면 하단 점 3개가 깜빡인다.
 */
@Composable
fun SplashScreen() {
    val lift = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { lift.animateTo(1f, tween(durationMillis = 550, easing = FastOutSlowInEasing)) }
        launch { textAlpha.animateTo(1f, tween(durationMillis = 450, delayMillis = 250)) }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        // 로고
        Box(
            modifier = Modifier
                .offset(y = -LIFT * lift.value)
                .graphicsLayer {
                    val scale = 1f - 0.25f * lift.value
                    scaleX = scale
                    scaleY = scale
                }
                .size(CIRCLE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Pets, contentDescription = null, tint = Color.White, modifier = Modifier.size(PAW))
        }

        // 앱 이름 + 문구 (로고 아래)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .offset(y = CIRCLE * 0.75f / 2 - LIFT + 52.dp)
                .graphicsLayer {
                    alpha = textAlpha.value
                    translationY = (1f - textAlpha.value) * 24f
                },
        ) {
            Text(
                "Porumi",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "우리 아이 표정으로 읽는 하루 기분",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        LoadingDots(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 48.dp)
                .graphicsLayer { alpha = textAlpha.value },
        )
    }
}

@Composable
private fun LoadingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { i ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, delayMillis = i * 200),
                    repeatMode = RepeatMode.Reverse,
                ),
            )
            Box(
                Modifier
                    .size(8.dp)
                    .graphicsLayer { this.alpha = alpha }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
