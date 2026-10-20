package br.com.equilibra.mobile.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Regressão do bug real: "clicar em Mais abria a listagem de Receitas".
 *
 * O padrão popUpTo(saveState)+restoreState salva os entries popados como
 * UM SEGMENTO (NavController: backStackStates[firstState.id] = [more, incomes])
 * e restaurá-lo reinstancia o segmento inteiro — caindo na tela profunda,
 * não no menu. O hub "Mais" nunca deve restaurar estado.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainTabNavigationTest {

    @get:Rule
    val compose = createComposeRule()

    private fun withNavGraph(block: (NavHostController) -> Unit) {
        var controller: NavHostController? = null
        compose.setContent {
            val navController = rememberNavController()
            controller = navController
            NavHost(navController = navController, startDestination = "dashboard") {
                composable("dashboard") { Box(modifier = Modifier) }
                composable("more") { Box(modifier = Modifier) }
                composable("incomes") { Box(modifier = Modifier) }
            }
        }
        compose.waitForIdle()
        block(controller!!)
    }

    @Test
    fun maisSempreAbreMenuMesmoAposTerVisitadoTelaProfunda() = withNavGraph { nav ->
        // Usuário abre o hub e desce para uma tela do menu (Receitas)
        nav.navigate("more", nav.tabNavOptions(MainTab.MORE))
        nav.navigate("incomes")
        // Troca de tab (Início) — popUpTo(saveState) salva [more, incomes] juntos
        nav.navigate("dashboard", nav.tabNavOptions(MainTab.DASHBOARD))
        compose.waitForIdle()
        assertEquals("dashboard", nav.currentDestination?.route)

        // Volta para "Mais": precisa abrir o MENU, não restaurar Receitas
        nav.navigate("more", nav.tabNavOptions(MainTab.MORE))
        compose.waitForIdle()
        assertEquals("more", nav.currentDestination?.route)
    }
}
