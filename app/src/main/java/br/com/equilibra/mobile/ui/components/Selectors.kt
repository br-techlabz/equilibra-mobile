package br.com.equilibra.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.equilibra.mobile.data.local.category.CategoryEntity
import br.com.equilibra.mobile.data.local.tag.TagEntity

/**
 * Task 3.13 — Seletor reutilizável de categoria (Room-first).
 *
 * - Consulta o cache local (Room) via [CategorySelectorViewModel]; sem rede.
 * - Filtra por aplicabilidade (EXPENSE|INCOME|BOTH) e apenas ativas —
 *   regra aplicada pela query do Room, alinhada ao backend.
 * - Seleção única (padrão) ou múltipla conforme o formulário consumidor.
 * - A seleção retorna sempre IDs estáveis (UUIDs) via [onSelectionChange].
 * - Estados de loading e vazio embutidos.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySelector(
    applicability: String,
    selectedIds: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    multiSelect: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: CategorySelectorViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    var categories by remember { mutableStateOf<List<CategoryEntity>?>(null) }

    LaunchedEffect(applicability) {
        viewModel.categories(applicability).collect { categories = it }
    }

    SelectorChipList(
        title = if (multiSelect) "Categorias" else "Categoria",
        options = categories.orEmpty().map { it.id to it.name },
        selectedIds = selectedIds,
        multiSelect = multiSelect,
        isLoading = isLoading && categories == null,
        loadingMessage = "Carregando categorias...",
        emptyMessage = "Nenhuma categoria disponível",
        optionTagPrefix = "category",
        onToggle = { id ->
            onSelectionChange(SelectionLogic.toggle(selectedIds, id, multiSelect))
        },
        modifier = modifier
    )
}

/**
 * Task 3.13 — Seletor reutilizável de tag (Room-first).
 *
 * - Consulta o cache local (Room) via [TagSelectorViewModel]; sem rede.
 * - Exibe apenas tags ativas (o cache da tela de gestão mantém as inativas).
 * - Seleção única ou múltipla; retorna IDs estáveis via [onSelectionChange].
 */
@Composable
fun TagSelector(
    selectedIds: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    multiSelect: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: TagSelectorViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    var tags by remember { mutableStateOf<List<TagEntity>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.tags().collect { tags = it }
    }

    SelectorChipList(
        title = if (multiSelect) "Tags" else "Tag",
        options = tags.orEmpty().map { it.id to it.name },
        selectedIds = selectedIds,
        multiSelect = multiSelect,
        isLoading = isLoading && tags == null,
        loadingMessage = "Carregando tags...",
        emptyMessage = "Nenhuma tag disponível",
        optionTagPrefix = "tag",
        onToggle = { id ->
            onSelectionChange(SelectionLogic.toggle(selectedIds, id, multiSelect))
        },
        modifier = modifier
    )
}

/**
 * Lista de chips compartilhada pelos seletores — sem duplicação de UI.
 * Cada chip carrega o ID estável em testTag e no contentDescription.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SelectorChipList(
    title: String,
    options: List<Pair<String, String>>,
    selectedIds: List<String>,
    multiSelect: Boolean,
    isLoading: Boolean,
    loadingMessage: String,
    emptyMessage: String,
    optionTagPrefix: String,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading -> {
                Text(
                    text = loadingMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = "$loadingMessage"
                    }
                )
            }

            options.isEmpty() -> {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = emptyMessage
                    }
                )
            }

            else -> {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    options.forEach { (id, label) ->
                        val selected = id in selectedIds
                        FilterChip(
                            selected = selected,
                            onClick = { onToggle(id) },
                            label = { Text(label) },
                            modifier = Modifier
                                .testTag("${optionTagPrefix}_$id")
                                .semantics {
                                    contentDescription =
                                        "$label, ${if (selected) "selecionada" else "não selecionada"}"
                                }
                        )
                    }
                }
            }
        }
    }
}
