package br.com.equilibra.mobile.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.local.category.CategoryEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.dto.category.CreateCategoryRequestDto
import br.com.equilibra.mobile.data.remote.dto.category.UpdateCategoryRequestDto
import br.com.equilibra.mobile.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CategoriesUiState {
    data object Loading : CategoriesUiState()
    data class Success(
        val categories: List<CategoryEntity>,
        val isRefreshing: Boolean = false,
        val error: String? = null
    ) : CategoriesUiState()
    data class Error(val message: String, val canRetry: Boolean = true) : CategoriesUiState()
}

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoriesUiState>(CategoriesUiState.Loading)
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    private var hasLoadedOnce = false
    private var currentApplicability: String? = null

    init {
        // Room-first: emite cache imediatamente e depois atualiza em background
        viewModelScope.launch {
            repository.observeCategories(null).collectLatest { cached ->
                val current = _uiState.value

                if (!hasLoadedOnce) {
                    if (cached.isEmpty()) {
                        _uiState.value = CategoriesUiState.Loading
                    } else {
                        _uiState.value = CategoriesUiState.Success(
                            categories = cached,
                            isRefreshing = true
                        )
                    }
                    hasLoadedOnce = true
                    refreshInBackground(null)
                } else {
                    // Atualização silenciosa — mantém estado, só troca lista
                    when (current) {
                        is CategoriesUiState.Success -> {
                            _uiState.value = current.copy(categories = cached)
                        }
                        is CategoriesUiState.Loading -> {
                            if (cached.isNotEmpty()) {
                                _uiState.value = CategoriesUiState.Success(categories = cached)
                            }
                        }
                        is CategoriesUiState.Error -> {
                            if (cached.isNotEmpty()) {
                                _uiState.value = CategoriesUiState.Success(categories = cached)
                            }
                        }
                    }
                }
            }
        }
    }

    fun observeCategories(applicability: String? = null) = repository.observeCategories(applicability)

    fun load(applicability: String? = null) {
        currentApplicability = applicability
        viewModelScope.launch {
            val result = repository.refresh(applicability, includeInactive = true)
            when (result) {
                is ApiResult.Success -> {
                    val current = _uiState.value
                    if (current is CategoriesUiState.Success) {
                        _uiState.value = current.copy(isRefreshing = false, error = null)
                    } else if (current is CategoriesUiState.Loading) {
                        val cached = repository.getAllCached(null)
                        _uiState.value = if (cached.isEmpty()) {
                            CategoriesUiState.Success(categories = emptyList())
                        } else {
                            CategoriesUiState.Success(categories = cached)
                        }
                    }
                }
                is ApiResult.HttpError -> handleRefreshError("Erro ao carregar categorias (${result.code}).")
                is ApiResult.NetworkError -> handleRefreshError("Sem conexão. Mostrando dados locais.")
                is ApiResult.UnknownError -> handleRefreshError("Erro inesperado. Mostrando dados locais.")
            }
        }
    }

    fun refresh() {
        val current = _uiState.value
        if (current is CategoriesUiState.Success) {
            _uiState.value = current.copy(isRefreshing = true, error = null)
        } else {
            _uiState.value = CategoriesUiState.Loading
        }
        refreshInBackground(currentApplicability)
    }

    fun dismissError() {
        val current = _uiState.value
        if (current is CategoriesUiState.Success) {
            _uiState.value = current.copy(error = null)
        }
    }

    private fun refreshInBackground(applicability: String?) {
        viewModelScope.launch {
            when (val result = repository.refresh(applicability, includeInactive = true)) {
                is ApiResult.Success -> {
                    val current = _uiState.value
                    if (current is CategoriesUiState.Success) {
                        _uiState.value = current.copy(isRefreshing = false, error = null)
                    } else if (current is CategoriesUiState.Loading) {
                        val cached = repository.getAllCached(null)
                        _uiState.value = if (cached.isEmpty()) {
                            CategoriesUiState.Success(categories = emptyList())
                        } else {
                            CategoriesUiState.Success(categories = cached)
                        }
                    }
                }
                is ApiResult.HttpError -> handleRefreshError("Erro ao carregar categorias (${result.code}).")
                is ApiResult.NetworkError -> handleRefreshError("Sem conexão. Mostrando dados locais.")
                is ApiResult.UnknownError -> handleRefreshError("Erro inesperado. Mostrando dados locais.")
            }
        }
    }

    private suspend fun handleRefreshError(message: String) {
        val cached = repository.getAllCached(null)
        val current = _uiState.value
        _uiState.value = if (cached.isNotEmpty()) {
            CategoriesUiState.Success(categories = cached, isRefreshing = false, error = message)
        } else if (current is CategoriesUiState.Loading) {
            CategoriesUiState.Error(message)
        } else {
            if (current is CategoriesUiState.Success) {
                current.copy(isRefreshing = false, error = message)
            } else {
                CategoriesUiState.Error(message)
            }
        }
    }

    // Escritas — delegam para o repository, que já faz upsert no Room

    suspend fun create(name: String, applicability: String): ApiResult<CategoryEntity> {
        return repository.create(name, applicability)
    }

    suspend fun update(id: String, name: String, applicability: String): ApiResult<CategoryEntity> {
        return repository.update(id, name, applicability)
    }

    suspend fun deactivate(id: String): ApiResult<CategoryEntity> {
        return repository.deactivate(id)
    }

    suspend fun activate(id: String): ApiResult<CategoryEntity> {
        return repository.activate(id)
    }

    /** Alterna ativo/inativo com base no estado em cache; erros viram snackbar. */
    fun toggleActive(id: String) {
        viewModelScope.launch {
            val cached = repository.getByIdCached(id) ?: return@launch
            val result = if (cached.active) repository.deactivate(id) else repository.activate(id)
            if (result is ApiResult.HttpError) {
                val current = _uiState.value
                if (current is CategoriesUiState.Success) {
                    _uiState.value = current.copy(error = "Erro ao alterar status (${result.code}).")
                }
            } else if (result is ApiResult.NetworkError) {
                val current = _uiState.value
                if (current is CategoriesUiState.Success) {
                    _uiState.value = current.copy(error = "Sem conexão. Status não alterado.")
                }
            } else if (result is ApiResult.UnknownError) {
                val current = _uiState.value
                if (current is CategoriesUiState.Success) {
                    _uiState.value = current.copy(error = "Erro inesperado ao alterar status.")
                }
            }
        }
    }
}