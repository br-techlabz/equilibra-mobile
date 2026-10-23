package br.com.equilibra.mobile.ui.statements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.local.creditcard.CreditCardEntity
import br.com.equilibra.mobile.data.local.statement.StatementEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.dto.creditcard.CreditCardStatusDto
import br.com.equilibra.mobile.data.remote.dto.statement.StatementStatusDto
import br.com.equilibra.mobile.data.repository.CreditCardRepository
import br.com.equilibra.mobile.data.repository.StatementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Task 7.3/7.4 — Estado da tela de faturas.
 *
 * [current] é a fatura atual devolvida pelo backend; [history] são as demais,
 * do ciclo mais recente para o mais antigo. Nenhum status ou agregado é
 * calculado aqui — os campos vêm prontos do Room/API. [confirmCloseId] e
 * [isClosing] controlam apenas a confirmação/envio do fechamento manual.
 */
data class StatementsUiState(
    val cards: List<CreditCardEntity> = emptyList(),
    val selectedCardId: String? = null,
    val current: StatementEntity? = null,
    val history: List<StatementEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    /** Fatura aguardando confirmação de fechamento (null = sem diálogo). */
    val confirmCloseId: String? = null,
    val isClosing: Boolean = false
) {
    val hasStatements: Boolean get() = current != null || history.isNotEmpty()
}

/**
 * Task 7.3/7.4 — Tela de faturas e fechamento manual.
 *
 * - Room-first: o Flow do cache alimenta a tela primeiro; a API sincroniza em
 *   background (lista + /current-statement) sem bloquear a leitura.
 * - A seleção de cartão troca a origem dos dados e dispara nova sincronização.
 * - A fatura atual é a que o backend devolve em /current-statement. Enquanto
 *   a API não responde, o ciclo mais recente do cache é usado como fallback
 *   de exibição — sem inferir status nem recalcular valores.
 * - Histórico é ordenado por cycleStart DESC (desempate por dueDate DESC).
 * - Falha de sincronização preserva o cache; sem cache vira erro recuperável.
 * - 404 em /current-statement significa "sem fatura atual" (não é erro).
 * - Fechamento manual (7.4): só a fatura atual OPEN pode ser fechada, com
 *   confirmação, envio único e ressincronização após a resposta do backend.
 */
