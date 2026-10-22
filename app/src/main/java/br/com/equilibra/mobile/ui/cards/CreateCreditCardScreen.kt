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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
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

/**
 * Fusos mais usados no Brasil; mais a opção "Padrão" (null — backend aplica
 * "America/Sao_Paulo"). Qualquer valor digitado fora da lista não é possível
 * aqui (dropdown fechado), o que elimina timezone inválido no cliente.
 */
object TimezoneOptions {
    const val DEFAULT_LABEL = "Padrão (America/Sao_Paulo)"
    val options: List<Pair<String?, String>> = listOf(
        null to DEFAULT_LABEL,
        "America/Sao_Paulo" to "America/Sao_Paulo (Brasília)",
        "America/Manaus" to "America/Manaus",
        "America/Belem" to "America/Belem",
        "America/Fortaleza" to "America/Fortaleza",
        "America/Recife" to "America/Recife",
        "America/Bahia" to "America/Bahia",
        "America/Boa_Vista" to "America/Boa_Vista",
        "America/East" to "America/East (Fernando de Noronha)",
        "UTC" to "UTC"
    )

    fun labelFor(value: String?): String =
        options.firstOrNull { it.first == value }?.second ?: value ?: DEFAULT_LABEL
}

/**
 * Task 6.4 — Tela de criação de cartão.
 *
 * Campos: nome, instituição, apelido (opcional), limite, fechamento (1-31),
 * vencimento (1-31) e timezone (dropdown; vazio usa o padrão do backend).
 * O formulário NÃO impõe fechamento antes do vencimento — o backend não
 * valida essa relação (ver CreditCardFormValidator).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCreditCardScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: CreateCreditCardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
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
            .semantics { contentDescription = "Tela de criar cartão" },
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Novo cartão",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
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
            onClick = viewModel::onCreateClick,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Criar cartão")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onBack) {
            Text("Voltar")
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}
