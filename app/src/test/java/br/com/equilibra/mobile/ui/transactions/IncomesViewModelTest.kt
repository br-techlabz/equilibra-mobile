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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class IncomesViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TransactionRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var categoryRepository: CategoryRepository

    private fun entity(id: String, description: String = "Receita $id") = TransactionEntity(
        id = id, type = "INCOME", status = "ACTIVE", description = description,
        occurredAt = "2026-10-01T12:00:00Z", amount = "100.00",
        destinationAccountId = "acc-2", categoryId = "cat-2",
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
            AccountEntity("acc-2", "Salário conta", "CHECKING", "0.00", "0.00", true,
                "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z")
        ))
        every { categoryRepository.observeCategories(any()) } returns flowOf(listOf(
            CategoryEntity("cat-2", "Salário", "INCOME", true,
                "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z")
        ))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vmWithSuccess(result: ApiResult<TransactionPage>): IncomesViewModel {
        coEvery { repository.listIncomes(any(), any()) } returns result
        return IncomesViewModel(repository, accountRepository, categoryRepository)
    }

    @Test
    fun `carga inicial traz primeira pagina`() = runTest {
        val vm = vmWithSuccess(ApiResult.Success(page(listOf(entity("1"), entity("2")), totalPages = 2, totalElements = 3)))
        advanceUntilIdle()

        val state = vm.uiState.value as TransactionsListUiState.Success
        assertEquals(listOf("1", "2"), state.items.map { it.id })
        assertEquals(0, state.page)
        assertFalse(state.endReached)
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
    fun `loadMore anexa proxima pagina sem duplicar`() = runTest {
        coEvery { repository.listIncomes(0, false) } returns ApiResult.Success(
            page(listOf(entity("1")), totalPages = 2, totalElements = 3)
        )
        coEvery { repository.listIncomes(1, false) } returns ApiResult.Success(
            page(listOf(entity("1"), entity("2")), page = 1, totalPages = 2, totalElements = 3)
        )
        val vm = IncomesViewModel(repository, accountRepository, categoryRepository)
        advanceUntilIdle()

        vm.loadMore()
        advanceUntilIdle()

        val state = vm.uiState.value as TransactionsListUiState.Success
        assertEquals(listOf("1", "2"), state.items.map { it.id })
        assertTrue(state.endReached)
    }

    @Test
    fun `loadMore nao busca quando endReached`() = runTest {
        val vm = vmWithSuccess(ApiResult.Success(page(listOf(entity("1")), totalPages = 1)))
        advanceUntilIdle()

        vm.loadMore()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.listIncomes(any(), any()) }
    }

    @Test
    fun `refresh recarrega da primeira pagina`() = runTest {
        coEvery { repository.listIncomes(any(), any()) } returns ApiResult.Success(page(listOf(entity("1"))))
        val vm = IncomesViewModel(repository, accountRepository, categoryRepository)
        advanceUntilIdle()

        vm.refresh()
        advanceUntilIdle()

        val state = vm.uiState.value as TransactionsListUiState.Success
        assertEquals(0, state.page)
        assertNull(state.error)
    }

    @Test
    fun `toggle includeCancelled recarrega com o flag invertido`() = runTest {
        coEvery { repository.listIncomes(any(), any()) } returns ApiResult.Success(page(listOf(entity("1"))))
        val vm = IncomesViewModel(repository, accountRepository, categoryRepository)
        advanceUntilIdle()

        vm.toggleIncludeCancelled()
        advanceUntilIdle()

        val state = vm.uiState.value as TransactionsListUiState.Success
        assertTrue(state.includeCancelled)
        coVerify { repository.listIncomes(0, true) }
    }

    @Test
    fun `erro http na carga inicial vira Error com retry`() = runTest {
        val vm = vmWithSuccess(ApiResult.HttpError(500, null, null))
        advanceUntilIdle()

        val state = vm.uiState.value as TransactionsListUiState.Error
        // Mensagem fala de receitas, não de despesas (rótulo por tipo)
        assertTrue(state.message.contains("receitas"))
        assertTrue(state.canRetry)
    }

    @Test
    fun `erro de rede na carga inicial vira Error`() = runTest {
        val vm = vmWithSuccess(ApiResult.NetworkError(java.io.IOException("offline")))
        advanceUntilIdle()

        assertTrue(vm.uiState.value is TransactionsListUiState.Error)
    }

    @Test
    fun `erro em refresh com itens preserva lista`() = runTest {
        coEvery { repository.listIncomes(any(), false) } returns ApiResult.Success(page(listOf(entity("1"))))
        val vm = IncomesViewModel(repository, accountRepository, categoryRepository)
        advanceUntilIdle()

        coEvery { repository.listIncomes(any(), false) } returns ApiResult.HttpError(500, null, null)
        vm.refresh()
        advanceUntilIdle()

        val state = vm.uiState.value as TransactionsListUiState.Success
        assertEquals(1, state.items.size)
        assertTrue(state.error != null)
    }

    @Test
    fun `expoe mapas de contas e categorias`() = runTest {
        val vm = vmWithSuccess(ApiResult.Success(page(emptyList())))
        advanceUntilIdle()

        assertEquals("Salário conta", vm.accountsByName.value["acc-2"]?.name)
        assertEquals("Salário", vm.categoriesByName.value["cat-2"]?.name)
    }
}
