package br.com.equilibra.mobile.ui.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import br.com.equilibra.mobile.data.remote.dto.creditcard.CreditCardStatusDto
import br.com.equilibra.mobile.domain.util.MoneyFormatter

/**
 * Task 6.5 — Tela de edição de cartão (reutiliza campos/validações da 6.4).
 *
 * - Salvar, Ativar/Desativar (com confirmação na desativação), Voltar
 * - Status e limites atuais exibidos (limites como o backend calculou)
 * - Desativação preserva compras e faturas: nada é apagado
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCreditCardScreen(
    onSuccess: () -> Unit = {},
    onBack: () -> Unit,
    viewModel: EditCreditCardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isSuccess, state.statusChanged) {
        if (state.isSuccess || state.statusChanged) {
            viewModel.consumeSuccess()
            onSuccess()
        }
    }

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
            .semantics { contentDescription = "Tela de editar cartão" },
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Editar cartão",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        state.card?.let { card ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Disponível: ${MoneyFormatter.format(card.availableLimit)} de ${MoneyFormatter.format(card.totalLimit)} • " +
                    CreditCardStatusDto.labelPtBr(card.status) +
                    if (card.status != CreditCardStatusDto.ACTIVE) "" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (state.notFound) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Cartão não encontrado. Ele pode ter sido removido em outro dispositivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onBack) {
                Text("Voltar")
            }
            return@Column
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Nome do cartão") },
            isError = state.nameError != null,
            supportingText = state.nameError?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.institution,
            onValueChange = viewModel::onInstitutionChange,
            label = { Text("Instituição") },
            isError = state.institutionError != null,
            supportingText = state.institutionError?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.nickname,
            onValueChange = viewModel::onNicknameChange,
            label = { Text("Apelido (opcional)") },
            isError = state.nicknameError != null,
            supportingText = state.nicknameError?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.limitInput,
            onValueChange = viewModel::onLimitChange,
            label = { Text("Limite total") },
            placeholder = { Text("0,00") },
            isError = state.limitError != null,
            supportingText = state.limitError?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.closingDayInput,
            onValueChange = viewModel::onClosingDayChange,
            label = { Text("Dia de fechamento") },
            placeholder = { Text("1-31") },
            isError = state.closingDayError != null,
            supportingText = state.closingDayError?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.dueDayInput,
            onValueChange = viewModel::onDueDayChange,
            label = { Text("Dia de vencimento") },
            placeholder = { Text("1-31") },
            isError = state.dueDayError != null,
            supportingText = state.dueDayError?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        var tzExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = tzExpanded,
            onExpandedChange = { tzExpanded = !tzExpanded }
        ) {
            OutlinedTextField(
                value = TimezoneOptions.labelFor(state.timezone),
                onValueChange = {},
                readOnly = true,
                label = { Text("Fuso horário") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(tzExpanded) },
                isError = state.timezoneError != null,
                supportingText = state.timezoneError?.let { { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = tzExpanded,
                onDismissRequest = { tzExpanded = false }
            ) {
                TimezoneOptions.options.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            viewModel.onTimezoneChange(value)
                            tzExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = viewModel::onSaveClick,
            enabled = !state.isLoading && !state.isToggling,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Salvar")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Ativar/Desativar
        val isActive = state.card?.status == CreditCardStatusDto.ACTIVE
        OutlinedButton(
            onClick = viewModel::onToggleActiveClick,
            enabled = state.card != null && !state.isLoading && !state.isToggling,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isToggling) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text(if (isActive) "Desativar cartão" else "Ativar cartão")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onBack) {
            Text("Voltar")
        }

        SnackbarHost(hostState = snackbarHostState)
    }

    // Diálogo de confirmação de desativação
    if (state.confirmDeactivateVisible) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissConfirmDeactivate,
            title = { Text("Desativar cartão?") },
            text = {
                Text(
                    "O cartão \"${state.name.ifBlank { "selecionado" }}\" ficará inativo. " +
                        "Compras e faturas já registradas são preservadas e o cartão para de " +
                        "aceitar novas associações até ser reativado."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmDeactivate) {
                    Text("Desativar")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissConfirmDeactivate) {
                    Text("Cancelar")
                }
            }
        )
    }
}
