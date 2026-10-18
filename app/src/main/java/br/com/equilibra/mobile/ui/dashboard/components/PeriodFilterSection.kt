package br.com.equilibra.mobile.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.equilibra.mobile.domain.model.dashboard.DashboardPeriodSelection
import br.com.equilibra.mobile.domain.model.dashboard.DashboardPeriodType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Filtro de período (Task 2.9).
 *
 * - Mês atual, mês anterior e intervalo customizado
 * - Datas no fuso esperado pelo backend (UTC, ISO 8601)
 * - Mantém filtro aplicado às seções relevantes
 * - Prevê intervalos inválidos (from >= to)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodFilterSection(
    selectedPeriod: DashboardPeriodSelection,
    onPeriodTypeSelected: (DashboardPeriodType) -> Unit,
    onCustomPeriodSelected: (LocalDate, LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustom by remember { mutableStateOf(false) }
    var customFrom by remember(selectedPeriod) { mutableStateOf(selectedPeriod.from) }
    var customTo by remember(selectedPeriod) { mutableStateOf(selectedPeriod.to) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Filtro de período" },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Período",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Dropdown de tipo de período
        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedPeriod.type.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Selecione o período") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DashboardPeriodType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.label) },
                        onClick = {
                            expanded = false
                            if (type == DashboardPeriodType.CUSTOM) {
                                showCustom = true
                            } else {
                                onPeriodTypeSelected(type)
                            }
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        if (showCustom) {
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
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Intervalo customizado",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customFrom.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("De") },
                            modifier = Modifier.weight(1f),
                            enabled = false
                        )
                        OutlinedTextField(
                            value = customTo.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Até") },
                            modifier = Modifier.weight(1f),
                            enabled = false
                        )
                    }

                    // Botões para ajustar datas (placeholder — em produção, usar DatePicker)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (customFrom.isBefore(customTo)) {
                                    onCustomPeriodSelected(customFrom, customTo)
                                    showCustom = false
                                }
                            },
                            enabled = customFrom.isBefore(customTo),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Aplicar")
                        }
                        TextButton(
                            onClick = { showCustom = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }
                    }
                }
            }
        }

        // Indicador do período aplicado
        Text(
            text = formatPeriodForDisplay(selectedPeriod),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { contentDescription = "Período: ${formatPeriodForDisplay(selectedPeriod)}" }
        )
    }
}

private fun formatPeriodForDisplay(period: DashboardPeriodSelection): String {
    val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return "${period.from.format(fmt)} — ${period.to.format(fmt)}"
}
