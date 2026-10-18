package br.com.equilibra.mobile.domain.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/**
 * Utilitário para formatação de valores monetários em pt-BR.
 *
 * Regras:
 * - Entrada sempre como String (evita conversão incorreta via Float/Double)
 * - Cálculos com BigDecimal (quando necessário, ex.: soma, diferença)
 * - Formatação com NumberFormat pt-BR (ex: R$ 1.234,56)
 */
object MoneyFormatter {

    private val ptBr: Locale = Locale("pt", "BR")

    /**
     * Formata um valor monetário (String) para exibição pt-BR.
     *
     * Converte a string para BigDecimal e formata com Locale pt-BR.
     * Em caso de erro de parsing, retorna o valor original.
     */
    fun format(value: String): String {
        return try {
            val decimal = BigDecimal(value)
            val format = NumberFormat.getCurrencyInstance(ptBr)
            format.format(decimal)
        } catch (_: Exception) {
            value
        }
    }

    /**
     * Converte String para BigDecimal de forma segura.
     * Retorna BigDecimal.ZERO em caso de falha.
     */
    fun parse(value: String): BigDecimal {
        return try {
            BigDecimal(value)
        } catch (_: Exception) {
            BigDecimal.ZERO
        }
    }

    /**
     * Retorna o valor como String sem formatação (para comparações/cálculos).
     */
    fun raw(value: String): BigDecimal = parse(value)
}
