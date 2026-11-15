package com.chani.porumi.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PetProfileTest {

    private val today = LocalDate(2026, 11, 15)

    @Test
    fun 한_살_이상은_살로_표시() {
        assertEquals("3살", ageLabel(LocalDate(2023, 11, 15), today))
        assertEquals("2살", ageLabel(LocalDate(2023, 11, 16), today)) // 생일 하루 전
    }

    @Test
    fun 한_살_미만은_개월로_표시() {
        assertEquals("11개월", ageLabel(LocalDate(2025, 11, 16), today))
        assertEquals("1개월", ageLabel(LocalDate(2026, 10, 15), today))
    }

    @Test
    fun 한_달_미만과_미래_생일() {
        assertEquals("1개월 미만", ageLabel(LocalDate(2026, 11, 1), today))
        assertEquals("1개월 미만", ageLabel(today, today))
        assertNull(ageLabel(LocalDate(2026, 11, 16), today))
    }

    @Test
    fun 프로필_한_줄은_품종과_나이() {
        val pet = Pet("1", "모모", Species.DOG, breed = "골든 리트리버", birthDate = LocalDate(2023, 5, 1))
        assertEquals("골든 리트리버 · 3살", pet.profileLine(today))
    }

    @Test
    fun 품종이_없으면_종_이름_생일이_없으면_나이_생략() {
        assertEquals("강아지", Pet("1", "모모", Species.DOG).profileLine(today))
        assertEquals("고양이 · 5개월", Pet("2", "나비", Species.CAT, breed = " ", birthDate = LocalDate(2026, 6, 10)).profileLine(today))
    }
}
