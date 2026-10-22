package br.com.equilibra.mobile.ui.cards

import java.math.BigDecimal
import java.time.ZoneId

/**
 * Task 6.4 — Validação do formulário de cartão.
 *
 * Regras espelham o contrato real do backend (CreateCreditCardRequest +
 * CreditCard.changeDetails):
 * - name/institution: obrigatórios, até 100 chars
 * - nickname: opcional, até 100 chars
 * - totalLimit: > 0, no máximo 2 casas decimais (backend @DecimalMin("0.01") + money())
 * - closingDay/dueDay: inteiros entre 1 e 31 (@Min(1) @Max(31))
 * - timezone: IANA zone ID válida; vazio → null (backend aplicará
 *   "America/Sao_Paulo")
 *
 * NOTA: o backend NÃO exige closingDay < dueDay (só valida cada dia em
 * 1..31). O prompt da task manda não assumir essa relação sem contrato —
 * então o formulário não impõe.
 */
object CreditCardFormValidator {

    fun validateName(name: String): String? = when {
        name.trim().isEmpty() -> "Informe o nome"
        name.trim().length > 100 -> "Nome deve ter no máximo 100 caracteres"
        else -> null
    }

    fun validateInstitution(institution: String): String? = when {
        institution.trim().isEmpty() -> "Informe a instituição"
        institution.trim().length > 100 -> "Instituição deve ter no máximo 100 caracteres"
        else -> null
    }

    fun validateNickname(nickname: String): String? = when {
        nickname.trim().length > 100 -> "Apelido deve ter no máximo 100 caracteres"
        else -> null
    }

    fun validateLimit(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "Informe o limite"
        val normalized = trimmed.replace(",", ".").replace(" ", "")
        return try {
            val bd = BigDecimal(normalized)
            // Backend: @DecimalMin("0.01") + totalLimit must be positive
            if (bd.signum() <= 0) return "Limite deve ser maior que zero"
            if (bd.scale() > 2) return "Máximo 2 casas decimais"
            null
        } catch (_: Exception) {
            "Valor inválido"
        }
    }

    fun normalizeLimit(input: String): String {
        val trimmed = input.trim().replace(",", ".").replace(" ", "")
        return try {
            BigDecimal(trimmed).setScale(2).toPlainString()
        } catch (_: Exception) {
            trimmed
        }
    }

    fun validateClosingDay(input: String): String? = validateDay(input, "fechamento")

    fun validateDueDay(input: String): String? = validateDay(input, "vencimento")

    private fun validateDay(input: String, label: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "Informe o dia de $label"
        val day = trimmed.toIntOrNull() ?: return "Dia inválido"
        if (day < 1 || day > 31) return "Dia deve ser entre 1 e 31"
        return null
    }

    fun normalizeDay(input: String): Int? = input.trim().toIntOrNull()

    /**
     * Valida timezone: null/vazio é válido (backend aplica default) —
     * sufixo " (padrão)" não deve aparecer no valor passado aqui.
     */
    fun validateTimezone(tz: String?): String? {
        if (tz.isNullOrBlank()) return null
        return try {
            ZoneId.of(tz.trim())
            null
        } catch (_: Exception) {
            "Fuso horário inválido"
        }
    }
}
