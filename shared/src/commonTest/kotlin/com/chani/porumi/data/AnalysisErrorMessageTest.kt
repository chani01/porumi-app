package com.chani.porumi.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** analyze-photo 상태 코드 계약 (supabase/functions/analyze-photo/handler_test.ts 와 짝) */
class AnalysisErrorMessageTest {

    @Test
    fun 얼굴이_안_보이거나_거절된_사진_422() {
        assertEquals("강아지나 고양이의 얼굴이 잘 보이는 사진으로 다시 시도해 주세요.", analysisErrorMessage(422))
    }

    @Test
    fun 요청_과다_429() {
        assertEquals("요청이 많아요. 잠시 후 다시 시도해 주세요.", analysisErrorMessage(429))
    }

    @Test
    fun 로그인_만료_401() {
        assertEquals("로그인이 만료됐어요. 다시 로그인해 주세요.", analysisErrorMessage(401))
    }

    @Test
    fun 그_밖의_코드는_상태_코드를_함께_표시() {
        for (code in listOf(400, 500, 502)) {
            val message = analysisErrorMessage(code)
            assertTrue(message.startsWith("분석 중 문제가 생겼어요."), message)
            assertTrue(message.endsWith("($code)"), message)
        }
    }
}
