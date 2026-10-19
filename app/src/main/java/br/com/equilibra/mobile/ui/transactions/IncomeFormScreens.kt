package br.com.equilibra.mobile.ui.transactions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Task 4.8 — Telas de criar/editar receita sobre o formulário e scaffold
 * compartilhados; config com categorias INCOME.
 */
@Composable
fun CreateIncomeScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: CreateIncomeViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val generalError by viewModel.generalError.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()

    TransactionFormScaffold(
        title = "Nova receita",
        formState = formState,
        config = TransactionFormConfig.income(),
        isSaving = isSaving,
        generalError = generalError,
        isSuccess = isSuccess,
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
        screenDescription = "Tela de criar receita"
    )
}

@Composable
fun EditIncomeScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: EditIncomeViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val generalError by viewModel.generalError.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()
    val editable by viewModel.editable.collectAsState()

    TransactionFormScaffold(
        title = "Editar receita",
        formState = formState,
        config = TransactionFormConfig.incomeEdit(),
        isSaving = isSaving,
        generalError = generalError,
        isSuccess = isSuccess,
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
        editable = editable,
        screenDescription = "Tela de editar receita"
    )
}
