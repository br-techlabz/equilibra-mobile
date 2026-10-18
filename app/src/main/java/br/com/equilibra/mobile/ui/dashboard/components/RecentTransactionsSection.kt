package br.com.equilibra.mobile.ui.dashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.equilibra.mobile.domain.model.dashboard.RecentTransaction
import br.com.equilibra.mobile.domain.util.MoneyFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Seção de transações recentes (Task 2.5).
 *
 * - Dados da API; ordenação respeita backend (por occurredAt descendente)
 * - Campos: tipo, descrição, data, conta/categoria (quando disponível), valor com semântica visual
 * - Diferencia canceladas (type/status CANCELLED) com ícone e estilo visual
 * - Clique abre detalhe somente se implementado (placeholder)
 * - Formatação de data em pt-BR e valores via BigDecimal
 * - Lista vazia e erro tratados no pai (DashboardScreen)
 */
@Composable
fun RecentTransactionsSection(
    transactions: List<RecentTransaction>,
    modifier: Modifier = Modifier,
    onTransactionClick: (RecentTransaction) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Transações recentes" },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Transações recentes",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (transactions.isEmpty()) {
            EmptyTransactionsCard()
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                // Ordenação respeita backend (já vem ordenada por occurredAt DESC)
                transactions.forEachIndexed { index, tx ->
                    RecentTransactionRow(
                        transaction = tx,
                        onClick = { onTransactionClick(tx) }
                    )
                    if (index < transactions.lastIndex) {
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
private fun EmptyTransactionsCard() {
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Nenhuma transação recente",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = "Nenhuma transação recente" }
            )
        }
    }
}

@Composable
private fun RecentTransactionRow(
    transaction: RecentTransaction,
    onClick: () -> Unit
) {
    val isCancelled = transaction.status.equals("CANCELLED", ignoreCase = true) ||
        transaction.type.equals("CANCELLED", ignoreCase = true)
    val isIncome = transaction.type.equals("INCOME", ignoreCase = true)
    val isTransfer = transaction.type.equals("TRANSFER", ignoreCase = true)

    val amountColor = when {
        isCancelled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        isIncome -> Color(0xFF2E7D32)
        isTransfer -> MaterialTheme.colorScheme.onSurface
        else -> Color(0xFFC62828)
    }

    val icon = when {
        isCancelled -> Icons.Default.Block
        isIncome -> Icons.Default.ArrowDownward
        isTransfer -> Icons.AutoMirrored.Filled.CompareArrows
        else -> Icons.Default.ArrowUpward
    }

    val formattedAmount = MoneyFormatter.format(transaction.amount)
    val prefix = when {
        isCancelled -> "" // sem prefixo quando cancelada
        isIncome -> "+"
        isTransfer -> ""
        else -> "-"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics {
                contentDescription = buildString {
                    append(transaction.description)
                    append(", ")
                    append(formatOccurredAt(transaction.occurredAt))
                    append(", ")
                    append(prefix)
                    append(formattedAmount)
                    if (isCancelled) append(", cancelada")
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isCancelled) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            } else {
                when {
                    isIncome -> Color(0xFF2E7D32)
                    isTransfer -> MaterialTheme.colorScheme.primary
                    else -> Color(0xFFC62828)
                }
            },
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = transaction.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (isCancelled) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatOccurredAt(transaction.occurredAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Conta ou categoria quando disponível
                val accountInfo = accountLabel(transaction)
                if (accountInfo != null) {
                    Text(
                        text = "• $accountInfo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isCancelled) {
                    Text(
                        text = "• Cancelada",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                }
            }

            // Payment method quando não é BANK_ACCOUNT
            if (transaction.paymentMethod != "BANK_ACCOUNT") {
                Text(
                    text = paymentMethodLabel(transaction.paymentMethod),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "$prefix$formattedAmount",
                style = MaterialTheme.typography.titleSmall.copy(
                    textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = amountColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!isCancelled && transaction.type.isNotBlank()) {
                Text(
                    text = typeLabel(transaction.type),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Formata a data de ocorrência em pt-BR (ex.: "15/10/2026").
 */
private fun formatOccurredAt(occurredAt: String): String {
    return try {
        val instant = Instant.parse(occurredAt)
        val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    } catch (_: Exception) {
        occurredAt
    }
}

/**
 * Label da conta (quando disponível).
 * Para BANK_ACCOUNT: mostra source ou destination; para CREDIT_CARD: mostra null.
 */
private fun accountLabel(transaction: RecentTransaction): String? {
    return when {
        transaction.sourceAccountId != null -> transaction.sourceAccountId
        transaction.destinationAccountId != null -> transaction.destinationAccountId
        else -> null
    }
}

private fun paymentMethodLabel(method: String): String {
    return when (method.uppercase()) {
        "CREDIT_CARD" -> "Cartão"
        "BANK_ACCOUNT" -> "Conta"
        else -> method
    }
}

private fun typeLabel(type: String): String {
    return when (type.uppercase()) {
        "INCOME" -> "Receita"
        "EXPENSE" -> "Despesa"
        "TRANSFER" -> "Transferência"
        "CANCELLED" -> "Cancelada"
        else -> type
    }
}
