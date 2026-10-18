package br.com.equilibra.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.equilibra.mobile.ui.home.HomeScreen
import br.com.equilibra.mobile.ui.login.LoginScreen
import br.com.equilibra.mobile.ui.register.RegisterScreen
import br.com.equilibra.mobile.ui.splash.SplashScreen

/**
 * Rotas principais do app.
 *
 * Estrutura de dois níveis:
 * - Auth: Splash, Login, Register (pré-autenticação)
 * - Main: MainTabsScreen (pós-autenticação, contém BottomNavigation interna)
 */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
}

/**
 * NavHost principal do app.
 *
 * Controla a navegação de alto nível entre fluxo de autenticação
 * e fluxo principal (Dashboard/Transações/Cartões/Relatórios/Mais).
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    // Cadastro não faz login automático; volta ao login
                    navController.popBackStack(Routes.LOGIN, inclusive = false)
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.MAIN) {
            MainTabsScreen(
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
