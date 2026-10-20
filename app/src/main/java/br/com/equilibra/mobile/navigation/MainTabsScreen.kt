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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import br.com.equilibra.mobile.ui.accounts.AccountsScreen
import br.com.equilibra.mobile.ui.accounts.CreateAccountScreen
import br.com.equilibra.mobile.ui.accounts.CategoriesScreen
import br.com.equilibra.mobile.ui.accounts.CreateCategoryScreen
import br.com.equilibra.mobile.ui.accounts.CreateTagScreen
import br.com.equilibra.mobile.ui.accounts.EditCategoryScreen
import br.com.equilibra.mobile.ui.accounts.EditTagScreen
import br.com.equilibra.mobile.ui.accounts.TagsScreen
import br.com.equilibra.mobile.ui.accounts.EditAccountScreen
import br.com.equilibra.mobile.ui.accounts.ReconcileAccountScreen
import br.com.equilibra.mobile.ui.cards.CardsScreen
import br.com.equilibra.mobile.ui.dashboard.DashboardScreen
import br.com.equilibra.mobile.ui.home.HomeViewModel
import br.com.equilibra.mobile.ui.more.MoreScreen
import br.com.equilibra.mobile.ui.reports.ReportsScreen
import br.com.equilibra.mobile.ui.transactions.CreateExpenseScreen
import br.com.equilibra.mobile.ui.transactions.CreateIncomeScreen
import br.com.equilibra.mobile.ui.transactions.EditExpenseScreen
import br.com.equilibra.mobile.ui.transactions.EditIncomeScreen
import br.com.equilibra.mobile.ui.transactions.ExpensesScreen
import br.com.equilibra.mobile.ui.transactions.HistoryScreen
import br.com.equilibra.mobile.ui.transactions.IncomesScreen
import br.com.equilibra.mobile.ui.transactions.TransactionDetailScreen
import br.com.equilibra.mobile.ui.transactions.TransactionsScreen
import br.com.equilibra.mobile.ui.transactions.TransferCreateScreen
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Tela principal com BottomNavigation e 5 abas.
 *
 * - Dashboard, Contas, Cartões, Relatórios, Mais
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
                onAccountClick = { id -> navController.navigate("accounts/$id/edit") },
                onCreateClick = { navController.navigate("accounts/create") }
            )
        }

        // Task 4.11 — histórico unificado (lê /transactions, sem cache).
        composable("history") {
            HistoryScreen(
                // Task 4.12 — detalhe por ID: o backend não tem /transactions/{id},
                // a rota leva tipo + id e o detalhe busca no endpoint do tipo.
                onTransactionClick = { type, id ->
                    navController.navigate("transactions/$type/$id")
                }
            )
        }

        composable(
            route = "transactions/{transactionType}/{transactionId}",
            arguments = listOf(
                navArgument("transactionType") { type = NavType.StringType },
                navArgument("transactionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("transactionType").orEmpty()
            TransactionDetailScreen(
                onEdit = { id ->
                    // Só despesa/receita expõem "Editar" (sem tela de edição de transferência)
                    when (type) {
                        "EXPENSE" -> navController.navigate("expenses/$id/edit")
                        "INCOME" -> navController.navigate("incomes/$id/edit")
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("accounts/create") {
            CreateAccountScreen(
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "accounts/{accountId}/edit",
            arguments = listOf(navArgument("accountId") { type = NavType.StringType })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId").orEmpty()
            EditAccountScreen(
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
                onReconcile = { navController.navigate("accounts/$accountId/reconcile") }
            )
        }

        composable(
            route = "accounts/{accountId}/reconcile",
            arguments = listOf(navArgument("accountId") { type = NavType.StringType })
        ) {
            ReconcileAccountScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(MainTab.CARDS.route) {
            CardsScreen()
        }

        composable(MainTab.REPORTS.route) {
            ReportsScreen()
        }

        composable("categories") {
            CategoriesScreen(
                onCategoryClick = { id -> navController.navigate("categories/$id/edit") },
                onCreateClick = { navController.navigate("categories/create") }
            )
        }

        composable("categories/create") {
            CreateCategoryScreen(
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "categories/{categoryId}/edit",
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) {
            EditCategoryScreen(
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable("expenses") {
            ExpensesScreen(
                onExpenseClick = { id -> navController.navigate("expenses/$id/edit") },
                onCreateClick = { navController.navigate("expenses/create") }
            )
        }

        composable("incomes") {
            IncomesScreen(
                onIncomeClick = { id -> navController.navigate("incomes/$id/edit") },
                onCreateClick = { navController.navigate("incomes/create") }
            )
        }

        composable("transfers/create") {
            TransferCreateScreen(
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable("incomes/create") {
            CreateIncomeScreen(
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "incomes/{incomeId}/edit",
            arguments = listOf(navArgument("incomeId") { type = NavType.StringType })
        ) {
            EditIncomeScreen(
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable("expenses/create") {
            CreateExpenseScreen(
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "expenses/{expenseId}/edit",
            arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
        ) {
            EditExpenseScreen(
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable("tags") {
            TagsScreen(
                onTagClick = { id -> navController.navigate("tags/$id/edit") },
                onCreateClick = { navController.navigate("tags/create") }
            )
        }

        composable("tags/create") {
            CreateTagScreen(
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "tags/{tagId}/edit",
            arguments = listOf(navArgument("tagId") { type = NavType.StringType })
        ) {
            EditTagScreen(
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(MainTab.MORE.route) {
            MoreScreen(
                onCategories = { navController.navigate("categories") },
                onTags = { navController.navigate("tags") },
                onExpenses = { navController.navigate("expenses") },
                onIncomes = { navController.navigate("incomes") },
                onTransfers = { navController.navigate("transfers/create") },
                onHistory = { navController.navigate("history") },
                onLogout = {
                    homeViewModel.onLogout(onLoggedOut)
                }
            )
        }
    }
}
