package br.com.equilibra.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.equilibra.mobile.data.local.account.AccountDao
import br.com.equilibra.mobile.data.local.account.AccountEntity

/**
 * Task 3.2 — EquilibraDatabase v2
 *
 * - Banco: equilibra.db, version = 2
 * - Migrations explícitas (Migrations.MIGRATION_1_2); sem fallback destrutivo em produção.
 * - PlaceholderEntity removido (tabela _room_placeholder não é mais necessária após v2 —
 *   deixada vazia na Migration; Room recria o catálogo).
 */
@Database(
    entities = [AccountEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = []
)
abstract class EquilibraDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao

    companion object {
        const val DATABASE_NAME = "equilibra.db"
        const val DATABASE_VERSION = 2
    }
}
