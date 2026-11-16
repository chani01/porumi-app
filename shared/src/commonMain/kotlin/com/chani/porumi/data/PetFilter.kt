package com.chani.porumi.data

import com.chani.porumi.domain.Pet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * 앱 전체에서 공유하는 "현재 선택된 아이".
 * 항상 한 마리가 선택된 상태를 유지한다: 선택값이 없거나 삭제됐으면 첫 번째 아이로 대체.
 */
class PetFilter(petRepository: PetRepository) {
    private val selectedPetId = MutableStateFlow<String?>(null)

    val selectedPet: Flow<Pet?> = combine(petRepository.pets, selectedPetId, ::resolveSelectedPet)

    fun select(petId: String) {
        selectedPetId.value = petId
    }

    fun clear() {
        selectedPetId.value = null
    }
}

/** 선택한 아이가 목록에 있으면 그 아이, 없으면(미선택·삭제됨) 첫 번째 아이, 목록이 비면 null */
internal fun resolveSelectedPet(pets: List<Pet>, selectedId: String?): Pet? =
    pets.firstOrNull { it.id == selectedId } ?: pets.firstOrNull()
