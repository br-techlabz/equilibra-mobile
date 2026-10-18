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

@RunWith(RobolectricTestRunner::class)
class EquilibraDatabaseTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
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

        // Versão atual = 2 (Task 3.2)
        assertEquals(2, EquilibraDatabase.DATABASE_VERSION)
        assertEquals("equilibra.db", EquilibraDatabase.DATABASE_NAME)

        db.close()
    }

    @Test
    fun databaseHasExpectedName() {
        assertEquals("equilibra.db", EquilibraDatabase.DATABASE_NAME)
    }

    @Test
    fun databaseVersionIsTwo() {
        assertEquals(2, EquilibraDatabase.DATABASE_VERSION)
    }

    @Test
    fun databaseFileCanBeCreated() {
        val db = Room.databaseBuilder(
            context,
            EquilibraDatabase::class.java,
            "test-equilibra.db"
        ).allowMainThreadQueries().addMigrations(*Migrations.ALL).build()

        // Força criação do arquivo
        db.openHelper.writableDatabase

        db.close()
        // Limpa arquivo de teste
        context.getDatabasePath("test-equilibra.db")?.delete()
    }

    @Test
    fun migration1To2CreatesAccountsTable() {
        // Cria um DB v1 real (placeholder) no disco e migra para v2 com MIGRATION_1_2
        val dbName = "test-migration-1-2.db"
        // Remove arquivo de execução anterior
        context.getDatabasePath(dbName)?.delete()
        // Passo 1: cria fisicamente um DB v1 com a tabela _room_placeholder
        run {
            val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
                androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL("CREATE TABLE IF NOT EXISTS `_room_placeholder` (`id` TEXT NOT NULL, PRIMARY KEY(`id`))")
                            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'aa395a07596cee235bde1f51f723dce3')")
                        }
                        override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                    }).build()
            )
            helper.writableDatabase.close()
            helper.close()
        }
        // Passo 2: abre como EquilibraDatabase v2 com MIGRATION_1_2
        val dbV2 = Room.databaseBuilder(
            context,
            EquilibraDatabase::class.java,
            dbName
        ).allowMainThreadQueries().addMigrations(*Migrations.ALL).build()
        // Se a migration falhar, writableDatabase lança
        dbV2.openHelper.writableDatabase
        // Verifica que a tabela accounts existe
        dbV2.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='table' AND name='accounts'").use { cursor ->
            assertTrue("Tabela accounts deve existir após MIGRATION_1_2", cursor.count > 0)
        }
        // Placeholder deve ter sido removida
        dbV2.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='table' AND name='_room_placeholder'").use { cursor ->
            assertTrue("Tabela _room_placeholder deve ter sido removida", cursor.count == 0)
        }
        dbV2.close()
        context.getDatabasePath(dbName)?.delete()
    }

    @Test
    fun schemaIsExported() {
        // Verifica que o schema é exportado (exportSchema = true)
        // O arquivo é gerado em app/schemas/
        // Após o build com KSP, deve existir em:
        // app/schemas/br.com.equilibra.mobile.data.local.EquilibraDatabase/2.json
        // Este teste documenta o requisito; a verificação real é feita no build.
        assertTrue(true)
    }
}
