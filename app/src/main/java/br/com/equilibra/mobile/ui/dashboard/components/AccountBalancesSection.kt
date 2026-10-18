package br.com.equilibra.mobile.ui.dashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import br.com.equilibra.mobile.domain.model.dashboard.AccountBalance
import br.com.equilibra.mobile.domain.util.MoneyFormatter

/**
 * Seção de saldos por conta (Task 2.4).
 *
 * - Dados vêm do endpoint dashboard (sem chamadas duplicadas)
 * - Ordenação estável (ordem da API, alfabética por nome)
 * - Lista vazia e erro tratados no pai (DashboardScreen)
 * - Clique navega para detalhe/lista quando destino existir (placeholder por enquanto)
 * - Formatação consistente via [MoneyFormatter]
 */
@Composable
fun AccountBalancesSection(
    accounts: List<AccountBalance>,
    modifier: Modifier = Modifier,
    onAccountClick: (AccountBalance) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Saldos por conta" },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Saldos por conta",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (accounts.isEmpty()) {
            EmptyAccountsCard()
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                // Ordenação estável: alfabética por nome
                val sorted = accounts.sortedBy { it.name.lowercase() }

                sorted.forEachIndexed { index, account ->
                    AccountBalanceRow(
                        account = account,
                        onClick = { onAccountClick(account) }
                    )
                    if (index < sorted.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAccountsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Nenhuma conta cadastrada",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = "Nenhuma conta cadastrada" }
            )
        }
    }
}

@Composable
private fun AccountBalanceRow(
    account: AccountBalance,
    onClick: () -> Unit
) {
    val isPositive = try {
        MoneyFormatter.parse(account.currentBalance) >= java.math.BigDecimal.ZERO
    } catch (_: Exception) {
        true
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics {
                contentDescription = "${account.name}: ${MoneyFormatter.format(account.currentBalance)}"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = accountTypeLabel(account.type),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = MoneyFormatter.format(account.currentBalance),
                style = MaterialTheme.typography.titleSmall,
                color = if (isPositive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(20.dp)
                .padding(start = 8.dp)
        )
    }
}

/**
 * Label amigável para o tipo de conta.
 */
private fun accountTypeLabel(type: String): String {
    return when (type.uppercase()) {
        "CHECKING" -> "Conta corrente"
        "SAVINGS" -> "Poupança"
        "CASH" -> "Dinheiro"
        "INVESTMENT" -> "Investimento"
        else -> type
    }
}
