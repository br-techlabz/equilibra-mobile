package br.com.equilibra.mobile.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Task 5.5 — Guarda de arquitetura: anexos NUNCA ficam no Room.
 *
 * Este teste é a versão verificável da política (além do documento em
 * `docs/ANEXOS-FORA-DO-ROOM.md`). Ele falha se alguém:
 * - adicionar tabela/coluna de anexo no schema exportado do Room;
 * - introduzir coluna com afinidade BLOB (binário no banco);
 * - criar entidade Room de anexo no código-fonte.
 */
class RoomAttachmentsPolicyTest {

    // O working dir dos testes unitários é o módulo :app
    private val moduleDir = File(System.getProperty("user.dir")).let {
        if (File(it, "schemas").exists()) it else File(it, "app")
    }
    private val schemaDir = File(moduleDir, "schemas/br.com.equilibra.mobile.data.local.EquilibraDatabase")
    private val sourceDir = File(moduleDir, "src/main/java/br/com/equilibra/mobile/data/local")

    @Test
    fun `schema mais recente do Room nao tem tabela de anexos`() {
        val latest = latestSchema()
        // A auditoria só vale se o schema exportado foi encontrado de fato
        assertTrue("schema do Room deve existir em $schemaDir", latest != null)
        latest ?: return
        val tables = TABLE_REGEX.findAll(latest).map { it.groupValues[1] }.toSet()

        assertTrue("nenhuma tabela deve conter 'attachment'", tables.none { "attachment" in it.lowercase() })
    }

    @Test
    fun `nenhuma coluna do Room usa afinidade BLOB`() {
        val latest = latestSchema()
        assertTrue("schema do Room deve existir em $schemaDir", latest != null)
        latest ?: return

        assertTrue(
            "Room não deve armazenar binários (BLOB): anexos ficam no backend",
            !BLOB_REGEX.containsMatchIn(latest)
        )
    }

    @Test
    fun `data local nao referencia attachment`() {
        val offenders = sourceDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                val text = file.readText()
                // Permite menções em KDoc/comentário sobre a regra; proíbe código.
                text.lineSequence().any { line ->
                    val code = line.trim()
                    "attachment" in code.lowercase() &&
                        !code.startsWith("*") &&
                        !code.startsWith("//") &&
                        !code.startsWith("/*")
                }
            }
            .map { it.name }
            .toList()

        assertEquals("nenhum arquivo de data/local deve referenciar anexos", emptyList<String>(), offenders)
    }

    /** Versão mais alta exportada (Room nomeia N.json). */
    private fun latestSchema(): String? {
        val files = schemaDir.listFiles { f -> f.name.endsWith(".json") } ?: return null
        val latest = files.maxByOrNull { it.nameWithoutExtension.toIntOrNull() ?: 0 } ?: return null
        return latest.readText()
    }

    private companion object {
        val TABLE_REGEX = Regex("\"tableName\"\\s*:\\s*\"([^\"]+)\"")
        val BLOB_REGEX = Regex("\"affinity\"\\s*:\\s*\"BLOB\"", RegexOption.IGNORE_CASE)
    }
}
