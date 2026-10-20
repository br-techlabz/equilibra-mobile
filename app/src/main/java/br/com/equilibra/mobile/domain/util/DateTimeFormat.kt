package br.com.equilibra.mobile.domain.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Task 4.13 — Formatação de datas pt-BR centralizada.
 *
 * - Padrões do app: dd/MM/yyyy, dd/MM/yyyy HH:mm e dd/MM (eixo de gráfico).
 * - Instantes ISO-8601 do backend são interpretados em UTC (contrato
 *   Instant), independente do fuso do dispositivo — mesma data no app
 *   inteiro, coerente com os filtros que também gravam em UTC.
 * - Null e entrada inválida caem no [fallback] em vez de lançar exceção
 *   na UI.
 */
object DateTimeFormat {

    private val datePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dateTimePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    private val shortDatePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")

    /** Instant ISO-8601 (UTC) → dd/MM/yyyy. */
    fun formatDate(isoInstant: String?, fallback: String = ""): String = try {
        if (isoInstant == null) fallback
        else Instant.parse(isoInstant)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .format(datePattern)
    } catch (_: Exception) {
        fallback
    }

    /** Instant ISO-8601 (UTC) → dd/MM/yyyy HH:mm; null/inválido → null. */
    fun formatDateTime(isoInstant: String?): String? = try {
        if (isoInstant == null) null
        else Instant.parse(isoInstant)
            .atZone(ZoneOffset.UTC)
            .format(dateTimePattern)
    } catch (_: Exception) {
        null
    }

    /** Instant ISO-8601 (UTC) → dd/MM (eixo curto de gráfico). */
    fun formatShortDate(isoInstant: String?, fallback: String? = isoInstant): String = try {
        if (isoInstant == null) ""
        else Instant.parse(isoInstant)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .format(shortDatePattern)
    } catch (_: Exception) {
        fallback ?: ""
    }

    /** LocalDate (já sem horário, ex.: seletor do dashboard) → dd/MM/yyyy. */
    fun formatDate(date: LocalDate): String = date.format(datePattern)

    /** LocalDate → dd/MM. */
    fun formatShortDate(date: LocalDate): String = date.format(shortDatePattern)
}
