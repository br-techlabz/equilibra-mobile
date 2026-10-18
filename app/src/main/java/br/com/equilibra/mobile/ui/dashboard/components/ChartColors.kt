package br.com.equilibra.mobile.ui.dashboard.components

import android.graphics.Color as AndroidColor

/**
 * Paleta diversificada para gráficos (Task 2.6/2.7).
 * Hues bem distintos para distinguir séries/fatias com facilidade.
 * Saturação e luminosidade equilibradas para contraste em fundo claro.
 */
object ChartColors {

    /**
     * Paleta para linhas do gráfico de evolução (BalanceEvolution).
     * 10 cores com hues espaçados (~36°), saturação alta.
     */
    val linePalette: List<Int> = listOf(
        AndroidColor.parseColor("#1E88E5"), // azul
        AndroidColor.parseColor("#D81B60"), // rosa
        AndroidColor.parseColor("#F4511E"), // laranja avermelhado
        AndroidColor.parseColor("#43A047"), // verde
        AndroidColor.parseColor("#5E35B1"), // roxo
        AndroidColor.parseColor("#00ACC1"), // ciano
        AndroidColor.parseColor("#FB8C00"), // laranja
        AndroidColor.parseColor("#3949AB"), // índigo
        AndroidColor.parseColor("#00897B"), // teal
        AndroidColor.parseColor("#E53935"), // vermelho
    )

    /**
     * Paleta para fatias de pizza (despesa e receita).
     * Mesma paleta diversificada — não restringe por tipo, maximiza distinção.
     */
    val piePalette: List<Int> = listOf(
        AndroidColor.parseColor("#1E88E5"), // azul
        AndroidColor.parseColor("#D81B60"), // rosa
        AndroidColor.parseColor("#F4511E"), // laranja avermelhado
        AndroidColor.parseColor("#43A047"), // verde
        AndroidColor.parseColor("#5E35B1"), // roxo
        AndroidColor.parseColor("#00ACC1"), // ciano
        AndroidColor.parseColor("#FB8C00"), // laranja
        AndroidColor.parseColor("#3949AB"), // índigo
        AndroidColor.parseColor("#00897B"), // teal
        AndroidColor.parseColor("#FBC02D"), // amarelo ouro
        AndroidColor.parseColor("#E53935"), // vermelho
        AndroidColor.parseColor("#6D4C41"), // marrom
    )

    fun forSeries(index: Int): Int = linePalette[index % linePalette.size]
    fun forSlice(index: Int): Int = piePalette[index % piePalette.size]
}
