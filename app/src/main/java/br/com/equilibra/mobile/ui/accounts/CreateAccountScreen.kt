package br.com.equilibra.mobile.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
 * Task 3.6 — Tela de criação de conta (formulário reutilizável via AccountForm).
 */
@Composable
fun CreateAccountScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: CreateAccountViewModel = hiltViewModel()
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
            .semantics { contentDescription = "Tela de criar conta" },
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Nova conta",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))

        AccountForm(
            state = AccountFormState(
                name = state.name,
                nameError = state.nameError,
                type = state.type,
                initialBalanceInput = state.initialBalanceInput,
                initialBalanceError = state.initialBalanceError
            ),
            onNameChange = viewModel::onNameChange,
            onTypeChange = viewModel::onTypeChange,
            onInitialBalanceChange = viewModel::onInitialBalanceChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = viewModel::onCreateClick,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Criar")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onBack) {
            Text("Voltar")
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}
