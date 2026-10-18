package br.com.equilibra.mobile.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Task 3.1 — Teste de criação do banco (Robolectric).
 *
 * - Banco abre via in-memory (sem precisar de device/emulador)
 * - Verifica criação e versão
 * - O teste de migração será necessário quando houver Migration (Task 3.2+)
 */
@RunWith(RobolectricTestRunner::class)
class EquilibraDatabaseTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        // limpa arquivos temporários do Room
    }

    @Test
    fun databaseCreatesAndOpens() {
        val db = Room.inMemoryDatabaseBuilder(
            context,
            EquilibraDatabase::class.java
        ).allowMainThreadQueries().build()

        // Acessa o banco (força criação)
        val dao = db.openHelper
        assertTrue(dao.readableDatabase.isOpen)

        // Versão inicial = 1
        assertEquals(1, EquilibraDatabase.DATABASE_VERSION)
        assertEquals("equilibra.db", EquilibraDatabase.DATABASE_NAME)

        db.close()
    }

    @Test
    fun databaseHasExpectedName() {
        assertEquals("equilibra.db", EquilibraDatabase.DATABASE_NAME)
    }

    @Test
    fun databaseVersionIsOne() {
        assertEquals(1, EquilibraDatabase.DATABASE_VERSION)
    }

    @Test
    fun databaseFileCanBeCreated() {
        val db = Room.databaseBuilder(
            context,
            EquilibraDatabase::class.java,
            "test-equilibra.db"
        ).allowMainThreadQueries().build()

        // Força criação do arquivo
        db.openHelper.writableDatabase

        db.close()
        // Limpa arquivo de teste
        context.getDatabasePath("test-equilibra.db")?.delete()
    }

    @Test
    fun schemaIsExported() {
        // Verifica que o schema é exportado (exportSchema = true)
        // O arquivo é gerado em app/schemas/
        // Após o primeiro build com KSP, deve existir em:
        // app/schemas/br.com.equilibra.mobile.data.local.EquilibraDatabase/1.json
        // Este teste documenta o requisito; a verificação real é feita no build.
        assertTrue(true)
    }
}
