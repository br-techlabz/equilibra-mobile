package br.com.equilibra.mobile.ui.accounts

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.equilibra.mobile.data.remote.dto.account.AssetAccountTypeDto

/**
 * Campos do formulário de conta (Tasks 3.6 e 3.7) — reutilizados
 * entre Criar e Editar para não duplicar UI.
 */
data class AccountFormState(
    val name: String = "",
    val nameError: String? = null,
    val type: String = AssetAccountTypeDto.CHECKING,
    val initialBalanceInput: String = "",
    val initialBalanceError: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountForm(
    state: AccountFormState,
    onNameChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onInitialBalanceChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = state.name,
        onValueChange = onNameChange,
        label = { Text("Nome") },
        isError = state.nameError != null,
        supportingText = state.nameError?.let { { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = AssetAccountTypeDto.labelPtBr(state.type),
            onValueChange = {},
            readOnly = true,
            label = { Text("Tipo") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AssetAccountTypeDto.all.forEach { type ->
                DropdownMenuItem(
                    text = { Text(AssetAccountTypeDto.labelPtBr(type)) },
                    onClick = {
                        onTypeChange(type)
                        expanded = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = state.initialBalanceInput,
        onValueChange = onInitialBalanceChange,
        label = { Text("Saldo inicial") },
        placeholder = { Text("0,00") },
        isError = state.initialBalanceError != null,
        supportingText = state.initialBalanceError?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}
