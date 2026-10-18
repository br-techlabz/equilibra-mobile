package br.com.equilibra.mobile.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.equilibra.mobile.ui.dashboard.components.AccountBalancesSection
import br.com.equilibra.mobile.ui.dashboard.components.BalanceEvolutionChart
import br.com.equilibra.mobile.ui.dashboard.components.RecentTransactionsSection
import br.com.equilibra.mobile.ui.dashboard.components.SummaryCards

/**
 * Tela de Dashboard (Task 2.6).
 *
 * Seções:
 * - SummaryCards (Task 2.3)
 * - AccountBalancesSection (Task 2.4) — saldos por conta
 * - RecentTransactionsSection (Task 2.5) — feed de transações recentes
 * - BalanceEvolutionChart (Task 2.6) — gráfico temporal via MPAndroidChart
 * - As Tasks 2.7–2.10 implementarão as demais seções
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onAccountClick: (String) -> Unit = {},
    onTransactionClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = "Tela de dashboard" }
    ) {
        when (val s = state) {
            is DashboardUiState.Loading -> LoadingContent()

            is DashboardUiState.Success -> SuccessContent(
                periodLabel = s.periodLabel,
                summary = s.dashboard.summary,
                accounts = s.dashboard.accounts,
                transactions = s.dashboard.recentTransactions,
                balanceEvolution = s.dashboard.balanceEvolution,
                onAccountClick = onAccountClick,
                onTransactionClick = onTransactionClick
            )

            is DashboardUiState.Error -> ErrorContent(
                message = s.message,
                canRetry = s.canRetry,
                onRetry = { viewModel.retry() }
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Carregando dashboard...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SuccessContent(
    periodLabel: String,
    summary: br.com.equilibra.mobile.domain.model.dashboard.DashboardSummary,
    accounts: List<br.com.equilibra.mobile.domain.model.dashboard.AccountBalance>,
    transactions: List<br.com.equilibra.mobile.domain.model.dashboard.RecentTransaction>,
    balanceEvolution: List<br.com.equilibra.mobile.domain.model.dashboard.BalanceSeries>,
    onAccountClick: (String) -> Unit,
    onTransactionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = periodLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { contentDescription = "Período: $periodLabel" }
        )

        SummaryCards(summary = summary)

        AccountBalancesSection(
            accounts = accounts,
            onAccountClick = { onAccountClick(it.accountId) }
        )

        RecentTransactionsSection(
            transactions = transactions,
            onTransactionClick = { onTransactionClick(it.id) }
        )

        BalanceEvolutionChart(
            series = balanceEvolution
        )

        // Seções futuras (Tasks 2.7–2.10) serão adicionadas aqui
    }
}

@Composable
private fun ErrorContent(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        if (canRetry) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Tentar novamente")
            }
        }
    }
}
