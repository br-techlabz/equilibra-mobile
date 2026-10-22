package br.com.equilibra.mobile.ui.purchases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.equilibra.mobile.data.local.purchase.PurchaseEntity
import br.com.equilibra.mobile.data.remote.dto.purchase.PurchaseStatusDto
import br.com.equilibra.mobile.domain.util.MoneyFormatter
import br.com.equilibra.mobile.ui.components.ListEmptyState
import br.com.equilibra.mobile.ui.components.ListError
import br.com.equilibra.mobile.ui.components.ListLoading
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Task 6.8 — Compras de um cartão.
 *
 * - Lista somente as compras do cartão da rota (filtro é do DAO).
 * - Valor total e ciclo de fatura vêm do backend; nada é recalculado aqui.
 * - Cancelar pede confirmação (compras canceladas continuam visíveis,
 *   marcadas — o backend preserva o histórico).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    onBack: () -> Unit,
    onCreateClick: (() -> Unit)? = null,
    viewModel: PurchasesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Tela de compras do cartão" }
    ) {
        when {
            state.isLoading -> ListLoading("Carregando compras...")
            state.purchases.isEmpty() && state.error != null ->
                ListError(message = state.error!!, onRetry = { viewModel.refresh() })
            else -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.purchases.isEmpty()) {
                    ListEmptyState(
                        title = "Nenhuma compra neste cartão",
                        subtitle = "Registre uma compra para acompanhar as parcelas",
                        actionLabel = onCreateClick?.let { "Nova compra" },
                        onAction = onCreateClick,
                        icon = Icons.Default.ShoppingCart
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Compras",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Row {
                                    if (onCreateClick != null) {
                                        TextButton(onClick = onCreateClick) { Text("Nova compra") }
                                    }
                                    TextButton(onClick = onBack) { Text("Voltar") }
                                }
                            }
                        }
                        items(state.purchases, key = { it.id }) { purchase ->
                            PurchaseRow(
                                purchase = purchase,
                                onCancel = { viewModel.onCancelClick(purchase.id) }
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    state.confirmCancelId?.let { id ->
        val alvo = state.purchases.firstOrNull { it.id == id }
        AlertDialog(
            onDismissRequest = viewModel::onDismissCancel,
            title = { Text("Cancelar compra?") },
            text = {
                Text(
                    "A compra \"${alvo?.description ?: "selecionada"}\" " +
                        "(${alvo?.let { MoneyFormatter.format(it.totalAmount) } ?: ""}) será marcada " +
                        "como cancelada. O histórico é preservado e não pode ser desfeito."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmCancel) { Text("Cancelar compra") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissCancel) { Text("Manter compra") }
            }
        )
    }
}

private val isoDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

@Composable
private fun PurchaseRow(purchase: PurchaseEntity, onCancel: () -> Unit) {
    val active = purchase.status == PurchaseStatusDto.ACTIVE
    val total = MoneyFormatter.format(purchase.totalAmount)
    val data = runCatching { LocalDate.parse(purchase.purchaseDate, isoDate) }.getOrNull()
    val dataTexto = data?.let { String.format("%02d/%02d/%04d", it.dayOfMonth, it.monthValue, it.year) }
        ?: purchase.purchaseDate
    val ciclo = runCatching { LocalDate.parse(purchase.cycleMonth, isoDate) }.getOrNull()
    val cicloTexto = ciclo?.let { "%02d/%04d".format(it.monthValue, it.year) } ?: purchase.cycleMonth
    val parcelas = if (purchase.installmentCount > 1) "${purchase.installmentCount}x" else "à vista"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${purchase.description}: $total em $parcelas, " +
                    "compra em $dataTexto, fatura de $cicloTexto, " +
                    "status ${PurchaseStatusDto.labelPtBr(purchase.status)}"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = purchase.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (active) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$dataTexto • $parcelas • fatura $cicloTexto",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = total, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = PurchaseStatusDto.labelPtBr(purchase.status),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
            if (active) {
                TextButton(onClick = onCancel) {
                    Text("Cancelar")
                }
            }
        }
    }
}
