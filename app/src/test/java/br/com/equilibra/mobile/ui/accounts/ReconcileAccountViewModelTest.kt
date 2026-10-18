package br.com.equilibra.mobile.ui.accounts

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import br.com.equilibra.mobile.data.local.account.AccountEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.dto.account.ReconciliationResultDto
import br.com.equilibra.mobile.data.repository.AccountRepository
import io.mockk.coEvery
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
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class ReconcileAccountViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AccountRepository

    private val accountId = "acc-1"
    private val cached = AccountEntity(
        id = accountId, name = "Conta", type = "CHECKING",
        initialBalance = "100.00", currentBalance = "1500.00",
        active = true, createdAt = "2024-01-01T00:00:00Z", updatedAt = "2024-01-01T00:00:00Z"
    )

    private fun result(
        calculated: String = "1500.00",
        informed: String = "1600.00",
        difference: String = "100.00"
    ) = ReconciliationResultDto(
        accountId = accountId,
        reconciliationDate = "2026-10-18",
        calculatedBalance = calculated,
        informedBalance = informed,
        difference = difference,
        adjustmentType = "INCOME",
        createdTransactionId = "tx-1"
    )

    private fun buildViewModel(): ReconcileAccountViewModel {
        val handle = SavedStateHandle(mapOf(ReconcileAccountViewModel.ARG_ACCOUNT_ID to accountId))
        return ReconcileAccountViewModel(repository, handle)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        coEvery { repository.getByIdCached(accountId) } returns cached
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `prefill do cache e datas atuais`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(cached, state.account)
        assertTrue(state.date.isNotEmpty())
        assertTrue(state.time.isNotEmpty())
    }

    @Test
    fun `diferença calculada em BigDecimal sem Double`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onInformedBalanceChange("1600.00")
        advanceUntilIdle()

        val diff = vm.uiState.value.previewDifference
        assertNotNull(diff)
        // 1600.00 - 1500.00 = 100.00
        assertEquals(0, BigDecimal("100.00").compareTo(diff!!))
    }

    @Test
    fun `diferença negativa quando informado menor`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onInformedBalanceChange("1400,00")
        advanceUntilIdle()

        val diff = vm.uiState.value.previewDifference!!
        assertTrue(diff.compareTo(BigDecimal.ZERO) < 0)
    }

    @Test
    fun `validação impede abrir confirmação com campo inválido`() = runTest {
        var calls = 0
        coEvery { repository.reconcile(any(), any(), any(), any()) } coAnswers {
            calls++; ApiResult.Success(result())
        }

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onInformedBalanceChange("") // vazio
        vm.onReconcileClick()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.confirmVisible)
        assertNotNull(vm.uiState.value.balanceError)
        assertEquals(0, calls)
    }

    @Test
    fun `hora invalida mostra erro`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onTimeChange("25:99")
        vm.onInformedBalanceChange("100.00")
        vm.onReconcileClick()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.confirmVisible)
        assertNotNull(vm.uiState.value.timeError)
    }

    @Test
    fun `confirmação exibida com valores corretos antes de enviar`() = runTest {
        var calls = 0
        coEvery { repository.reconcile(any(), any(), any(), any()) } coAnswers {
            calls++; ApiResult.Success(result())
        }

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onInformedBalanceChange("1600.00")
        vm.onReconcileClick()
        advanceUntilIdle()

        // Confirmação visível, NÃO enviou ainda
        assertTrue(vm.uiState.value.confirmVisible)
        assertEquals(0, calls)

        // Confirma → envia UMA vez
        vm.onConfirmSubmit()
        advanceUntilIdle()

        assertEquals(1, calls)
        assertTrue(vm.uiState.value.isSuccess)
        assertNotNull(vm.uiState.value.result)
        assertFalse(vm.uiState.value.isSubmitting)
    }

    @Test
    fun `reconciliação enviada uma vez (guard isSubmitting)`() = runTest {
        var calls = 0
        coEvery { repository.reconcile(any(), any(), any(), any()) } coAnswers {
            calls++
            kotlinx.coroutines.delay(300)
            ApiResult.Success(result())
        }

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onInformedBalanceChange("1600.00")
        vm.onReconcileClick()
        advanceUntilIdle()
        vm.onConfirmSubmit()
        vm.onConfirmSubmit() // duplo clique — ignorado pelo guard
        advanceUntilIdle()

        assertEquals(1, calls)
    }

    @Test
    fun `dismiss confirmação não envia`() = runTest {
        var calls = 0
        coEvery { repository.reconcile(any(), any(), any(), any()) } coAnswers {
            calls++; ApiResult.Success(result())
        }

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onInformedBalanceChange("1600.00")
        vm.onReconcileClick()
        advanceUntilIdle()
        vm.onDismissConfirm()
        advanceUntilIdle()

        assertEquals(0, calls)
        assertFalse(vm.uiState.value.confirmVisible)
    }

    @Test
    fun `falha de rede mostra erro e não marca sucesso`() = runTest {
        coEvery { repository.reconcile(any(), any(), any(), any()) } returns
            ApiResult.NetworkError(java.io.IOException())

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onInformedBalanceChange("1600.00")
        vm.onReconcileClick()
        advanceUntilIdle()
        vm.onConfirmSubmit()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSuccess)
        assertNull(vm.uiState.value.result)
        assertNotNull(vm.uiState.value.generalError)
        assertFalse(vm.uiState.value.isSubmitting)
    }

    @Test
    fun `falha HTTP 400 mostra mensagem`() = runTest {
        coEvery { repository.reconcile(any(), any(), any(), any()) } returns
            ApiResult.HttpError(400, "Bad Request", null)

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onInformedBalanceChange("1600.00")
        vm.onReconcileClick()
        advanceUntilIdle()
        vm.onConfirmSubmit()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.generalError)
        assertFalse(vm.uiState.value.isSuccess)
    }

    @Test
    fun `resultado exibe valores da API (adjustmentType e transactionId)`() = runTest {
        coEvery { repository.reconcile(any(), any(), any(), any()) } returns ApiResult.Success(result())

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onInformedBalanceChange("1600.00")
        vm.onReconcileClick()
        advanceUntilIdle()
        vm.onConfirmSubmit()
        advanceUntilIdle()

        val r = vm.uiState.value.result!!
        assertEquals("INCOME", r.adjustmentType)
        assertEquals("tx-1", r.createdTransactionId)
        assertEquals("100.00", r.difference)
    }
}
