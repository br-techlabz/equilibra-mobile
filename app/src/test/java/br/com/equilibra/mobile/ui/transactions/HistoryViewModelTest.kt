package br.com.equilibra.mobile.ui.transactions

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.equilibra.mobile.data.local.transaction.TransactionEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.HistoryFilters
import br.com.equilibra.mobile.data.repository.TransactionHistoryRepository
import br.com.equilibra.mobile.data.repository.TransactionPage
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TransactionHistoryRepository
    private lateinit var viewModel: HistoryViewModel

    private fun entity(id: String) = TransactionEntity(
        id = id, type = "EXPENSE", status = "ACTIVE", description = "Item $id",
        occurredAt = "2026-10-01T12:00:00Z", amount = "10.00",
        createdAt = "2026-10-01T12:00:00Z", updatedAt = "2026-10-01T12:00:00Z"
    )

    private fun page(items: List<TransactionEntity>, page: Int = 0, totalPages: Int = 1, totalElements: Long = items.size.toLong()) =
        TransactionPage(items, page, totalPages, totalElements)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        coEvery { repository.list(any(), any(), any()) } returns ApiResult.Success(page(emptyList()))
        viewModel = HistoryViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `carga inicial sem filtros usa pagina zero`() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value as HistoryUiState.Success
        assertEquals(0, state.page)
        assertEquals(HistoryFilters.empty(), state.filters)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `aplicar filtros recarrega da pagina zero com os filtros`() = runTest {
        coEvery { repository.list(any(), any(), any()) } returns
            ApiResult.Success(page(listOf(entity("1"))))
        advanceUntilIdle()

        val filters = HistoryFilters(
            type = "INCOME", status = null, fromIso = "2026-10-01T00:00:00Z",
            toIso = null, accountId = "acc-2", categoryId = null, tagIds = emptyList()
        )
        viewModel.applyFilters(filters)
        advanceUntilIdle()

        val state = viewModel.uiState.value as HistoryUiState.Success
        assertEquals(filters, state.filters)
        assertEquals(0, state.page)
        assertEquals(listOf("1"), state.items.map { it.id })
        coVerify { repository.list(0, filters, 20) }
    }

    @Test
    fun `loadMore anexa proxima pagina sem duplicar`() = runTest {
        coEvery { repository.list(0, any(), any()) } returns ApiResult.Success(
            page(listOf(entity("1")), totalPages = 2, totalElements = 3)
        )
        coEvery { repository.list(1, any(), any()) } returns ApiResult.Success(
            page(listOf(entity("1"), entity("2")), page = 1, totalPages = 2, totalElements = 3)
        )
        viewModel = HistoryViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value as HistoryUiState.Success
        assertEquals(listOf("1", "2"), state.items.map { it.id })
        assertTrue(state.endReached)
    }

    @Test
    fun `loadMore nao busca quando endReached`() = runTest {
        advanceUntilIdle() // página única vazia → endReached

        viewModel.loadMore()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.list(any(), any(), any()) }
    }

    @Test
    fun `erro inicial vira Error com retry`() = runTest {
        coEvery { repository.list(any(), any(), any()) } returns ApiResult.HttpError(500, null, null)
        viewModel = HistoryViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value as HistoryUiState.Error
        assertTrue(state.message.contains("500"))
        assertTrue(state.canRetry)
    }

    @Test
    fun `erro em refresh com itens preserva lista`() = runTest {
        coEvery { repository.list(any(), any(), any()) } returns ApiResult.Success(page(listOf(entity("1"))))
        viewModel = HistoryViewModel(repository)
        advanceUntilIdle()

        coEvery { repository.list(any(), any(), any()) } returns ApiResult.NetworkError(IOException())
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value as HistoryUiState.Success
        assertEquals(1, state.items.size)
        assertTrue(state.error != null)
    }

    @Test
    fun `limpar filtros recarrega sem filtros`() = runTest {
        val filters = HistoryFilters("EXPENSE", null, null, null, null, null, emptyList())
        viewModel.applyFilters(filters)
        advanceUntilIdle()

        viewModel.clearFilters()
        advanceUntilIdle()

        val state = viewModel.uiState.value as HistoryUiState.Success
        assertEquals(HistoryFilters.empty(), state.filters)
    }

    @Test
    fun `dismissError limpa mensagem`() = runTest {
        coEvery { repository.list(any(), any(), any()) } returns ApiResult.Success(page(listOf(entity("1"))))
        viewModel = HistoryViewModel(repository)
        advanceUntilIdle()

        coEvery { repository.list(any(), any(), any()) } returns ApiResult.HttpError(500, null, null)
        viewModel.refresh()
        advanceUntilIdle()

        viewModel.dismissError()
        advanceUntilIdle()

        assertNull((viewModel.uiState.value as HistoryUiState.Success).error)
    }

    private fun IOException() = java.io.IOException("offline")
}
