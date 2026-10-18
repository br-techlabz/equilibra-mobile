package br.com.equilibra.mobile.ui.accounts

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.equilibra.mobile.data.local.account.AccountEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.AccountRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AccountRepository
    private lateinit var cacheFlow: MutableStateFlow<List<AccountEntity>>

    private fun entity(id: String, name: String) = AccountEntity(
        id = id, name = name, type = "CHECKING",
        initialBalance = "100.00", currentBalance = "100.00",
        active = true, createdAt = "2024-01-01T00:00:00Z", updatedAt = "2024-01-01T00:00:00Z"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        cacheFlow = MutableStateFlow(emptyList())
        repository = mockk(relaxed = true)
        every { repository.observeAccounts() } returns cacheFlow
        coEvery { repository.getAllCached() } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cache vazio mostra Loading e depois busca API`() = runTest {
        coEvery { repository.refresh(any()) } returns ApiResult.Success(emptyList())

        val vm = AccountsViewModel(repository)
        advanceUntilIdle()

        // Com cache vazio e API retornando vazio, deve ir para Success vazio
        assertTrue(vm.uiState.value is AccountsUiState.Success)
        assertEquals(0, (vm.uiState.value as AccountsUiState.Success).accounts.size)
    }

    @Test
    fun `cache com dados mostra Success imediatamente`() = runTest {
        cacheFlow.value = listOf(entity("1", "A"), entity("2", "B"))
        coEvery { repository.refresh(any()) } returns ApiResult.Success(cacheFlow.value)
        coEvery { repository.getAllCached() } returns cacheFlow.value

        val vm = AccountsViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as AccountsUiState.Success
        assertEquals(2, state.accounts.size)
    }

    @Test
    fun `erro de rede com cache mostra Success com erro`() = runTest {
        cacheFlow.value = listOf(entity("1", "A"))
        coEvery { repository.getAllCached() } returns cacheFlow.value
        coEvery { repository.refresh(any()) } returns ApiResult.NetworkError(java.io.IOException("offline"))

        val vm = AccountsViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as AccountsUiState.Success
        assertEquals(1, state.accounts.size)
        assertTrue(state.error != null)
        assertTrue(state.error!!.contains("Sem conexão", ignoreCase = true) || state.error!!.contains("conexão", ignoreCase = true))
    }

    @Test
    fun `erro de rede sem cache mostra Error`() = runTest {
        cacheFlow.value = emptyList()
        coEvery { repository.refresh(any()) } returns ApiResult.NetworkError(java.io.IOException("offline"))
        coEvery { repository.getAllCached() } returns emptyList()

        val vm = AccountsViewModel(repository)
        advanceUntilIdle()

        assertTrue(vm.uiState.value is AccountsUiState.Error)
    }

    @Test
    fun `pull-to-refresh recarrega e atualiza`() = runTest {
        cacheFlow.value = listOf(entity("1", "A"))
        coEvery { repository.refresh(any()) } returns ApiResult.Success(cacheFlow.value)
        coEvery { repository.getAllCached() } returns cacheFlow.value

        val vm = AccountsViewModel(repository)
        advanceUntilIdle()

        val updated = listOf(entity("1", "A"), entity("2", "B"))
        coEvery { repository.refresh(any()) } returns ApiResult.Success(updated)
        coEvery { repository.getAllCached() } returns updated

        vm.refresh()
        advanceUntilIdle()

        // Após refresh, cache emite novo valor
        cacheFlow.value = updated
        advanceUntilIdle()

        val state = vm.uiState.value as AccountsUiState.Success
        assertEquals(2, state.accounts.size)
    }

    @Test
    fun `dismissError limpa erro`() = runTest {
        cacheFlow.value = listOf(entity("1", "A"))
        coEvery { repository.getAllCached() } returns cacheFlow.value
        coEvery { repository.refresh(any()) } returns ApiResult.NetworkError(java.io.IOException())

        val vm = AccountsViewModel(repository)
        advanceUntilIdle()

        assertTrue((vm.uiState.value as AccountsUiState.Success).error != null)

        vm.dismissError()
        advanceUntilIdle()

        assertEquals(null, (vm.uiState.value as AccountsUiState.Success).error)
    }

    @Test
    fun `atualizacao silenciosa quando cache muda`() = runTest {
        cacheFlow.value = listOf(entity("1", "A"))
        coEvery { repository.refresh(any()) } returns ApiResult.Success(cacheFlow.value)
        coEvery { repository.getAllCached() } returns cacheFlow.value

        val vm = AccountsViewModel(repository)
        advanceUntilIdle()

        // Simula atualização silenciosa do Room
        cacheFlow.value = listOf(entity("1", "A Atualizada"))
        advanceUntilIdle()

        assertEquals("A Atualizada", (vm.uiState.value as AccountsUiState.Success).accounts[0].name)
    }
}
