package br.com.equilibra.mobile.ui.transactions

import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/**
 * Task 4.4 — Validação do formulário reutilizável de transação.
 *
 * - Descrição: obrigatoria, máx. 255 (constraint do backend).
 * - Valor: positivo, parseável, máx. 2 casas; rejeita em vez de arredondar
 *   (sem arredondamentos indevidos); aceita vírgula pt-BR.
 * - Data: dd/MM/aaaa estrita; convertida para Instant ISO-8601 (UTC, início
 *   do dia) para o contrato da API.
 * - Notas: máx. 4000 (constraint do backend).
 */
object TransactionFormValidator {

    private val dateFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

    fun validateDescription(input: String): String? = when {
        input.trim().isEmpty() -> "Informe a descrição"
        input.trim().length > 255 -> "Descrição deve ter no máximo 255 caracteres"
        else -> null
    }

    fun validateAmount(input: String): String? {
        if (input.trim().isEmpty()) return "Informe o valor"
        val normalized = normalizeAmount(input)
        return try {
            val value = BigDecimal(normalized)
            if (value.scale() > 2) return "Máximo 2 casas decimais"
            if (value.signum() <= 0) return "Valor deve ser maior que zero"
            null
        } catch (_: Exception) {
            "Valor inválido"
        }
    }

    /**
     * Normaliza entrada pt-BR para o formato da API (ponto decimal),
     * preservando a escala original — nunca arredonda.
     *
     * "1.234,56" → "1234.56"; "10,5" → "10.5"; "10.999" permanece (e é
     * rejeitado pela validação de escala).
     */
    fun normalizeAmount(input: String): String {
        var text = input.trim().replace("R$", "").replace(" ", "")
        if (text.contains(",")) {
            // Vírgula presente → convenção pt-BR: ponto é separador de milhar
            text = text.replace(".", "").replace(",", ".")
        }
        return text
    }

    fun validateNotes(notes: String): String? =
        if (notes.length > 4000) "Notas devem ter no máximo 4000 caracteres" else null

    fun validateDate(ddMMyyyy: String): String? = try {
        parseDate(ddMMyyyy)
        null
    } catch (_: Exception) {
        "Data inválida (use dd/MM/aaaa)"
    }

    fun parseDate(ddMMyyyy: String): LocalDate =
        LocalDate.parse(ddMMyyyy.trim(), dateFormat)

    /** Data no início do dia em UTC — fuso único do contrato Instant do backend. */
    fun toInstantIso(ddMMyyyy: String): String? = try {
        parseDate(ddMMyyyy).atStartOfDay(ZoneOffset.UTC).toInstant().toString()
    } catch (_: Exception) {
        null
    }

    /**
     * Fim do dia em UTC — o BETWEEN do backend é inclusivo, então o
     * filtro "até" precisa cobrir o dia inteiro (Task 4.11).
     */
    fun toInstantEndOfDayIso(ddMMyyyy: String): String? = try {
        parseDate(ddMMyyyy)
            .atTime(23, 59, 59, 999_999_999)
            .atZone(ZoneOffset.UTC)
            .toInstant()
            .toString()
    } catch (_: Exception) {
        null
    }

    /** Instant ISO-8601 → dd/MM/yyyy (exibição no formulário de edição). */
    fun toDisplayDate(isoInstant: String): String = try {
        java.time.Instant.parse(isoInstant)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    } catch (_: Exception) {
        ""
    }
}
