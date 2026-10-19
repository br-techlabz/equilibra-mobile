package br.com.equilibra.mobile.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.ui.graphics.vector.ImageVector

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
