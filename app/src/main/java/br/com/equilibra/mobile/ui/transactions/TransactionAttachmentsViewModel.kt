package br.com.equilibra.mobile.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.attachment.AttachmentPolicy
import br.com.equilibra.mobile.data.attachment.attachmentErrorFrom
import br.com.equilibra.mobile.data.attachment.messagePtBr
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.dto.attachment.AttachmentDto
import br.com.equilibra.mobile.data.repository.AttachmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Task 5.3 — Estado da seção de anexos de uma transação.
 *
 * Só metadata em memória (Task 5.5): a lista vem da API e nunca é persistida.
 */
data class AttachmentsUiState(
    val isLoading: Boolean = false,
    val items: List<AttachmentDto> = emptyList(),
    val isUploading: Boolean = false,
    val uploadingFileName: String? = null,
    val deletingId: String? = null,
    /** Erro de carga (com retry) — quando não há lista para preservar. */
    val loadError: String? = null,
    /** Erro de ação (upload/exclusão) — exibido como snackbar. */
    val actionError: String? = null,
    /** Aviso de sucesso (ex.: "Anexo excluído"). */
    val notice: String? = null
) {
    val isFull: Boolean get() = items.size >= AttachmentPolicy.MAX_PER_TRANSACTION
    val canUpload: Boolean get() = !isUploading && !isFull
}

/**
 * Task 5.3 — Anexos da transação (upload, listagem, exclusão).
 *
 * - Fonte única: API (binários não ficam em Room nem em memória).
 * - Upload: um por vez (anti duplo envio); a lista é recarregada da API após
 *   sucesso para refletir a metadata real do servidor.
 * - Exclusão: com confirmação na tela; item bloqueado durante a chamada.
 * - Download é delegado ao seletor SAF da tela (não passa por aqui para não
 *   segurar bytes em memória).
 */
@HiltViewModel
class TransactionAttachmentsViewModel @Inject constructor(
    private val repository: AttachmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttachmentsUiState(isLoading = true))
    val uiState: StateFlow<AttachmentsUiState> = _uiState.asStateFlow()

    /** Transação associada; definida pela tela ao abrir. */
    private var transactionId: String? = null

    fun bind(transactionId: String) {
        if (this.transactionId == transactionId) return
        this.transactionId = transactionId
        load()
    }

    fun retry() = load()

    /** Chamado pela tela com o resultado do upload feito pelo SAF. */
    fun onUploadResult(result: ApiResult<AttachmentDto>) {
        when (result) {
            is ApiResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    uploadingFileName = null,
                    notice = "Anexo \"${result.data.originalFileName}\" enviado."
                )
                load(silent = true)
            }
            else -> {
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    uploadingFileName = null,
                    actionError = attachmentErrorFrom(result)?.messagePtBr()
                        ?: "Não foi possível enviar o anexo."
                )
            }
        }
    }

    fun onUploadStarted(fileName: String? = null) {
        if (_uiState.value.isUploading) return
        _uiState.value = _uiState.value.copy(isUploading = true, uploadingFileName = fileName)
    }

    fun cancelUpload() {
        if (!_uiState.value.isUploading) return
        _uiState.value = _uiState.value.copy(isUploading = false, uploadingFileName = null)
    }

    /** Exclui após confirmação explícita na tela. */
    fun delete(attachment: AttachmentDto) {
        val current = _uiState.value
        if (current.deletingId != null) return
        _uiState.value = current.copy(deletingId = attachment.id)
        viewModelScope.launch {
            when (val result = repository.delete(attachment.id)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        deletingId = null,
                        notice = "Anexo excluído.",
                        items = _uiState.value.items.filterNot { it.id == attachment.id }
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(
                        deletingId = null,
                        actionError = attachmentErrorFrom(result)?.messagePtBr()
                            ?: "Não foi possível excluir o anexo."
                    )
                }
            }
        }
    }

    fun clearActionError() {
        if (_uiState.value.actionError != null) {
            _uiState.value = _uiState.value.copy(actionError = null)
        }
    }

    fun clearNotice() {
        if (_uiState.value.notice != null) {
            _uiState.value = _uiState.value.copy(notice = null)
        }
    }

    /** Confirma que a falha exige retry de carga (404 etc. viram erro de tela). */
    fun dismissLoadError() {
        if (_uiState.value.loadError != null) {
            _uiState.value = _uiState.value.copy(loadError = null)
        }
    }

    private fun load(silent: Boolean = false) {
        val id = transactionId ?: return
        viewModelScope.launch {
            if (!silent) _uiState.value = _uiState.value.copy(isLoading = true, loadError = null)
            when (val result = repository.list(id)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    items = result.data,
                    loadError = null
                )
                else -> {
                    val message = attachmentErrorFrom(result)?.messagePtBr()
                        ?: "Não foi possível carregar os anexos."
                    val hadItems = _uiState.value.items.isNotEmpty()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        loadError = if (hadItems) null else message,
                        actionError = if (hadItems) message else _uiState.value.actionError
                    )
                }
            }
        }
    }

    /** Tipos aceitos pelo backend, para o seletor de arquivos. */
    val allowedMimeTypes: Array<String> get() = AttachmentPolicy.ALLOWED_MIME_TYPES.toTypedArray()
}
