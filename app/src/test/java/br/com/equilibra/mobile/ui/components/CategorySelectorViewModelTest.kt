package br.com.equilibra.mobile.ui.components

import br.com.equilibra.mobile.data.local.category.CategoryEntity
import br.com.equilibra.mobile.data.repository.CategoryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CategorySelectorViewModelTest {

    private lateinit var repository: CategoryRepository
    private lateinit var viewModel: CategorySelectorViewModel

    private fun entity(id: String, name: String, applicability: String = "EXPENSE") = CategoryEntity(
        id = id, name = name, applicability = applicability, active = true,
        createdAt = "2024-01-01T00:00:00Z", updatedAt = "2024-01-01T00:00:00Z"
    )

    @Before
    fun setUp() {
        repository = mockk()
        viewModel = CategorySelectorViewModel(repository)
    }

    @Test
    fun `delega observacao para repository com aplicabilidade informada`() = runTest {
        val expected = listOf(entity("1", "Mercado"))
        every { repository.observeCategories("EXPENSE") } returns flowOf(expected)

        val result = viewModel.categories("EXPENSE").first()

        assertEquals(expected, result)
        verify { repository.observeCategories("EXPENSE") }
    }

    @Test
    fun `lista vem filtrada e apenas ativas pelo repository`() = runTest {
        // observeByApplicability do Room já exclui inatas e filtra EXPENSE→EXPENSE+BOTH;
        // aqui o seletor apenas consome esse fluxo
        val expected = listOf(entity("1", "Mercado"), entity("2", "Flexível", applicability = "BOTH"))
        every { repository.observeCategories("EXPENSE") } returns flowOf(expected)

        assertEquals(2, viewModel.categories("EXPENSE").first().size)
    }

    @Test
    fun `isLoadingComecaTrueEViraFalseAposPrimeiraEmissao`() = runTest {
        every { repository.observeCategories(any()) } returns flowOf(listOf(entity("1", "A")))

        assertTrue(viewModel.isLoading.value)

        viewModel.categories("BOTH").first()
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `sem aplicabilidade retorna todas as categorias ativas`() = runTest {
        // Filtro do histórico (Task 4.11): sem recorte por EXPENSE/INCOME
        every { repository.observeCategories(null) } returns flowOf(listOf(
            entity("1", "Mercado", applicability = "EXPENSE"),
            entity("2", "Salário", applicability = "INCOME")
        ))

        val result = viewModel.categories().first()

        assertEquals(listOf("1", "2"), result.map { it.id })
        verify { repository.observeCategories(null) }
    }
}
