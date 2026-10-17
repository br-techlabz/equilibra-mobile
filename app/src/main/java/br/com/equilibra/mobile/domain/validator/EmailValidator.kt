package br.com.equilibra.mobile.domain.validator

object EmailValidator {
    private const val MAX_LENGTH = 255
    private val PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+\$")

    fun validate(email: String): String? {
        val trimmed = email.trim()
        if (trimmed.isBlank()) return "Informe o e-mail"
        if (trimmed.length > MAX_LENGTH) return "E-mail deve ter no máximo 255 caracteres"
        if (!PATTERN.matches(trimmed)) return "E-mail inválido"
        return null
    }

    fun isValid(email: String): Boolean = validate(email) == null

    fun normalize(email: String): String = email.trim().lowercase()
}
