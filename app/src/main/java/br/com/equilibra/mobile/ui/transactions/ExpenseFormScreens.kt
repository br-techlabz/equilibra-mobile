package br.com.equilibra.mobile.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Task 4.6 — Telas de criar/editar despesa sobre o formulário reutilizável.
 *
 * - Sucesso (repository já atualizou o Room) navega para trás.
 * - Erros HTTP/rede viram snackbar; salvamento bloqueia duplo toque.
 * - Edição de despesa cancelada é bloqueada (regra do backend).
 */
@Composable
fun CreateExpenseScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: CreateExpenseViewModel = hiltViewModel()
) {
    ExpenseFormScaffold(
        title = "Nova despesa",
        formState = viewModel.formState.collectAsState().value,
        config = TransactionFormConfig.expense(),
        isSaving = viewModel.isSaving.collectAsState().value,
        generalError = viewModel.generalError.collectAsState().value,
        isSuccess = viewModel.isSuccess.collectAsState().value,
        onDescriptionChange = viewModel::onDescriptionChange,
        onAmountChange = viewModel::onAmountChange,
        onDateChange = viewModel::onDateChange,
        onAccountChange = viewModel::onAccountChange,
        onCategoryChange = viewModel::onCategoryChange,
        onTagsChange = viewModel::onTagsChange,
        onNotesChange = viewModel::onNotesChange,
        onSave = viewModel::onSaveClick,
        onClearError = viewModel::clearGeneralError,
        onConsumeSuccess = viewModel::consumeSuccess,
        onSuccess = onSuccess,
        onBack = onBack,
        editable = true,
        screenDescription = "Tela de criar despesa"
    )
}

@Composable
fun EditExpenseScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: EditExpenseViewModel = hiltViewModel()
) {
    ExpenseFormScaffold(
        title = "Editar despesa",
        formState = viewModel.formState.collectAsState().value,
        config = TransactionFormConfig.expenseEdit(),
        isSaving = viewModel.isSaving.collectAsState().value,
        generalError = viewModel.generalError.collectAsState().value,
        isSuccess = viewModel.isSuccess.collectAsState().value,
        onDescriptionChange = viewModel::onDescriptionChange,
        onAmountChange = viewModel::onAmountChange,
        onDateChange = viewModel::onDateChange,
        onAccountChange = viewModel::onAccountChange,
        onCategoryChange = viewModel::onCategoryChange,
        onTagsChange = viewModel::onTagsChange,
        onNotesChange = viewModel::onNotesChange,
        onSave = viewModel::onSaveClick,
        onClearError = viewModel::clearGeneralError,
        onConsumeSuccess = viewModel::consumeSuccess,
        onSuccess = onSuccess,
        onBack = onBack,
        editable = viewModel.editable.collectAsState().value,
        screenDescription = "Tela de editar despesa"
    )
}

@Composable
private fun ExpenseFormScaffold(
    title: String,
    formState: TransactionFormState,
    config: TransactionFormConfig,
    isSaving: Boolean,
    generalError: String?,
    isSuccess: Boolean,
    onDescriptionChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onAccountChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onClearError: () -> Unit,
    onConsumeSuccess: () -> Unit,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    editable: Boolean,
    screenDescription: String
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            onConsumeSuccess()
            onSuccess()
        }
    }

    LaunchedEffect(generalError) {
        generalError?.let {
            snackbarHostState.showSnackbar(it)
            onClearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.semantics { contentDescription = screenDescription }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TransactionForm(
                state = formState,
                config = config,
                onDescriptionChange = onDescriptionChange,
                onAmountChange = onAmountChange,
                onDateChange = onDateChange,
                onAccountChange = onAccountChange,
                onCategoryChange = onCategoryChange,
                onTagsChange = onTagsChange,
                onNotesChange = onNotesChange,
                modifier = Modifier.weight(1f)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!editable) {
                    Text(
                        text = "Esta transação foi cancelada e não pode ser editada.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = onSave,
                    enabled = !isSaving && editable,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Salvar")
                    }
                }

                TextButton(onClick = onBack, enabled = !isSaving) {
                    Text("Voltar")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
