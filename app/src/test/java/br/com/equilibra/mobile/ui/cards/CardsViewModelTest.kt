package br.com.equilibra.mobile.ui.cards

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.equilibra.mobile.data.local.creditcard.CreditCardEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.CreditCardRepository
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
class CardsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: CreditCardRepository
    private lateinit var cacheFlow: MutableStateFlow<List<CreditCardEntity>>

    private fun entity(id: String, name: String, status: String = "ACTIVE") = CreditCardEntity(
        id = id, name = name, institution = "Banco", nickname = null,
        totalLimit = "1000.00", committedLimit = "0.00", reservedLimit = "0.00", availableLimit = "1000.00",
        closingDay = 20, dueDay = 5, timezone = "America/Sao_Paulo", status = status,
        createdAt = "2026-10-22T00:00:00Z", updatedAt = "2026-10-22T00:00:00Z"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        cacheFlow = MutableStateFlow(emptyList())
        repository = mockk(relaxed = true)
        every { repository.observeCards() } returns cacheFlow
        coEvery { repository.getAllCached() } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cache vazio mostra Loading e depois busca API`() = runTest {
        coEvery { repository.refresh() } returns ApiResult.Success(emptyList())

        val vm = CardsViewModel(repository)
        advanceUntilIdle()

        assertTrue(vm.uiState.value is CardsUiState.Success)
        assertTrue((vm.uiState.value as CardsUiState.Success).cards.isEmpty())
    }

    @Test
    fun `cache com dados emite Success imediato e atualiza de fundo`() = runTest {
        cacheFlow.value = listOf(entity("1", "A"))
        // Simula o ciclo real: refresh() da API escreve no DAO, DAO re-emite o flow
        coEvery { repository.refresh() } answers {
            cacheFlow.value = listOf(entity("1", "A"), entity("2", "B"))
            ApiResult.Success(cacheFlow.value)
        }

        val vm = CardsViewModel(repository)
        advanceUntilIdle()

        val s = vm.uiState.value as CardsUiState.Success
        assertEquals(listOf("1", "2"), s.cards.map { it.id })
    }

    @Test
    fun `refresh emite lista da API`() = runTest {
        coEvery { repository.refresh() } answers {
            cacheFlow.value = listOf(entity("1", "A"), entity("2", "B"))
            ApiResult.Success(cacheFlow.value)
        }

        val vm = CardsViewModel(repository)
        advanceUntilIdle()

        val s = vm.uiState.value as CardsUiState.Success
        assertEquals(listOf("1", "2"), s.cards.map { it.id })
    }

    @Test
    fun `erro de rede com cache mostra dados locais e snackbar`() = runTest {
        cacheFlow.value = listOf(entity("1", "Local"))
        coEvery { repository.refresh() } returns ApiResult.NetworkError(java.io.IOException("offline"))

        val vm = CardsViewModel(repository)
        advanceUntilIdle()

        val s = vm.uiState.value as CardsUiState.Success
        assertEquals(listOf("Local"), s.cards.map { it.name })
        assertEquals("Sem conexão. Mostrando dados locais.", s.error)
    }

    @Test
    fun `erro de rede sem cache mostra Error`() = runTest {
        coEvery { repository.refresh() } returns ApiResult.NetworkError(java.io.IOException("offline"))

        val vm = CardsViewModel(repository)
        advanceUntilIdle()

        assertTrue(vm.uiState.value is CardsUiState.Error)
    }

    @Test
    fun `http error com cache presente cai em Success com erro recuperável`() = runTest {
        cacheFlow.value = listOf(entity("1", "A"))
        coEvery { repository.refresh() } returns ApiResult.HttpError(500, "boom", null)

        val vm = CardsViewModel(repository)
        advanceUntilIdle()

        val s = vm.uiState.value as CardsUiState.Success
        assertEquals(listOf("A"), s.cards.map { it.name })
        assertTrue(s.error!!.contains("500"))
    }

    @Test
    fun `dismissError limpa o erro`() = runTest {
        cacheFlow.value = listOf(entity("1", "A"))
        coEvery { repository.refresh() } returns ApiResult.NetworkError(java.io.IOException("offline"))

        val vm = CardsViewModel(repository)
        advanceUntilIdle()

        assertTrue(vm.uiState.value is CardsUiState.Success && (vm.uiState.value as CardsUiState.Success).error != null)

        vm.dismissError()
        advanceUntilIdle()

        val s = vm.uiState.value as CardsUiState.Success
        assertEquals(null, s.error)
        assertEquals(listOf("A"), s.cards.map { it.name })
    }

    @Test
    fun `pull to refresh re-carrega`() = runTest {
        cacheFlow.value = listOf(entity("1", "A"))
        // 1ª chamada (init): mantém lista; 2ª (refresh manual): adiciona
        var calls = 0
        coEvery { repository.refresh() } answers {
            calls++
            if (calls >= 2) {
                cacheFlow.value = listOf(entity("1", "A"), entity("9", "Novo"))
            }
            ApiResult.Success(cacheFlow.value)
        }

        val vm = CardsViewModel(repository)
        advanceUntilIdle()

        vm.refresh()
        advanceUntilIdle()

        val s = vm.uiState.value as CardsUiState.Success
        assertEquals(listOf("1", "9"), s.cards.map { it.id })
        assertTrue(!s.isRefreshing)
    }

    // Fluxo de update do cache pós CRUD (usado a partir das Tasks 6.4/6.5)

    // Fluxo de update do cache pós CRUD (usado a partir das Tasks 6.4/6.5)

    @Test
    fun `atualizacao silenciosa do cache mantem estado Success`() = runTest {
        cacheFlow.value = listOf(entity("1", "A"))
        coEvery { repository.refresh() } returns ApiResult.Success(listOf(entity("1", "A")))

        val vm = CardsViewModel(repository)
        advanceUntilIdle()

        cacheFlow.value = listOf(entity("1", "A"), entity("2", "B"))
        advanceUntilIdle()

        val s = vm.uiState.value as CardsUiState.Success
        assertEquals(listOf("1", "2"), s.cards.map { it.id })
    }
}
