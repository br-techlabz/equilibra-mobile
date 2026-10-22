package br.com.equilibra.mobile.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.equilibra.mobile.data.local.EquilibraDatabase
import br.com.equilibra.mobile.data.local.account.AccountEntity
import br.com.equilibra.mobile.data.local.category.CategoryEntity
import br.com.equilibra.mobile.data.local.creditcard.CreditCardEntity
import br.com.equilibra.mobile.data.local.tag.TagEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.AuthApi
import br.com.equilibra.mobile.data.remote.PersistentCookieJar
import br.com.equilibra.mobile.data.session.SessionManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response

/**
 * Bug real encontrado no emulador: um usuário recém-criado sem categorias
 * via as 8 categorias de outra conta no seletor, porque o logout limpava
 * sessão e cookies mas NÃO o banco Room. Como dado financeiro local, isso
 * é vazamento entre contas — o logout precisa apagar o cache inteiro.
 */
@RunWith(RobolectricTestRunner::class)
class AuthRepositoryLogoutTest {

    private lateinit var db: EquilibraDatabase
    private lateinit var authApi: AuthApi
    private lateinit var sessionManager: SessionManager
    private lateinit var cookieJar: PersistentCookieJar
    private lateinit var repository: AuthRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EquilibraDatabase::class.java
        ).allowMainThreadQueries().build()
        authApi = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        cookieJar = mockk(relaxed = true)
        repository = AuthRepository(authApi, sessionManager, cookieJar, db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedEverything() {
        db.categoryDao().upsertAll(
            listOf(
                CategoryEntity(
                    id = "cat-1", name = "Alimentação", applicability = "EXPENSE",
                    active = true, createdAt = "2026-10-01T00:00:00Z", updatedAt = "2026-10-01T00:00:00Z"
                )
            )
        )
        db.tagDao().upsertAll(
            listOf(
                TagEntity(
                    id = "tag-1", name = "Viagem", active = true,
                    createdAt = "2026-10-01T00:00:00Z", updatedAt = "2026-10-01T00:00:00Z"
                )
            )
        )
        db.accountDao().upsertAll(
            listOf(
                AccountEntity(
                    id = "acc-1", name = "Conta", type = "CHECKING", initialBalance = "0.00",
                    currentBalance = "0.00", active = true,
                    createdAt = "2026-10-01T00:00:00Z", updatedAt = "2026-10-01T00:00:00Z"
                )
            )
        )
        db.creditCardDao().upsert(
            CreditCardEntity(
                id = "card-1", name = "Nubank", institution = "Nubank", nickname = null,
                totalLimit = "1000.00", committedLimit = "0.00", reservedLimit = "0.00",
                availableLimit = "1000.00", closingDay = 25, dueDay = 2,
                timezone = "America/Sao_Paulo", status = "ACTIVE",
                createdAt = "2026-10-01T00:00:00Z", updatedAt = "2026-10-01T00:00:00Z"
            )
        )
    }

    @Test
    fun logoutLimpaTodoOCacheLocal() = runTest {
        seedEverything()
        coEvery { authApi.logout() } returns Response.success(null)

        repository.logout()

        assertTrue("categorias devem sumir", db.categoryDao().getAll().isEmpty())
        assertTrue("tags devem sumir", db.tagDao().getAll().isEmpty())
        assertTrue("contas devem sumir", db.accountDao().getAll().isEmpty())
        assertTrue("cartoes devem sumir", db.creditCardDao().getAll().isEmpty())
    }

    @Test
    fun logoutLimpaCacheMesmoSeBackendFalhar() = runTest {
        seedEverything()
        coEvery { authApi.logout() } throws java.io.IOException("offline")

        repository.logout()

        assertTrue(db.categoryDao().getAll().isEmpty())
    }

    @Test
    fun logoutContinuaLimpandoSessaoECookies() = runTest {
        coEvery { authApi.logout() } returns Response.success(null)

        repository.logout()

        coVerify { sessionManager.clearSession() }
        verify { cookieJar.clear() }
    }

    @Test
    fun loginNaoApagaCacheDeOutraContaAntesDoLogout() = runTest {
        // Garantia de que a limpeza esta no logout e nao no login: um login
        // nao deve destruir o cache de quem ja esta usando o app.
        seedEverything()
        coEvery { authApi.login(any()) } returns Response.success(
            br.com.equilibra.mobile.data.remote.dto.LoginResponseDto(
                accessToken = "t", expiresIn = 3600L
            )
        )
        coEvery { authApi.getCurrentUser() } returns Response.success(
            br.com.equilibra.mobile.data.remote.dto.CurrentUserDto(
                id = "u1", email = "a@b.com", createdAt = "2026-10-01T00:00:00Z"
            )
        )

        repository.login("a@b.com", "senha")

        assertEquals(1, db.categoryDao().getAll().size)
    }
}
