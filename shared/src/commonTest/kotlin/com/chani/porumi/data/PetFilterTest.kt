package com.chani.porumi.data

import com.chani.porumi.domain.Pet
import com.chani.porumi.domain.Species
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PetFilterTest {

    private val choco = Pet(id = "1", name = "초코", species = Species.DOG)
    private val nabi = Pet(id = "2", name = "나비", species = Species.CAT)
    private val pets = listOf(choco, nabi)

    @Test
    fun 선택한_아이가_있으면_그_아이() {
        assertEquals(nabi, resolveSelectedPet(pets, "2"))
    }

    @Test
    fun 선택하지_않았으면_첫_번째_아이() {
        assertEquals(choco, resolveSelectedPet(pets, null))
    }

    @Test
    fun 선택한_아이가_삭제됐으면_첫_번째_아이() {
        assertEquals(choco, resolveSelectedPet(pets, "deleted"))
    }

    @Test
    fun 아이가_없으면_null() {
        assertNull(resolveSelectedPet(emptyList(), "1"))
    }
}
