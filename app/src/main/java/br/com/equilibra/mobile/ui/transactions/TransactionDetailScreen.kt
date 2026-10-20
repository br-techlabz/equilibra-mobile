package br.com.equilibra.mobile.ui.transactions

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.repository.AccountRepository
import br.com.equilibra.mobile.data.repository.CategoryRepository
import br.com.equilibra.mobile.domain.util.MoneyFormatter
import br.com.equilibra.mobile.ui.components.ListError
import br.com.equilibra.mobile.ui.components.ListLoading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Rótulos (nomes de conta/categoria) do detalhe.
 *
 * O detalhe em si vem sempre da API; só os nomes vêm do cache de catálogos.
 * É um fluxo de observação próprio porque a tela de detalhe não compartilha
 * backstack entry com o histórico (Task 4.11), que tem o seu
 * [HistoryLabelsViewModel].
 */
@HiltViewModel
class TransactionDetailLabelsViewModel @Inject constructor(
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    val accountNames = accountRepository.observeAccounts()
        .map { list -> list.associate { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val categoryNames = categoryRepository.observeCategories(null)
        .map { list -> list.associate { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}

/**
 * Task 4.12 — Detalhe da transação.
 *
 * - Dados completos e status do endpoint do tipo ([TransactionDetailViewModel]),
 *   sempre da API; nomes de conta/categoria resolvidos do cache.
 * - Ações conforme tipo/status: "Editar" e "Cancelar transação" (esta só com
 *   confirmação, por ser destrutiva). Pagamento de fatura não tem nenhuma.
 * - Seção de anexos com estado provisório explícito até a Sprint 5.
 * - Estados: loading, erro+retry, aviso/erro de cancelamento como snackbar.
 */
@Composable
fun TransactionDetailScreen(
    onEdit: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
    labelsViewModel: TransactionDetailLabelsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val accountNames by labelsViewModel.accountNames.collectAsState()
    val categoryNames by labelsViewModel.categoryNames.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        val success = state as? TransactionDetailUiState.Success ?: return@LaunchedEffect
        success.notice?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNotice()
        }
        success.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .semantics { contentDescription = "Tela de detalhe da transação" }
        ) {
            TextButton(onClick = onBack) {
                Text("Voltar")
            }
            when (val s = state) {
                is TransactionDetailUiState.Loading -> ListLoading("Carregando transação...")

                is TransactionDetailUiState.Success -> DetailContent(
                    state = s,
                    accountNames = accountNames,
                    categoryNames = categoryNames,
                    onEdit = onEdit,
                    onCancelClick = { showCancelDialog = true }
                )

                // Retry fica interno (padrão do app): quem navega não tem o ViewModel
                is TransactionDetailUiState.Error -> ListError(
                    message = s.message,
                    onRetry = { viewModel.retry() }
                )
            }
        }
    }

    if (showCancelDialog && state is TransactionDetailUiState.Success) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancelar transação") },
            text = {
                Text("Esta ação não pode ser desfeita. O saldo da conta será recalculado pelo sistema.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancel()
                    },
                    enabled = !(state as TransactionDetailUiState.Success).isCancelling
                ) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Voltar") }
            }
        )
    }
}

@Composable
private fun DetailContent(
    state: TransactionDetailUiState.Success,
    accountNames: Map<String, String>,
    categoryNames: Map<String, String>,
    onEdit: (String) -> Unit,
    onCancelClick: () -> Unit
) {
    val detail = state.detail
    val cancelled = detail.status == "CANCELLED"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = detail.description,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = MoneyFormatter.format(detail.amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (cancelled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            if (cancelled) {
                Text(
                    text = "Cancelada",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            val fields = TransactionDetailPresenter.fields(
                detail = detail,
                accountNames = accountNames,
                categoryNames = categoryNames
            )
            Column(modifier = Modifier.padding(16.dp)) {
                fields.forEachIndexed { index, field ->
                    DetailRow(label = field.label, value = field.value)
                    if (index < fields.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }

        AttachmentsSection(
            message = TransactionDetailPresenter.attachmentsState(null).message.orEmpty()
        )

        if (detail.canEdit) {
            OutlinedButton(
                onClick = { onEdit(detail.id) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Editar") }
        }
        if (detail.canCancel) {
            OutlinedButton(
                onClick = onCancelClick,
                enabled = !state.isCancelling,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isCancelling) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Cancelar transação")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(0.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { contentDescription = "$label: $value" }
        )
    }
}

/**
 * Seção provisória de anexos (Sprint 5): mostra estado claro em vez de
 * uma ação quebrada ou inexistente.
 */
@Composable
private fun AttachmentsSection(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Anexos",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
