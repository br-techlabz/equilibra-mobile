package br.com.equilibra.mobile.ui.transactions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Task 4.6 — Telas de criar/editar despesa sobre o formulário e scaffold
 * compartilhados ([TransactionForm]/[TransactionFormScaffold]).
 */
@Composable
fun CreateExpenseScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: CreateExpenseViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val generalError by viewModel.generalError.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()

    TransactionFormScaffold(
        title = "Nova despesa",
        formState = formState,
        config = TransactionFormConfig.expense(),
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
        screenDescription = "Tela de criar despesa"
    )
}

@Composable
fun EditExpenseScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: EditExpenseViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val generalError by viewModel.generalError.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()
    val editable by viewModel.editable.collectAsState()
    val isCancelling by viewModel.isCancelling.collectAsState()
    val notice by viewModel.notice.collectAsState()

    TransactionFormScaffold(
        title = "Editar despesa",
        formState = formState,
        config = TransactionFormConfig.expenseEdit(),
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
        screenDescription = "Tela de editar despesa",
        onCancelTransaction = viewModel::cancel,
        isCancelling = isCancelling,
        notice = notice,
        onClearNotice = viewModel::clearNotice
    )
}
