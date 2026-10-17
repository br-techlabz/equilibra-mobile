package br.com.equilibra.mobile.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo raiz de DI. Adicione bindings globais aqui conforme as próximas tasks.
 * Exemplo de dependência verificável: [AppInfo].
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppInfo(): AppInfo = AppInfo(
        appName = "Equilibra",
        versionName = "1.0.0"
    )
}

/**
 * Dependência de exemplo para validar que Hilt está configurado corretamente.
 * Será substituída por dependências reais nas próximas tasks.
 */
data class AppInfo(
    val appName: String,
    val versionName: String
)
