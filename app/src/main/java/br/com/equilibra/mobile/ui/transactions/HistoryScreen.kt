package br.com.equilibra.mobile.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.equilibra.mobile.data.repository.HistoryFilters
import br.com.equilibra.mobile.ui.components.ListEmptyState
import br.com.equilibra.mobile.ui.components.ListError
import br.com.equilibra.mobile.ui.components.ListLoading

/**
 * Task 4.11 — Histórico unificado.
 *
 * - Fonte única: GET /transactions (a tela nunca lê/escreve Room; os
 *   seletores de filtro consultam o cache local apenas para montar opções).
 * - Paginação real com "Carregar mais" (size 20) e pull-to-refresh.
 * - Filtros combinados via [HistoryFilterSheet]; resumo dos filtros ativos
 *   fica visível no topo com atalho para limpar.
 * - Estados: loading, erro+retry, vazio (com/sem filtros), erro em
 *   refresh preservando itens (snackbar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onTransactionClick: ((String) -> Unit)? = null,
    viewModel: HistoryViewModel = hiltViewModel(),
    labelsViewModel: HistoryLabelsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val accountNames by labelsViewModel.accountNames.collectAsState()
    val categoryNames by labelsViewModel.categoryNames.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilters by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        val error = (state as? HistoryUiState.Success)?.error
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Tela de histórico unificado" }
    ) {
        when (val s = state) {
            is HistoryUiState.Loading -> ListLoading("Carregando histórico...")

            is HistoryUiState.Success -> HistoryContent(
                state = s,
                accountNames = accountNames,
                categoryNames = categoryNames,
                onOpenFilters = { showFilters = true },
                onClearFilters = { viewModel.clearFilters() },
                onRefresh = { viewModel.refresh() },
                onLoadMore = { viewModel.loadMore() },
                onItemClick = onTransactionClick,
                onTransactionClickAvailable = onTransactionClick != null
            )

            is HistoryUiState.Error -> ListError(
                message = s.message,
                onRetry = { viewModel.retry() }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showFilters) {
        HistoryFilterSheet(
            applied = (state as? HistoryUiState.Success)?.filters ?: HistoryFilters.empty(),
            onApply = {
                showFilters = false
                viewModel.applyFilters(it)
            },
            onClear = {
                showFilters = false
                viewModel.clearFilters()
            },
            onDismiss = { showFilters = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryContent(
    state: HistoryUiState.Success,
    accountNames: Map<String, String>,
    categoryNames: Map<String, String>,
    onOpenFilters: () -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onItemClick: ((String) -> Unit)?,
    onTransactionClickAvailable: Boolean
) {
    val hasFilters = state.filters != HistoryFilters.empty()

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Histórico",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasFilters) {
                        FilterChip(
                            selected = false,
                            onClick = onClearFilters,
                            label = { Text("Limpar") }
                        )
                    }
                    FilterChip(
                        selected = hasFilters,
                        onClick = onOpenFilters,
                        label = { Text("Filtros") }
                    )
                }
            }

            FilterSummary(filters = state.filters, totalElements = state.totalElements)

            Spacer(modifier = Modifier.height(16.dp))

            if (state.items.isEmpty()) {
                ListEmptyState(
                    title = "Nenhuma transação encontrada",
                    subtitle = if (hasFilters) {
                        "Ajuste ou limpe os filtros para ver mais resultados"
                    } else {
                        "Transações aparecem aqui"
                    },
                    actionLabel = if (hasFilters) "Limpar filtros" else null,
                    onAction = if (hasFilters) onClearFilters else null
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        TransactionRow(
                            item = item,
                            accountName = (item.sourceAccountId ?: item.destinationAccountId)
                                ?.let { accountNames[it] },
                            categoryName = item.categoryId?.let { categoryNames[it] },
                            leadingDetail = HistoryFilterOptions.typeLabel(item.type),
                            onClick = { onItemClick?.invoke(item.id) },
                            // Detalhe por ID chega na Task 4.12; sem destino, sem clique
                            clickable = onTransactionClickAvailable
                        )
                    }

                    if (!state.endReached) {
                        item {
                            Button(
                                onClick = onLoadMore,
                                enabled = !state.isLoadingMore,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                if (state.isLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.height(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Carregar mais")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Resumo legível dos filtros aplicados + total de resultados do backend.
 */
@Composable
private fun FilterSummary(filters: HistoryFilters, totalElements: Long) {
    val parts = buildList {
        filters.type?.let { add(HistoryFilterOptions.typeLabel(it)) }
        filters.status?.let { status ->
            add(if (status == HistoryFilterOptions.STATUS_CANCELLED) "Canceladas" else "Ativas")
        }
        filters.fromIso?.let { add("de ${TransactionFormValidator.toDisplayDate(it)}") }
        filters.toIso?.let { add("até ${TransactionFormValidator.toDisplayDateExclusiveEnd(it)}") }
        if (filters.accountId != null) add("conta filtrada")
        if (filters.categoryId != null) add("categoria filtrada")
        if (filters.tagIds.isNotEmpty()) {
            add("${filters.tagIds.size} tag(s)")
        }
    }

    Text(
        text = if (parts.isEmpty()) {
            "$totalElements transação(ões)"
        } else {
            "${parts.joinToString(" • ")} — $totalElements transação(ões)"
        },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(top = 4.dp)
            .semantics {
                contentDescription = if (parts.isEmpty()) {
                    "Sem filtros, $totalElements resultados"
                } else {
                    "Filtros ativos: ${parts.joinToString(", ")}, $totalElements resultados"
                }
            }
    )
}
