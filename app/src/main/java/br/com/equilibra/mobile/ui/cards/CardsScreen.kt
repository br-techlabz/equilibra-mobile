package br.com.equilibra.mobile.ui.cards

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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import br.com.equilibra.mobile.data.local.creditcard.CreditCardEntity
import br.com.equilibra.mobile.data.remote.dto.creditcard.CreditCardStatusDto
import br.com.equilibra.mobile.domain.util.MoneyFormatter
import br.com.equilibra.mobile.ui.components.ListEmptyState
import br.com.equilibra.mobile.ui.components.ListError
import br.com.equilibra.mobile.ui.components.ListLoading

/**
 * Task 6.3 — Lista de cartões de crédito.
 *
 * - Mostra limite total (título do valor) e disponível (destaque),
 *   ambos calculados pelo backend (nunca no cliente).
 * - Cartões ativos/inativos visualmente distintos ("• Inativo").
 * - Empty/loading/error/pull-to-refresh; snackbar para erros recuperáveis.
 * - Acessibilidade: contentDescription com nome + limites por cartão.
 *
 * onCreate fica desabilitado nesta task (criação é a Task 6.4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    onCardClick: (String) -> Unit = {},
    viewModel: CardsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        val error = (state as? CardsUiState.Success)?.error
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Tela de cartões" }
    ) {
        when (val s = state) {
            is CardsUiState.Loading -> ListLoading("Carregando cartões...")
            is CardsUiState.Success -> SuccessContent(
                cards = s.cards,
                isRefreshing = s.isRefreshing,
                onRefresh = { viewModel.refresh() },
                onCardClick = onCardClick
            )
            is CardsUiState.Error -> ListError(
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
    cards: List<CreditCardEntity>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onCardClick: (String) -> Unit
) {
    if (cards.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                ListEmptyState(
                    title = "Nenhum cartão cadastrado",
                    subtitle = "Os cartões podem ser criados em uma próxima atualização do app",
                    icon = Icons.Default.CreditCard
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
                Text(
                    text = "Cartões",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(cards, key = { it.id }) { card ->
                CreditCardRow(
                    card = card,
                    onClick = { onCardClick(card.id) }
                )
            }
        }
    }
}

@Composable
private fun CreditCardRow(card: CreditCardEntity, onClick: () -> Unit) {
    val active = card.status == CreditCardStatusDto.ACTIVE
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${card.name}: limite total ${MoneyFormatter.format(card.totalLimit)}, " +
                    "disponível ${MoneyFormatter.format(card.availableLimit)}, " +
                    "status ${CreditCardStatusDto.labelPtBr(card.status)}"
            }
            .clickable(role = Role.Button, onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.surfaceVariant
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
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (active) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = CreditCardStatusDto.labelPtBr(card.status) +
                            " • fecha dia ${card.closingDay}, vence dia ${card.dueDay}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = MoneyFormatter.format(card.availableLimit),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (active) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "de ${MoneyFormatter.format(card.totalLimit)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
