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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import br.com.equilibra.mobile.data.remote.dto.account.AssetAccountTypeDto
import br.com.equilibra.mobile.domain.util.MoneyFormatter

/**
 * Task 3.7 — Tela de edição de conta (reutiliza AccountForm da Task 3.6).
 *
 * - Salvar, Ativar/Desativar (com confirmação na desativação), Voltar
 * - Status atual exibido; falha preserva estado anterior (form inalterado)
 */
@Composable
fun EditAccountScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    onReconcile: () -> Unit = {},
    viewModel: EditAccountViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isSuccess, state.statusChanged) {
        if (state.isSuccess || state.statusChanged) {
            viewModel.consumeSuccess()
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
            .semantics { contentDescription = "Tela de editar conta" },
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Editar conta",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        state.account?.let { acc ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Saldo atual: ${MoneyFormatter.format(acc.currentBalance)}" +
                    if (!acc.active) " • Inativa" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        AccountForm(
            state = state.form,
            onNameChange = viewModel::onNameChange,
            onTypeChange = viewModel::onTypeChange,
            onInitialBalanceChange = viewModel::onInitialBalanceChange
        )

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
        val isActive = state.account?.active == true
        OutlinedButton(
            onClick = viewModel::onToggleActiveClick,
            enabled = state.account != null && !state.isLoading && !state.isToggling,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isToggling) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text(if (isActive) "Desativar conta" else "Ativar conta")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onReconcile,
            enabled = !state.isLoading && !state.isToggling,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reconciliar saldo")
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
            title = { Text("Desativar conta?") },
            text = {
                Text(
                    "A conta \"${state.form.name}\" ficará inativa. " +
                        "Transações e relatórios passam a ignorá-la até ser reativada. " +
                        "Você pode reativar a qualquer momento."
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
