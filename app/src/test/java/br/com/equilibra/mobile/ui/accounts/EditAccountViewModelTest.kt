package br.com.equilibra.mobile.ui.accounts

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import br.com.equilibra.mobile.data.local.account.AccountEntity
import br.com.equilibra.mobile.data.remote.ApiResult
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

@OptIn(ExperimentalCoroutinesApi::class)
class EditAccountViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AccountRepository

    private val accountId = "acc-1"
    private val cachedAccount = AccountEntity(
        id = accountId, name = "Conta Original", type = "CHECKING",
        initialBalance = "100.00", currentBalance = "150.00",
        active = true, createdAt = "2024-01-01T00:00:00Z", updatedAt = "2024-01-01T00:00:00Z"
    )

    private fun buildViewModel(): EditAccountViewModel {
        val handle = SavedStateHandle(mapOf(EditAccountViewModel.ARG_ACCOUNT_ID to accountId))
        return EditAccountViewModel(repository, handle)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        coEvery { repository.getByIdCached(accountId) } returns cachedAccount
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `prefill a partir do cache`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(cachedAccount, state.account)
        assertEquals("Conta Original", state.form.name)
        assertEquals("CHECKING", state.form.type)
        assertEquals("100.00", state.form.initialBalanceInput)
    }

    @Test
    fun `salvar com sucesso marca isSuccess e atualiza conta`() = runTest {
        val updated = cachedAccount.copy(name = "Nova", type = "SAVINGS")
        coEvery { repository.update(accountId, "Nova", "SAVINGS", "100.00") } returns ApiResult.Success(updated)

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onNameChange("Nova")
        vm.onTypeChange("SAVINGS")
        vm.onSaveClick()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isSuccess)
        assertFalse(vm.uiState.value.isLoading)
        assertEquals("Nova", vm.uiState.value.account!!.name)
    }

    @Test
    fun `falha no salvar preserva estado anterior (form inalterado)`() = runTest {
        coEvery { repository.update(any(), any(), any(), any()) } returns ApiResult.HttpError(500, "err", null)

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onNameChange("Alterado")
        vm.onSaveClick()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isSuccess)
        assertNotNull(state.generalError)
        // Form mantém o valor digitado; conta original preservada
        assertEquals("Alterado", state.form.name)
        assertEquals(cachedAccount, state.account)
    }

    @Test
    fun `salvar com nome vazio mostra erro local sem chamar API`() = runTest {
        var calls = 0
        coEvery { repository.update(any(), any(), any(), any()) } coAnswers {
            calls++
            ApiResult.Success(cachedAccount)
        }

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onNameChange("   ")
        vm.onSaveClick()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.form.nameError)
        assertEquals(0, calls)
    }

    @Test
    fun `deactivar exibe confirmacao antes de executar`() = runTest {
        var calls = 0
        coEvery { repository.deactivate(accountId) } coAnswers {
            calls++
            ApiResult.Success(cachedAccount.copy(active = false))
        }

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onToggleActiveClick()
        advanceUntilIdle()

        // Não executou ainda — aguarda confirmação
        assertTrue(vm.uiState.value.confirmDeactivateVisible)
        assertEquals(0, calls)

        vm.onConfirmDeactivate()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.confirmDeactivateVisible)
        assertEquals(1, calls)
        assertEquals(false, vm.uiState.value.account!!.active)
        assertTrue(vm.uiState.value.statusChanged)
    }

    @Test
    fun `dismiss confirmacao nao executa toggle`() = runTest {
        var calls = 0
        coEvery { repository.deactivate(accountId) } coAnswers {
            calls++
            ApiResult.Success(cachedAccount)
        }

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onToggleActiveClick()
        advanceUntilIdle()
        vm.onDismissConfirmDeactivate()
        advanceUntilIdle()

        assertEquals(0, calls)
        assertEquals(true, vm.uiState.value.account!!.active)
    }

    @Test
    fun `ativar nao exige confirmacao`() = runTest {
        val inactive = cachedAccount.copy(active = false)
        coEvery { repository.getByIdCached(accountId) } returns inactive
        coEvery { repository.activate(accountId) } returns ApiResult.Success(inactive.copy(active = true))

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onToggleActiveClick()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.confirmDeactivateVisible)
        assertEquals(true, vm.uiState.value.account!!.active)
        assertTrue(vm.uiState.value.statusChanged)
    }

    @Test
    fun `falha no toggle preserva ativo anterior e mostra erro`() = runTest {
        coEvery { repository.deactivate(accountId) } returns ApiResult.HttpError(409, "conflict", null)

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onToggleActiveClick()
        vm.onConfirmDeactivate()
        advanceUntilIdle()

        // Estado preservado: conta continua ativa, erro exibido
        assertEquals(true, vm.uiState.value.account!!.active)
        assertNotNull(vm.uiState.value.generalError)
        assertFalse(vm.uiState.value.statusChanged)
    }

    @Test
    fun `falha de rede no salvar mostra erro de conexao`() = runTest {
        coEvery { repository.update(any(), any(), any(), any()) } returns ApiResult.NetworkError(java.io.IOException())

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onNameChange("X")
        vm.onSaveClick()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.generalError!!.contains("conexão", ignoreCase = true) ||
            vm.uiState.value.generalError!!.contains("conexão", ignoreCase = true))
    }

    @Test
    fun `duplo clique em salvar nao dispara segundo update`() = runTest {
        var calls = 0
        coEvery { repository.update(any(), any(), any(), any()) } coAnswers {
            calls++
            kotlinx.coroutines.delay(200)
            ApiResult.Success(cachedAccount)
        }

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onSaveClick()
        vm.onSaveClick()
        advanceUntilIdle()

        assertEquals(1, calls)
    }
}
