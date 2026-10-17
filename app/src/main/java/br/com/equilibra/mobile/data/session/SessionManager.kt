package br.com.equilibra.mobile.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

/**
 * Persistência de sessão segura.
 *
 * - [accessToken] é armazenado em [EncryptedSharedPreferences] (Android Keystore, AES256_GCM).
 * - [userId] e [email] ficam no DataStore Preferences (dados não sensíveis, mas privados).
 * - O refresh token é um cookie httpOnly gerenciado pelo OkHttp CookieJar; não é exposto aqui.
 *
 * Todos os métodos são testáveis via injeção; nenhum token é registrado em log.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore: DataStore<Preferences> = context.sessionDataStore

    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_EMAIL = stringPreferencesKey("email")
    }

    // ── AccessToken (segredo) ──

    fun getAccessToken(): String? = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)

    suspend fun saveAccessToken(token: String) {
        encryptedPrefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun observeAccessToken(): Flow<String?> = dataStore.data.map { null } // placeholder; token is in EncryptedPrefs

    // ── UserId / Email (DataStore) ──

    val userIdFlow: Flow<String?> = dataStore.data.map { it[KEY_USER_ID] }
    val emailFlow: Flow<String?> = dataStore.data.map { it[KEY_EMAIL] }

    suspend fun saveUserId(userId: String) {
        dataStore.edit { it[KEY_USER_ID] = userId }
    }

    suspend fun saveEmail(email: String) {
        dataStore.edit { it[KEY_EMAIL] = email }
    }

    suspend fun saveUserSession(userId: String, email: String, accessToken: String) {
        saveAccessToken(accessToken)
        dataStore.edit {
            it[KEY_USER_ID] = userId
            it[KEY_EMAIL] = email
        }
    }

    suspend fun getUserId(): String? = dataStore.data.map { it[KEY_USER_ID] }.first()
    suspend fun getEmail(): String? = dataStore.data.map { it[KEY_EMAIL] }.first()

    suspend fun clearSession() {
        encryptedPrefs.edit().clear().apply()
        dataStore.edit { it.clear() }
    }

    suspend fun isLoggedIn(): Boolean = getAccessToken() != null
}
