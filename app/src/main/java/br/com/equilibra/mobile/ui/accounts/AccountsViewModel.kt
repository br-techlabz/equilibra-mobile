package br.com.equilibra.mobile.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.local.account.AccountEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AccountsUiState {
    data object Loading : AccountsUiState()
    data class Success(
        val accounts: List<AccountEntity>,
        val isRefreshing: Boolean = false,
        val error: String? = null
    ) : AccountsUiState()
    data class Error(val message: String, val canRetry: Boolean = true) : AccountsUiState()
}

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AccountsUiState>(AccountsUiState.Loading)
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    private var hasLoadedOnce = false

    init {
        // Room-first: emite cache imediatamente e depois atualiza em background
        viewModelScope.launch {
            repository.observeAccounts().collectLatest { cached ->
                val current = _uiState.value

                if (!hasLoadedOnce) {
                    if (cached.isEmpty()) {
                        _uiState.value = AccountsUiState.Loading
                    } else {
                        _uiState.value = AccountsUiState.Success(accounts = cached, isRefreshing = true)
                    }
                    hasLoadedOnce = true
                    refreshInBackground()
                } else {
                    // Atualização silenciosa — mantém estado, só troca lista
                    when (current) {
                        is AccountsUiState.Success -> {
                            _uiState.value = current.copy(accounts = cached)
                        }
                        is AccountsUiState.Loading -> {
                            // Se ainda estava loading e agora tem cache, mostra
                            if (cached.isNotEmpty()) {
                                _uiState.value = AccountsUiState.Success(accounts = cached)
                            }
                        }
                        is AccountsUiState.Error -> {
                            // Se havia erro e agora tem cache, recupera
                            if (cached.isNotEmpty()) {
                                _uiState.value = AccountsUiState.Success(accounts = cached)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun refreshInBackground() {
        viewModelScope.launch {
            when (val result = repository.refresh(includeInactive = true)) {
                is ApiResult.Success -> {
                    // observeAccounts já vai emitir a nova lista; só limpa isRefreshing
                    val current = _uiState.value
                    if (current is AccountsUiState.Success) {
                        _uiState.value = current.copy(isRefreshing = false, error = null)
                    } else if (current is AccountsUiState.Loading) {
                        // Se a lista estava vazia, observeAccounts vai emitir; se ainda não, mostra lista
                        val cached = repository.getAllCached()
                        _uiState.value = if (cached.isEmpty()) {
                            // Servidor retornou vazio legitimamente
                            AccountsUiState.Success(accounts = emptyList())
                        } else {
                            AccountsUiState.Success(accounts = cached)
                        }
                    }
                }
                is ApiResult.HttpError -> {
                    handleRefreshError("Erro ao carregar contas (${result.code}).")
                }
                is ApiResult.NetworkError -> {
                    handleRefreshError("Sem conexão. Mostrando dados locais.")
                }
                is ApiResult.UnknownError -> {
                    handleRefreshError("Erro inesperado. Mostrando dados locais.")
                }
            }
        }
    }

    private suspend fun handleRefreshError(message: String) {
        val cached = repository.getAllCached()
        val current = _uiState.value
        _uiState.value = if (cached.isNotEmpty()) {
            // Tem cache → mostra com erro recuperável
            AccountsUiState.Success(accounts = cached, isRefreshing = false, error = message)
        } else if (current is AccountsUiState.Loading) {
            // Sem cache e ainda em loading → erro
            AccountsUiState.Error(message)
        } else {
            // Já tem Success com dados → adiciona erro
            if (current is AccountsUiState.Success) {
                current.copy(isRefreshing = false, error = message)
            } else {
                AccountsUiState.Error(message)
            }
        }
    }

    fun refresh() {
        val current = _uiState.value
        if (current is AccountsUiState.Success) {
            _uiState.value = current.copy(isRefreshing = true, error = null)
        } else {
            _uiState.value = AccountsUiState.Loading
        }
        refreshInBackground()
    }

    fun dismissError() {
        val current = _uiState.value
        if (current is AccountsUiState.Success) {
            _uiState.value = current.copy(error = null)
        }
    }
}
