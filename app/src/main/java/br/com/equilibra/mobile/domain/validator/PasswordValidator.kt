package br.com.equilibra.mobile.domain.validator

/**
 * Regras alinhadas ao backend PasswordPolicy:
 * - mínimo 8, máximo 128
 * - não pode ser vazia nem só espaços
 * - unicode preservado, sem trim silencioso
 */
object PasswordValidator {
    const val MIN_LENGTH = 8
    const val MAX_LENGTH = 128

    fun validate(password: String): String? {
        if (password.isEmpty()) return "Informe a senha"
        if (password.isBlank()) return "Senha não pode conter apenas espaços"
        if (password.length < MIN_LENGTH) return "Senha deve ter pelo menos $MIN_LENGTH caracteres"
        if (password.length > MAX_LENGTH) return "Senha deve ter no máximo $MAX_LENGTH caracteres"
        return null
    }

    fun isValid(password: String): Boolean = validate(password) == null
}
