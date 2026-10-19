package br.com.equilibra.mobile.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Task 4.8 — Scaffold compartilhado das telas de criar/editar transação
 * (despesa da Task 4.6 e receita desta task).
 *
 * - Sucesso (repository já atualizou o Room) navega para trás.
 * - Erros HTTP/rede viram snackbar; salvamento bloqueia duplo toque.
 * - Transação cancelada é bloqueada (regra do backend).
 */
@Composable
internal fun TransactionFormScaffold(
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
    screenDescription: String,
    /** Task 4.10 — cancelamento com confirmação; null esconde a ação. */
    onCancelTransaction: (() -> Unit)? = null,
    isCancelling: Boolean = false,
    notice: String? = null,
    onClearNotice: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showCancelDialog by remember { mutableStateOf(false) }

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

    LaunchedEffect(notice) {
        notice?.let {
            snackbarHostState.showSnackbar(it)
            onClearNotice()
        }
    }

    if (showCancelDialog) {
        CancelTransactionDialog(
            onConfirm = {
                showCancelDialog = false
                onCancelTransaction?.invoke()
            },
            onDismiss = { showCancelDialog = false }
        )
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

                if (onCancelTransaction != null && editable) {
                    TextButton(
                        onClick = { showCancelDialog = true },
                        enabled = !isSaving && !isCancelling
                    ) {
                        Text(
                            text = "Cancelar transação",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Task 4.10 — Confirmação de ação destrutiva antes de cancelar.
 */
@Composable
internal fun CancelTransactionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancelar transação") },
        text = {
            Text("Esta ação não pode ser desfeita. A transação ficará registrada como cancelada.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Sim, cancelar",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Voltar")
            }
        }
    )
}
