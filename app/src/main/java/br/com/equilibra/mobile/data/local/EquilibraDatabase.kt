package br.com.equilibra.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Task 3.1 — Configurar Room
 *
 * - Banco: equilibra.db (version = 1)
 * - exportSchema = true → schemas em app/schemas (versionado no git)
 * - Migrações futuras devem ser explícitas; sem fallback destrutivo em produção.
 *
 * Entidades serão adicionadas nas próximas tasks (3.2, 3.9, 3.11, etc.)
 * e cada nova versão deve incluir Migration explícita.
 */
@Database(
    entities = [PlaceholderEntity::class],
    version = 1,
    exportSchema = true,
    autoMigrations = []
)
abstract class EquilibraDatabase : RoomDatabase() {

    abstract fun placeholderDao(): PlaceholderDao

    companion object {
        const val DATABASE_NAME = "equilibra.db"
        const val DATABASE_VERSION = 1
    }
}
