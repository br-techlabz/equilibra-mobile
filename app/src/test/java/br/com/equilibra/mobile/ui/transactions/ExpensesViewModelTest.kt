package br.com.equilibra.mobile.ui.transactions

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.equilibra.mobile.data.local.account.AccountEntity
import br.com.equilibra.mobile.data.local.category.CategoryEntity
import br.com.equilibra.mobile.data.local.transaction.TransactionEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.AccountRepository
import br.com.equilibra.mobile.data.repository.CategoryRepository
import br.com.equilibra.mobile.data.repository.TransactionPage
import br.com.equilibra.mobile.data.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TransactionRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var categoryRepository: CategoryRepository

    private fun entity(id: String, description: String = "Despesa $id") = TransactionEntity(
        id = id, type = "EXPENSE", status = "ACTIVE", description = description,
        occurredAt = "2026-10-01T12:00:00Z", amount = "10.50",
        sourceAccountId = "acc-1", categoryId = "cat-1",
        createdAt = "2026-10-01T12:00:00Z", updatedAt = "2026-10-01T12:00:00Z"
    )

    private fun page(items: List<TransactionEntity>, page: Int = 0, totalPages: Int = 1, totalElements: Long = items.size.toLong()) =
        TransactionPage(items, page, totalPages, totalElements)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        accountRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        every { accountRepository.observeAccounts() } returns flowOf(listOf(
            AccountEntity("acc-1", "Nubank", "CHECKING", "0.00", "0.00", true,
                "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z")
        ))
        every { categoryRepository.observeCategories(any()) } returns flowOf(listOf(
            CategoryEntity("cat-1", "Mercado", "EXPENSE", true,
                "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z")
        ))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vmWithSuccess(result: ApiResult<TransactionPage>): ExpensesViewModel {
        coEvery { repository.listExpenses(any(), any()) } returns result
        return ExpensesViewModel(repository, accountRepository, categoryRepository)
    }

    @Test
    fun `carga inicial traz primeira pagina`() = runTest {
        val vm = vmWithSuccess(ApiResult.Success(page(listOf(entity("1"), entity("2")), page = 0, totalPages = 2, totalElements = 3)))
        advanceUntilIdle()

        val state = vm.uiState.value as TransactionsListUiState.Success
        assertEquals(listOf("1", "2"), state.items.map { it.id })
        assertEquals(0, state.page)
        assertEquals(2, state.totalPages)
        assertFalse(state.endReached)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `primeira pagina vazia vira Success vazio`() = runTest {
        val vm = vmWithSuccess(ApiResult.Success(page(emptyList())))
        advanceUntilIdle()

        val state = vm.uiState.value as TransactionsListUiState.Success
        assertTrue(state.items.isEmpty())
        assertTrue(state.endReached)
    }

    @Test
    fun `paginaIntermediariaNaoMarcaEndReached`() = runTest {
        // Pages são 0-based: página 1 de 3 ainda tem mais
        val vm = vmWithSuccess(ApiResult.Success(page(listOf(entity("1")), page = 1, totalPages = 3, totalElements = 45)))
        advanceUntilIdle()

        val state = vm.uiState.value as TransactionsListUiState.Success
        assertFalse(state.endReached)
    }

    @Test
    fun `loadMore anexa proxima pagina sem duplicar`() = runTest {
        coEvery { repository.listExpenses(0, false) } returns ApiResult.Success(
            page(listOf(entity("1")), page = 0, totalPages = 2, totalElements = 3)
        )
        coEvery { repository.listExpenses(1, false) } returns ApiResult.Success(
            // "1" repete (paginação deslizante no backend) — VM deve deduplicar
            page(listOf(entity("1"), entity("2")), page = 1, totalPages = 2, totalElements = 3)
        )
        val vm = ExpensesViewModel(repository, accountRepository, categoryRepository)
        advanceUntilIdle()

        vm.loadMore()
        advanceUntilIdle()

        val state = vm.uiState.value as TransactionsListUiState.Success
        assertEquals(listOf("1", "2"), state.items.map { it.id })
        assertEquals(1, state.page)
        assertTrue(state.endReached) // página 1 de 2 → fim
    }

    @Test
    fun `loadMore nao busca quando endReached`() = runTest {
        val vm = vmWithSuccess(ApiResult.Success(page(listOf(entity("1")), page = 0, totalPages = 1)))
        advanceUntilIdle()

        vm.loadMore()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.listExpenses(any(), any()) }
    }

    @Test
    fun `refresh recarrega da primeira pagina e limpa erro`() = runTest {
        coEvery { repository.listExpenses(any(), any()) } returns ApiResult.Success(page(listOf(entity("1"))))
        val vm = ExpensesViewModel(repository, accountRepository, categoryRepository)
        advanceUntilIdle()

        vm.refresh()
        advanceUntilIdle()

        val state = vm.uiState.value as TransactionsListUiState.Success
        assertEquals(0, state.page)
        assertFalse(state.isRefreshing)
        assertNull(state.error)
    }

    @Test
    fun `toggle includeCancelled recarrega com o flag invertido`() = runTest {
        coEvery { repository.listExpenses(any(), any()) } returns ApiResult.Success(page(listOf(entity("1"))))
        val vm = ExpensesViewModel(repository, accountRepository, categoryRepository)
        advanceUntilIdle()

        vm.toggleIncludeCancelled()
        advanceUntilIdle()

        val state = vm.uiState.value as TransactionsListUiState.Success
        assertTrue(state.includeCancelled)
        coVerify { repository.listExpenses(0, true) }
    }

    @Test
    fun `erro http na carga inicial vira estado Error com retry`() = runTest {
        val vm = vmWithSuccess(ApiResult.HttpError(500, null, null))
        advanceUntilIdle()

        val state = vm.uiState.value as TransactionsListUiState.Error
        assertTrue(state.message.contains("500"))
        assertTrue(state.canRetry)
    }

    @Test
    fun `erro de rede na carga inicial vira Error`() = runTest {
        val vm = vmWithSuccess(ApiResult.NetworkError(java.io.IOException("offline")))
        advanceUntilIdle()

        assertTrue(vm.uiState.value is TransactionsListUiState.Error)
    }

    @Test
    fun `erro em refresh com itens preserva lista e expoe mensagem`() = runTest {
        coEvery { repository.listExpenses(any(), false) } returns ApiResult.Success(page(listOf(entity("1"))))
        val vm = ExpensesViewModel(repository, accountRepository, categoryRepository)
        advanceUntilIdle()

        coEvery { repository.listExpenses(any(), false) } returns ApiResult.HttpError(500, null, null)
        vm.refresh()
        advanceUntilIdle()

        val state = vm.uiState.value as TransactionsListUiState.Success
        assertEquals(1, state.items.size)
        assertTrue(state.error != null)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `expoe mapas de contas e categorias para nomes na tela`() = runTest {
        val vm = vmWithSuccess(ApiResult.Success(page(emptyList())))
        advanceUntilIdle()

        assertEquals("Nubank", vm.accountsByName.value["acc-1"]?.name)
        assertEquals("Mercado", vm.categoriesByName.value["cat-1"]?.name)
    }
}
