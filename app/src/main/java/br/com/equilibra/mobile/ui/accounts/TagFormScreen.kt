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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Contrato do backend: name @NotBlank @Size(max = 100). */
private const val NAME_MAX_LENGTH = 100

data class TagFormState(
    val name: String = "",
    val nameError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val generalError: String? = null,
    val isSuccess: Boolean = false,
    val loadedId: String? = null
)

/**
 * Task 3.12 — Formulário de tag (criar/editar) com validação.
 *
 * - Edit mode: lê `tagId` do SavedStateHandle, carrega cache e sincroniza.
 * - Create mode: sem tagId, envia create.
 * - Valida nome não vazio e max 100 (constraint do backend).
 * - Escrita só conclui após sucesso da API (repository faz upsert no Room).
 */
@HiltViewModel
class TagFormViewModel @Inject constructor(
    private val repository: TagRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tagId: String? = savedStateHandle.get<String>("tagId")

    private val _uiState = MutableStateFlow(TagFormState())
    val uiState: StateFlow<TagFormState> = _uiState.asStateFlow()

    init {
        if (tagId != null) {
            viewModelScope.launch {
                val cached = repository.getByIdCached(tagId)
                if (cached != null) {
                    _uiState.value = _uiState.value.copy(
                        name = cached.name,
                        loadedId = cached.id
                    )
                }
                when (val result = repository.refreshById(tagId)) {
                    is ApiResult.Success -> {
                        val entity = result.data
                        _uiState.value = _uiState.value.copy(
                            name = entity.name,
                            loadedId = entity.id
                        )
                    }
                    is ApiResult.NetworkError -> {
                        if (cached == null) {
                            _uiState.value = _uiState.value.copy(
                                generalError = "Sem conexão. Não foi possível carregar a tag."
                            )
                        }
                    }
                    else -> {
                        if (cached == null) {
                            _uiState.value = _uiState.value.copy(
                                generalError = "Erro ao carregar tag."
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
        when {
            name.isEmpty() -> {
                _uiState.value = state.copy(nameError = "Informe o nome da tag")
                return
            }
            name.length > NAME_MAX_LENGTH -> {
                _uiState.value = state.copy(
                    nameError = "Máximo de $NAME_MAX_LENGTH caracteres"
                )
                return
            }
        }

        _uiState.value = state.copy(isSaving = true, generalError = null, nameError = null)

        viewModelScope.launch {
            val id = state.loadedId ?: tagId
            val result = if (id == null) {
                repository.create(name)
            } else {
                repository.update(id, name)
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
fun CreateTagScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: TagFormViewModel = hiltViewModel()
) {
    TagFormContent(
        title = "Nova tag",
        state = viewModel.uiState.collectAsState().value,
        onNameChange = viewModel::onNameChange,
        onSave = viewModel::onSaveClick,
        onClearError = viewModel::clearGeneralError,
        onConsumeSuccess = viewModel::consumeSuccess,
        onSuccess = onSuccess,
        onBack = onBack,
        screenDescription = "Tela de criar tag"
    )
}

@Composable
fun EditTagScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: TagFormViewModel = hiltViewModel()
) {
    TagFormContent(
        title = "Editar tag",
        state = viewModel.uiState.collectAsState().value,
        onNameChange = viewModel::onNameChange,
        onSave = viewModel::onSaveClick,
        onClearError = viewModel::clearGeneralError,
        onConsumeSuccess = viewModel::consumeSuccess,
        onSuccess = onSuccess,
        onBack = onBack,
        screenDescription = "Tela de editar tag"
    )
}

@Composable
private fun TagFormContent(
    title: String,
    state: TagFormState,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onClearError: () -> Unit,
    onConsumeSuccess: () -> Unit,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    screenDescription: String
) {
    val snackbarHostState = remember { SnackbarHostState() }

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
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            ),
            modifier = Modifier.fillMaxWidth()
        )

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
