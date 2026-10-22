package br.com.equilibra.mobile.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import br.com.equilibra.mobile.data.attachment.AttachmentPolicy
import br.com.equilibra.mobile.data.attachment.DocumentAttachmentPicker
import br.com.equilibra.mobile.data.attachment.DocumentMetadata
import br.com.equilibra.mobile.data.attachment.attachmentErrorFrom
import br.com.equilibra.mobile.data.attachment.messagePtBr
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.dto.attachment.AttachmentDto
import kotlinx.coroutines.launch

/**
 * Task 5.2 — Seletor de arquivos via Storage Access Framework.
 *
 * - `ActivityResultContracts.OpenDocument` (contrato moderno, sem permissão
 *   ampla de armazenamento): o acesso vem do grant temporário da URI.
 * - `mimeTypes` restringe o navegador do SAF à whitelist do backend, mas o
 *   app ainda valida tipo/tamanho — provedores podem devolver outro tipo.
 * - Cancelamento (URI nula) é tratado silenciosamente: nenhum erro, nenhum
 *   efeito colateral.
 * - A URI não é retida além do envio; o upload consome o stream e envia.
 */
@Composable
fun DocumentPickerButton(
    picker: DocumentAttachmentPicker,
    transactionId: String,
    onUploaded: (AttachmentDto) -> Unit,
    onError: (String) -> Unit,
    onStateChange: (isUploading: Boolean) -> Unit = {},
    enabled: Boolean = true,
    label: String = "Anexar arquivo",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedName by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        // Cancelou: nada acontece (critério de aceite da task)
        if (uri == null) return@rememberLauncherForActivityResult
        onStateChange(true)
        scope.launch {
            try {
                val meta = picker.readMetadata(context.contentResolver, uri)
                if (meta != null) selectedName = meta.fileName
                when (val result = picker.upload(context.contentResolver, transactionId, uri)) {
                    is ApiResult.Success -> onUploaded(result.data)
                    else -> onError(uploadErrorMessage(result, meta))
                }
            } finally {
                onStateChange(false)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = {
                // Só tipos aceitos aparecem; o backend revalida a assinatura
                launcher.launch(AttachmentPolicy.ALLOWED_MIME_TYPES.toTypedArray())
            },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Selecionar arquivo para anexar" }
        ) {
            Text(if (selectedName != null) "Substituir arquivo" else label)
        }
        selectedName?.let { name ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun uploadErrorMessage(result: ApiResult<*>, meta: DocumentMetadata?): String =
    attachmentErrorFrom(result)?.messagePtBr()
        ?: meta?.let { "Não foi possível anexar ${it.fileName}." }
        ?: "Não foi possível anexar o arquivo."
