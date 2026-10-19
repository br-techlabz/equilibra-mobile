package br.com.equilibra.mobile.ui.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.equilibra.mobile.ui.components.ListError
import br.com.equilibra.mobile.ui.components.ListLoading

/**
 * Task 4.7 — Lista de receitas com paginação real da API.
 *
 * - Mesma base paginada das despesas ([IncomesViewModel] estende
 *   [PaginatedTransactionsViewModel]); exibição em [TransactionListPane].
 * - Filtro de canceladas via includeCancelled (contrato do /incomes);
 *   receita cancelada diferenciada visualmente (label + alpha).
 * - Conta exibida é o destino (destinationAccountId — receita entra).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomesScreen(
    onIncomeClick: (String) -> Unit = {},
    onCreateClick: () -> Unit = {},
    viewModel: IncomesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val accountsByName by viewModel.accountsByName.collectAsState()
    val categoriesByName by viewModel.categoriesByName.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        val error = (state as? TransactionsListUiState.Success)?.error
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Tela de receitas" }
    ) {
        when (val s = state) {
            is TransactionsListUiState.Loading -> ListLoading("Carregando receitas...")

            is TransactionsListUiState.Success -> TransactionListPane(
                title = "Receitas",
                state = s,
                accountsByName = accountsByName,
                categoriesByName = categoriesByName,
                accountRef = { it.destinationAccountId },
                createLabel = "Nova",
                emptyTitle = "Nenhuma receita",
                emptySubtitle = "Receitas criadas aparecem aqui",
                canCreate = false, // criar/editar receita chega na Task 4.8
                onCreateClick = onCreateClick,
                onItemClick = onIncomeClick,
                onRefresh = { viewModel.refresh() },
                onLoadMore = { viewModel.loadMore() },
                onToggleIncludeCancelled = { viewModel.toggleIncludeCancelled() }
            )

            is TransactionsListUiState.Error -> ListError(
                message = s.message,
                onRetry = { viewModel.retry() }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
