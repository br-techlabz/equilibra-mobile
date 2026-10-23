package br.com.equilibra.mobile.ui.statements

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.equilibra.mobile.data.local.creditcard.CreditCardEntity
import br.com.equilibra.mobile.data.local.statement.StatementEntity
import br.com.equilibra.mobile.data.remote.dto.creditcard.CreditCardStatusDto
import br.com.equilibra.mobile.data.remote.dto.statement.StatementStatusDto
import br.com.equilibra.mobile.domain.util.DateTimeFormat
import br.com.equilibra.mobile.domain.util.MoneyFormatter
import br.com.equilibra.mobile.ui.components.ListEmptyState
import br.com.equilibra.mobile.ui.components.ListError
import br.com.equilibra.mobile.ui.components.ListLoading
import br.com.equilibra.mobile.ui.components.SelectorChipList
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Task 7.3 — Tela de faturas.
 *
 * - Seletor de cartão (todos os cartões, ativos e inativos).
 * - Fatura atual separada do histórico; histórico do ciclo mais recente para
 *   o mais antigo (cycleStart DESC, desempate por dueDate DESC).
 * - Status sempre visível (Aberta, Fechada, Vencida, Parcialmente paga, Paga,
 *   Cancelada). Nenhum valor é recalculado; tudo vem do backend/Room.
 * - Room-first: o cache aparece antes; a API atualiza em background.
 * - Pull-to-refresh, loading, vazio e erro com retry; snackbar para erros
 *   recuperáveis quando já existe conteúdo em tela.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementsScreen(
    onBack: () -> Unit,
    viewModel: StatementsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error, state.hasStatements) {
        val message = state.error
        // Sem conteúdo, o erro fica na tela (ListError) em vez de sumir no
        // snackbar. Com conteúdo, o dado local permanece e o aviso é passageiro.
        if (message != null && state.hasStatements) {
            snackbarHostState.showSnackbar(message)
            viewModel.dismissError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Tela de faturas" }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header(onBack = onBack)

            if (state.cards.isNotEmpty()) {
                CardSelector(
                    cards = state.cards,
                    selectedCardId = state.selectedCardId,
                    onCardSelected = viewModel::onCardSelected
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    state.isLoading -> ListLoading("Carregando faturas...")

                    state.cards.isEmpty() -> ListEmptyState(
                        title = "Nenhum cartão cadastrado",
                        subtitle = "Cadastre um cartão para acompanhar as faturas",
                        icon = Icons.Default.CreditCard
                    )

                    !state.hasStatements && state.error != null -> ListError(
                        message = state.error!!,
                        onRetry = viewModel::refresh
                    )

                    else -> PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        StatementsContent(
                            current = state.current,
                            history = state.history
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Faturas",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        TextButton(onClick = onBack) {
            Text("Voltar")
        }
    }
}

@Composable
private fun CardSelector(
    cards: List<CreditCardEntity>,
    selectedCardId: String?,
    onCardSelected: (String) -> Unit
) {
    SelectorChipList(
        title = "Cartão",
        options = cards.map { card ->
            card.id to if (card.status == CreditCardStatusDto.ACTIVE) {
                card.name
            } else {
                "${card.name} (inativo)"
            }
        },
        selectedIds = listOfNotNull(selectedCardId),
        multiSelect = false,
        isLoading = false,
        loadingMessage = "",
        emptyMessage = "",
        optionTagPrefix = "statement_card",
        onToggle = onCardSelected,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun StatementsContent(
    current: StatementEntity?,
    history: List<StatementEntity>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (current == null && history.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxHeight(0.7f)) {
                    ListEmptyState(
                        title = "Nenhuma fatura para este cartão",
                        subtitle = "As faturas aparecem aqui depois da primeira compra",
                        icon = Icons.Default.CreditCard
                    )
                }
            }
        } else {
            current?.let { statement ->
                item(key = "current_${statement.id}") {
                    CurrentStatementCard(statement)
                }
            }

            if (history.isNotEmpty()) {
                item(key = "history_title") {
                    Text(
                        text = "Histórico",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(history, key = { it.id }) { statement ->
                    StatementHistoryRow(statement)
                }
            }
        }
    }
}

@Composable
private fun CurrentStatementCard(statement: StatementEntity) {
    val status = StatementStatusDto.labelPtBr(statement.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Fatura atual: ciclo ${cycleRange(statement)}, " +
                    "vencimento ${formatDate(statement.dueDate)}, status $status, " +
                    "total ${MoneyFormatter.format(statement.totalAmount)}, " +
                    "pago ${MoneyFormatter.format(statement.paidAmount)}, " +
                    "pendente ${MoneyFormatter.format(statement.outstandingAmount)}"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Fatura atual",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Ciclo ${cycleRange(statement)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor(statement.status)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Vence em ${formatDate(statement.dueDate)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            AmountRow("Total", statement.totalAmount)
            AmountRow("Pago", statement.paidAmount)
            AmountRow("Pendente", statement.outstandingAmount, emphasize = true)
        }
    }
}

@Composable
private fun AmountRow(
    label: String,
    value: String,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = MoneyFormatter.format(value),
            style = if (emphasize) MaterialTheme.typography.titleSmall
            else MaterialTheme.typography.bodySmall,
            fontWeight = if (emphasize) FontWeight.SemiBold else null
        )
    }
}

@Composable
private fun StatementHistoryRow(statement: StatementEntity) {
    val status = StatementStatusDto.labelPtBr(statement.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Fatura do ciclo ${cycleMonth(statement.cycleStart)}, " +
                    "vencimento ${formatDate(statement.dueDate)}, status $status, " +
                    "total ${MoneyFormatter.format(statement.totalAmount)}, " +
                    "pago ${MoneyFormatter.format(statement.paidAmount)}, " +
                    "pendente ${MoneyFormatter.format(statement.outstandingAmount)}"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ciclo ${cycleMonth(statement.cycleStart)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Vence em ${formatDate(statement.dueDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = MoneyFormatter.format(statement.totalAmount),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor(statement.status)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Pago ${MoneyFormatter.format(statement.paidAmount)} • " +
                    "Pendente ${MoneyFormatter.format(statement.outstandingAmount)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun statusColor(status: String) = when (status) {
    StatementStatusDto.OVERDUE,
    StatementStatusDto.CANCELLED -> MaterialTheme.colorScheme.error

    StatementStatusDto.PAID -> MaterialTheme.colorScheme.primary

    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private val isoDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

private fun parseDate(iso: String): LocalDate? =
    runCatching { LocalDate.parse(iso, isoDate) }.getOrNull()

private fun formatDate(iso: String): String =
    parseDate(iso)?.let(DateTimeFormat::formatDate) ?: iso

private fun cycleRange(statement: StatementEntity): String =
    "${formatDate(statement.cycleStart)} a ${formatDate(statement.cycleEnd)}"

private fun cycleMonth(iso: String): String {
    val date = parseDate(iso) ?: return iso
    return "%02d/%04d".format(date.monthValue, date.year)
}
