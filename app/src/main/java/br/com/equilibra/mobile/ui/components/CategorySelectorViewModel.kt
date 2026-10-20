package br.com.equilibra.mobile.ui.components

import androidx.lifecycle.ViewModel
import br.com.equilibra.mobile.data.local.category.CategoryEntity
import br.com.equilibra.mobile.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Task 3.13 — ViewModel do seletor de categoria.
 *
 * - Consulta Room via CategoryRepository (cache local, sem chamada de rede).
 * - Filtro por aplicabilidade e "apenas ativas" é a query do Room
 *   (observeByApplicability), alinhada à regra do backend.
 */
@HiltViewModel
class CategorySelectorViewModel @Inject constructor(
    private val repository: CategoryRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Categorias ativas filtradas por aplicabilidade (EXPENSE | INCOME | BOTH). */
    fun categories(applicability: String): Flow<List<CategoryEntity>> =
        repository.observeCategories(applicability)
            .onEach { _isLoading.value = false }

    /**
     * Todas as categorias ativas, sem filtro de aplicabilidade
     * (filtro do histórico — Task 4.11).
     */
    fun categories(): Flow<List<CategoryEntity>> =
        repository.observeCategories(null)
            .map { list -> list.filter { it.active } }
            .onEach { _isLoading.value = false }
}