@HiltViewModel
class StatementsViewModel @Inject constructor(
    private val statementRepository: StatementRepository,
    private val cardRepository: CreditCardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatementsUiState())
    val uiState: StateFlow<StatementsUiState> = _uiState.asStateFlow()

    private val selectedCardId = MutableStateFlow<String?>(null)

    /** Id da fatura atual segundo a API. */
    private var currentId: String? = null

    /** true quando a API já respondeu sobre a fatura atual (inclusive 404). */
    private var currentKnown = false

    private val newestFirst = compareByDescending<StatementEntity> { it.cycleStart }
        .thenByDescending { it.dueDate }

    init {
        observeCards()
        observeStatements()
    }

    private fun observeCards() {
        viewModelScope.launch {
            cardRepository.observeCards().collect { cards ->
                val selected = selectedCardId.value
                val chosen = if (cards.any { it.id == selected }) {
                    selected
                } else {
                    cards.firstOrNull { it.status == CreditCardStatusDto.ACTIVE }?.id
                        ?: cards.firstOrNull()?.id
                }

                if (chosen == null) {
                    currentId = null
                    currentKnown = false
                    _uiState.update {
                        it.copy(
                            cards = cards,
                            selectedCardId = null,
                            current = null,
                            history = emptyList(),
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                            confirmCloseId = null,
                            isClosing = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(cards = cards, selectedCardId = chosen) }
                    if (chosen != selected) selectedCardId.value = chosen
                }
            }
        }
    }

    private fun observeStatements() {
        viewModelScope.launch {
            selectedCardId.collectLatest { cardId ->
                currentId = null
                currentKnown = false

                if (cardId == null) {
                    _uiState.update {
                        it.copy(
                            selectedCardId = null,
                            current = null,
                            history = emptyList(),
                            isLoading = false,
                            isRefreshing = false,
                            confirmCloseId = null,
                            isClosing = false
                        )
                    }
                    return@collectLatest
                }

                _uiState.update {
                    it.copy(
                        selectedCardId = cardId,
                        current = null,
                        history = emptyList(),
                        isLoading = true,
                        isRefreshing = false,
                        error = null,
                        confirmCloseId = null,
                        isClosing = false
                    )
                }

                launch {
                    statementRepository.observeByCard(cardId).collect { cached ->
                        applyCache(cached)
                    }
                }

                sync(cardId)
            }
        }
    }

    /** Atualiza a fatura de um cartão, mantendo a tela utilizável durante o sync. */
    fun refresh() {
        val cardId = selectedCardId.value ?: return
        val hasData = _uiState.value.hasStatements
        _uiState.update {
            it.copy(
                isLoading = !hasData,
                isRefreshing = hasData,
                error = null
            )
        }
        viewModelScope.launch { sync(cardId) }
    }

    fun onCardSelected(cardId: String) {
        if (cardId == selectedCardId.value) return
        if (_uiState.value.cards.none { it.id == cardId }) return

        currentId = null
        currentKnown = false
        _uiState.update {
            it.copy(
                selectedCardId = cardId,
                current = null,
                history = emptyList(),
                isLoading = true,
                isRefreshing = false,
                error = null,
                confirmCloseId = null,
                isClosing = false
            )
        }
        selectedCardId.value = cardId
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Task 7.4 — Abre a confirmação apenas para a fatura atual e aberta.
     * O status vem do backend; o app não presume que uma fatura possa ser
     * fechada por estar vencida ou por qualquer regra local.
     */
    fun onCloseClick(statementId: String) {
        val state = _uiState.value
        if (state.isClosing) return
        val current = state.current ?: return
        if (current.id != statementId || current.status != StatementStatusDto.OPEN) return

        _uiState.update { it.copy(confirmCloseId = statementId, error = null) }
    }

    fun onDismissClose() {
        if (_uiState.value.isClosing) return
        _uiState.update { it.copy(confirmCloseId = null) }
    }

    /**
     * Fecha a fatura confirmada. O envio é único: [isClosing] é marcado antes
     * de sair da thread principal e novas confirmações são ignoradas.
     *
     * Em sucesso, o repository já gravou a resposta no Room; em seguida a
     * lista e o /current-statement são ressincronizados para refletir a nova
     * fatura do cartão. Nenhum status é previsto pelo cliente.
     */
    fun onConfirmClose() {
        val state = _uiState.value
        if (state.isClosing) return

        val statementId = state.confirmCloseId ?: return
        val current = state.current
        if (current?.id != statementId || current.status != StatementStatusDto.OPEN) {
            _uiState.update { it.copy(confirmCloseId = null) }
            return
        }
        val cardId = state.selectedCardId ?: return

        _uiState.update { it.copy(isClosing = true, error = null) }

        viewModelScope.launch {
            val result = statementRepository.close(cardId, current.cycleStart)
            // O cartão pode ter sido trocado durante a chamada; nesse caso o
            // estado exibido pertence a outro cartão e não deve ser alterado.
            if (selectedCardId.value != cardId) return@launch

            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isClosing = false, confirmCloseId = null) }
                    refresh()
                }
                is ApiResult.HttpError -> {
                    val message = when (result.code) {
                        404 -> "Fatura não encontrada."
                        409 -> "Não foi possível fechar: a fatura já está fechada."
                        else -> "Erro ao fechar fatura (${result.code})."
                    }
                    _uiState.update {
                        it.copy(isClosing = false, confirmCloseId = null, error = message)
                    }
                }
                is ApiResult.NetworkError -> _uiState.update {
                    it.copy(
                        isClosing = false,
                        confirmCloseId = null,
                        error = NETWORK_MESSAGE
                    )
                }
                is ApiResult.UnknownError -> _uiState.update {
                    it.copy(
                        isClosing = false,
                        confirmCloseId = null,
                        error = "Erro inesperado ao fechar fatura."
                    )
                }
            }
        }
    }

    private fun applyCache(cached: List<StatementEntity>) {
        val sorted = cached.sortedWith(newestFirst)
        val current = when {
            currentKnown -> currentId?.let { id ->
                sorted.firstOrNull { it.id == id }
                    ?: _uiState.value.current?.takeIf { it.id == id }
            }
            else -> sorted.firstOrNull()
        }
        val history = sorted.filterNot { it.id == current?.id }
        val hasData = cached.isNotEmpty()

        _uiState.update { state ->
            state.copy(
                current = current,
                history = history,
                isLoading = if (hasData) false else state.isLoading,
                isRefreshing = state.isRefreshing || (hasData && state.isLoading),
                // Se o sync falhou antes de o Room emitir, o erro genérico
                // vira "mostrando dados locais" assim que o cache aparece.
                error = if (hasData && state.error == NETWORK_MESSAGE) {
                    "Sem conexão. Mostrando dados locais."
                } else {
                    state.error
                }
            )
        }
    }

    private suspend fun sync(cardId: String) {
        var failure: SyncFailure? = null

        when (val result = statementRepository.refreshByCard(cardId)) {
            is ApiResult.Success -> Unit
            is ApiResult.HttpError -> failure = SyncFailure("Erro ao carregar faturas (${result.code}).")
            is ApiResult.NetworkError -> failure = SyncFailure(NETWORK_MESSAGE, network = true)
            is ApiResult.UnknownError -> failure = SyncFailure("Erro inesperado ao carregar faturas.")
        }

        when (val result = statementRepository.refreshCurrent(cardId)) {
            is ApiResult.Success -> if (selectedCardId.value == cardId) {
                currentKnown = true
                currentId = result.data.id
                applyCurrent(result.data)
            }
            is ApiResult.HttpError -> if (result.code == 404) {
                if (selectedCardId.value == cardId) {
                    currentKnown = true
                    currentId = null
                    moveCurrentToHistory()
                }
            } else if (failure == null) {
                failure = SyncFailure("Erro ao carregar a fatura atual (${result.code}).")
            }
            is ApiResult.NetworkError -> if (failure == null) {
                failure = SyncFailure(NETWORK_MESSAGE, network = true)
            }
            is ApiResult.UnknownError -> if (failure == null) {
                failure = SyncFailure("Erro inesperado ao carregar a fatura atual.")
            }
        }

        if (selectedCardId.value == cardId) finishSync(failure)
    }

    /**
     * Atualiza a fatura atual sem esperar a próxima emissão do Room. Se a
     * fatura anterior estava marcada como atual (fallback), ela volta para o
     * histórico. Nenhum campo é recalculado: [data] é copiada do backend.
     */
    private fun applyCurrent(data: StatementEntity) {
        _uiState.update { state ->
            val conhecidas = buildList {
                state.current?.let(::add)
                addAll(state.history)
            }
            state.copy(
                current = data,
                history = conhecidas
                    .filterNot { it.id == data.id }
                    .distinctBy { it.id }
                    .sortedWith(newestFirst)
            )
        }
    }

    /** API respondeu 404: não há fatura atual; o cache vira histórico. */
    private fun moveCurrentToHistory() {
        _uiState.update { state ->
            val todas = buildList {
                state.current?.let(::add)
                addAll(state.history)
            }
            state.copy(
                current = null,
                history = todas.distinctBy { it.id }.sortedWith(newestFirst)
            )
        }
    }

    private fun finishSync(failure: SyncFailure?) {
        val hasData = _uiState.value.hasStatements
        val message = when {
            failure == null -> null
            failure.network && hasData -> "Sem conexão. Mostrando dados locais."
            else -> failure.message
        }
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                error = message
            )
        }
    }

    private data class SyncFailure(
        val message: String,
        val network: Boolean = false
    )

    private companion object {
        const val NETWORK_MESSAGE = "Sem conexão. Tente novamente."
    }
}
