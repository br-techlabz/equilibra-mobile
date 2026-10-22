package br.com.equilibra.mobile.ui.purchases

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.equilibra.mobile.ui.components.CategorySelector

/**
 * Task 6.8 — Cadastro de compra parcelada (1 a 60 vezes).
 *
 * - O ciclo da fatura NÃO é calculado aqui: o backend devolve cycleMonth
 *   e a lista/detalhe exibem.
 * - Categorias vêm do seletor compartilhado (aplicabilidade EXPENSE).
 */
@Composable
fun CreatePurchaseScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: CreatePurchaseViewModel = hiltViewModel(),
    /**
     * Slot do seletor de categoria: o padrão usa o [CategorySelector]
     * compartilhado (Hilt/Room). Testes de tela passam um substituto, já que
     * o seletor real exige o grafo de DI.
     */
    categorySelector: @Composable (() -> Unit)? = null
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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .semantics { contentDescription = "Tela de nova compra" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Nova compra",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text("Descrição") },
            isError = state.descriptionError != null,
            supportingText = state.descriptionError?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.amount,
            onValueChange = viewModel::onAmountChange,
            label = { Text("Valor total") },
            placeholder = { Text("0,00") },
            isError = state.amountError != null,
            supportingText = state.amountError?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.date,
            onValueChange = viewModel::onDateChange,
            label = { Text("Data da compra") },
            placeholder = { Text("dd/MM/aaaa") },
            isError = state.dateError != null,
            supportingText = state.dateError?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.installments,
            onValueChange = viewModel::onInstallmentsChange,
            label = { Text("Parcelas (1 a 60)") },
            isError = state.installmentsError != null,
            supportingText = state.installmentsError?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (categorySelector != null) {
            categorySelector()
        } else {
            CategorySelector(
                applicability = "EXPENSE",
                selectedIds = listOfNotNull(state.categoryId),
                onSelectionChange = { ids -> viewModel.onCategoryChange(ids.firstOrNull()) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        state.categoryError?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = viewModel::onSaveClick,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Registrar compra")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onBack) {
            Text("Voltar")
        }
    }
    }
}
