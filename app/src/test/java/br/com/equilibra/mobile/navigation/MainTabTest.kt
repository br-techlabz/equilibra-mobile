package br.com.equilibra.mobile.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class MainTabTest {

    @Test
    fun temCincoAbasDefinidas() {
        assertEquals(5, MainTab.entries.size)
    }

    @Test
    fun primeiraAbaEDashboard() {
        assertEquals(MainTab.DASHBOARD, MainTab.entries.first())
        assertEquals("dashboard", MainTab.DASHBOARD.route)
        assertEquals("Início", MainTab.DASHBOARD.label)
    }

    @Test
    fun ultimaAbaEMais() {
        assertEquals(MainTab.MORE, MainTab.entries.last())
        assertEquals("more", MainTab.MORE.route)
        assertEquals("Mais", MainTab.MORE.label)
    }

    @Test
    fun rotasSaoUnicas() {
        val routes = MainTab.entries.map { it.route }
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun labelsSaoUnicas() {
        val labels = MainTab.entries.map { it.label }
        assertEquals(labels.size, labels.toSet().size)
    }

    @Test
    fun abasNaOrdemEsperada() {
        val expected = listOf("dashboard", "transactions", "cards", "reports", "more")
        assertEquals(expected, MainTab.entries.map { it.route })
    }

    @Test
    fun routesDeNavegacaoPrincipalExistem() {
        assertEquals("splash", Routes.SPLASH)
        assertEquals("login", Routes.LOGIN)
        assertEquals("register", Routes.REGISTER)
        assertEquals("main", Routes.MAIN)
    }
}
