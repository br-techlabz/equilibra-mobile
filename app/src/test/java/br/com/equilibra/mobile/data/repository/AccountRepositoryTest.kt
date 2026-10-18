package br.com.equilibra.mobile.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.equilibra.mobile.data.local.EquilibraDatabase
import br.com.equilibra.mobile.data.local.account.AccountDao
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.AssetAccountApi
import br.com.equilibra.mobile.data.remote.dto.account.AssetAccountDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
class AccountRepositoryTest {

    private lateinit var db: EquilibraDatabase
    private lateinit var dao: AccountDao
    private lateinit var api: AssetAccountApi
    private lateinit var repository: AccountRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EquilibraDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.accountDao()
        api = mockk()
        repository = AccountRepository(api, dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun dto(
        id: String = "id-$idSeq",
        name: String = "Conta $idSeq",
        type: String = "CHECKING",
        initialBalance: String = "100.00",
        currentBalance: String = "100.00",
        active: Boolean = true
    ) = AssetAccountDto(
        id = id,
        name = name,
        type = type,
        initialBalance = initialBalance,
        currentBalance = currentBalance,
        active = active,
        createdAt = "2024-01-01T00:00:00Z",
        updatedAt = "2024-01-01T00:00:00Z"
    )

    // refresh — sucesso atualiza cache

    @Test
    fun refreshPersistesNoRoomEmSucesso() = runTest {
        coEvery { api.list(any()) } returns Response.success(listOf(dto(id = "1", name = "A"), dto(id = "2", name = "B")))

        val result = repository.refresh()

        assertTrue(result is ApiResult.Success)
        assertEquals(2, dao.getAll().size)
        assertEquals("A", dao.getAll().first { it.id == "1" }.name)
    }

    @Test
    fun refreshRemoveLocaisQueNaoExistemNoServidor() = runTest {
        // Preenche cache com 3 locais
        dao.upsertAll(listOf(
            br.com.equilibra.mobile.data.local.account.AccountEntity("1", "A", "CHECKING", "10.00", "10.00", true, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z"),
            br.com.equilibra.mobile.data.local.account.AccountEntity("2", "B", "CHECKING", "10.00", "10.00", true, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z"),
            br.com.equilibra.mobile.data.local.account.AccountEntity("3", "C", "CHECKING", "10.00", "10.00", true, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z")
        ))

        // Servidor retorna apenas 1 e 3
        coEvery { api.list(any()) } returns Response.success(listOf(dto(id = "1"), dto(id = "3")))

        repository.refresh(includeInactive = true)

        val ids = dao.getAll().map { it.id }
        assertEquals(setOf("1", "3"), ids.toSet())
    }

    @Test
    fun refreshNaoApagaCacheEmErroDeRede() = runTest {
        dao.upsert(br.com.equilibra.mobile.data.local.account.AccountEntity("1", "Local", "CHECKING", "10.00", "10.00", true, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z"))
        coEvery { api.list(any()) } throws java.io.IOException("offline")

        val result = repository.refresh()

        assertTrue(result is ApiResult.NetworkError)
        assertEquals(1, dao.getAll().size)
        assertEquals("Local", dao.getAll()[0].name)
    }

    @Test
    fun refreshNaoApagaCacheEmHttpError() = runTest {
        dao.upsert(br.com.equilibra.mobile.data.local.account.AccountEntity("1", "Local", "CHECKING", "10.00", "10.00", true, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z"))
        coEvery { api.list(any()) } returns Response.error(500, "err".toResponseBody())

        val result = repository.refresh()

        assertTrue(result is ApiResult.HttpError)
        assertEquals(1, dao.getAll().size)
    }

    // offline: leitura do cache funciona mesmo sem API

    @Test
    fun observeAllEmiteCacheMesmoOffline() = runTest {
        dao.upsert(br.com.equilibra.mobile.data.local.account.AccountEntity("1", "Cache", "CHECKING", "10.00", "10.00", true, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z"))

        val list = repository.observeAccounts().first()

        assertEquals(1, list.size)
        assertEquals("Cache", list[0].name)
    }

    // create — só atualiza Room após sucesso

    @Test
    fun createPersisteNoRoomAposSucesso() = runTest {
        coEvery { api.create(any()) } returns Response.success(dto(id = "new", name = "Nova"))

        val result = repository.create("Nova", "CHECKING", "50.00")

        assertTrue(result is ApiResult.Success)
        assertNotNull(dao.getById("new"))
        assertEquals("Nova", dao.getById("new")!!.name)
    }

    @Test
    fun createNaoPersisteEmErro() = runTest {
        coEvery { api.create(any()) } returns Response.error(400, "bad".toResponseBody())

        val result = repository.create("X", "CHECKING", "10.00")

        assertTrue(result is ApiResult.HttpError)
        assertEquals(0, dao.getAll().size)
    }

    @Test
    fun createNaoPersisteEmNetworkError() = runTest {
        coEvery { api.create(any()) } throws java.io.IOException("offline")

        val result = repository.create("X", "CHECKING", "10.00")

        assertTrue(result is ApiResult.NetworkError)
        assertEquals(0, dao.getAll().size)
    }

    // update

    @Test
    fun updatePersisteNoRoomAposSucesso() = runTest {
        dao.upsert(br.com.equilibra.mobile.data.local.account.AccountEntity("1", "Old", "CHECKING", "10.00", "10.00", true, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z"))
        coEvery { api.update(any(), any()) } returns Response.success(dto(id = "1", name = "New", type = "SAVINGS"))

        val result = repository.update("1", "New", "SAVINGS", "20.00")

        assertTrue(result is ApiResult.Success)
        assertEquals("New", dao.getById("1")!!.name)
        assertEquals("SAVINGS", dao.getById("1")!!.type)
    }

    @Test
    fun updateNaoPersisteEmErro() = runTest {
        dao.upsert(br.com.equilibra.mobile.data.local.account.AccountEntity("1", "Old", "CHECKING", "10.00", "10.00", true, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z"))
        coEvery { api.update(any(), any()) } returns Response.error(500, "err".toResponseBody())

        val result = repository.update("1", "New", "CHECKING", "10.00")

        assertTrue(result is ApiResult.HttpError)
        assertEquals("Old", dao.getById("1")!!.name)
    }

    // deactivate/activate — só após sucesso

    @Test
    fun deactivatePersisteNoRoomAposSucesso() = runTest {
        dao.upsert(br.com.equilibra.mobile.data.local.account.AccountEntity("1", "A", "CHECKING", "10.00", "10.00", true, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z"))
        coEvery { api.deactivate(any()) } returns Response.success(dto(id = "1", active = false))

        val result = repository.deactivate("1")

        assertTrue(result is ApiResult.Success)
        assertEquals(false, dao.getById("1")!!.active)
    }

    @Test
    fun deactivateNaoPersisteEmErro() = runTest {
        dao.upsert(br.com.equilibra.mobile.data.local.account.AccountEntity("1", "A", "CHECKING", "10.00", "10.00", true, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z"))
        coEvery { api.deactivate(any()) } returns Response.error(500, "err".toResponseBody())

        val result = repository.deactivate("1")

        assertTrue(result is ApiResult.HttpError)
        assertEquals(true, dao.getById("1")!!.active)
    }

    @Test
    fun activatePersisteNoRoomAposSucesso() = runTest {
        dao.upsert(br.com.equilibra.mobile.data.local.account.AccountEntity("1", "A", "CHECKING", "10.00", "10.00", false, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z"))
        coEvery { api.activate(any()) } returns Response.success(dto(id = "1", active = true))

        val result = repository.activate("1")

        assertTrue(result is ApiResult.Success)
        assertEquals(true, dao.getById("1")!!.active)
    }

    companion object {
        private var idSeq = 0
    }
}
