package br.com.equilibra.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.equilibra.mobile.data.local.account.AccountDao
import br.com.equilibra.mobile.data.local.account.AccountEntity
import br.com.equilibra.mobile.data.local.category.CategoryDao
import br.com.equilibra.mobile.data.local.category.CategoryEntity

/**
 * Task 3.9 — EquilibraDatabase v3
 *
 * - Banco: equilibra.db, version = 3 (contas + categorias)
 * - Migrations explícitas (Migrations.MIGRATION_1_2, MIGRATION_2_3);
 *   sem fallback destrutivo em produção.
 */
@Database(
    entities = [AccountEntity::class, CategoryEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = []
)
abstract class EquilibraDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao

    abstract fun categoryDao(): CategoryDao

    companion object {
        const val DATABASE_NAME = "equilibra.db"
        const val DATABASE_VERSION = 3
    }
}
