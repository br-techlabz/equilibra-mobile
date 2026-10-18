package br.com.equilibra.mobile.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.equilibra.mobile.domain.model.dashboard.CardSummary
import br.com.equilibra.mobile.domain.util.MoneyFormatter
import java.math.BigDecimal

/**
 * Seção de resumo de cartão (Task 2.8).
 *
 * - Campos reais da API: pendingStatements, nextDueAmount, committedLimit, availableLimit
 * - Não calcula faturas se backend já fornece agregados
 * - Trate ausência de cartões e faturas (valores "0.00" → mensagem "Nenhum cartão")
 * - Navegação para Cartões quando disponível (via onNavigateToCards)
 */
@Composable
fun CardSummarySection(
    cardSummary: CardSummary,
    modifier: Modifier = Modifier,
    onNavigateToCards: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Resumo de cartão" },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Resumo de cartão",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onNavigateToCards) {
                Text("Ver cartões")
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }
        }

        if (isNoCards(cardSummary)) {
            EmptyCardSummaryCard()
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CardSummaryItem(
                        icon = Icons.Default.PendingActions,
                        label = "Pendências",
                        value = MoneyFormatter.format(cardSummary.pendingStatements),
                        contentDesc = "Pendências: ${MoneyFormatter.format(cardSummary.pendingStatements)}"
                    )
                    CardSummaryItem(
                        icon = Icons.Default.CalendarMonth,
                        label = "Próximo vencimento",
                        value = MoneyFormatter.format(cardSummary.nextDueAmount),
                        contentDesc = "Próximo vencimento: ${MoneyFormatter.format(cardSummary.nextDueAmount)}"
                    )
                    CardSummaryItem(
                        icon = Icons.Default.AccountBalance,
                        label = "Limite comprometido",
                        value = MoneyFormatter.format(cardSummary.committedLimit),
                        contentDesc = "Limite comprometido: ${MoneyFormatter.format(cardSummary.committedLimit)}"
                    )
                    CardSummaryItem(
                        icon = Icons.Default.CreditCard,
                        label = "Limite disponível",
                        value = MoneyFormatter.format(cardSummary.availableLimit),
                        contentDesc = "Limite disponível: ${MoneyFormatter.format(cardSummary.availableLimit)}"
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCardSummaryCard() {
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
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.height(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Nenhum cartão cadastrado",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = "Nenhum cartão cadastrado" }
            )
            Text(
                text = "Cadastre um cartão para ver o resumo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CardSummaryItem(
    icon: ImageVector,
    label: String,
    value: String,
    contentDesc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = contentDesc },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Verifica se não há cartões (todos os valores são "0.00" ou vazios).
 */
private fun isNoCards(cardSummary: CardSummary): Boolean {
    val zero = BigDecimal("0.00")
    return try {
        BigDecimal(cardSummary.pendingStatements) == zero &&
            BigDecimal(cardSummary.nextDueAmount) == zero &&
            BigDecimal(cardSummary.committedLimit) == zero &&
            BigDecimal(cardSummary.availableLimit) == zero
    } catch (_: Exception) {
        false
    }
}
