package br.com.equilibra.mobile.data.local.account

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Task 3.2 — AccountEntity
 *
 * - UUID como String (PK)
 * - BigDecimal serializado como String (initialBalance/currentBalance sem perda)
 * - Timestamps ISO-8601 (Instant.toString) para createdAt/updatedAt
 * - syncedAt epoch millis para controle de sincronização (nullable)
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    /**
     * Tipo da conta — enum AssetAccountType serializado como String
     * Valores: CHECKING, SAVINGS, CASH, INVESTMENT, DIGITAL, OTHER
     */
    val type: String,
    /** BigDecimal como String (precision 19, scale 2) — sem perda via Double/Float */
    val initialBalance: String,
    /** Saldo atual calculado pelo backend — também como String */
    val currentBalance: String,
    val active: Boolean,
    /** Instant ISO-8601 (ex: 2024-01-01T12:00:00Z) */
    val createdAt: String,
    /** Instant ISO-8601 */
    val updatedAt: String,
    /** Epoch millis da última sincronização bem sucedida — null se nunca sincronizado */
    val syncedAt: Long? = null
)
