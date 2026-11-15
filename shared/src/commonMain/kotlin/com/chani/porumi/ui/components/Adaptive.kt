package com.chani.porumi.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 넓은 화면(폴더블 펼침 · 태블릿 · 가로 모드) 대응 폭 제한.
 * 화면이 이보다 넓으면 내용은 가운데에 두고 양옆은 배경만 보인다.
 */
object AdaptiveWidth {
    /** 일반 화면 (피드, 리포트, 기록, 마이, 설정, 상세) */
    val Content: Dp = 640.dp

    /** 로그인·첫 등록처럼 입력 위주 화면 */
    val Form: Dp = 440.dp

    /** 떠 있는 탭바 */
    val TabBar: Dp = 520.dp
}

/** 가로 공간을 다 채우되 [max] 보다 넓어지지 않고 가운데 정렬 */
fun Modifier.centeredMaxWidth(max: Dp): Modifier =
    fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = max).fillMaxWidth()

/** 화면 전체(세로 포함)를 채우되 가로는 [max] 까지, 가운데 정렬 */
fun Modifier.centeredMaxSize(max: Dp): Modifier =
    fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = max).fillMaxSize()
