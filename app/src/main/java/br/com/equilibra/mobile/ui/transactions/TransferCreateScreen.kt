package br.com.equilibra.mobile.ui.transactions

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.equilibra.mobile.ui.components.AccountSelector
import br.com.equilibra.mobile.ui.components.CategorySelector
import br.com.equilibra.mobile.ui.components.TagSelector

/**
 * Task 4.9 — Tela de transferência entre contas.
 *
 * - Campos: descrição, valor, data, conta origem, conta destino, categoria
 *   (obrigatória no contrato), tags e notas.
 * - Origem = destino é bloqueado no cliente (erro inline antes do envio).
 * - Payload único de transferência; saldos ficam com o backend.
 * - Categorias do seletor filtradas por BOTH (válidas nos dois sentidos);
 *   o backend aceita qualquer categoria ativa — filtro é decisão do cliente.
 */
@Composable
fun TransferCreateScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: CreateTransferViewModel = hiltViewModel()
) {
    val state by viewModel.formState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val generalError by viewModel.generalError.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            viewModel.consumeSuccess()
            onSuccess()
        }
    }

    LaunchedEffect(generalError) {
        generalError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearGeneralError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.semantics { contentDescription = "Tela de transferência" }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Nova transferência",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Descrição") },
                isError = state.descriptionError != null,
                supportingText = state.descriptionError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.amount,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Valor (R$)") },
                isError = state.amountError != null,
                supportingText = state.amountError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.occurredDate,
                onValueChange = viewModel::onDateChange,
                label = { Text("Data (dd/MM/aaaa)") },
                isError = state.dateError != null,
                supportingText = state.dateError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            AccountSelector(
                selectedId = state.sourceAccountId,
                // Obrigatórias: retap (null) é ignorado — validação exige no envio
                onSelectionChange = { id -> if (id != null) viewModel.onSourceAccountChange(id) }
            )
            state.sourceAccountError?.let { FieldError(it) }

            AccountSelector(
                selectedId = state.destinationAccountId,
                onSelectionChange = { id -> if (id != null) viewModel.onDestinationAccountChange(id) }
            )
            state.destinationAccountError?.let { FieldError(it) }

            CategorySelector(
                applicability = "BOTH",
                selectedIds = listOfNotNull(state.categoryId),
                onSelectionChange = { ids -> ids.firstOrNull()?.let(viewModel::onCategoryChange) },
                multiSelect = false
            )
            state.categoryError?.let { FieldError(it) }

            TagSelector(
                selectedIds = state.tagIds,
                onSelectionChange = viewModel::onTagsChange,
                multiSelect = true
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notas (opcional)") },
                isError = state.notesError != null,
                supportingText = state.notesError?.let { { Text(it) } },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::onSaveClick,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Transferir")
                }
            }

            TextButton(onClick = onBack, enabled = !isSaving) {
                Text("Voltar")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FieldError(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error
    )
}
