package br.com.equilibra.mobile.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrations explícitas — sem fallback destrutivo em produção.
 *
 * Cada task que adicionar entidade/bump de versão deve registrar Migration aqui
 * e bumpa DATABASE_VERSION.
 */
object Migrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `accounts` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `initialBalance` TEXT NOT NULL,
                    `currentBalance` TEXT NOT NULL,
                    `active` INTEGER NOT NULL,
                    `createdAt` TEXT NOT NULL,
                    `updatedAt` TEXT NOT NULL,
                    `syncedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            // Remove tabela placeholder da v1 — evita divergência entre fresh install (v2) e upgrade (v1→v2)
            db.execSQL("DROP TABLE IF EXISTS `_room_placeholder`")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `categories` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `applicability` TEXT NOT NULL,
                    `active` INTEGER NOT NULL,
                    `createdAt` TEXT NOT NULL,
                    `updatedAt` TEXT NOT NULL,
                    `syncedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `tags` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `active` INTEGER NOT NULL,
                    `createdAt` TEXT NOT NULL,
                    `updatedAt` TEXT NOT NULL,
                    `syncedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `transactions` (
                    `id` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `occurredAt` TEXT NOT NULL,
                    `amount` TEXT NOT NULL,
                    `categoryId` TEXT,
                    `sourceAccountId` TEXT,
                    `destinationAccountId` TEXT,
                    `notes` TEXT,
                    `createdAt` TEXT NOT NULL,
                    `updatedAt` TEXT NOT NULL,
                    `syncedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type` ON `transactions` (`type`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_occurredAt` ON `transactions` (`occurredAt`)")
        }
    }

    /**
     * Task 6.1 — Tabela `credit_cards` (cache local de cartões de crédito).
     * Sem índices: a entity não declara nenhum (Room valida schema após migration).
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `credit_cards` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `institution` TEXT NOT NULL,
                    `nickname` TEXT,
                    `totalLimit` TEXT NOT NULL,
                    `committedLimit` TEXT NOT NULL,
                    `reservedLimit` TEXT NOT NULL,
                    `availableLimit` TEXT NOT NULL,
                    `closingDay` INTEGER NOT NULL,
                    `dueDay` INTEGER NOT NULL,
                    `timezone` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `createdAt` TEXT NOT NULL,
                    `updatedAt` TEXT NOT NULL,
                    `syncedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }

    /**
     * Task 6.6 — Tabelas `credit_card_purchases` e `purchase_installments`.
     * Índices e FK espelham o que a Entity declara (Room valida o schema).
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `credit_card_purchases` (
                    `id` TEXT NOT NULL,
                    `creditCardId` TEXT NOT NULL,
                    `categoryId` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `totalAmount` TEXT NOT NULL,
                    `purchaseDate` TEXT NOT NULL,
                    `purchaseAt` TEXT,
                    `installmentCount` INTEGER NOT NULL,
                    `cycleMonth` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `createdAt` TEXT NOT NULL,
                    `updatedAt` TEXT NOT NULL,
                    `syncedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_credit_card_purchases_creditCardId_purchaseDate` " +
                    "ON `credit_card_purchases` (`creditCardId`, `purchaseDate`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_credit_card_purchases_purchaseDate` " +
                    "ON `credit_card_purchases` (`purchaseDate`)"
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `purchase_installments` (
                    `id` TEXT NOT NULL,
                    `purchaseId` TEXT NOT NULL,
                    `sequenceNumber` INTEGER NOT NULL,
                    `totalInstallments` INTEGER NOT NULL,
                    `principalAmount` TEXT NOT NULL,
                    `cycleMonth` TEXT NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`purchaseId`) REFERENCES `credit_card_purchases`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_purchase_installments_purchaseId` " +
                    "ON `purchase_installments` (`purchaseId`)"
            )
        }
    }

    /**
     * Task 7.1 — Tabelas `credit_card_statements` e `statement_items`.
     * Índices e FK espelham o que as Entities declaram (Room valida o schema).
     * Sem FK para credit_cards (mesma decisão de PurchaseEntity): a API é a
     * fonte da verdade e o cache de faturas não deve depender do refresh de
     * cartões.
     */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `credit_card_statements` (
                    `id` TEXT NOT NULL,
                    `creditCardId` TEXT NOT NULL,
                    `cycleStart` TEXT NOT NULL,
                    `cycleEnd` TEXT NOT NULL,
                    `closingAt` TEXT NOT NULL,
                    `dueDate` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `totalAmount` TEXT NOT NULL,
                    `paidAmount` TEXT NOT NULL,
                    `outstandingAmount` TEXT NOT NULL,
                    `syncedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_credit_card_statements_creditCardId_cycleStart` " +
                    "ON `credit_card_statements` (`creditCardId`, `cycleStart`)"
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `statement_items` (
                    `id` TEXT NOT NULL,
                    `statementId` TEXT NOT NULL,
                    `purchaseId` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `sequenceNumber` INTEGER NOT NULL,
                    `totalInstallments` INTEGER NOT NULL,
                    `amount` TEXT NOT NULL,
                    `cycleMonth` TEXT NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`statementId`) REFERENCES `credit_card_statements`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_statement_items_statementId` " +
                    "ON `statement_items` (`statementId`)"
            )
        }
    }

    val ALL = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8
    )
}
