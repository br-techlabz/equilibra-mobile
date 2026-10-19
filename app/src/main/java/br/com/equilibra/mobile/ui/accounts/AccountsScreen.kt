package br.com.equilibra.mobile.ui.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.equilibra.mobile.data.local.account.AccountEntity
import br.com.equilibra.mobile.data.remote.dto.account.AssetAccountTypeDto
import br.com.equilibra.mobile.domain.util.MoneyFormatter
import br.com.equilibra.mobile.ui.components.ListEmptyState
import br.com.equilibra.mobile.ui.components.ListError
import br.com.equilibra.mobile.ui.components.ListLoading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onAccountClick: (String) -> Unit = {},
    onCreateClick: () -> Unit = {},
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        val error = (state as? AccountsUiState.Success)?.error
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Tela de contas" }
    ) {
        when (val s = state) {
            is AccountsUiState.Loading -> ListLoading("Carregando contas...")
            is AccountsUiState.Success -> SuccessContent(
                accounts = s.accounts,
                isRefreshing = s.isRefreshing,
                onRefresh = { viewModel.refresh() },
                onAccountClick = onAccountClick,
                onCreateClick = onCreateClick
            )
            is AccountsUiState.Error -> ListError(
                message = s.message,
                onRetry = { viewModel.refresh() }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessContent(
    accounts: List<AccountEntity>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onAccountClick: (String) -> Unit,
    onCreateClick: () -> Unit
) {
    if (accounts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                ListEmptyState(
                    title = "Nenhuma conta cadastrada",
                    subtitle = "Crie sua primeira conta para começar",
                    actionLabel = "Criar conta",
                    onAction = onCreateClick,
                    icon = Icons.Default.AccountBalanceWallet
                )
            }
        }
        return
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contas",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onCreateClick) {
                        Text("Nova conta")
                    }
                }
            }

            items(accounts, key = { it.id }) { account ->
                AccountRow(
                    account = account,
                    onClick = { onAccountClick(account.id) }
                )
            }
        }
    }
}

@Composable
private fun AccountRow(account: AccountEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${account.name}: ${MoneyFormatter.format(account.currentBalance)}" }
            .clickable(role = Role.Button, onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (account.active) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = if (account.active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (account.active) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = AssetAccountTypeDto.labelPtBr(account.type) + if (!account.active) " • Inativa" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = MoneyFormatter.format(account.currentBalance),
                style = MaterialTheme.typography.titleSmall,
                color = if (account.active) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
