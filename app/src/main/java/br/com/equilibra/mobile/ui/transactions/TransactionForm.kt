package br.com.equilibra.mobile.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.equilibra.mobile.ui.components.AccountSelector
import br.com.equilibra.mobile.ui.components.CategorySelector
import br.com.equilibra.mobile.ui.components.TagSelector

/**
 * Task 4.4 — Tipo de transação do formulário base.
 * Despesa e receita compartilham o mesmo formulário; o tipo configura
 * os campos (ex.: aplicabilidade da categoria).
 */
enum class TransactionFormKind { EXPENSE, INCOME }

/**
 * Configuração do formulário por tipo — campos e filtros por [kind].
 */
data class TransactionFormConfig(
    val kind: TransactionFormKind,
    val title: String,
    /** Aplicabilidade repassada ao CategorySelector (EXPENSE ou INCOME). */
    val categoryApplicability: String
) {
    companion object {
        fun expense() = TransactionFormConfig(
            kind = TransactionFormKind.EXPENSE,
            title = "Nova despesa",
            categoryApplicability = "EXPENSE"
        )

        fun expenseEdit() = TransactionFormConfig(
            kind = TransactionFormKind.EXPENSE,
            title = "Editar despesa",
            categoryApplicability = "EXPENSE"
        )

        fun income() = TransactionFormConfig(
            kind = TransactionFormKind.INCOME,
            title = "Nova receita",
            categoryApplicability = "INCOME"
        )

        fun incomeEdit() = TransactionFormConfig(
            kind = TransactionFormKind.INCOME,
            title = "Editar receita",
            categoryApplicability = "INCOME"
        )
    }
}

/**
 * Task 4.4 — Formulário reutilizável de transação (despesa/receita).
 *
 * - Estado hoisted: recebe [TransactionFormState] + callbacks de edição.
 * - Configurável por tipo via [TransactionFormConfig].
 * - Campos: descrição, valor (pt-BR, sem arredondamento), data dd/MM/aaaa,
 *   conta, categoria (filtrada por aplicabilidade), tags e notas.
 * - Reutiliza os seletores Room-first da Sprint 3 (categoria/tag) e o novo
 *   seletor de conta; erros exibidos inline por campo.
 */
@Composable
fun TransactionForm(
    state: TransactionFormState,
    config: TransactionFormConfig,
    onDescriptionChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onAccountChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = config.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { contentDescription = config.title }
        )

        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            label = { Text("Descrição") },
            isError = state.descriptionError != null,
            supportingText = state.descriptionError?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Campo descrição" }
        )

        OutlinedTextField(
            value = state.amount,
            onValueChange = onAmountChange,
            label = { Text("Valor (R$)") },
            isError = state.amountError != null,
            supportingText = state.amountError?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Campo valor" }
        )

        OutlinedTextField(
            value = state.occurredDate,
            onValueChange = onDateChange,
            label = { Text("Data (dd/MM/aaaa)") },
            isError = state.dateError != null,
            supportingText = state.dateError?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Campo data" }
        )

        AccountSelector(
            selectedId = state.accountId,
            // Conta obrigatória: retap envia null (limpa) — ignorado aqui;
            // a validação exige a conta no envio
            onSelectionChange = { id -> if (id != null) onAccountChange(id) }
        )
        state.accountError?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        CategorySelector(
            applicability = config.categoryApplicability,
            selectedIds = listOfNotNull(state.categoryId),
            onSelectionChange = { ids -> ids.firstOrNull()?.let(onCategoryChange) },
            multiSelect = false
        )
        state.categoryError?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        TagSelector(
            selectedIds = state.tagIds,
            onSelectionChange = onTagsChange,
            multiSelect = true
        )

        OutlinedTextField(
            value = state.notes,
            onValueChange = onNotesChange,
            label = { Text("Notas (opcional)") },
            isError = state.notesError != null,
            supportingText = state.notesError?.let { { Text(it) } },
            minLines = 3,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Campo notas" }
        )
    }
}
