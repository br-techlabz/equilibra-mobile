package br.com.equilibra.mobile.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.equilibra.mobile.domain.model.dashboard.DashboardSummary
import br.com.equilibra.mobile.domain.util.MoneyFormatter
import br.com.equilibra.mobile.ui.theme.LocalFinancialColors

/**
 * Cards de resumo financeiro (Task 2.3).
 *
 * Exibe: receita, despesa, saldo líquido e patrimônio.
 * - Valores vindos da API (período aplicado)
 * - Moeda pt-BR via [MoneyFormatter]
 * - Sem conversão Float/Double (BigDecimal)
 * - Cores semânticas via [LocalFinancialColors] (Task 4.13, tema claro/escuro):
 *   receita verde, despesa vermelha, saldo/netWorth neutro
 * - Acessibilidade: contentDescription nos valores e labels — o tipo nunca
 *   depende só da cor (rótulo + ícone direcional acompanham)
 */
@Composable
fun SummaryCards(
    summary: DashboardSummary,
    modifier: Modifier = Modifier
) {
    val financialColors = LocalFinancialColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Resumo financeiro" },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Linha 1: Receita e Despesa
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                label = "Receita",
                value = MoneyFormatter.format(summary.income),
                icon = Icons.Default.ArrowDownward,
                iconTint = financialColors.income,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                label = "Despesa",
                value = MoneyFormatter.format(summary.expense),
                icon = Icons.Default.ArrowUpward,
                iconTint = financialColors.expense,
                modifier = Modifier.weight(1f)
            )
        }

        // Linha 2: Saldo e Patrimônio
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                label = "Saldo líquido",
                value = MoneyFormatter.format(summary.net),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconTint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                label = "Patrimônio",
                value = MoneyFormatter.format(summary.netWorth),
                icon = Icons.Default.AccountBalance,
                iconTint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Card individual de resumo.
 *
 * - label: descrição (ex.: "Receita")
 * - value: valor formatado (ex.: "R$ 1.234,56")
 * - icon: ícone semântico
 * - iconTint: cor do ícone (ex.: verde para receita)
 */
@Composable
private fun SummaryCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.semantics {
            contentDescription = "$label: $value"
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.height(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
