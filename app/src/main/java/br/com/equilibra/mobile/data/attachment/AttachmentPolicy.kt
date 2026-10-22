package br.com.equilibra.mobile.data.attachment

/**
 * Task 5.1 — Política de anexos do contrato real do backend
 * (`AttachmentProperties` + `AttachmentFilePolicy`), espelhada no app para
 * validar ANTES do upload (fail-fast) e exibir limites na UI.
 *
 * O servidor continua sendo a autoridade: valida tamanho, MIME declarado e
 * assinatura (magic bytes) do conteúdo. O cliente valida o que consegue sem
 * ler o arquivo inteiro e nunca assume que a extensão prova o tipo.
 */
object AttachmentPolicy {

    /** Limite do backend: equilibra.attachments.max-file-size (10 MB). */
    const val MAX_FILE_BYTES: Long = 10L * 1024 * 1024

    /** Limite do backend: max-attachments-per-transaction. */
    const val MAX_PER_TRANSACTION: Int = 10

    /** Whitelist do backend (GIF/SVG/HTML/executáveis são rejeitados). */
    val ALLOWED_MIME_TYPES: Set<String> = setOf(
        "application/pdf",
        "image/jpeg",
        "image/png",
        "image/webp"
    )

    /**
     * Nome original aceito pelo backend (`TransactionAttachment.validateFileName`):
     * não vazio, até 255 caracteres, sem CR/LF/NUL e sem path traversal.
     */
    const val MAX_FILE_NAME_LENGTH: Int = 255

    fun isAllowedMimeType(mime: String?): Boolean =
        mime?.trim()?.lowercase() in ALLOWED_MIME_TYPES

    fun isWithinSizeLimit(sizeBytes: Long): Boolean =
        sizeBytes in 1..MAX_FILE_BYTES

    /** Espelha [TransactionAttachment.validateFileName] para falhar antes do upload. */
    fun isValidFileName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        if (name.length > MAX_FILE_NAME_LENGTH) return false
        if (name.any { it == '\r' || it == '\n' || it == '\u0000' }) return false
        if (name.contains("../") || name.contains("..\\")) return false
        return true
    }
}
