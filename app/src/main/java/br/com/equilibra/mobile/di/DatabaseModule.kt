package br.com.equilibra.mobile.di

import android.content.Context
import androidx.room.Room
import br.com.equilibra.mobile.data.local.EquilibraDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Task 3.1 — Configurar Room
 *
 * - Cria o banco equilibra.db via DI (Hilt)
 * - Sem fallbackToDestructiveMigration em produção (migrações explícitas)
 * - Plugins/DAOs serão adicionados nas próximas tasks (3.2, 3.9, 3.11)
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
        ).build()

    // DAOs serão expostos aqui quando criados, ex:
    // @Provides fun provideAccountDao(db: EquilibraDatabase): AccountDao = db.accountDao()
}
