package br.com.equilibra.mobile.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val APPLICABILITY_OPTIONS = listOf(
    "BOTH" to "Despesa e Receita",
    "EXPENSE" to "Somente despesa",
    "INCOME" to "Somente receita"
)

data class CategoryFormState(
    val name: String = "",
    val applicability: String = "BOTH",
    val nameError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val generalError: String? = null,
    val isSuccess: Boolean = false,
    val loadedId: String? = null
)

/**
 * Task 3.10 — Formulário de categoria (criar/editar) com validação.
 *
 * - Edit mode: lê `categoryId` do SavedStateHandle, carrega cache e sincroniza.
 * - Create mode: sem categoryId, envia create.
 * - Escrita só conclui após sucesso da API (repository faz upsert no Room).
 */
@HiltViewModel
class CategoryFormViewModel @Inject constructor(
    private val repository: CategoryRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val categoryId: String? = savedStateHandle.get<String>("categoryId")

    private val _uiState = MutableStateFlow(CategoryFormState())
    val uiState: StateFlow<CategoryFormState> = _uiState.asStateFlow()

    init {
        if (categoryId != null) {
            viewModelScope.launch {
                val cached = repository.getByIdCached(categoryId)
                if (cached != null) {
                    _uiState.value = _uiState.value.copy(
                        name = cached.name,
                        applicability = cached.applicability,
                        loadedId = cached.id
                    )
                }
                when (val result = repository.refreshById(categoryId)) {
                    is ApiResult.Success -> {
                        val entity = result.data
                        _uiState.value = _uiState.value.copy(
                            name = entity.name,
                            applicability = entity.applicability,
                            loadedId = entity.id
                        )
                    }
                    is ApiResult.NetworkError -> {
                        if (cached == null) {
                            _uiState.value = _uiState.value.copy(
                                generalError = "Sem conexão. Não foi possível carregar a categoria."
                            )
                        }
                    }
                    else -> {
                        if (cached == null) {
                            _uiState.value = _uiState.value.copy(
                                generalError = "Erro ao carregar categoria."
                            )
                        }
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value, nameError = null, generalError = null)
    }

    fun onApplicabilityChange(value: String) {
        _uiState.value = _uiState.value.copy(applicability = value, generalError = null)
    }

    fun clearGeneralError() {
        _uiState.value = _uiState.value.copy(generalError = null)
    }

    fun consumeSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }

    fun onSaveClick() {
        val state = _uiState.value
        if (state.isSaving) return

        val name = state.name.trim()
        if (name.isEmpty()) {
            _uiState.value = state.copy(nameError = "Informe o nome da categoria")
            return
        }

        _uiState.value = state.copy(isSaving = true, generalError = null, nameError = null)

        viewModelScope.launch {
            val id = state.loadedId ?: categoryId
            val result = if (id == null) {
                repository.create(name, state.applicability)
            } else {
                repository.update(id, name, state.applicability)
            }
            when (result) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true)
                }
                is ApiResult.HttpError -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        generalError = "Erro ao salvar (${result.code}). Verifique os dados."
                    )
                }
                is ApiResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        generalError = "Sem conexão. Tente novamente."
                    )
                }
                is ApiResult.UnknownError -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        generalError = "Erro inesperado ao salvar."
                    )
                }
            }
        }
    }
}

@Composable
fun CreateCategoryScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: CategoryFormViewModel = hiltViewModel()
) {
    CategoryFormContent(
        title = "Nova categoria",
        state = viewModel.uiState.collectAsState().value,
        onNameChange = viewModel::onNameChange,
        onApplicabilityChange = viewModel::onApplicabilityChange,
        onSave = viewModel::onSaveClick,
        onClearError = viewModel::clearGeneralError,
        onConsumeSuccess = viewModel::consumeSuccess,
        onSuccess = onSuccess,
        onBack = onBack,
        screenDescription = "Tela de criar categoria"
    )
}

@Composable
fun EditCategoryScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: CategoryFormViewModel = hiltViewModel()
) {
    CategoryFormContent(
        title = "Editar categoria",
        state = viewModel.uiState.collectAsState().value,
        onNameChange = viewModel::onNameChange,
        onApplicabilityChange = viewModel::onApplicabilityChange,
        onSave = viewModel::onSaveClick,
        onClearError = viewModel::clearGeneralError,
        onConsumeSuccess = viewModel::consumeSuccess,
        onSuccess = onSuccess,
        onBack = onBack,
        screenDescription = "Tela de editar categoria"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFormContent(
    title: String,
    state: CategoryFormState,
    onNameChange: (String) -> Unit,
    onApplicabilityChange: (String) -> Unit,
    onSave: () -> Unit,
    onClearError: () -> Unit,
    onConsumeSuccess: () -> Unit,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    screenDescription: String
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var dropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onConsumeSuccess()
            onSuccess()
        }
    }

    LaunchedEffect(state.generalError) {
        state.generalError?.let {
            snackbarHostState.showSnackbar(it)
            onClearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .semantics { contentDescription = screenDescription },
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text("Nome") },
            isError = state.nameError != null,
            supportingText = state.nameError?.let { { Text(it) } },
            singleLine = true,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        val selectedLabel = APPLICABILITY_OPTIONS
            .firstOrNull { it.first == state.applicability }?.second ?: state.applicability
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Aplicabilidade") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                APPLICABILITY_OPTIONS.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onApplicabilityChange(value)
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSave,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Salvar")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onBack, enabled = !state.isSaving) {
            Text("Voltar")
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}
