package br.com.equilibra.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.equilibra.mobile.data.local.account.AccountDao
import br.com.equilibra.mobile.data.local.account.AccountEntity
import br.com.equilibra.mobile.data.local.category.CategoryDao
import br.com.equilibra.mobile.data.local.category.CategoryEntity
import br.com.equilibra.mobile.data.local.creditcard.CreditCardDao
import br.com.equilibra.mobile.data.local.creditcard.CreditCardEntity
import br.com.equilibra.mobile.data.local.tag.TagDao
import br.com.equilibra.mobile.data.local.tag.TagEntity
import br.com.equilibra.mobile.data.local.transaction.TransactionDao
import br.com.equilibra.mobile.data.local.transaction.TransactionEntity

/**
 * Task 6.1 — EquilibraDatabase v6
 *
 * - Banco: equilibra.db, version = 6 (contas + categorias + tags + transações + cartões)
 * - Migrations explícitas (Migrations.ALL); sem fallback destrutivo em produção.
 */
@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        TransactionEntity::class,
        CreditCardEntity::class
    ],
    version = 6,
    exportSchema = true,
    autoMigrations = []
)
abstract class EquilibraDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao

    abstract fun categoryDao(): CategoryDao

    abstract fun tagDao(): TagDao

    abstract fun transactionDao(): TransactionDao

    /** Task 6.1 — cache local de cartões (API-first, Task 6.2). */
    abstract fun creditCardDao(): CreditCardDao

    companion object {
        const val DATABASE_NAME = "equilibra.db"
        const val DATABASE_VERSION = 6
    }
}
