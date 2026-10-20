package br.com.equilibra.mobile.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.repository.AccountRepository
import br.com.equilibra.mobile.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Task 4.11 — Rótulos (nome de conta/categoria) para o histórico.
 *
 * Separado do [HistoryViewModel] de propósito: os dados das transações vêm
 * SEMPRE de /transactions e nunca do cache; aqui só se observa o cache de
 * contas/categorias para traduzir os IDs em nomes exibíveis — o mesmo cache
 * que alimenta os seletores de filtro.
 */
@HiltViewModel
class HistoryLabelsViewModel @Inject constructor(
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val _accountNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val accountNames: StateFlow<Map<String, String>> = _accountNames.asStateFlow()

    private val _categoryNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val categoryNames: StateFlow<Map<String, String>> = _categoryNames.asStateFlow()

    init {
        viewModelScope.launch {
            accountRepository.observeAccounts().collect { list ->
                _accountNames.value = list.associate { it.id to it.name }
            }
        }
        viewModelScope.launch {
            categoryRepository.observeCategories(null).collect { list ->
                _categoryNames.value = list.associate { it.id to it.name }
            }
        }
    }
}
