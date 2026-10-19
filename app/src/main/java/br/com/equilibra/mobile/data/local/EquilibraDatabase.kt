package br.com.equilibra.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.equilibra.mobile.data.local.account.AccountDao
import br.com.equilibra.mobile.data.local.account.AccountEntity
import br.com.equilibra.mobile.data.local.category.CategoryDao
import br.com.equilibra.mobile.data.local.category.CategoryEntity
import br.com.equilibra.mobile.data.local.tag.TagDao
import br.com.equilibra.mobile.data.local.tag.TagEntity

/**
 * Task 3.11 — EquilibraDatabase v4
 *
 * - Banco: equilibra.db, version = 4 (contas + categorias + tags)
 * - Migrations explícitas (Migrations.ALL); sem fallback destrutivo em produção.
 */
@Database(
    entities = [AccountEntity::class, CategoryEntity::class, TagEntity::class],
    version = 4,
    exportSchema = true,
    autoMigrations = []
)
abstract class EquilibraDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao

    abstract fun categoryDao(): CategoryDao

    abstract fun tagDao(): TagDao

    companion object {
        const val DATABASE_NAME = "equilibra.db"
        const val DATABASE_VERSION = 4
    }
}
