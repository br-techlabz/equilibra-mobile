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
 * Task 4.5 — Lista de despesas com paginação real da API.
 *
 * - Paginação: carrega página a página (size 20), "Carregar mais" no fim;
 *   nunca busca todas as páginas de uma vez.
 * - Filtro de canceladas via includeCancelled (único filtro suportado pelo
 *   endpoint /expenses).
 * - Estados: loading, vazio, erro+retry, pull-to-refresh; erro durante
 *   refresh preserva itens (snackbar).
 * - A exibição compartilhada vive em [TransactionListPane] (Task 4.7);
 *   conta exibida é a origem (sourceAccountId).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    onExpenseClick: (String) -> Unit = {},
    onCreateClick: () -> Unit = {},
    viewModel: ExpensesViewModel = hiltViewModel()
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
            .semantics { contentDescription = "Tela de despesas" }
    ) {
        when (val s = state) {
            is TransactionsListUiState.Loading -> ListLoading("Carregando despesas...")

            is TransactionsListUiState.Success -> TransactionListPane(
                title = "Despesas",
                state = s,
                accountsByName = accountsByName,
                categoriesByName = categoriesByName,
                accountRef = { it.sourceAccountId },
                createLabel = "Nova",
                emptyTitle = "Nenhuma despesa",
                emptySubtitle = "Despesas criadas aparecem aqui",
                onCreateClick = onCreateClick,
                onItemClick = onExpenseClick,
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
