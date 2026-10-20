package br.com.equilibra.mobile.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Task 4.13 — Cores semânticas financeiras centralizadas por tema.
 *
 * Verde = receita, vermelho = despesa, com variação claro/escuro para
 * manter contraste legível nos dois temas (Material 300/800 no claro,
 * 200/300 no escuro). Componentes nunca usam a cor sozinha: sempre
 * acompanham sinal (+/-), ícone direcional e rótulo do tipo.
 */
data class FinancialColors(
    val income: Color,
    val expense: Color
)

/** Paleta padrão (fallback fora de [EquilibraTheme], ex.: testes). */
val LocalFinancialColors = staticCompositionLocalOf {
    LightFinancialColors
}

internal val LightFinancialColors = FinancialColors(
    income = Color(0xFF2E7D32), // green 800
    expense = Color(0xFFC62828)  // red 800
)

internal val DarkFinancialColors = FinancialColors(
    income = Color(0xFF81C784), // green 300 — legível em fundo escuro
    expense = Color(0xFFEF9A9A)  // red 300
)
