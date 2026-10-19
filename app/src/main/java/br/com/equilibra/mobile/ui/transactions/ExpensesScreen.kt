package br.com.equilibra.mobile.ui.transactions

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.equilibra.mobile.data.local.account.AccountEntity
import br.com.equilibra.mobile.data.local.category.CategoryEntity
import br.com.equilibra.mobile.data.local.transaction.TransactionEntity
import br.com.equilibra.mobile.domain.util.MoneyFormatter
import br.com.equilibra.mobile.ui.components.ListEmptyState
import br.com.equilibra.mobile.ui.components.ListError
import br.com.equilibra.mobile.ui.components.ListLoading
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val displayDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun formatOccurredDate(iso: String): String = try {
    Instant.parse(iso).atZone(ZoneOffset.UTC).toLocalDate().format(displayDate)
} catch (_: Exception) {
    iso
}

/**
 * Task 4.5 — Lista de despesas com paginação real da API.
 *
 * - Paginação: carrega página a página (size 20), "Carregar mais" no fim;
 *   nunca busca todas as páginas de uma vez.
 * - Filtro de canceladas via includeCancelled (único filtro suportado pelo
 *   endpoint /expenses).
 * - Estados: loading, vazio, erro+retry, pull-to-refresh; erro durante
 *   refresh preserva itens (snackbar).
 * - Nomes de conta/categoria resolvidos do cache Room.
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
        val error = (state as? ExpensesUiState.Success)?.error
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
            is ExpensesUiState.Loading -> ListLoading("Carregando despesas...")

            is ExpensesUiState.Success -> SuccessContent(
                state = s,
                accountsByName = accountsByName,
                categoriesByName = categoriesByName,
                onRefresh = { viewModel.refresh() },
                onLoadMore = { viewModel.loadMore() },
                onToggleIncludeCancelled = { viewModel.toggleIncludeCancelled() },
                onExpenseClick = onExpenseClick,
                onCreateClick = onCreateClick
            )

            is ExpensesUiState.Error -> ListError(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessContent(
    state: ExpensesUiState.Success,
    accountsByName: Map<String, AccountEntity>,
    categoriesByName: Map<String, CategoryEntity>,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onToggleIncludeCancelled: () -> Unit,
    onExpenseClick: (String) -> Unit,
    onCreateClick: () -> Unit
) {
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
                    text = "Despesas",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.includeCancelled,
                        onClick = onToggleIncludeCancelled,
                        label = { Text("Canceladas") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = onCreateClick,
                        label = { Text("Nova") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.items.isEmpty()) {
                ListEmptyState(
                    title = "Nenhuma despesa",
                    subtitle = if (state.includeCancelled) {
                        "Nenhuma despesa encontrada"
                    } else {
                        "Despesas criadas aparecem aqui"
                    },
                    actionLabel = "Criar despesa",
                    onAction = onCreateClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        ExpenseRow(
                            item = item,
                            accountName = item.sourceAccountId?.let { accountsByName[it]?.name },
                            categoryName = item.categoryId?.let { categoriesByName[it]?.name },
                            onClick = { onExpenseClick(item.id) }
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

@Composable
private fun ExpenseRow(
    item: TransactionEntity,
    accountName: String?,
    categoryName: String?,
    onClick: () -> Unit
) {
    val cancelled = item.status == "CANCELLED"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription =
                    "${item.description}, ${MoneyFormatter.format(item.amount)}" +
                        if (cancelled) ", cancelada" else ""
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (cancelled) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (cancelled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val details = listOfNotNull(
                    formatOccurredDate(item.occurredAt),
                    categoryName,
                    accountName
                ).joinToString(" • ")
                Text(
                    text = details,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (cancelled) {
                    Text(
                        text = "Cancelada",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Text(
                text = MoneyFormatter.format(item.amount),
                style = MaterialTheme.typography.titleSmall,
                color = if (cancelled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
