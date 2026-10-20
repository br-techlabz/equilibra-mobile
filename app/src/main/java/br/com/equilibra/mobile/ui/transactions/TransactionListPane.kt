package br.com.equilibra.mobile.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.equilibra.mobile.data.local.account.AccountEntity
import br.com.equilibra.mobile.data.local.category.CategoryEntity
import br.com.equilibra.mobile.data.local.transaction.TransactionEntity
import br.com.equilibra.mobile.domain.util.DateTimeFormat
import br.com.equilibra.mobile.domain.util.MoneyFormatter
import br.com.equilibra.mobile.ui.components.ListEmptyState

/**
 * Task 4.7 — Conteúdo compartilhado das listas paginadas de transações
 * (despesas da Task 4.5 e receitas desta task — mesmos estados e paginação).
 */

// Task 4.13 — delega ao formatador central (UTC, dd/MM/yyyy)
internal fun formatOccurredDate(iso: String): String =
    DateTimeFormat.formatDate(iso, fallback = iso)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionListPane(
    title: String,
    state: TransactionsListUiState.Success,
    accountsByName: Map<String, AccountEntity>,
    categoriesByName: Map<String, CategoryEntity>,
    /** Referência da conta exibida: origem para despesa, destino para receita. */
    accountRef: (TransactionEntity) -> String?,
    createLabel: String,
    emptyTitle: String,
    emptySubtitle: String,
    /** false esconde "Nova"/CTA enquanto a task de criação não entrega a tela. */
    canCreate: Boolean = true,
    onCreateClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onToggleIncludeCancelled: () -> Unit
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.includeCancelled,
                        onClick = onToggleIncludeCancelled,
                        label = { Text("Canceladas") }
                    )
                    if (canCreate) {
                        FilterChip(
                            selected = false,
                            onClick = onCreateClick,
                            label = { Text(createLabel) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.items.isEmpty()) {
                ListEmptyState(
                    title = emptyTitle,
                    subtitle = if (state.includeCancelled) "$emptyTitle" else emptySubtitle,
                    actionLabel = if (canCreate) createLabel else null,
                    onAction = if (canCreate) onCreateClick else null
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
                            accountName = accountRef(item)?.let { accountsByName[it]?.name },
                            categoryName = item.categoryId?.let { categoriesByName[it]?.name },
                            onClick = { onItemClick(item.id) }
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
internal fun TransactionRow(
    item: TransactionEntity,
    accountName: String?,
    categoryName: String?,
    onClick: () -> Unit,
    /** Detalhe inicial (ex.: tipo no histórico unificado da Task 4.11). */
    leadingDetail: String? = null,
    /** false quando ainda não existe tela de destino (nenhuma ação aparente). */
    clickable: Boolean = true
) {
    val cancelled = item.status == "CANCELLED"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (clickable) Modifier.clickable(role = Role.Button, onClick = onClick)
                else Modifier
            )
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
                    leadingDetail,
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
