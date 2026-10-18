package br.com.equilibra.mobile.ui.accounts

import java.math.BigDecimal

/**
 * Validação compartilhada do formulário de conta (Tasks 3.6 e 3.7).
 * Reutilizada por Create/Editar para não duplicar regras.
 */
object AccountFormValidator {

    fun validateName(name: String): String? = when {
        name.trim().isEmpty() -> "Informe o nome"
        name.trim().length > 100 -> "Nome deve ter no máximo 100 caracteres"
        else -> null
    }

    fun validateMoney(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "Informe o saldo inicial"
        val normalized = trimmed.replace(",", ".").replace(" ", "")
        return try {
            val bd = BigDecimal(normalized)
            if (bd.scale() > 2) return "Máximo 2 casas decimais"
            null
        } catch (_: Exception) {
            "Valor inválido"
        }
    }

    fun normalizeMoney(input: String): String {
        val trimmed = input.trim().replace(",", ".").replace(" ", "")
        return try {
            BigDecimal(trimmed).setScale(2).toPlainString()
        } catch (_: Exception) {
            trimmed
        }
    }
}
