package br.com.equilibra.mobile.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.equilibra.mobile.domain.util.MoneyFormatter
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Task 3.8 — Tela de reconciliação de conta.
 *
 * - Saldo esperado (cache), data/hora e saldo informado
 * - Diferença preview (BigDecimal)
 * - Confirmação antes de enviar (envio único)
 * - Resultado da API: calculado, informado, diferença, ajuste (backend gera o ajuste)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconcileAccountScreen(
    onBack: () -> Unit,
    viewModel: ReconcileAccountViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.generalError) {
        state.generalError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearGeneralError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .semantics { contentDescription = "Tela de reconciliação" },
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Reconciliar conta",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        state.account?.let { acc ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = acc.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Saldo esperado (do cache local — nada é criado localmente)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Saldo esperado (pelo sistema)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = MoneyFormatter.format(state.account?.currentBalance ?: "0"),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics {
                        contentDescription = "Saldo esperado: ${MoneyFormatter.format(state.account?.currentBalance ?: "0")}"
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Data (DatePicker) + Hora
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.date,
                onValueChange = {},
                readOnly = true,
                label = { Text("Data") },
                isError = state.dateError != null,
                supportingText = state.dateError?.let { { Text(it) } },
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) {
                        Text("Escolher")
                    }
                }
            )
            OutlinedTextField(
                value = state.time,
                onValueChange = viewModel::onTimeChange,
                label = { Text("Hora (HH:mm)") },
                isError = state.timeError != null,
                supportingText = state.timeError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Saldo informado
        OutlinedTextField(
            value = state.informedBalanceInput,
            onValueChange = viewModel::onInformedBalanceChange,
            label = { Text("Saldo informado") },
            placeholder = { Text("0,00") },
            isError = state.balanceError != null,
            supportingText = state.balanceError?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        // Preview da diferença (BigDecimal — sem Double)
        state.previewDifference?.let { diff ->
            Spacer(modifier = Modifier.height(16.dp))
            val isZero = diff.compareTo(BigDecimal.ZERO) == 0
            Text(
                text = "Diferença prevista: ${MoneyFormatter.format(diff.toPlainString())}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isZero) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = viewModel::onReconcileClick,
            enabled = !state.isSubmitting && state.account != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Reconciliar")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onBack) {
            Text(if (state.isSuccess) "Concluir" else "Voltar")
        }

        // Resultado da API
        state.result?.let { result ->
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Reconciliação concluída",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    ResultRow("Saldo calculado", MoneyFormatter.format(result.calculatedBalance))
                    ResultRow("Saldo informado", MoneyFormatter.format(result.informedBalance))
                    ResultRow("Diferença", MoneyFormatter.format(result.difference))
                    result.adjustmentType?.let {
                        ResultRow("Ajuste criado", it)
                    }
                    result.createdTransactionId?.let {
                        ResultRow("Lançamento", it)
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState)
    }

    // DatePicker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = runCatching {
                LocalDate.parse(state.date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            }.getOrNull() ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val d = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.onDateChange(d.toString())
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Diálogo de confirmação antes de enviar (mostra esperado/informado/diferença)
    if (state.confirmVisible) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissConfirm,
            title = { Text("Confirmar reconciliação?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Saldo esperado: ${MoneyFormatter.format(state.account?.currentBalance ?: "0")}")
                    state.informed?.let {
                        Text("Saldo informado: ${MoneyFormatter.format(it.toPlainString())}")
                    }
                    state.previewDifference?.let {
                        val isZero = it.compareTo(BigDecimal.ZERO) == 0
                        Text(
                            text = "Diferença: ${MoneyFormatter.format(it.toPlainString())}",
                            color = if (isZero) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = "Um ajuste será criado no servidor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmSubmit) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissConfirm) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
