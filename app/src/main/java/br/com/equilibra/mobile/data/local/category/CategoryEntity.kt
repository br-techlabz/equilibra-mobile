package br.com.equilibra.mobile.data.local.category

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Task 3.9 — CategoryEntity
 *
 * - UUID String PK
 * - applicability: EXPENSE | INCOME | BOTH (String)
 * - active: estado ativo; syncedAt epoch millis nullable
 * - createdAt/updatedAt: Instant ISO-8601
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** EXPENSE, INCOME ou BOTH */
    val applicability: String,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,
    /** Epoch millis da última sincronização bem-sucedida */
    val syncedAt: Long? = null
)
