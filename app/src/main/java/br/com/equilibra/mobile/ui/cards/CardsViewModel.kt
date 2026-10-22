package br.com.equilibra.mobile.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.local.creditcard.CreditCardEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.CreditCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Task 6.3 — Estados da lista de cartões.
 */
sealed class CardsUiState {
    data object Loading : CardsUiState()
    data class Success(
        val cards: List<CreditCardEntity>,
        val isRefreshing: Boolean = false,
        val error: String? = null
    ) : CardsUiState()
    data class Error(val message: String, val canRetry: Boolean = true) : CardsUiState()
}

/**
 * Task 6.3 — CardsViewModel
 *
 * Mesmo fluxo do AccountsViewModel: cache-first (Room emite imediatamente)
 * seguido de refresh da API em background; erro de rede cai no cache local
 * com snackbar recuperável. A lista mostra TODOS os cartões (ativos e
 * inativos, sinalizados visualmente) — a API list() vai com includeInactive.
 */
@HiltViewModel
class CardsViewModel @Inject constructor(
    private val repository: CreditCardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CardsUiState>(CardsUiState.Loading)
    val uiState: StateFlow<CardsUiState> = _uiState.asStateFlow()

    private var hasLoadedOnce = false

    init {
        viewModelScope.launch {
            repository.observeCards().collectLatest { cached ->
                val current = _uiState.value

                if (!hasLoadedOnce) {
                    if (cached.isEmpty()) {
                        _uiState.value = CardsUiState.Loading
                    } else {
                        _uiState.value = CardsUiState.Success(cards = cached, isRefreshing = true)
                    }
                    hasLoadedOnce = true
                    refreshInBackground()
                } else {
                    when (current) {
                        is CardsUiState.Success -> _uiState.value = current.copy(cards = cached)
                        is CardsUiState.Loading -> {
                            if (cached.isNotEmpty()) {
                                _uiState.value = CardsUiState.Success(cards = cached)
                            }
                        }
                        is CardsUiState.Error -> {
                            if (cached.isNotEmpty()) {
                                _uiState.value = CardsUiState.Success(cards = cached)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun refreshInBackground() {
        viewModelScope.launch {
            when (val result = repository.refresh()) {
                is ApiResult.Success -> {
                    val current = _uiState.value
                    if (current is CardsUiState.Success) {
                        _uiState.value = current.copy(isRefreshing = false, error = null)
                    } else if (current is CardsUiState.Loading) {
                        val cached = repository.getAllCached()
                        _uiState.value = if (cached.isEmpty()) {
                            CardsUiState.Success(cards = emptyList())
                        } else {
                            CardsUiState.Success(cards = cached)
                        }
                    }
                }
                is ApiResult.HttpError -> {
                    handleRefreshError("Erro ao carregar cartões (${result.code}).")
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
            CardsUiState.Success(cards = cached, isRefreshing = false, error = message)
        } else if (current is CardsUiState.Loading) {
            CardsUiState.Error(message)
        } else {
            if (current is CardsUiState.Success) {
                current.copy(isRefreshing = false, error = message)
            } else {
                CardsUiState.Error(message)
            }
        }
    }

    fun refresh() {
        val current = _uiState.value
        if (current is CardsUiState.Success) {
            _uiState.value = current.copy(isRefreshing = true, error = null)
        } else {
            _uiState.value = CardsUiState.Loading
        }
        refreshInBackground()
    }

    fun dismissError() {
        val current = _uiState.value
        if (current is CardsUiState.Success) {
            _uiState.value = current.copy(error = null)
        }
    }
}
