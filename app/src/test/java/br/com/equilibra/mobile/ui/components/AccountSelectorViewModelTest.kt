package br.com.equilibra.mobile.ui.components

import br.com.equilibra.mobile.data.local.account.AccountEntity
import br.com.equilibra.mobile.data.repository.AccountRepository
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

class AccountSelectorViewModelTest {

    private lateinit var repository: AccountRepository
    private lateinit var viewModel: AccountSelectorViewModel

    private fun entity(id: String, name: String, active: Boolean = true) = AccountEntity(
        id = id, name = name, type = "CHECKING",
        initialBalance = "0.00", currentBalance = "0.00",
        active = active, createdAt = "2024-01-01T00:00:00Z", updatedAt = "2024-01-01T00:00:00Z"
    )

    @Before
    fun setUp() {
        repository = mockk()
        viewModel = AccountSelectorViewModel(repository)
    }

    @Test
    fun exibeApenasContasAtivas() = runTest {
        every { repository.observeAccounts() } returns flowOf(listOf(
            entity("1", "Nubank", active = true),
            entity("2", "Antiga", active = false),
            entity("3", "Itaú", active = true)
        ))

        val result = viewModel.accounts().first()

        assertEquals(listOf("1", "3"), result.map { it.id })
    }

    @Test
    fun listaVaziaQuandoNenhumaAtiva() = runTest {
        every { repository.observeAccounts() } returns flowOf(listOf(entity("1", "Inativa", active = false)))

        assertEquals(0, viewModel.accounts().first().size)
    }

    @Test
    fun isLoadingComecaTrueEViraFalseAposPrimeiraEmissao() = runTest {
        every { repository.observeAccounts() } returns flowOf(listOf(entity("1", "A")))

        assertTrue(viewModel.isLoading.value)
        viewModel.accounts().first()
        assertFalse(viewModel.isLoading.value)
    }
}
