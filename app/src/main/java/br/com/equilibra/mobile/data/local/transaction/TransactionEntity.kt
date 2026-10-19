package br.com.equilibra.mobile.data.local.transaction

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Task 4.1 — TransactionEntity (cache das últimas ~100 transações)
 *
 * Cache unificado das transações recentes (despesa/receita/transferência),
 * espelhando o shape do TransactionHistoryResponse do backend:
 *
 * - type: EXPENSE | INCOME | TRANSFER | STATEMENT_PAYMENT
 * - status: ACTIVE | CANCELLED
 * - amount: decimal como String (sem perda de precisão, padrão do projeto)
 * - occurredAt/createdAt/updatedAt: Instant ISO-8601
 * - categoryId/sourceAccountId/destinationAccountId: IDs opcionais
 *   (ex.: transferência não tem categoryId obrigatória; despesa não tem destino)
 * - syncedAt: epoch millis da última sincronização bem-sucedida
 *
 * Limite de 100 linhas é aplicado pelo DAO (trimToRecent) — as listas usam
 * paginação real da API; este cache é só para leitura rápida/recente.
 * Índices: type (consultas por tipo) e occurredAt (ordenação recentes).
 */
@Entity(
    tableName = "transactions",
    indices = [Index(value = ["type"]), Index(value = ["occurredAt"])]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val status: String,
    val description: String,
    val occurredAt: String,
    val amount: String,
    val categoryId: String? = null,
    val sourceAccountId: String? = null,
    val destinationAccountId: String? = null,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String,
    /** Epoch millis da última sincronização bem-sucedida */
    val syncedAt: Long? = null
)
