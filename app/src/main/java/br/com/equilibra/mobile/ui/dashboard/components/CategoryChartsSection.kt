package br.com.equilibra.mobile.ui.dashboard.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import br.com.equilibra.mobile.domain.model.dashboard.CategoryExpense
import br.com.equilibra.mobile.domain.util.MoneyFormatter
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.math.BigDecimal

/**
 * Seção de gráficos por categoria (Task 2.7) — formato pizza.
 *
 * - Dados agregados da API: expenseByCategory e incomeByCategory
 * - Permite distinguir despesa/receita (cores: vermelho para despesa, verde para receita)
 * - Inclui legenda, cores semânticas acessíveis e fallback quando não houver categorias
 * - Pizza (PieChart) via MPAndroidChart + AndroidView — adapta-se à largura
 */
@Composable
fun CategoryChartsSection(
    expenseByCategory: List<CategoryExpense>,
    incomeByCategory: List<CategoryExpense>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Gráficos por categoria" },
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Despesas por categoria",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (expenseByCategory.isEmpty()) {
            EmptyCategoryCard("Nenhuma despesa por categoria no período")
        } else {
            CategoryChartCard(
                categories = expenseByCategory,
                isExpense = true
            )
            CategoryTotalsCard(
                categories = expenseByCategory,
                title = "Total de despesas"
            )
        }

        HorizontalDivider()

        Text(
            text = "Receitas por categoria",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (incomeByCategory.isEmpty()) {
            EmptyCategoryCard("Nenhuma receita por categoria no período")
        } else {
            CategoryChartCard(
                categories = incomeByCategory,
                isExpense = false
            )
            CategoryTotalsCard(
                categories = incomeByCategory,
                title = "Total de receitas"
            )
        }
    }
}

@Composable
private fun EmptyCategoryCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .semantics { contentDescription = message }
        )
    }
}

@Composable
private fun CategoryChartCard(
    categories: List<CategoryExpense>,
    isExpense: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        val colors = if (isExpense) {
            // Despesa: vermelho (acessível)
            listOf(
                AndroidColor.parseColor("#C62828"),
                AndroidColor.parseColor("#EF5350"),
                AndroidColor.parseColor("#FF8A80"),
                AndroidColor.parseColor("#B71C1C"),
                AndroidColor.parseColor("#FF6F00"),
                AndroidColor.parseColor("#E65100")
            )
        } else {
            // Receita: verde (acessível)
            listOf(
                AndroidColor.parseColor("#2E7D32"),
                AndroidColor.parseColor("#66BB6A"),
                AndroidColor.parseColor("#A5D6A7"),
                AndroidColor.parseColor("#1B5E20"),
                AndroidColor.parseColor("#388E3C"),
                AndroidColor.parseColor("#81C784")
            )
        }

        AndroidView(
            factory = { ctx ->
                PieChart(ctx).apply {
                    description.isEnabled = false
                    setDrawEntryLabels(false)
                    isDrawHoleEnabled = true
                    holeRadius = 42f
                    transparentCircleRadius = 46f
                    setHoleColor(AndroidColor.TRANSPARENT)
                    setTransparentCircleColor(AndroidColor.TRANSPARENT)
                    setTransparentCircleAlpha(0)
                    legend.isEnabled = false
                    isRotationEnabled = true
                    isHighlightPerTapEnabled = true
                    setExtraOffsets(12f, 12f, 12f, 12f)
                    animateY(600)
                }
            },
            update = { chart ->
                val entries = categories.map { cat ->
                    val amount = try {
                        BigDecimal(cat.amount).toFloat()
                    } catch (_: Exception) {
                        0f
                    }
                    PieEntry(amount)
                }

                val dataSet = PieDataSet(entries, if (isExpense) "Despesa" else "Receita").apply {
                    sliceSpace = 2f
                    selectionShift = 6f
                    setColors(colors.take(categories.size))
                    setDrawValues(false)
                }

                chart.data = PieData(dataSet)
                // texto central com total
                val total = categories.fold(BigDecimal.ZERO) { acc, cat ->
                    acc + MoneyFormatter.parse(cat.amount)
                }
                chart.centerText = MoneyFormatter.format(total.toString())
                chart.setCenterTextSize(13f)
                chart.setCenterTextColor(AndroidColor.DKGRAY)
                chart.invalidate()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(8.dp)
        )

        // Legenda manual (cores + nome + valor) — mantém consistência e acessibilidade
        CategoryLegend(categories = categories, colors = colors)
    }
}

@Composable
private fun CategoryLegend(
    categories: List<CategoryExpense>,
    colors: List<Int>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        categories.forEachIndexed { index, cat ->
            val color = colors.getOrElse(index % colors.size) { colors[0] }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(12.dp)
                            .padding(1.dp)
                            .semantics { contentDescription = cat.categoryName }
                    ) {
                        androidx.compose.material3.Surface(
                            modifier = Modifier.size(10.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = Color(color)
                        ) {}
                    }
                    Text(
                        text = cat.categoryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = MoneyFormatter.format(cat.amount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CategoryTotalsCard(
    categories: List<CategoryExpense>,
    title: String
) {
    val total = categories.fold(BigDecimal.ZERO) { acc, cat ->
        acc + MoneyFormatter.parse(cat.amount)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = MoneyFormatter.format(total.toString()),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.semantics { contentDescription = "$title: ${MoneyFormatter.format(total.toString())}" }
            )
        }
    }
}
