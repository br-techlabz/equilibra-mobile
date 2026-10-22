package br.com.equilibra.mobile.data.local.purchase

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Task 6.6 — DAO de compras de cartão.
 *
 * Ordenação por purchaseDate DESC e depois createdAt DESC, igual ao
 * findAllByOwnerIdAndCreditCardIdOrderByPurchaseDateDesc do backend.
 */
@Dao
interface PurchaseDao {

    // Leitura

    /** Lista observável por cartão (cache-first — Task 6.7/Task 6.8). */
    @Query(
        """
        SELECT * FROM credit_card_purchases
        WHERE creditCardId = :cardId
        ORDER BY purchaseDate DESC, createdAt DESC
        """
    )
    fun observeByCard(cardId: String): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM credit_card_purchases WHERE id = :id LIMIT 1")
    suspend fun getPurchaseById(id: String): PurchaseEntity?

    @Query("SELECT * FROM credit_card_purchases WHERE id = :id LIMIT 1")
    fun observePurchaseById(id: String): Flow<PurchaseEntity?>

    @Query("SELECT * FROM credit_card_purchases WHERE creditCardId = :cardId")
    suspend fun getByCard(cardId: String): List<PurchaseEntity>

    /** Parcelas da compra, em ordem de sequência (1..n). */
    @Query("SELECT * FROM purchase_installments WHERE purchaseId = :purchaseId ORDER BY sequenceNumber ASC")
    suspend fun getInstallments(purchaseId: String): List<InstallmentEntity>

    @Query("SELECT * FROM purchase_installments WHERE purchaseId = :purchaseId ORDER BY sequenceNumber ASC")
    fun observeInstallments(purchaseId: String): Flow<List<InstallmentEntity>>

    // Escrita

    /**
     * @Upsert (e não REPLACE) de propósito: REPLACE apaga a linha antes de
     * inserir, e a FK CASCADE levaria as parcelas junto. Com @Upsert uma
     * compra existente vira UPDATE e as parcelas permanecem (ex.: cancelar).
     */
    @Upsert
    suspend fun upsertPurchase(entity: PurchaseEntity)

    @Upsert
    suspend fun upsertPurchases(entities: List<PurchaseEntity>)

    @Upsert
    suspend fun upsertInstallment(entity: InstallmentEntity)

    @Upsert
    suspend fun upsertInstallments(entities: List<InstallmentEntity>)

    /**
     * Substitui a compra e suas parcelas numa única transação: uma compra
     * atualizada nunca fica com parcelas de uma versão anterior.
     */
    @Transaction
    suspend fun upsertPurchaseWithInstallments(
        purchase: PurchaseEntity,
        installments: List<InstallmentEntity>
    ) {
        upsertPurchase(purchase)
        deleteInstallments(purchase.id)
        upsertInstallments(installments)
    }

    @Query("DELETE FROM purchase_installments WHERE purchaseId = :purchaseId")
    suspend fun deleteInstallments(purchaseId: String)

    @Query("DELETE FROM credit_card_purchases WHERE creditCardId = :cardId AND id NOT IN (:keepIds)")
    suspend fun deleteByCardNotIn(cardId: String, keepIds: List<String>)

    @Query("DELETE FROM credit_card_purchases WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM credit_card_purchases")
    suspend fun clearAll()

    /**
     * Remove as compras do cartão ausentes em [keepIds]; lista vazia limpa
     * todas as do cartão. Evita `NOT IN ()`, inválido no SQLite.
     * As parcelas vão junto (FK CASCADE).
     */
    @Transaction
    suspend fun deleteByCardNotInSafe(cardId: String, keepIds: List<String>) {
        if (keepIds.isEmpty()) {
            deleteByCardOnly(cardId)
        } else {
            deleteByCardNotIn(cardId, keepIds)
        }
    }

    @Query("DELETE FROM credit_card_purchases WHERE creditCardId = :cardId")
    suspend fun deleteByCardOnly(cardId: String)
}
