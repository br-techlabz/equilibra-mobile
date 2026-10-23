package br.com.equilibra.mobile.di

import android.content.Context
import androidx.room.Room
import br.com.equilibra.mobile.data.local.EquilibraDatabase
import br.com.equilibra.mobile.data.local.Migrations
import br.com.equilibra.mobile.data.local.account.AccountDao
import br.com.equilibra.mobile.data.local.category.CategoryDao
import br.com.equilibra.mobile.data.local.creditcard.CreditCardDao
import br.com.equilibra.mobile.data.local.purchase.PurchaseDao
import br.com.equilibra.mobile.data.local.statement.StatementDao
import br.com.equilibra.mobile.data.local.tag.TagDao
import br.com.equilibra.mobile.data.local.transaction.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Task 3.2 — DatabaseModule
 *
 * - Cria o banco equilibra.db via DI (Hilt), version = 6
 * - Migrations explícitas (Migrations.ALL); sem fallback destrutivo em produção.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): EquilibraDatabase =
        Room.databaseBuilder(
            context,
            EquilibraDatabase::class.java,
            EquilibraDatabase.DATABASE_NAME
        )
            .addMigrations(*Migrations.ALL)
            .build()

    @Provides
    fun provideAccountDao(db: EquilibraDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideCategoryDao(db: EquilibraDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTagDao(db: EquilibraDatabase): TagDao = db.tagDao()

    @Provides
    fun provideTransactionDao(db: EquilibraDatabase): TransactionDao = db.transactionDao()

    /** Task 6.1 — cache de cartões de crédito. */
    @Provides
    fun provideCreditCardDao(db: EquilibraDatabase): CreditCardDao = db.creditCardDao()

    /** Task 6.6 — cache de compras de cartão. */
    @Provides
    fun providePurchaseDao(db: EquilibraDatabase): PurchaseDao = db.purchaseDao()

    /** Task 7.1 — cache de faturas de cartão. */
    @Provides
    fun provideStatementDao(db: EquilibraDatabase): StatementDao = db.statementDao()
}
