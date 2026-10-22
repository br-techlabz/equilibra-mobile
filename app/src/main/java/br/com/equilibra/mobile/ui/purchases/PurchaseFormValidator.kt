package br.com.equilibra.mobile.ui.purchases

import br.com.equilibra.mobile.data.remote.dto.purchase.InstallmentRules
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/**
 * Task 6.8 — Validação do formulário de compra.
 *
 * Espelha o CreateCreditCardPurchaseRequest do backend:
 * - description @NotBlank @Size(max = 255)
 * - purchaseDate @NotNull
 * - totalAmount @NotNull @DecimalMin("0.01"), 2 casas (padrão money do backend)
 * - installmentCount @NotNull @Min(1) @Max(60) — reutiliza [InstallmentRules]
 * - creditCardId/categoryId @NotBlank
 *
 * O formulário NÃO calcula ciclo de fatura nem valores de parcela: isso é
 * regra do servidor (Task 6.6/6.9 só exibem o que a API devolveu).
 */
object PurchaseFormValidator {

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

    /** Entrada pt-BR → formato da API (ponto decimal), sem arredondar. */
    fun normalizeAmount(input: String): String {
        var text = input.trim().replace("R$", "").replace(" ", "")
        if (text.contains(",")) {
            text = text.replace(".", "").replace(",", ".")
        }
        return text
    }

    fun validateInstallments(input: String): String? {
        if (input.trim().isEmpty()) return "Informe o número de parcelas"
        val count = input.trim().toIntOrNull() ?: return "Número de parcelas inválido"
        return if (InstallmentRules.isValid(count)) null
        else "Parcelas devem ser entre ${InstallmentRules.MIN} e ${InstallmentRules.MAX}"
    }

    fun validateDate(ddMMyyyy: String): String? = try {
        parseDate(ddMMyyyy)
        null
    } catch (_: Exception) {
        if (ddMMyyyy.isBlank()) "Informe a data" else "Data inválida (use dd/MM/aaaa)"
    }

    fun validateCategory(categoryId: String?): String? =
        if (categoryId.isNullOrBlank()) "Selecione a categoria" else null

    fun validateCard(cardId: String?): String? =
        if (cardId.isNullOrBlank()) "Cartão não informado" else null

    fun parseDate(ddMMyyyy: String): LocalDate =
        LocalDate.parse(ddMMyyyy.trim(), dateFormat)

    /** dd/MM/aaaa → LocalDate ISO-8601 ("2026-10-15"), formato do contrato. */
    fun toLocalDateIso(ddMMyyyy: String): String? = try {
        parseDate(ddMMyyyy).toString()
    } catch (_: Exception) {
        null
    }

    /**
     * dd/MM/aaaa → Instant ISO-8601 no início do dia UTC — mesma convenção
     * do formulário de transações (fuso único do contrato).
     */
    fun toPurchaseAtIso(ddMMyyyy: String): String? = try {
        parseDate(ddMMyyyy).atStartOfDay(ZoneOffset.UTC).toInstant().toString()
    } catch (_: Exception) {
        null
    }
}
