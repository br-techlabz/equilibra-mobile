package br.com.equilibra.mobile.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.equilibra.mobile.ui.accounts.AccountsScreen
import br.com.equilibra.mobile.ui.accounts.CreateAccountScreen
import br.com.equilibra.mobile.ui.cards.CardsScreen
import br.com.equilibra.mobile.ui.dashboard.DashboardScreen
import br.com.equilibra.mobile.ui.home.HomeViewModel
import br.com.equilibra.mobile.ui.more.MoreScreen
import br.com.equilibra.mobile.ui.reports.ReportsScreen
import br.com.equilibra.mobile.ui.transactions.TransactionsScreen
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Tela principal com BottomNavigation e 5 abas.
 *
 * - Dashboard, Transações, Cartões, Relatórios, Mais
 * - Estado das abas é preservado ao trocar (saveState/restoreState + launchSingleTop)
 * - Cada aba é uma composable separada para manter o estado isolado
 */
@Composable
fun MainTabsScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            MainBottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        MainTabsNavHost(
            navController = navController,
            homeViewModel = homeViewModel,
            onLoggedOut = onLoggedOut,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

@Composable
private fun MainBottomNavigationBar(
    navController: NavHostController
) {
    val items = MainTab.entries

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        items.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label) },
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        // Pop até o start destination para manter backstack limpo
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
private fun MainTabsNavHost(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = MainTab.DASHBOARD.route,
        modifier = modifier
    ) {
        composable(MainTab.DASHBOARD.route) {
            DashboardScreen()
        }

        composable(MainTab.TRANSACTIONS.route) {
            AccountsScreen(
                onAccountClick = { /* Task 3.7: editar */ },
                onCreateClick = { navController.navigate("accounts/create") }
            )
        }

        composable("accounts/create") {
            CreateAccountScreen(
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(MainTab.CARDS.route) {
            CardsScreen()
        }

        composable(MainTab.REPORTS.route) {
            ReportsScreen()
        }

        composable(MainTab.MORE.route) {
            MoreScreen(
                onLogout = {
                    homeViewModel.onLogout(onLoggedOut)
                }
            )
        }
    }
}
