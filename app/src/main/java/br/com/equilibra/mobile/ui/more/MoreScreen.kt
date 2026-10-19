package br.com.equilibra.mobile.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Item de configuração exibido na tela "Mais".
 *
 * @param icon Ícone do item
 * @param label Título do item
 * @param subtitle Descrição do item (opcional)
 * @param enabled Se false, o item aparece desabilitado e não responde a cliques
 * @param onClick Ação ao clicar no item
 */
data class MoreMenuItem(
    val icon: ImageVector,
    val label: String,
    val subtitle: String? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

/**
 * Tela "Mais" — ponto de entrada para áreas ainda não entregues.
 *
 * Regras de navegação:
 * - Itens habilitados executam ação real (ex.: logout).
 * - Itens desabilitados aparecem com alpha reduzido e NÃO respondem a clique.
 * - Nenhum item gera link quebrado: ou leva a uma tela existente, ou não faz nada.
 *
 * Áreas planejadas para sprints futuras (exibidas como "Em breve"):
 * - Notificações
 * - Segurança
 * - Configurações
 * - Ajuda / Suporte
 * - Privacidade
 */
@Composable
fun MoreScreen(
    onLogout: () -> Unit,
    onCategories: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val comingSoonMessage = "Em breve"

    val items = listOf(
        MoreMenuItem(
            icon = Icons.Default.AccountCircle,
            label = "Perfil",
            subtitle = comingSoonMessage,
            enabled = false,
            onClick = {}
        ),
        MoreMenuItem(
            icon = Icons.Default.Category,
            label = "Categorias",
            subtitle = "Criar, editar e ativar categorias",
            onClick = onCategories
        ),
        MoreMenuItem(
            icon = Icons.Default.Notifications,
            label = "Notificações",
            subtitle = comingSoonMessage,
            enabled = false,
            onClick = {}
        ),
        MoreMenuItem(
            icon = Icons.Default.Security,
            label = "Segurança",
            subtitle = comingSoonMessage,
            enabled = false,
            onClick = {}
        ),
        MoreMenuItem(
            icon = Icons.Default.Settings,
            label = "Configurações",
            subtitle = comingSoonMessage,
            enabled = false,
            onClick = {}
        ),
        MoreMenuItem(
            icon = Icons.AutoMirrored.Filled.Help,
            label = "Ajuda",
            subtitle = comingSoonMessage,
            enabled = false,
            onClick = {}
        ),
        MoreMenuItem(
            icon = Icons.AutoMirrored.Filled.Send,
            label = "Privacidade",
            subtitle = comingSoonMessage,
            enabled = false,
            onClick = {}
        ),
        MoreMenuItem(
            icon = Icons.AutoMirrored.Filled.Logout,
            label = "Sair da conta",
            subtitle = null,
            enabled = true,
            onClick = onLogout
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .semantics { contentDescription = "Tela de mais opções" }
    ) {
        Text(
            text = "Mais",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        HorizontalDivider()

        items.forEachIndexed { index, item ->
            MoreItemRow(item = item)
            if (index < items.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun MoreItemRow(item: MoreMenuItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (item.enabled) {
                    Modifier.clickable(role = Role.Button, onClick = item.onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .semantics {
                contentDescription = if (item.enabled) {
                    item.label
                } else {
                    "${item.label}, em breve"
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (item.enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
            if (item.subtitle != null) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    }
                )
            }
        }
    }
}
