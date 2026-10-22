package br.com.equilibra.mobile.ui.transactions

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.equilibra.mobile.data.attachment.AttachmentPolicy
import br.com.equilibra.mobile.data.attachment.DocumentAttachmentPicker
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.dto.attachment.AttachmentDto
import br.com.equilibra.mobile.data.repository.AttachmentRepository
import kotlinx.coroutines.launch

/**
 * Task 5.3 — Seção de anexos da transação.
 *
 * - Upload: seletor SAF (5.2) restrito à whitelist; um envio por vez.
 * - Lista: metadata vinda da API; download salva pelo seletor de destino do
 *   SAF (`CreateDocument`), sem passar pelo armazenamento do app.
 * - Exclusão: sempre com confirmação (ação destrutiva).
 * - Estados: carregando, vazio, erro com retry, progresso e snackbar — a
 *   tela pai exibe os avisos para manter um único SnackbarHost.
 */
@Composable
fun TransactionAttachmentsSection(
    transactionId: String,
    onActionError: (String) -> Unit,
    onNotice: (String) -> Unit,
    viewModel: TransactionAttachmentsViewModel = hiltViewModel(),
    picker: DocumentAttachmentPicker,
    previewLoader: AttachmentPreviewLoader,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<AttachmentDto?>(null) }
    var pendingDownload by remember { mutableStateOf<AttachmentDto?>(null) }
    var pendingPreview by remember { mutableStateOf<AttachmentDto?>(null) }

    LaunchedEffect(transactionId) { viewModel.bind(transactionId) }
    LaunchedEffect(state.actionError) {
        state.actionError?.let { onActionError(it); viewModel.clearActionError() }
    }
    LaunchedEffect(state.notice) {
        state.notice?.let { onNotice(it); viewModel.clearNotice() }
    }

    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult // cancelou
        viewModel.onUploadStarted()
        scope.launch {
            val result = picker.upload(context.contentResolver, transactionId, uri)
            viewModel.onUploadResult(result)
        }
    }

    // CreateDocument: o usuário escolhe onde salvar; nada vai para o app
    val downloadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destination: Uri? ->
        val attachment = pendingDownload ?: return@rememberLauncherForActivityResult
        pendingDownload = null
        if (destination == null) return@rememberLauncherForActivityResult // cancelou
        scope.launch {
            when (val result = picker.saveDownload(context.contentResolver, attachment.id, destination)) {
                is ApiResult.Success -> onNotice("Download concluído (${result.data} bytes).")
                else -> onActionError("Não foi possível baixar o anexo.")
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Anexos da transação" },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Anexos",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${state.items.size}/${AttachmentPolicy.MAX_PER_TRANSACTION}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            when {
                state.isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Carregando anexos...", style = MaterialTheme.typography.bodySmall)
                }

                state.loadError != null -> Column {
                    Text(
                        text = state.loadError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = { viewModel.retry() }) { Text("Tentar novamente") }
                }

                state.items.isEmpty() -> Text(
                    text = "Nenhum anexo. Adicione PDF, JPG, PNG ou WEBP (até 10 MB).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.items.forEach { attachment ->
                        AttachmentRow(
                            attachment = attachment,
                            isDeleting = state.deletingId == attachment.id,
                            onDownload = {
                                pendingDownload = attachment
                                downloadLauncher.launch(attachment.originalFileName)
                            },
                            onDelete = { pendingDelete = attachment },
                            onPreview = { pendingPreview = attachment }
                        )
                    }
                }
            }

            if (state.isFull) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Limite de ${AttachmentPolicy.MAX_PER_TRANSACTION} anexos atingido.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { uploadLauncher.launch(AttachmentPolicy.ALLOWED_MIME_TYPES.toTypedArray()) },
                    enabled = state.canUpload,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Anexar arquivo")
                    }
                }
            }
        }
    }

    pendingPreview?.let { attachment ->
        AttachmentPreviewDialog(
            attachment = attachment,
            loader = previewLoader,
            onDownload = {
                pendingPreview = null
                pendingDownload = attachment
                downloadLauncher.launch(attachment.originalFileName)
            },
            onDismiss = { pendingPreview = null }
        )
    }

    pendingDelete?.let { attachment ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Excluir anexo") },
            text = { Text("Excluir \"${attachment.originalFileName}\"? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    viewModel.delete(attachment)
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun AttachmentRow(
    attachment: AttachmentDto,
    isDeleting: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onPreview: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${attachment.originalFileName}, ${formatAttachmentSize(attachment.sizeBytes)}"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.originalFileName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatAttachmentSize(attachment.sizeBytes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isDeleting) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            IconButton(onClick = onPreview) {
                Icon(Icons.Default.Visibility, contentDescription = "Pré-visualizar ${attachment.originalFileName}")
            }
            IconButton(onClick = onDownload) {
                Icon(Icons.Default.Download, contentDescription = "Baixar ${attachment.originalFileName}")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir ${attachment.originalFileName}")
            }
        }
    }
}

/** Tamanho legível pt-BR (KB/MB com uma casa). */
internal fun formatAttachmentSize(sizeBytes: Long): String = when {
    sizeBytes < 1024 -> "$sizeBytes B"
    sizeBytes < 1024 * 1024 -> String.format(java.util.Locale("pt", "BR"), "%.1f KB", sizeBytes / 1024.0)
    else -> String.format(java.util.Locale("pt", "BR"), "%.1f MB", sizeBytes / (1024.0 * 1024.0))
}
