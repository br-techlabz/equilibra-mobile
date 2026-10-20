package br.com.equilibra.mobile.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.equilibra.mobile.data.repository.HistoryFilters
import br.com.equilibra.mobile.ui.components.AccountSelector
import br.com.equilibra.mobile.ui.components.CategorySelector
import br.com.equilibra.mobile.ui.components.TagSelector

/**
 * Task 4.11 — Painel de filtros combinados do histórico unificado.
 *
 * - Filtros: tipo, status, período (datas dd/MM/aaaa), conta, categoria e
 *   tags — todos opcionais e combináveis; "Aplicar" envia o rascunho ao
 *   ViewModel, que recarrega da página 0.
 * - O rascunho é local: fechar o painel sem aplicar não altera a consulta.
 * - Datas inválidas ou intervalo invertido bloqueiam "Aplicar".
 * - Usa os seletores Room-first existentes (sem chamadas de rede extras) —
 *   o endpoint /transactions segue sendo a única fonte dos resultados.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun HistoryFilterSheet(
    applied: HistoryFilters,
    onApply: (HistoryFilters) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember { mutableStateOf(HistoryFilterDraft.from(applied)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .semantics { contentDescription = "Filtros do histórico" },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Filtros",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            FilterChipRow(
                title = "Tipo",
                options = HistoryFilterOptions.types,
                selected = draft.type,
                onSelected = { draft = draft.copy(type = it) }
            )

            FilterChipRow(
                title = "Status",
                options = HistoryFilterOptions.statuses,
                selected = draft.status,
                onSelected = { draft = draft.copy(status = it) }
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Período",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.fromDate,
                        onValueChange = { draft = draft.copy(fromDate = it) },
                        label = { Text("De") },
                        placeholder = { Text("dd/MM/aaaa") },
                        isError = draft.fromError != null,
                        supportingText = draft.fromError?.let { { Text(it) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "Filtro de data inicial" }
                    )
                    OutlinedTextField(
                        value = draft.toDate,
                        onValueChange = { draft = draft.copy(toDate = it) },
                        label = { Text("Até") },
                        placeholder = { Text("dd/MM/aaaa") },
                        isError = draft.toError != null || draft.rangeError != null,
                        supportingText = (draft.toError ?: draft.rangeError)?.let { { Text(it) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "Filtro de data final" }
                    )
                }
            }

            AccountSelector(
                selectedId = draft.accountId,
                onSelectionChange = { draft = draft.copy(accountId = it) }
            )

            CategorySelector(
                applicability = null, // todas as categorias ativas
                selectedIds = listOfNotNull(draft.categoryId),
                onSelectionChange = { ids ->
                    draft = draft.copy(categoryId = ids.firstOrNull())
                },
                multiSelect = false
            )

            TagSelector(
                selectedIds = draft.tagIds,
                onSelectionChange = { draft = draft.copy(tagIds = it) },
                multiSelect = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        draft = HistoryFilterDraft.empty()
                        onClear()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Limpar filtros")
                }
                Button(
                    onClick = { onApply(draft.toFilters()) },
                    enabled = draft.isValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Aplicar")
                }
            }
        }
    }
}

/**
 * Linha de chips de seleção única com toggle (tocar no escolhido limpa).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipRow(
    title: String,
    options: List<Pair<String, String>>,
    selected: String?,
    onSelected: (String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelected(if (value == selected) null else value) },
                    label = { Text(label) },
                    modifier = Modifier.semantics {
                        contentDescription =
                            "$label, ${if (value == selected) "selecionado" else "não selecionado"}"
                    }
                )
            }
        }
    }
}
