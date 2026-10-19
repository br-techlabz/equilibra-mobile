package br.com.equilibra.mobile.ui.components

/**
 * Task 3.13 — Lógica de seleção compartilhada pelos seletores de categoria/tag.
 *
 * - Seleção única: selecionar substitui; tocar de novo desseleciona.
 * - Seleção múltipla: alterna pertencer/não pertencer, preservando ordem.
 * - IDs são estáveis (UUIDs do backend) — a seleção retorna sempre os IDs.
 */
object SelectionLogic {

    fun toggle(selected: List<String>, id: String, multiSelect: Boolean): List<String> {
        return if (!multiSelect) {
            if (id in selected) emptyList() else listOf(id)
        } else {
            if (id in selected) selected - id else selected + id
        }
    }
}
