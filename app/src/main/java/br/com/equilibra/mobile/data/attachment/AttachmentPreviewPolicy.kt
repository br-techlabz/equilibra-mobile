package br.com.equilibra.mobile.data.attachment

/**
 * Task 5.4 — Quais anexos podem ser pré-visualizados e como.
 *
 * Só formatos que o backend aceita E que o app sabe renderizar com segurança
 * (sem executar conteúdo): imagens raster (JPEG/PNG/WEBP) e PDF. SVG/HTML/
 * executáveis nunca chegam ao storage (whitelist do backend), e mesmo que
 * chegassem cairiam em [PreviewKind.None].
 */
enum class PreviewKind {
    /** Renderiza com decodificador de imagem do Android/Coil. */
    IMAGE,

    /** Renderiza páginas com `PdfRenderer` (API nativa), só leitura. */
    PDF,

    /** Sem preview: oferece abrir/salvar (fallback explícito). */
    None
}

object AttachmentPreviewPolicy {

    fun kindFor(contentType: String?): PreviewKind = when (contentType?.trim()?.lowercase()) {
        "image/jpeg", "image/jpg", "image/png", "image/webp" -> PreviewKind.IMAGE
        "application/pdf" -> PreviewKind.PDF
        else -> PreviewKind.None
    }

    /**
     * Limite para abrir preview automaticamente. Acima disso o usuário
     * escolhe abrir explicitamente (evita decodificar arquivos enormes e
     * travar a tela).
     */
    const val MAX_AUTO_PREVIEW_BYTES: Long = 10L * 1024 * 1024

    fun canPreview(contentType: String?, sizeBytes: Long): Boolean =
        kindFor(contentType) != PreviewKind.None && sizeBytes in 1..MAX_AUTO_PREVIEW_BYTES

    /** Texto do fallback quando não há preview (sempre com ação de salvar). */
    fun fallbackMessage(contentType: String?): String = when (kindFor(contentType)) {
        PreviewKind.None -> "Este tipo de arquivo não tem pré-visualização. Use o download para abrir."
        PreviewKind.IMAGE, PreviewKind.PDF -> "Pré-visualização indisponível para este arquivo."
    }
}
