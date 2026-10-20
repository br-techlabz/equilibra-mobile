package br.com.equilibra.mobile.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.navOptions

/**
 * Abas principais da navegação pós-login.
 *
 * Ordem definida:
 * 1. Dashboard (início)
 * 2. Contas (lista de contas)
 * 3. Cartões
 * 4. Relatórios
 * 5. Mais (entrada para áreas ainda não entregues)
 */
enum class MainTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Início", Icons.Default.Home),
    TRANSACTIONS("transactions", "Contas", Icons.Default.AccountBalanceWallet),
    CARDS("cards", "Cartões", Icons.Default.CreditCard),
    REPORTS("reports", "Relatórios", Icons.Default.InsertChart),
    MORE("more", "Mais", Icons.Default.MoreHoriz)
}

/**
 * Opções de navegação das tabs da [MainBottomNavigationBar].
 *
 * [MainTab.MORE] nunca restaura estado: o popUpTo(saveState) salva os
 * entries popados como UM SEGMENTO (ex.: [more, incomes]) e o
 * restoreState reinstalaria a tela profunda no topo — clicar em "Mais"
 * abria a listagem de Receitas em vez do menu (bug reproduzido em
 * MainTabNavigationTest). As demais tabs mantêm o retorno ao ponto onde
 * o usuário estava (padrão oficial de bottom nav).
 */
internal fun NavHostController.tabNavOptions(tab: MainTab): NavOptions = navOptions {
    // Pop até o start destination para manter backstack limpo
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = tab != MainTab.MORE
}
