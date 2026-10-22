package br.com.equilibra.mobile.data.attachment

/** Texto pt-BR de cada falha tipada (Task 5.2 — usado pela tela na 5.3). */
fun AttachmentError.messagePtBr(): String = when (this) {
    AttachmentError.InvalidFile ->
        "Arquivo inválido. Use PDF, JPG, PNG ou WEBP com até 10 MB."
    AttachmentError.NotFound ->
        "Transação ou anexo não encontrado."
    is AttachmentError.Conflict -> when (reason) {
        AttachmentError.ConflictReason.CANCELLED_TRANSACTION ->
            "Transação cancelada não recebe anexos."
        AttachmentError.ConflictReason.LIMIT_REACHED ->
            "Limite de ${AttachmentPolicy.MAX_PER_TRANSACTION} anexos por transação atingido."
        AttachmentError.ConflictReason.UNKNOWN ->
            "Não foi possível anexar o arquivo."
    }
    AttachmentError.TooLarge -> "Arquivo acima de 10 MB."
    is AttachmentError.Http -> "Erro ao processar o anexo (${code})."
    is AttachmentError.Network -> "Sem conexão. Verifique sua internet."
    is AttachmentError.Unknown -> "Erro inesperado ao processar o anexo."
    is AttachmentError.Generic -> message ?: "Não foi possível processar o anexo."
}
