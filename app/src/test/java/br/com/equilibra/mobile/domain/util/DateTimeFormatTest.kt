package br.com.equilibra.mobile.domain.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.TimeZone

/**
 * Task 4.13 — Formatação de datas centralizada.
 *
 * Garante o contrato UTC (Instant do backend → mesma data em qualquer
 * fuso do dispositivo) e o fallback seguro para null/entrada inválida.
 */
class DateTimeFormatTest {

    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `instant em UTC ignora fuso do dispositivo`() {
        // 23:59Z: em UTC-3 o dia local já seria 02/10, em UTC+14 ainda 01/10.
        // O app deve mostrar 01/10 nos dois (contrato UTC do backend).
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))
        assertEquals("01/10/2026", DateTimeFormat.formatDate("2026-10-01T23:59:59Z"))

        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
        assertEquals("01/10/2026", DateTimeFormat.formatDate("2026-10-01T23:59:59Z"))
    }

    @Test
    fun `formatDate aplica fallback para null e invalido`() {
        assertEquals("", DateTimeFormat.formatDate(null))
        assertEquals("-", DateTimeFormat.formatDate(null, fallback = "-"))
        assertEquals("", DateTimeFormat.formatDate("não-é-data"))
        assertEquals("entrada", DateTimeFormat.formatDate("não-é-data", fallback = "entrada"))
    }

    @Test
    fun `formatDateTime exibe data e hora em UTC`() {
        assertEquals("01/10/2026 12:00", DateTimeFormat.formatDateTime("2026-10-01T12:00:00Z"))
        assertEquals("01/10/2026 23:59", DateTimeFormat.formatDateTime("2026-10-01T23:59:59Z"))
    }

    @Test
    fun `formatDateTime retorna null para null e invalido`() {
        assertNull(DateTimeFormat.formatDateTime(null))
        assertNull(DateTimeFormat.formatDateTime("lixo"))
    }

    @Test
    fun `formatShortDate formata eixo curto de grafico`() {
        assertEquals("01/10", DateTimeFormat.formatShortDate("2026-10-01T12:00:00Z"))
        assertEquals("", DateTimeFormat.formatShortDate(null))
        // fallback preserva o comportamento anterior (prefixo ISO da entrada)
        assertEquals("2026-10-0", DateTimeFormat.formatShortDate("2026-10-0", fallback = "2026-10-0".take(10)))
    }

    @Test
    fun `overloads de LocalDate formatam dd MM yyyy e dd MM`() {
        val date = LocalDate.of(2026, 12, 31)
        assertEquals("31/12/2026", DateTimeFormat.formatDate(date))
        assertEquals("31/12", DateTimeFormat.formatShortDate(date))
    }
}
