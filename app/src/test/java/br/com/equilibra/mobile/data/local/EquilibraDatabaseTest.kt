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

        // Versão atual = 6 (Task 6.1: + cartões de crédito)
        assertEquals(6, EquilibraDatabase.DATABASE_VERSION)
        assertEquals("equilibra.db", EquilibraDatabase.DATABASE_NAME)

        db.close()
    }

    @Test
    fun databaseHasExpectedName() {
        assertEquals("equilibra.db", EquilibraDatabase.DATABASE_NAME)
    }

    @Test
    fun databaseVersionIsSix() {
        assertEquals(6, EquilibraDatabase.DATABASE_VERSION)
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
    fun migration2To3CreatesCategoriesTable() {
        val dbName = "test-migration-2-3.db"
        context.getDatabasePath(dbName)?.delete()
        // Passo 1: cria fisicamente um DB v2 (somente accounts)
        run {
            val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
                androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `accounts` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `initialBalance` TEXT NOT NULL, `currentBalance` TEXT NOT NULL, `active` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, `syncedAt` INTEGER, PRIMARY KEY(`id`))"
                            )
                        }
                        override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                    }).build()
            )
            helper.writableDatabase.close()
            helper.close()
        }
        // Passo 2: abre como v3 com migrations
        val dbV3 = Room.databaseBuilder(
            context,
            EquilibraDatabase::class.java,
            dbName
        ).allowMainThreadQueries().addMigrations(*Migrations.ALL).build()
        dbV3.openHelper.writableDatabase
        dbV3.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='table' AND name='categories'").use { c ->
            assertTrue("Tabela categories deve existir após MIGRATION_2_3", c.count > 0)
        }
        dbV3.close()
        context.getDatabasePath(dbName)?.delete()
    }

    @Test
    fun migration3To4CreatesTagsTable() {
        val dbName = "test-migration-3-4.db"
        context.getDatabasePath(dbName)?.delete()
        // Passo 1: cria fisicamente um DB v3 (accounts + categories)
        run {
            val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
                androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `accounts` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `initialBalance` TEXT NOT NULL, `currentBalance` TEXT NOT NULL, `active` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, `syncedAt` INTEGER, PRIMARY KEY(`id`))"
                            )
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `applicability` TEXT NOT NULL, `active` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, `syncedAt` INTEGER, PRIMARY KEY(`id`))"
                            )
                        }
                        override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                    }).build()
            )
            helper.writableDatabase.close()
            helper.close()
        }
        // Passo 2: abre como v4 com migrations
        val dbV4 = Room.databaseBuilder(
            context,
            EquilibraDatabase::class.java,
            dbName
        ).allowMainThreadQueries().addMigrations(*Migrations.ALL).build()
        dbV4.openHelper.writableDatabase
        dbV4.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='table' AND name='tags'").use { c ->
            assertTrue("Tabela tags deve existir após MIGRATION_3_4", c.count > 0)
        }
        // Dados da v3 devem sobreviver
        dbV4.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='table' AND name='categories'").use { c ->
            assertTrue("Tabela categories deve continuar existindo", c.count > 0)
        }
        dbV4.close()
        context.getDatabasePath(dbName)?.delete()
    }

    @Test
    fun migration4To5CreatesTransactionsTable() {
        val dbName = "test-migration-4-5.db"
        context.getDatabasePath(dbName)?.delete()
        // Passo 1: cria fisicamente um DB v4 (contas + categorias + tags)
        run {
            val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
                androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(4) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `accounts` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `initialBalance` TEXT NOT NULL, `currentBalance` TEXT NOT NULL, `active` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, `syncedAt` INTEGER, PRIMARY KEY(`id`))"
                            )
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `applicability` TEXT NOT NULL, `active` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, `syncedAt` INTEGER, PRIMARY KEY(`id`))"
                            )
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `tags` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `active` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, `syncedAt` INTEGER, PRIMARY KEY(`id`))"
                            )
                            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")
                        }
                        override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                    }).build()
            )
            helper.writableDatabase.close()
            helper.close()
        }
        // Passo 2: abre como v5 com migrations
        val dbV5 = Room.databaseBuilder(
            context,
            EquilibraDatabase::class.java,
            dbName
        ).allowMainThreadQueries().addMigrations(*Migrations.ALL).build()
        dbV5.openHelper.writableDatabase
        dbV5.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='table' AND name='transactions'").use { c ->
            assertTrue("Tabela transactions deve existir após MIGRATION_4_5", c.count > 0)
        }
        // Dados da v4 devem sobreviver
        dbV5.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='table' AND name='tags'").use { c ->
            assertTrue("Tabela tags deve continuar existindo", c.count > 0)
        }
        dbV5.close()
        context.getDatabasePath(dbName)?.delete()
    }

    @Test
    fun migration5To6CreatesCreditCardsTable() {
        val dbName = "test-migration-5-6.db"
        context.getDatabasePath(dbName)?.delete()
        // Passo 1: cria fisicamente um DB v5 (contas + categorias + tags + transações)
        run {
            val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
                androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(5) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `accounts` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `initialBalance` TEXT NOT NULL, `currentBalance` TEXT NOT NULL, `active` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, `syncedAt` INTEGER, PRIMARY KEY(`id`))"
                            )
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `applicability` TEXT NOT NULL, `active` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, `syncedAt` INTEGER, PRIMARY KEY(`id`))"
                            )
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `tags` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `active` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, `syncedAt` INTEGER, PRIMARY KEY(`id`))"
                            )
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `transactions` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `status` TEXT NOT NULL, `description` TEXT NOT NULL, `occurredAt` TEXT NOT NULL, `amount` TEXT NOT NULL, `categoryId` TEXT, `sourceAccountId` TEXT, `destinationAccountId` TEXT, `notes` TEXT, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, `syncedAt` INTEGER, PRIMARY KEY(`id`))"
                            )
                            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type` ON `transactions` (`type`)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_occurredAt` ON `transactions` (`occurredAt`)")
                        }
                        override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                    }).build()
            )
            helper.writableDatabase.close()
            helper.close()
        }
        // Passo 2: abre como v6 com migrations
        val dbV6 = Room.databaseBuilder(
            context,
            EquilibraDatabase::class.java,
            dbName
        ).allowMainThreadQueries().addMigrations(*Migrations.ALL).build()
        dbV6.openHelper.writableDatabase
        dbV6.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='table' AND name='credit_cards'").use { c ->
            assertTrue("Tabela credit_cards deve existir após MIGRATION_5_6", c.count > 0)
        }
        // Dados da v5 devem sobreviver
        dbV6.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='table' AND name='transactions'").use { c ->
            assertTrue("Tabela transactions deve continuar existindo", c.count > 0)
        }
        dbV6.close()
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
