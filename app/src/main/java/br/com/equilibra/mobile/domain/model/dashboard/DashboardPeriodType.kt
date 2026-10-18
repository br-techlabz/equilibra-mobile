package br.com.equilibra.mobile.domain.model.dashboard

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Tipos de período do filtro (Task 2.9).
 *
 * Seriais para exibição no dropdown. Datas sempre em UTC (ISO 8601)
 * para o backend (ZoneOffset.UTC).
 */
enum class DashboardPeriodType(val label: String) {
    CURRENT_MONTH("Mês atual"),
    PREVIOUS_MONTH("Mês anterior"),
    CUSTOM("Período customizado")
}

/**
 * Período com datas em LocalDate (fuso do aparelho) para exibição,
 * mas serialização sempre para UTC (Instant ISO 8601).
 */
data class DashboardPeriodSelection(
    val type: DashboardPeriodType,
    val from: LocalDate,
    val to: LocalDate
) {
    /** Serializa para ISO 8601 UTC (ex: 2026-10-01T00:00:00Z). */
    fun toIso(): Pair<String, String> {
        val fromIso = from.atStartOfDay(ZoneOffset.UTC).toInstant().toString()
        val toIso = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toString()
        return fromIso to toIso
    }

    /** Valida que from é antes de to. */
    fun isValid(): Boolean = from.isBefore(to)

    companion object {
        fun currentMonth(): DashboardPeriodSelection {
            val now = LocalDate.now(ZoneId.systemDefault())
            return DashboardPeriodSelection(
                type = DashboardPeriodType.CURRENT_MONTH,
                from = now.withDayOfMonth(1),
                to = now.withDayOfMonth(now.lengthOfMonth())
            )
        }

        fun previousMonth(): DashboardPeriodSelection {
            val now = LocalDate.now(ZoneId.systemDefault())
            val prev = now.minusMonths(1)
            return DashboardPeriodSelection(
                type = DashboardPeriodType.PREVIOUS_MONTH,
                from = prev.withDayOfMonth(1),
                to = prev.withDayOfMonth(prev.lengthOfMonth())
            )
        }

        fun custom(from: LocalDate, to: LocalDate): DashboardPeriodSelection {
            return DashboardPeriodSelection(
                type = DashboardPeriodType.CUSTOM,
                from = from,
                to = to
            )
        }
    }
}
