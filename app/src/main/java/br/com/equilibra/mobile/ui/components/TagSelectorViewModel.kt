package br.com.equilibra.mobile.ui.components

import androidx.lifecycle.ViewModel
import br.com.equilibra.mobile.data.local.tag.TagEntity
import br.com.equilibra.mobile.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Task 3.13 — ViewModel do seletor de tag.
 *
 * - Consulta Room via TagRepository (cache local, sem chamada de rede).
 * - Exibe apenas tags ativas (o cache inclui inativas para a tela de gestão).
 */
@HiltViewModel
class TagSelectorViewModel @Inject constructor(
    private val repository: TagRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Tags ativas em ordem alfabética ((Room-first)). */
    fun tags(): Flow<List<TagEntity>> =
        repository.observeTags()
            .map { list -> list.filter { it.active } }
            .onEach { _isLoading.value = false }
}
