package br.com.equilibra.mobile.data.local.tag

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Task 3.11 — TagEntity
 *
 * - UUID String PK
 * - name: max 100 chars (constraint do backend; validado no remoto)
 * - active: estado ativo; syncedAt epoch millis nullable
 * - createdAt/updatedAt: Instant ISO-8601
 * - Índice em name: suporta a listagem ordenada COLLATE NOCASE.
 *   Unicidade de nome normalizado por usuário é regra do backend
 *   (Tag.normalizeName) e o erro remoto é a fonte de verdade —
 *   não há UNIQUE local para não criar falso conflito com caixa/acentos.
 */
@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"])]
)
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,
    /** Epoch millis da última sincronização bem-sucedida */
    val syncedAt: Long? = null
)
