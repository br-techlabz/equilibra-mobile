package br.com.equilibra.mobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade placeholder para permitir que o Room compile na Task 3.1 (versão 1).
 *
 * A Task 3.1 exige banco, DAOs, schemas e testes sem criar ainda o domínio real.
 * O Room/KSP exige ao menos uma entidade em @Database. Este placeholder
 * satisfaz a compilação; será substituído pelas entidades reais
 * (AccountEntity, CategoryEntity, TagEntity) nas próximas tasks com
 * migrations explícitas (version 2+).
 *
 * Não é usado em produção — tabela `_room_placeholder` permanece vazia.
 */
@Entity(tableName = "_room_placeholder")
data class PlaceholderEntity(
    @PrimaryKey val id: String = "placeholder"
)
