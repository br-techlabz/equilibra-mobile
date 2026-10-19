package br.com.equilibra.mobile.ui.components

import br.com.equilibra.mobile.data.local.tag.TagEntity
import br.com.equilibra.mobile.data.repository.TagRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TagSelectorViewModelTest {

    private lateinit var repository: TagRepository
    private lateinit var viewModel: TagSelectorViewModel

    private fun entity(id: String, name: String, active: Boolean = true) = TagEntity(
        id = id, name = name, active = active,
        createdAt = "2024-01-01T00:00:00Z", updatedAt = "2024-01-01T00:00:00Z"
    )

    @Before
    fun setUp() {
        repository = mockk()
        viewModel = TagSelectorViewModel(repository)
    }

    @Test
    fun `exibeApenasTagsAtivas`() = runTest {
        every { repository.observeTags() } returns flowOf(listOf(
            entity("1", "Ativa", active = true),
            entity("2", "Inativa", active = false),
            entity("3", "TambemAtiva", active = true)
        ))

        val result = viewModel.tags().first()

        assertEquals(listOf("1", "3"), result.map { it.id })
    }

    @Test
    fun `listaVaziaQuandoNenhumaAtiva`() = runTest {
        every { repository.observeTags() } returns flowOf(listOf(entity("1", "Inativa", active = false)))

        assertEquals(0, viewModel.tags().first().size)
    }

    @Test
    fun `isLoadingComecaTrueEViraFalseAposPrimeiraEmissao`() = runTest {
        every { repository.observeTags() } returns flowOf(listOf(entity("1", "A")))

        assertTrue(viewModel.isLoading.value)

        viewModel.tags().first()
        assertFalse(viewModel.isLoading.value)
    }
}
