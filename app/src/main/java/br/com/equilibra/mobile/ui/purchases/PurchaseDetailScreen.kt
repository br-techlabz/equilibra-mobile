package br.com.equilibra.mobile.ui.purchases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.equilibra.mobile.data.local.purchase.InstallmentEntity
import br.com.equilibra.mobile.data.remote.dto.purchase.PurchaseStatusDto
import br.com.equilibra.mobile.domain.util.MoneyFormatter
import br.com.equilibra.mobile.ui.components.ListError
import br.com.equilibra.mobile.ui.components.ListLoading
import java.time.LocalDate

/**
 * Task 6.9 — Detalhe da compra.
 *
 * - Valor total, parcelas e ciclos exatamente como o backend devolveu;
 *   nenhum campo ausente é inferido nem recalculado.
 * - Compra cancelada é exibida com o status explícito e sem ação de cancelar.
 * - 404 é estado terminal ("não encontrada"), distinto de erro recuperável.
 */
@Composable
fun PurchaseDetailScreen(
    onBack: () -> Unit,
    viewModel: PurchaseDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        val s = state as? PurchaseDetailUiState.Success ?: return@LaunchedEffect
        s.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state) {
        val s = state as? PurchaseDetailUiState.Success ?: return@LaunchedEffect
        s.warning?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearWarning()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Tela de detalhe da compra" }
    ) {
        when (val s = state) {
            is PurchaseDetailUiState.Loading -> ListLoading("Carregando compra...")

            is PurchaseDetailUiState.NotFound -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Compra não encontrada",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ela pode ter sido removida em outro dispositivo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onBack) { Text("Voltar") }
            }

            is PurchaseDetailUiState.Error -> ListError(
                message = s.message,
                onRetry = { viewModel.retry() }
            )

            is PurchaseDetailUiState.Success -> DetailContent(
                state = s,
                onCancelClick = viewModel::onCancelClick,
                onBack = onBack
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    val s = state as? PurchaseDetailUiState.Success
    if (s?.confirmCancelVisible == true) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissCancel,
            title = { Text("Cancelar compra?") },
            text = {
                Text(
                    "A compra \"${s.purchase.description}\" será marcada como cancelada. " +
                        "O histórico é preservado e não pode ser desfeito."
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

@Composable
private fun DetailContent(
    state: PurchaseDetailUiState.Success,
    onCancelClick: () -> Unit,
    onBack: () -> Unit
) {
    val purchase = state.purchase
    val ativa = purchase.status == PurchaseStatusDto.ACTIVE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = purchase.description,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = MoneyFormatter.format(purchase.totalAmount),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = PurchaseStatusDto.labelPtBr(purchase.status),
                style = MaterialTheme.typography.labelLarge,
                color = if (ativa) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (purchase.installmentCount > 1) "${purchase.installmentCount} parcelas"
            else "À vista",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Compra em ${formatIsoDate(purchase.purchaseDate)} • fatura de ${formatCycle(purchase.cycleMonth)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Parcelas",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state.installments.isEmpty()) {
            Text(
                text = "O backend não retornou parcelas para esta compra.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    state.installments.forEachIndexed { index, parcela ->
                        InstallmentRow(parcela)
                        if (index != state.installments.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (state.canCancel) {
            Button(
                onClick = onCancelClick,
                enabled = !state.isCancelling,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isCancelling) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Cancelar compra")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Voltar")
        }
    }
}

@Composable
private fun InstallmentRow(parcela: InstallmentEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics {
                contentDescription =
                    "Parcela ${parcela.sequenceNumber} de ${parcela.totalInstallments}: " +
                        "${MoneyFormatter.format(parcela.principalAmount)}, fatura de " +
                        formatCycle(parcela.cycleMonth)
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${parcela.sequenceNumber}/${parcela.totalInstallments}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "fatura de ${formatCycle(parcela.cycleMonth)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = MoneyFormatter.format(parcela.principalAmount),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** "2026-10-15" → "15/10/2026"; entrada inesperada é exibida como veio. */
private fun formatIsoDate(iso: String): String = runCatching {
    val d = LocalDate.parse(iso)
    "%02d/%02d/%04d".format(d.dayOfMonth, d.monthValue, d.year)
}.getOrDefault(iso)

/** "2026-11-01" → "11/2026"; entrada inesperada é exibida como veio. */
private fun formatCycle(iso: String): String = runCatching {
    val d = LocalDate.parse(iso)
    "%02d/%04d".format(d.monthValue, d.year)
}.getOrDefault(iso)
