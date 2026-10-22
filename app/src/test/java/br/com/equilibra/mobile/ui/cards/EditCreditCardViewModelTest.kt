package br.com.equilibra.mobile.ui.cards

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import br.com.equilibra.mobile.data.local.creditcard.CreditCardEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.CreditCardRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class EditCreditCardViewModelTest {

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

    private fun entity(
        id: String = "c1",
        status: String = "ACTIVE",
        tz: String = "America/Sao_Paulo",
        nickname: String? = "Roxinho"
    ) = CreditCardEntity(
        id = id, name = "Nubank Roxinho", institution = "Nubank", nickname = nickname,
        totalLimit = "10000.00", committedLimit = "250.00", reservedLimit = "0.00", availableLimit = "9750.00",
        closingDay = 25, dueDay = 2, timezone = tz, status = status,
        createdAt = "2026-10-22T00:00:00Z", updatedAt = "2026-10-22T00:00:00Z"
    )

    private fun vm(card: CreditCardEntity? = entity()) = EditCreditCardViewModel(
        repository,
        SavedStateHandle(mapOf("cardId" to (card?.id ?: "c1")))
    ).also {
        coEvery { repository.getByIdCached(any()) } returns card
        coEvery { repository.refreshById(any()) } returns
            if (card != null) ApiResult.Success(card) else ApiResult.HttpError(404, "nf", null)
    }

    // carregamento

    @Test
    fun `preenche formulario do cache`() = runTest {
        val v = vm()
        advanceUntilIdle()

        val s = v.uiState.value
        assertEquals("Nubank Roxinho", s.name)
        assertEquals("Nubank", s.institution)
        assertEquals("Roxinho", s.nickname)
        assertEquals("10000.00", s.limitInput)
        assertEquals("25", s.closingDayInput)
        assertEquals("2", s.dueDayInput)
        assertEquals("America/Sao_Paulo", s.timezone)
        assertEquals("ACTIVE", s.card!!.status)
    }

    @Test
    fun `cache vazio com 404 mostra notFound`() = runTest {
        val v = vm(card = null)
        advanceUntilIdle()

        assertTrue(v.uiState.value.notFound)
    }

    // salvar

    @Test
    fun `salvar envia campos normalizados`() = runTest {
        val v = vm()
        advanceUntilIdle()
        coEvery {
            repository.update(any(), any(), any(), any(), any(), any(), any(), any())
        } returns ApiResult.Success(entity())

        v.onSaveClick()
        advanceUntilIdle()

        assertTrue(v.uiState.value.isSuccess)
        coVerify {
            repository.update("c1", "Nubank Roxinho", "Nubank", "Roxinho", "10000.00", 25, 2, "America/Sao_Paulo")
        }
    }

    @Test
    fun `campos invalidos bloqueiam remota`() = runTest {
        val v = vm()
        advanceUntilIdle()
        v.onLimitChange("0,00")
        v.onClosingDayChange("0")

        v.onSaveClick()
        advanceUntilIdle()

        assertNotNull(v.uiState.value.limitError)
        assertNotNull(v.uiState.value.closingDayError)
        coVerify(exactly = 0) { repository.update(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `falha preserva valores do formulario`() = runTest {
        val v = vm()
        advanceUntilIdle()
        v.onLimitChange("5000.00")
        coEvery { repository.update(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.HttpError(409, "conf", "{\"detail\":\"Conflito de versão.\"}")

        v.onSaveClick()
        advanceUntilIdle()

        val s = v.uiState.value
        assertEquals("5000.00", s.limitInput)  // form preservado
        assertEquals("Conflito de versão.", s.generalError)
        assertFalse(s.isLoading)
    }

    // ativar / desativar

    @Test
    fun `desativar pede confirmacao antes da remota`() = runTest {
        val v = vm(entity(status = "ACTIVE"))
        advanceUntilIdle()

        v.onToggleActiveClick()
        advanceUntilIdle()

        assertTrue(v.uiState.value.confirmDeactivateVisible)
        coVerify(exactly = 0) { repository.deactivate(any()) }
    }

    @Test
    fun `confirmar desativacao chama remota e atualiza card`() = runTest {
        val card = entity(status = "ACTIVE")
        val v = vm(card)
        advanceUntilIdle()
        coEvery { repository.deactivate("c1") } returns ApiResult.Success(card.copy(status = "INACTIVE"))

        v.onToggleActiveClick()
        v.onConfirmDeactivate()
        advanceUntilIdle()

        val s = v.uiState.value
        assertEquals("INACTIVE", s.card!!.status)
        assertTrue(s.statusChanged)
        coVerify(exactly = 1) { repository.deactivate("c1") }
    }

    @Test
    fun `cancelar desativacao nao chama remota`() = runTest {
        val v = vm(entity(status = "ACTIVE"))
        advanceUntilIdle()

        v.onToggleActiveClick()
        v.onDismissConfirmDeactivate()
        advanceUntilIdle()

        assertFalse(v.uiState.value.confirmDeactivateVisible)
        coVerify(exactly = 0) { repository.deactivate(any()) }
    }

    @Test
    fun `desativar em falha remota preserva status local`() = runTest {
        val card = entity(status = "ACTIVE")
        val v = vm(card)
        advanceUntilIdle()
        coEvery { repository.deactivate("c1") } returns
            ApiResult.HttpError(409, "conf", "{\"detail\":\"Cartão tem compras ativas.\"}")

        v.onToggleActiveClick()
        v.onConfirmDeactivate()
        advanceUntilIdle()

        val s = v.uiState.value
        assertEquals("ACTIVE", s.card!!.status)  // estado local preservado
        assertEquals("Cartão tem compras ativas.", s.generalError)
    }

    @Test
    fun `ativar sem confirmacao`() = runTest {
        val card = entity(status = "INACTIVE")
        val v = vm(card)
        advanceUntilIdle()
        coEvery { repository.activate("c1") } returns ApiResult.Success(card.copy(status = "ACTIVE"))

        v.onToggleActiveClick()
        advanceUntilIdle()

        val s = v.uiState.value
        assertEquals("ACTIVE", s.card!!.status)
        assertFalse(s.confirmDeactivateVisible)
        coVerify(exactly = 1) { repository.activate("c1") }
    }

    @Test
    fun `desativar erro de rede preserva status`() = runTest {
        val card = entity(status = "ACTIVE")
        val v = vm(card)
        advanceUntilIdle()
        coEvery { repository.deactivate("c1") } returns ApiResult.NetworkError(java.io.IOException())

        v.onToggleActiveClick()
        v.onConfirmDeactivate()
        advanceUntilIdle()

        val s = v.uiState.value
        assertEquals("ACTIVE", s.card!!.status)
        assertTrue(s.generalError!!.contains("Sem conexão"))
    }
}
