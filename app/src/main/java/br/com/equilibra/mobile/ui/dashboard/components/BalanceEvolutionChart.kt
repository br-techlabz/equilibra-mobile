package br.com.equilibra.mobile.ui.dashboard.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import br.com.equilibra.mobile.domain.model.dashboard.BalanceSeries
import br.com.equilibra.mobile.domain.util.MoneyFormatter
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Gráfico de evolução de saldo (Task 2.6).
 *
 * - Usa MPAndroidChart (LineChart) via AndroidView (Compose)
 * - Dados: balanceEvolution da API (lista de BalanceSeries por conta)
 * - Eixos: X = datas (dd/MM), Y = valores (R$)
 * - Sem dados: card com mensagem, não quebra
 * - Valores correspondem à API (BigDecimal, sem Double)
 * - Legível em telas pequenas (altura fixa, scroll da tela pai)
 */
@Composable
fun BalanceEvolutionChart(
    series: List<BalanceSeries>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Gráfico de evolução de saldo" },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Evolução de saldo",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (series.isEmpty() || series.all { it.points.isEmpty() }) {
            EmptyChartCard()
        } else {
            ChartCard(series = series)
        }
    }
}

@Composable
private fun EmptyChartCard() {
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
                .padding(24.dp)
        ) {
            Text(
                text = "Sem dados para exibir",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = "Sem dados para exibir" }
            )
            Text(
                text = "Faça transações no período para ver a evolução",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChartCard(series: List<BalanceSeries>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        val context = LocalContext.current
        val dateLabels = remember(series) {
            // Usa os pontos da primeira série como referência de datas
            series.firstOrNull()?.points?.map { point ->
                formatChartDate(point.at)
            } ?: emptyList()
        }

        val chartColor = MaterialTheme.colorScheme.primary
        val textColor = MaterialTheme.colorScheme.onSurfaceVariant

        AndroidView(
            factory = { ctx ->
                LineChart(ctx).apply {
                    // Configurações gerais
                    description.isEnabled = false
                    legend.isEnabled = series.size > 1
                    setTouchEnabled(true)
                    isDragEnabled = true
                    setScaleEnabled(true)
                    setPinchZoom(false)
                    setBackgroundColor(AndroidColor.TRANSPARENT)

                    // Eixo X
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.textColor = AndroidColor.GRAY
                    xAxis.textSize = 9f
                    xAxis.granularity = 1f
                    xAxis.setDrawGridLines(false)
                    xAxis.labelRotationAngle = -45f
                    xAxis.valueFormatter = IndexAxisValueFormatter(dateLabels)

                    // Eixo Y
                    axisLeft.textColor = AndroidColor.GRAY
                    axisLeft.textSize = 9f
                    axisLeft.setDrawGridLines(true)
                    axisLeft.gridColor = AndroidColor.LTGRAY
                    axisLeft.valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return MoneyFormatter.format(value.toBigDecimal().toString())
                        }
                    }

                    axisRight.isEnabled = false
                }
            },
            update = { chart ->
                // Converte series para Entry (x = índice, y = balance)
                val dataSets = series.mapIndexed { seriesIndex, s ->
                    val entries = s.points.mapIndexed { index, point ->
                        val balance = try {
                            BigDecimal(point.balance).toFloat()
                        } catch (_: Exception) {
                            0f
                        }
                        Entry(index.toFloat(), balance)
                    }
                    val lineColor = ChartColors.forSeries(seriesIndex)
                    LineDataSet(entries, s.accountName).apply {
                        color = lineColor
                        setCircleColor(lineColor)
                        lineWidth = 2f
                        setDrawCircles(true)
                        circleRadius = 3f
                        setDrawValues(false) // não exibe valores nos pontos (poluição visual)
                        mode = LineDataSet.Mode.LINEAR
                        setDrawFilled(false)
                        valueTextColor = AndroidColor.GRAY
                        valueTextSize = 9f
                    }
                }

                chart.data = LineData(dataSets)
                chart.invalidate()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(8.dp)
        )
    }
}

private fun formatChartDate(isoInstant: String): String {
    return try {
        val instant = Instant.parse(isoInstant)
        val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        date.format(DateTimeFormatter.ofPattern("dd/MM"))
    } catch (_: Exception) {
        isoInstant.take(10)
    }
}
