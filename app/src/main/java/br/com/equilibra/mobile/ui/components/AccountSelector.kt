package br.com.equilibra.mobile.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import br.com.equilibra.mobile.data.local.account.AccountEntity
import br.com.equilibra.mobile.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Task 4.4/4.11 — Seletor de conta (Room-first), mesmos padrões dos
 * seletores de categoria/tag da Task 3.13.
 *
 * - Consulta o cache local (Room) via [AccountSelectorViewModel]; sem rede.
 * - Exibe apenas contas ativas.
 * - Seleção única com toggle: tocar na selecionada envia null (limpa) —
 *   em formulários (conta obrigatória) o consumidor ignora o null e a
 *   validação exige a conta; no filtro do histórico (4.11) o null zera
 *   o filtro ("qualquer conta").
 */
@Composable
fun AccountSelector(
    selectedId: String?,
    onSelectionChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountSelectorViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    var accounts by remember { mutableStateOf<List<AccountEntity>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.accounts().collect { accounts = it }
    }

    val options = accounts.orEmpty().map { it.id to it.name }

    SelectorChipList(
        title = "Conta",
        options = options,
        selectedIds = listOfNotNull(selectedId),
        multiSelect = false,
        isLoading = isLoading && accounts == null,
        loadingMessage = "Carregando contas...",
        emptyMessage = "Nenhuma conta ativa disponível",
        optionTagPrefix = "account",
        onToggle = { id ->
            onSelectionChange(
                SelectionLogic.toggle(listOfNotNull(selectedId), id, multiSelect = false).firstOrNull()
            )
        },
        modifier = modifier
    )
}

/**
 * Task 4.4 — ViewModel do seletor de conta.
 */
@HiltViewModel
class AccountSelectorViewModel @Inject constructor(
    private val repository: AccountRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Contas ativas em ordem alfabética (cache Room). */
    fun accounts(): Flow<List<AccountEntity>> =
        repository.observeAccounts()
            .map { list -> list.filter { it.active } }
            .onEach { _isLoading.value = false }
}
