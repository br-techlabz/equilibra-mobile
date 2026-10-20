package br.com.equilibra.mobile.di

import br.com.equilibra.mobile.BuildConfig
import br.com.equilibra.mobile.data.remote.AuthApi
import br.com.equilibra.mobile.data.remote.AuthInterceptor
import br.com.equilibra.mobile.data.remote.CategoryApi
import br.com.equilibra.mobile.data.remote.AssetAccountApi
import br.com.equilibra.mobile.data.remote.DashboardApi
import br.com.equilibra.mobile.data.remote.ExpenseApi
import br.com.equilibra.mobile.data.remote.IncomeApi
import br.com.equilibra.mobile.data.remote.TransferApi
import br.com.equilibra.mobile.data.remote.PersistentCookieJar
import br.com.equilibra.mobile.data.remote.TagApi
import br.com.equilibra.mobile.data.remote.TokenAuthenticator
import br.com.equilibra.mobile.data.remote.TransactionHistoryApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cookieJar: PersistentCookieJar,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
            redactHeader("Cookie")
            redactHeader("Set-Cookie")
        }
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .authenticator(tokenAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideDashboardApi(retrofit: Retrofit): DashboardApi = retrofit.create(DashboardApi::class.java)

    @Provides
    @Singleton
    fun provideAssetAccountApi(retrofit: Retrofit): AssetAccountApi = retrofit.create(AssetAccountApi::class.java)

    @Provides
    @Singleton
    fun provideCategoryApi(retrofit: Retrofit): CategoryApi = retrofit.create(CategoryApi::class.java)

    @Provides
    @Singleton
    fun provideTagApi(retrofit: Retrofit): TagApi = retrofit.create(TagApi::class.java)

    @Provides
    @Singleton
    fun provideExpenseApi(retrofit: Retrofit): ExpenseApi = retrofit.create(ExpenseApi::class.java)

    @Provides
    @Singleton
    fun provideIncomeApi(retrofit: Retrofit): IncomeApi = retrofit.create(IncomeApi::class.java)

    @Provides
    @Singleton
    fun provideTransferApi(retrofit: Retrofit): TransferApi = retrofit.create(TransferApi::class.java)

    @Provides
    @Singleton
    fun provideTransactionHistoryApi(retrofit: Retrofit): TransactionHistoryApi =
        retrofit.create(TransactionHistoryApi::class.java)
}
