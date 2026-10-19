package br.com.equilibra.mobile.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.equilibra.mobile.data.local.EquilibraDatabase
import br.com.equilibra.mobile.data.local.tag.TagDao
import br.com.equilibra.mobile.data.local.tag.TagEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.TagApi
import br.com.equilibra.mobile.data.remote.dto.tag.CreateTagRequestDto
import br.com.equilibra.mobile.data.remote.dto.tag.TagDto
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
class TagRepositoryTest {

    private lateinit var db: EquilibraDatabase
    private lateinit var dao: TagDao
    private lateinit var api: TagApi
    private lateinit var repository: TagRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EquilibraDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.tagDao()
        api = mockk()
        repository = TagRepository(api, dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun dto(
        id: String = "id-$idSeq",
        name: String = "Tag $idSeq",
        active: Boolean = true
    ) = TagDto(
        id = id,
        name = name,
        active = active,
        createdAt = "2024-01-01T00:00:00Z",
        updatedAt = "2024-01-01T00:00:00Z"
    )

    private fun entity(
        id: String,
        name: String,
        active: Boolean = true
    ) = TagEntity(
        id = id, name = name, active = active,
        createdAt = "2024-01-01T00:00:00Z", updatedAt = "2024-01-01T00:00:00Z"
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
        dao.upsertAll(listOf(entity("1", "A"), entity("2", "B"), entity("3", "C")))

        // Servidor retorna apenas 1 e 3
        coEvery { api.list(any()) } returns Response.success(listOf(dto(id = "1"), dto(id = "3")))

        repository.refresh(includeInactive = true)

        assertEquals(setOf("1", "3"), dao.getAll().map { it.id }.toSet())
    }

    @Test
    fun refreshNaoApagaCacheEmErroDeRede() = runTest {
        dao.upsert(entity("1", "Local"))
        coEvery { api.list(any()) } throws java.io.IOException("offline")

        val result = repository.refresh()

        assertTrue(result is ApiResult.NetworkError)
        assertEquals(1, dao.getAll().size)
        assertEquals("Local", dao.getAll()[0].name)
    }

    @Test
    fun refreshNaoApagaCacheEmHttpError() = runTest {
        dao.upsert(entity("1", "Local"))
        coEvery { api.list(any()) } returns Response.error(500, "err".toResponseBody())

        val result = repository.refresh()

        assertTrue(result is ApiResult.HttpError)
        assertEquals(1, dao.getAll().size)
    }

    // offline: leitura do cache funciona mesmo sem API

    @Test
    fun observeAllEmiteCacheMesmoOffline() = runTest {
        dao.upsert(entity("1", "Cache"))

        val list = repository.observeTags().first()

        assertEquals(1, list.size)
        assertEquals("Cache", list[0].name)
    }

    @Test
    fun refreshByIdPersisteNoRoom() = runTest {
        coEvery { api.get("1") } returns Response.success(dto(id = "1", name = "Servidor"))

        val result = repository.refreshById("1")

        assertTrue(result is ApiResult.Success)
        assertEquals("Servidor", dao.getById("1")!!.name)
    }

    // create — só atualiza Room após sucesso

    @Test
    fun createPersisteNoRoomAposSucesso() = runTest {
        coEvery { api.create(any()) } returns Response.success(dto(id = "new", name = "Nova"))

        val result = repository.create("Nova")

        assertTrue(result is ApiResult.Success)
        assertNotNull(dao.getById("new"))
        assertEquals("Nova", dao.getById("new")!!.name)
    }

    @Test
    fun createEnviaApenasNome() = runTest {
        coEvery { api.create(any()) } returns Response.success(dto(id = "new", name = "Nova"))

        repository.create("Nova")

        io.mockk.slot<CreateTagRequestDto>().let { slot ->
            io.mockk.coVerify { api.create(capture(slot)) }
            assertEquals("Nova", slot.captured.name)
        }
    }

    @Test
    fun createNaoPersisteEmErro() = runTest {
        coEvery { api.create(any()) } returns Response.error(400, "bad".toResponseBody())

        val result = repository.create("X")

        assertTrue(result is ApiResult.HttpError)
        assertEquals(0, dao.getAll().size)
    }

    @Test
    fun createNaoPersisteEmNetworkError() = runTest {
        coEvery { api.create(any()) } throws java.io.IOException("offline")

        val result = repository.create("X")

        assertTrue(result is ApiResult.NetworkError)
        assertEquals(0, dao.getAll().size)
    }

    // update

    @Test
    fun updatePersisteNoRoomAposSucesso() = runTest {
        dao.upsert(entity("1", "Old"))
        coEvery { api.update(any(), any()) } returns Response.success(dto(id = "1", name = "New"))

        val result = repository.update("1", "New")

        assertTrue(result is ApiResult.Success)
        assertEquals("New", dao.getById("1")!!.name)
    }

    @Test
    fun updateNaoPersisteEmErro() = runTest {
        dao.upsert(entity("1", "Old"))
        coEvery { api.update(any(), any()) } returns Response.error(500, "err".toResponseBody())

        val result = repository.update("1", "New")

        assertTrue(result is ApiResult.HttpError)
        assertEquals("Old", dao.getById("1")!!.name)
    }

    // deactivate/activate — só após sucesso

    @Test
    fun deactivatePersisteNoRoomAposSucesso() = runTest {
        dao.upsert(entity("1", "A", active = true))
        coEvery { api.deactivate(any()) } returns Response.success(dto(id = "1", active = false))

        val result = repository.deactivate("1")

        assertTrue(result is ApiResult.Success)
        assertEquals(false, dao.getById("1")!!.active)
    }

    @Test
    fun deactivateNaoPersisteEmErro() = runTest {
        dao.upsert(entity("1", "A", active = true))
        coEvery { api.deactivate(any()) } returns Response.error(500, "err".toResponseBody())

        val result = repository.deactivate("1")

        assertTrue(result is ApiResult.HttpError)
        assertEquals(true, dao.getById("1")!!.active)
    }

    @Test
    fun activatePersisteNoRoomAposSucesso() = runTest {
        dao.upsert(entity("1", "A", active = false))
        coEvery { api.activate(any()) } returns Response.success(dto(id = "1", active = true))

        val result = repository.activate("1")

        assertTrue(result is ApiResult.Success)
        assertEquals(true, dao.getById("1")!!.active)
    }

    @Test
    fun activateNaoPersisteEmErro() = runTest {
        dao.upsert(entity("1", "A", active = false))
        coEvery { api.activate(any()) } returns Response.error(500, "err".toResponseBody())

        val result = repository.activate("1")

        assertTrue(result is ApiResult.HttpError)
        assertEquals(false, dao.getById("1")!!.active)
    }

    companion object {
        private var idSeq = 0
    }
}
