package com.chani.porumi.data

import com.chani.porumi.domain.Analysis
import com.chani.porumi.domain.Pet
import com.chani.porumi.domain.Species
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import com.chani.porumi.domain.NOTE_MAX_LENGTH

enum class AuthState { Loading, SignedOut, SignedIn }

class AuthRepository internal constructor(private val supabase: SupabaseClient) {

    val authState: Flow<AuthState> = supabase.auth.sessionStatus.map {
        when (it) {
            is SessionStatus.Authenticated -> AuthState.SignedIn
            is SessionStatus.NotAuthenticated -> AuthState.SignedOut
            is SessionStatus.Initializing -> AuthState.Loading
            // 토큰 갱신 실패(오프라인 등): 세션은 남아 있으므로 로그인 상태 유지
            is SessionStatus.RefreshFailure -> AuthState.SignedIn
        }
    }

    val currentEmail: String? get() = supabase.auth.currentUserOrNull()?.email

    suspend fun signIn(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /** @return true 면 바로 로그인됨, false 면 이메일 인증이 필요함 */
    suspend fun signUp(email: String, password: String): Boolean {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        return supabase.auth.currentSessionOrNull() != null
    }

    /**
     * SNS 로그인: 로그인 화면(브라우저/Custom Tabs)을 띄우고 바로 반환한다.
     * 세션은 딥링크로 돌아온 뒤 handleAuthDeeplink 에서 만들어지고 authState 로 전달된다.
     */
    suspend fun signInWithSocial(social: SocialLogin) {
        supabase.auth.signInWith(social.provider)
    }

    suspend fun signOut() = supabase.auth.signOut()

    suspend fun changePassword(newPassword: String) {
        supabase.auth.updateUser { password = newPassword }
    }

    /** 서버(delete-account)에서 사진·기록·계정을 모두 삭제한 뒤 로컬 세션 정리 */
    suspend fun deleteAccount() {
        supabase.functions.invoke("delete-account")
        runCatching { supabase.auth.clearSession() }
    }
}

class PetRepository internal constructor(private val supabase: SupabaseClient) {

    private val _pets = MutableStateFlow<List<Pet>>(emptyList())
    val pets: StateFlow<List<Pet>> = _pets.asStateFlow()

    /** 서버에서 한 번이라도 목록을 받아왔는지 (첫 아이 등록 화면 노출 판단용) */
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    suspend fun refresh() {
        _pets.value = supabase.from("pets")
            .select { order("created_at", Order.ASCENDING) }
            .decodeList<PetDto>()
            .mapNotNull { it.toDomain() }
        _loaded.value = true
    }

    suspend fun add(name: String, species: Species, breed: String?, birthDate: LocalDate?): Pet {
        val pet = supabase.from("pets")
            .insert(petDto(name, species, breed, birthDate)) { select() }
            .decodeSingle<PetDto>()
            .toDomain()!!
        _pets.update { it + pet }
        return pet
    }

    suspend fun update(id: String, name: String, species: Species, breed: String?, birthDate: LocalDate?) {
        val dto = petDto(name, species, breed, birthDate)
        supabase.from("pets").update(dto) {
            filter { eq("id", id) }
        }
        _pets.update { list ->
            list.map { if (it.id == id) it.copy(name = dto.name, species = species, breed = dto.breed, birthDate = birthDate) else it }
        }
    }

    private fun petDto(name: String, species: Species, breed: String?, birthDate: LocalDate?) = NewPetDto(
        name = name.trim(),
        species = species.name,
        breed = breed?.trim()?.takeIf { it.isNotEmpty() },
        birthDate = birthDate,
    )

    suspend fun delete(id: String) {
        supabase.from("pets").delete { filter { eq("id", id) } }
        _pets.update { list -> list.filterNot { it.id == id } }
    }

    fun clear() {
        _pets.value = emptyList()
        _loaded.value = false
    }
}

class AnalysisException(message: String) : Exception(message)

class AnalysisRepository internal constructor(private val supabase: SupabaseClient) {

    private val json = Json { ignoreUnknownKeys = true }

    private val _analyses = MutableStateFlow<List<Analysis>>(emptyList())

    /** 최신순 정렬된 내 분석 기록 전체 (홈 피드와 리포트가 함께 사용) */
    val analyses: StateFlow<List<Analysis>> = _analyses.asStateFlow()

    suspend fun refresh() {
        _analyses.value = supabase.from("analyses")
            .select {
                order("taken_at", Order.DESCENDING)
                limit(FEED_LIMIT)
            }
            .decodeList<AnalysisDto>()
            .mapNotNull { it.toDomain() }
    }

    fun find(id: String): Analysis? = _analyses.value.firstOrNull { it.id == id }

    /**
     * 1) Storage 에 업로드 → 2) Edge Function 으로 분석 요청 → 3) 결과를 로컬 목록 맨 앞에 추가
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun analyze(jpegBytes: ByteArray, petId: String?): Analysis {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw AnalysisException("로그인이 필요해요.")
        val path = "$userId/${Uuid.random()}.jpg"

        supabase.storage.from(PHOTO_BUCKET).upload(path, jpegBytes) {
            upsert = false
            contentType = ContentType.Image.JPEG
        }

        val response = try {
            supabase.functions.invoke(
                function = "analyze-photo",
                body = AnalyzeRequest(
                    imagePath = path,
                    petId = petId,
                    takenAt = Clock.System.now().toString(),
                ),
            )
        } catch (e: RestException) {
            throw AnalysisException(
                when (e.statusCode) {
                    422 -> "강아지나 고양이의 얼굴이 잘 보이는 사진으로 다시 시도해 주세요."
                    429 -> "요청이 많아요. 잠시 후 다시 시도해 주세요."
                    401 -> "로그인이 만료됐어요. 다시 로그인해 주세요."
                    else -> "분석 중 문제가 생겼어요. (${e.statusCode})"
                }
            )
        }

        val analysis = json.decodeFromString<AnalysisDto>(response.bodyAsText()).toDomain()
            ?: throw AnalysisException("분석 결과를 읽지 못했어요.")
        _analyses.update { listOf(analysis) + it }
        return analysis
    }

    /** 오늘의 이야기 저장/수정. 빈 문자열이면 지운다 */
    suspend fun updateNote(id: String, note: String?): String? {
        val value = note?.trim()?.take(NOTE_MAX_LENGTH)?.takeIf { it.isNotEmpty() }
        supabase.from("analyses").update({ set("note", value) }) {
            filter { eq("id", id) }
        }
        _analyses.update { list -> list.map { if (it.id == id) it.copy(note = value) else it } }
        return value
    }

    suspend fun delete(analysis: Analysis) {
        supabase.from("analyses").delete { filter { eq("id", analysis.id) } }
        runCatching { supabase.storage.from(PHOTO_BUCKET).delete(analysis.imagePath) }
        _analyses.update { list -> list.filterNot { it.id == analysis.id } }
    }

    fun clear() {
        _analyses.value = emptyList()
    }

    private companion object {
        const val FEED_LIMIT = 500L
    }
}
