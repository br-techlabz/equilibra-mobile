package br.com.equilibra.mobile.ui.transactions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.equilibra.mobile.data.attachment.AttachmentPreviewPolicy
import br.com.equilibra.mobile.data.attachment.PreviewKind
import br.com.equilibra.mobile.data.remote.dto.attachment.AttachmentDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Task 5.4 — Preview de imagem/PDF do anexo.
 *
 * - Formato seguro: imagem decodificada em bitmap e PDF renderizado pelo
 *   `PdfRenderer` nativo (conteúdo tratado como dado; nada é executado).
 * - Formatos não suportados ou arquivo grande demais caem no fallback com a
 *   ação de salvar — nunca uma tela em branco.
 * - O trabalho roda fora da composição (efeito por anexo) e é cancelado ao
 *   fechar; o bitmap vive só enquanto este diálogo existe (lifecycle).
 */
@Composable
fun AttachmentPreviewDialog(
    attachment: AttachmentDto,
    loader: AttachmentPreviewLoader,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val previewable = AttachmentPreviewPolicy.canPreview(attachment.contentType, attachment.sizeBytes)
    var state by remember(attachment.id) {
        mutableStateOf<PreviewUiState>(
            if (previewable) PreviewUiState.Loading else PreviewUiState.Unavailable(null)
        )
    }

    LaunchedEffect(attachment.id) {
        if (!previewable) return@LaunchedEffect
        val loaded = withContext(Dispatchers.Default) {
            loader.load(attachment.id, attachment.contentType, attachment.sizeBytes)
        }
        state = if (loaded != null) {
            PreviewUiState.Ready(loaded.kind, loaded.image)
        } else {
            PreviewUiState.Unavailable(AttachmentPreviewPolicy.fallbackMessage(attachment.contentType))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .semantics { contentDescription = "Pré-visualização de ${attachment.originalFileName}" },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = attachment.originalFileName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (state) {
                        is PreviewUiState.Ready -> when ((state as PreviewUiState.Ready).kind) {
                            PreviewKind.PDF -> "PDF — primeira página"
                            else -> "Imagem"
                        }
                        else -> formatAttachmentSize(attachment.sizeBytes)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 480.dp)
                        .padding(vertical = 12.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    when (val s = state) {
                        is PreviewUiState.Loading -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Text("Carregando pré-visualização...", style = MaterialTheme.typography.bodySmall)
                        }

                        is PreviewUiState.Ready -> Image(
                            bitmap = s.image,
                            contentDescription = "Pré-visualização de ${attachment.originalFileName}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )

                        is PreviewUiState.Unavailable -> Text(
                            text = s.message
                                ?: AttachmentPreviewPolicy.fallbackMessage(attachment.contentType),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDownload) { Text("Baixar") }
                    TextButton(onClick = onDismiss) { Text("Fechar") }
                }
            }
        }
    }
}

/** Estado interno do preview. */
sealed interface PreviewUiState {
    data object Loading : PreviewUiState
    data class Ready(val kind: PreviewKind, val image: androidx.compose.ui.graphics.ImageBitmap) : PreviewUiState
    data class Unavailable(val message: String?) : PreviewUiState
}
