package br.com.equilibra.mobile.ui.purchases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import br.com.equilibra.mobile.data.local.purchase.PurchaseEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.PurchaseRepository
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Task 6.8 — Cadastro de compra parcelada.
 *
 * - Valida antes de chamar a API (1..60 parcelas, valor positivo, data real)
 * - Envia valores normalizados e datas no formato do contrato
 * - Só sinaliza sucesso depois da confirmação remota
 * - Falha mantém o formulário preenchido para o usuário corrigir
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreatePurchaseViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: PurchaseRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun created(id: String = "novo") = PurchaseEntity(
        id = id,
        creditCardId = "c1",
        categoryId = "cat-1",
        description = "Notebook",
        totalAmount = "1234.56",
        purchaseDate = "2026-10-15",
        purchaseAt = "2026-10-15T00:00:00Z",
        installmentCount = 3,
        cycleMonth = "2026-11-01",
        status = "ACTIVE",
        createdAt = "2026-10-15T14:30:00Z",
        updatedAt = "2026-10-15T14:30:00Z"
    )

    private fun viewModel(cardId: String = "c1") = CreatePurchaseViewModel(
        repository,
        SavedStateHandle(mapOf("cardId" to cardId))
    )

    @Test
    fun formularioIniciaComCartaoDaRota() {
        val vm = viewModel("c9")
        assertEquals("c9", vm.uiState.value.creditCardId)
    }

    @Test
    fun camposInvalidosBloqueiamRemota() = runTest {
        val vm = viewModel()
        vm.onInstallmentsChange("61")
        vm.onAmountChange("0,00")
        vm.onDateChange("31/02/2026")

        vm.onSaveClick()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertNotNull(s.installmentsError)
        assertNotNull(s.amountError)
        assertNotNull(s.dateError)
        coVerify(exactly = 0) { repository.create(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun categoriaAusenteBloqueiaRemota() = runTest {
        val vm = viewModel()
        preencherValido(vm, comCategoria = false)

        vm.onSaveClick()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.categoryError)
        coVerify(exactly = 0) { repository.create(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun envioUsaValoresNormalizadosEDatasDoContrato() = runTest {
        coEvery { repository.create(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(created())
        val vm = viewModel()
        preencherValido(vm)

        vm.onSaveClick()
        advanceUntilIdle()

        coVerify {
            repository.create(
                description = "Notebook",
                purchaseDate = "2026-10-15",
                purchaseAt = "2026-10-15T00:00:00Z",
                totalAmount = "1234.56",
                installmentCount = 3,
                creditCardId = "c1",
                categoryId = "cat-1"
            )
        }
        assertTrue(vm.uiState.value.isSuccess)
    }

    @Test
    fun parcelasNoLimiteSaoAceitas() = runTest {
        coEvery { repository.create(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(created())

        listOf("1", "60").forEach { count ->
            val vm = viewModel()
            preencherValido(vm)
            vm.onInstallmentsChange(count)
            vm.onSaveClick()
            advanceUntilIdle()
            assertNull("parcelas=$count", vm.uiState.value.installmentsError)
            assertTrue("parcelas=$count", vm.uiState.value.isSuccess)
        }
    }

    @Test
    fun falhaRemotaMantemFormularioPreenchido() = runTest {
        coEvery { repository.create(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.HttpError(409, "limite", null)
        val vm = viewModel()
        preencherValido(vm)

        vm.onSaveClick()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.isSuccess)
        assertEquals("Notebook", s.description)
        // o formulario preserva exatamente o que o usuario digitou
        assertEquals("1.234,56", s.amount)
        assertEquals("3", s.installments)
        assertNotNull(s.generalError)
    }

    @Test
    fun erroDeRedeSinalizaSemPerderDados() = runTest {
        coEvery { repository.create(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.NetworkError(IOException("offline"))
        val vm = viewModel()
        preencherValido(vm)

        vm.onSaveClick()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.generalError!!.contains("Sem conexão"))
        assertEquals("cat-1", vm.uiState.value.categoryId)
    }

    @Test
    fun naoEnviaDuasVezesEnquantoSalva() = runTest {
        coEvery { repository.create(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(created())
        val vm = viewModel()
        preencherValido(vm)

        vm.onSaveClick()
        vm.onSaveClick()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.create(any(), any(), any(), any(), any(), any(), any()) }
    }

    private fun preencherValido(vm: CreatePurchaseViewModel, comCategoria: Boolean = true) {
        vm.onDescriptionChange("Notebook")
        vm.onAmountChange("1.234,56")
        vm.onDateChange("15/10/2026")
        vm.onInstallmentsChange("3")
        if (comCategoria) vm.onCategoryChange("cat-1")
    }
}
