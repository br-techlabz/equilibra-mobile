package br.com.equilibra.mobile.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Tela de Home (legado — Sprint 1).
 *
 * A navegação principal agora é feita pela [MainTabsScreen] (Sprint 2).
 * Este composable é mantido para referência histórica, mas não é mais
 * usado no fluxo de navegação ativo.
 *
 * O logout agora é acionado pela aba "Mais" ([br.com.equilibra.mobile.ui.more.MoreScreen]).
 */
@Deprecated(
    message = "Use MainTabsScreen para navegação principal. Mantido apenas para referência.",
    replaceWith = ReplaceWith("MainTabsScreen")
)
@Composable
fun HomeScreen(
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bem-vindo ao Equilibra",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(32.dp))

        CircularProgressIndicator()

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Carregando...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
