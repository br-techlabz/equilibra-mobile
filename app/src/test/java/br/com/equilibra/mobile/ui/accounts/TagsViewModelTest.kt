package br.com.equilibra.mobile.ui.accounts

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.equilibra.mobile.data.local.tag.TagEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.TagRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TagsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TagRepository
    private lateinit var cacheFlow: MutableStateFlow<List<TagEntity>>

    private fun entity(id: String, name: String, active: Boolean = true) = TagEntity(
        id = id, name = name, active = active,
        createdAt = "2024-01-01T00:00:00Z", updatedAt = "2024-01-01T00:00:00Z"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        cacheFlow = MutableStateFlow(emptyList())
        repository = mockk(relaxed = true)
        every { repository.observeTags() } returns cacheFlow
        coEvery { repository.getAllCached() } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cache vazio mostra Loading e depois busca API`() = runTest {
        coEvery { repository.refresh(any()) } returns ApiResult.Success(emptyList())

        val vm = TagsViewModel(repository)
        advanceUntilIdle()

        assertTrue(vm.uiState.value is TagsUiState.Success)
        assertEquals(0, (vm.uiState.value as TagsUiState.Success).tags.size)
    }

    @Test
    fun `cache com dados mostra Success imediatamente`() = runTest {
        cacheFlow.value = listOf(entity("1", "A"), entity("2", "B"))
        coEvery { repository.refresh(any()) } returns ApiResult.Success(cacheFlow.value)
        coEvery { repository.getAllCached() } returns cacheFlow.value

        val vm = TagsViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as TagsUiState.Success
        assertEquals(2, state.tags.size)
    }

    @Test
    fun `refresh com erro de rede mantém cache e expõe mensagem`() = runTest {
        cacheFlow.value = listOf(entity("1", "Cache"))
        coEvery { repository.refresh(any()) } returns ApiResult.NetworkError(java.io.IOException("offline"))
        coEvery { repository.getAllCached() } returns cacheFlow.value

        val vm = TagsViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as TagsUiState.Success
        assertEquals(1, state.tags.size)
        assertEquals("Cache", state.tags[0].name)
        assertEquals("Sem conexão. Mostrando dados locais.", state.error)
    }

    @Test
    fun `refresh manual com HttpError mostra erro com código`() = runTest {
        cacheFlow.value = listOf(entity("1", "Cache"))
        coEvery { repository.refresh(any()) } returns ApiResult.HttpError(500, null, null)
        coEvery { repository.getAllCached() } returns cacheFlow.value

        val vm = TagsViewModel(repository)
        advanceUntilIdle()

        vm.refresh()
        advanceUntilIdle()

        val state = vm.uiState.value as TagsUiState.Success
        assertEquals("Erro ao carregar tags (500).", state.error)
        assertEquals(false, state.isRefreshing)
    }

    @Test
    fun `dismissError limpa mensagem`() = runTest {
        cacheFlow.value = listOf(entity("1", "Cache"))
        coEvery { repository.refresh(any()) } returns ApiResult.HttpError(500, null, null)
        coEvery { repository.getAllCached() } returns cacheFlow.value

        val vm = TagsViewModel(repository)
        advanceUntilIdle()

        vm.dismissError()
        advanceUntilIdle()

        assertNull((vm.uiState.value as TagsUiState.Success).error)
    }

    @Test
    fun `toggleActive com tag ativa chama deactivate`() = runTest {
        cacheFlow.value = listOf(entity("1", "Ativa", active = true))
        coEvery { repository.refresh(any()) } returns ApiResult.Success(cacheFlow.value)
        coEvery { repository.getAllCached() } returns cacheFlow.value
        coEvery { repository.getByIdCached("1") } returns entity("1", "Ativa", active = true)
        coEvery { repository.deactivate("1") } returns ApiResult.Success(entity("1", "Ativa", active = false))

        val vm = TagsViewModel(repository)
        advanceUntilIdle()

        vm.toggleActive("1")
        advanceUntilIdle()

        coVerify { repository.deactivate("1") }
    }

    @Test
    fun `toggleActive com tag inativa chama activate`() = runTest {
        cacheFlow.value = listOf(entity("1", "Inativa", active = false))
        coEvery { repository.refresh(any()) } returns ApiResult.Success(cacheFlow.value)
        coEvery { repository.getAllCached() } returns cacheFlow.value
        coEvery { repository.getByIdCached("1") } returns entity("1", "Inativa", active = false)
        coEvery { repository.activate("1") } returns ApiResult.Success(entity("1", "Inativa", active = true))

        val vm = TagsViewModel(repository)
        advanceUntilIdle()

        vm.toggleActive("1")
        advanceUntilIdle()

        coVerify { repository.activate("1") }
    }

    @Test
    fun `toggleActive com erro expõe mensagem no estado`() = runTest {
        cacheFlow.value = listOf(entity("1", "Ativa", active = true))
        coEvery { repository.refresh(any()) } returns ApiResult.Success(cacheFlow.value)
        coEvery { repository.getAllCached() } returns cacheFlow.value
        coEvery { repository.getByIdCached("1") } returns entity("1", "Ativa", active = true)
        coEvery { repository.deactivate("1") } returns ApiResult.HttpError(409, null, null)

        val vm = TagsViewModel(repository)
        advanceUntilIdle()

        vm.toggleActive("1")
        advanceUntilIdle()

        val state = vm.uiState.value as TagsUiState.Success
        assertEquals("Erro ao alterar status (409).", state.error)
    }
}
