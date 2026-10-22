package br.com.equilibra.mobile.ui.cards

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.local.creditcard.CreditCardEntity
import br.com.equilibra.mobile.data.repository.CreditCardRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class CreateCreditCardViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: CreditCardRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fillValidForm(vm: CreateCreditCardViewModel) {
        vm.onNameChange("Nubank Roxinho")
        vm.onInstitutionChange("Nubank")
        vm.onNicknameChange("Roxinho")
        vm.onLimitChange("10000,00")
        vm.onClosingDayChange("25")
        vm.onDueDayChange("2")
        vm.onTimezoneChange(null)
    }

    @Test
    fun `criacao valida envia campos normalizados`() = runTest {
        fillValidForm(CreateCreditCardViewModel(repository).also { })
        val vm = CreateCreditCardViewModel(repository)
        fillValidForm(vm)
        coEvery {
            repository.create(any(), any(), any(), any(), any(), any(), any())
        } returns ApiResult.Success(entityFor("id-1"))

        vm.onCreateClick()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isSuccess)
        coVerify {
            repository.create(
                "Nubank Roxinho", "Nubank", "Roxinho", "10000.00", 25, 2, null
            )
        }
    }

    @Test
    fun `campos invalidos bloqueiam chamada remota`() = runTest {
        val vm = CreateCreditCardViewModel(repository)
        vm.onNameChange("")
        vm.onLimitChange("0")

        vm.onCreateClick()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.nameError)
        assertNotNull(vm.uiState.value.limitError)
        coVerify(exactly = 0) { repository.create(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `dia fora de 1-31 bloqueia`() = runTest {
        val vm = CreateCreditCardViewModel(repository)
        fillValidForm(vm)
        vm.onClosingDayChange("32")

        vm.onCreateClick()
        advanceUntilIdle()

        assertEquals("Dia deve ser entre 1 e 31", vm.uiState.value.closingDayError)
        coVerify(exactly = 0) { repository.create(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `backend 400 com erros de campo e mapeado para os campos`() = runTest {
        val vm = CreateCreditCardViewModel(repository)
        fillValidForm(vm)
        val body = """
            {"type":"https://equilibra.com.br/problems/validation-failed","title":"Validation failed",
             "status":400,"detail":"One or more fields are invalid.",
             "errors":[{"field":"totalLimit","message":"Limite inválido"},
                       {"field":"closingDay","message":"Closing day must be between 1 and 31"}]}
        """.trimIndent()
        coEvery { repository.create(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.HttpError(400, "Bad Request", body)

        vm.onCreateClick()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("Limite inválido", s.limitError)
        assertEquals("Closing day must be between 1 and 31", s.closingDayError)
        assertNull(s.generalError)
        assertTrue(!s.isLoading)
    }

    @Test
    fun `backend 400 sem erros de campo mostra erro geral`() = runTest {
        val vm = CreateCreditCardViewModel(repository)
        fillValidForm(vm)
        coEvery { repository.create(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.HttpError(400, "Bad Request", "{\"title\":\"Validation failed\",\"status\":400,\"detail\":\"One or more fields are invalid.\"}")

        vm.onCreateClick()
        advanceUntilIdle()

        assertEquals("Dados inválidos. Verifique os campos.", vm.uiState.value.generalError)
    }

    @Test
    fun `erro de rede mostra mensagem geral`() = runTest {
        val vm = CreateCreditCardViewModel(repository)
        fillValidForm(vm)
        coEvery { repository.create(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.NetworkError(java.io.IOException())

        vm.onCreateClick()
        advanceUntilIdle()

        assertEquals("Sem conexão. Verifique sua internet e tente novamente.", vm.uiState.value.generalError)
        assertTrue(!vm.uiState.value.isLoading)
    }

    @Test
    fun `erro de outro codigo mostra mensagem com status`() = runTest {
        val vm = CreateCreditCardViewModel(repository)
        fillValidForm(vm)
        coEvery { repository.create(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.HttpError(500, "ISE", null)

        vm.onCreateClick()
        advanceUntilIdle()

        assertEquals("Erro ao criar cartão (500).", vm.uiState.value.generalError)
    }

    @Test
    fun `double click nao dispara duas criacoes`() = runTest {
        val vm = CreateCreditCardViewModel(repository)
        fillValidForm(vm)
        coEvery { repository.create(any(), any(), any(), any(), any(), any(), any()) } coAnswers {
            kotlinx.coroutines.delay(1000)
            ApiResult.Success(entityFor("id-1"))
        }

        vm.onCreateClick()
        vm.onCreateClick()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.create(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `timezone customizada e enviada`() = runTest {
        val vm = CreateCreditCardViewModel(repository)
        fillValidForm(vm)
        vm.onTimezoneChange("Europe/Lisbon")
        coEvery { repository.create(any(), any(), any(), any(), any(), any(), any()) } returns ApiResult.Success(entityFor("id-1", tz = "Europe/Lisbon"))

        vm.onCreateClick()
        advanceUntilIdle()

        coVerify { repository.create(any(), any(), any(), any(), any(), any(), "Europe/Lisbon") }
        assertTrue(vm.uiState.value.isSuccess)
    }

    private fun entityFor(id: String, tz: String = "America/Sao_Paulo") = CreditCardEntity(
        id = id, name = "N", institution = "B", nickname = null,
        totalLimit = "100.00", committedLimit = "0.00", reservedLimit = "0.00", availableLimit = "100.00",
        closingDay = 20, dueDay = 5, timezone = tz, status = "ACTIVE",
        createdAt = "2026-10-22T00:00:00Z", updatedAt = "2026-10-22T00:00:00Z"
    )
}
