package br.com.equilibra.mobile.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.local.tag.TagEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TagsUiState {
    data object Loading : TagsUiState()
    data class Success(
        val tags: List<TagEntity>,
        val isRefreshing: Boolean = false,
        val error: String? = null
    ) : TagsUiState()
    data class Error(val message: String, val canRetry: Boolean = true) : TagsUiState()
}

/**
 * Task 3.12 — ViewModel da lista de tags (Room-first).
 *
 * - Abre com cache do Room e sincroniza em background.
 * - pull-to-refresh consulta a API; erros não apagam cache.
 * - Toggle ativo/inativo só persiste após sucesso remoto.
 */
@HiltViewModel
class TagsViewModel @Inject constructor(
    private val repository: TagRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TagsUiState>(TagsUiState.Loading)
    val uiState: StateFlow<TagsUiState> = _uiState.asStateFlow()

    private var hasLoadedOnce = false

    init {
        // Room-first: emite cache imediatamente e depois atualiza em background
        viewModelScope.launch {
            repository.observeTags().collectLatest { cached ->
                val current = _uiState.value

                if (!hasLoadedOnce) {
                    if (cached.isEmpty()) {
                        _uiState.value = TagsUiState.Loading
                    } else {
                        _uiState.value = TagsUiState.Success(
                            tags = cached,
                            isRefreshing = true
                        )
                    }
                    hasLoadedOnce = true
                    refreshInBackground()
                } else {
                    // Atualização silenciosa — mantém estado, só troca lista
                    when (current) {
                        is TagsUiState.Success -> {
                            _uiState.value = current.copy(tags = cached)
                        }
                        is TagsUiState.Loading -> {
                            if (cached.isNotEmpty()) {
                                _uiState.value = TagsUiState.Success(tags = cached)
                            }
                        }
                        is TagsUiState.Error -> {
                            if (cached.isNotEmpty()) {
                                _uiState.value = TagsUiState.Success(tags = cached)
                            }
                        }
                    }
                }
            }
        }
    }

    fun refresh() {
        val current = _uiState.value
        if (current is TagsUiState.Success) {
            _uiState.value = current.copy(isRefreshing = true, error = null)
        } else {
            _uiState.value = TagsUiState.Loading
        }
        refreshInBackground()
    }

    fun dismissError() {
        val current = _uiState.value
        if (current is TagsUiState.Success) {
            _uiState.value = current.copy(error = null)
        }
    }

    private fun refreshInBackground() {
        viewModelScope.launch {
            when (val result = repository.refresh(includeInactive = true)) {
                is ApiResult.Success -> {
                    val current = _uiState.value
                    if (current is TagsUiState.Success) {
                        _uiState.value = current.copy(isRefreshing = false, error = null)
                    } else if (current is TagsUiState.Loading) {
                        val cached = repository.getAllCached()
                        _uiState.value = TagsUiState.Success(
                            tags = if (cached.isEmpty()) emptyList() else cached
                        )
                    }
                }
                is ApiResult.HttpError -> handleRefreshError("Erro ao carregar tags (${result.code}).")
                is ApiResult.NetworkError -> handleRefreshError("Sem conexão. Mostrando dados locais.")
                is ApiResult.UnknownError -> handleRefreshError("Erro inesperado. Mostrando dados locais.")
            }
        }
    }

    private suspend fun handleRefreshError(message: String) {
        val cached = repository.getAllCached()
        val current = _uiState.value
        _uiState.value = if (cached.isNotEmpty()) {
            TagsUiState.Success(tags = cached, isRefreshing = false, error = message)
        } else if (current is TagsUiState.Loading) {
            TagsUiState.Error(message)
        } else {
            if (current is TagsUiState.Success) {
                current.copy(isRefreshing = false, error = message)
            } else {
                TagsUiState.Error(message)
            }
        }
    }

    // Escritas — delegam para o repository, que já faz upsert no Room

    suspend fun create(name: String): ApiResult<TagEntity> {
        return repository.create(name)
    }

    suspend fun update(id: String, name: String): ApiResult<TagEntity> {
        return repository.update(id, name)
    }

    /** Alterna ativo/inativo com base no estado em cache; erros viram snackbar. */
    fun toggleActive(id: String) {
        viewModelScope.launch {
            val cached = repository.getByIdCached(id) ?: return@launch
            val result = if (cached.active) repository.deactivate(id) else repository.activate(id)
            when (result) {
                is ApiResult.HttpError -> {
                    val current = _uiState.value
                    if (current is TagsUiState.Success) {
                        _uiState.value = current.copy(error = "Erro ao alterar status (${result.code}).")
                    }
                }
                is ApiResult.NetworkError -> {
                    val current = _uiState.value
                    if (current is TagsUiState.Success) {
                        _uiState.value = current.copy(error = "Sem conexão. Status não alterado.")
                    }
                }
                is ApiResult.UnknownError -> {
                    val current = _uiState.value
                    if (current is TagsUiState.Success) {
                        _uiState.value = current.copy(error = "Erro inesperado ao alterar status.")
                    }
                }
                is ApiResult.Success -> Unit // Room já atualizado pelo repository
            }
        }
    }
}
