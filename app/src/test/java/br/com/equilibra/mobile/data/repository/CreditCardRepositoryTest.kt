package br.com.equilibra.mobile.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.equilibra.mobile.data.local.EquilibraDatabase
import br.com.equilibra.mobile.data.local.creditcard.CreditCardDao
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.CreditCardApi
import br.com.equilibra.mobile.data.remote.dto.creditcard.CreateCreditCardRequestDto
import br.com.equilibra.mobile.data.remote.dto.creditcard.CreditCardDto
import br.com.equilibra.mobile.data.remote.dto.creditcard.UpdateCreditCardRequestDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
class CreditCardRepositoryTest {

    private lateinit var db: EquilibraDatabase
    private lateinit var dao: CreditCardDao
    private lateinit var api: CreditCardApi
    private lateinit var repository: CreditCardRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EquilibraDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.creditCardDao()
        api = mockk()
        repository = CreditCardRepository(api, dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private var seq = 0

    private fun dto(
        id: String = "id-${seq++}",
        name: String = "Cartão @$id",
        status: String = "ACTIVE",
        committed: String = "0.00",
        available: String = "1000.00"
    ) = CreditCardDto(
        id = id,
        name = name,
        institution = "Banco",
        nickname = null,
        totalLimit = "1000.00",
        committedLimit = committed,
        reservedLimit = "0.00",
        availableLimit = available,
        closingDay = 20,
        dueDay = 5,
        timezone = "America/Sao_Paulo",
        status = status,
        createdAt = "2026-10-22T00:00:00Z",
        updatedAt = "2026-10-22T00:00:00Z"
    )

    /** Para semear o cache diretamente no DAO (DAO espera Entity, não DTO). */
    private fun entity(id: String, name: String = "Cartão @$id", status: String = "ACTIVE") =
        br.com.equilibra.mobile.data.local.creditcard.CreditCardEntity(
            id = id,
            name = name,
            institution = "Banco",
            nickname = null,
            totalLimit = "1000.00",
            committedLimit = "0.00",
            reservedLimit = "0.00",
            availableLimit = "1000.00",
            closingDay = 20,
            dueDay = 5,
            timezone = "America/Sao_Paulo",
            status = status,
            createdAt = "2026-10-22T00:00:00Z",
            updatedAt = "2026-10-22T00:00:00Z",
            syncedAt = null
        )

    // refresh — sucesso atualiza cache; falha preserva

    @Test
    fun refreshPersisteNoRoomEmSucesso() = runTest {
        coEvery { api.list(any()) } returns Response.success(listOf(dto(id = "1", name = "A"), dto(id = "2", name = "B")))

        val result = repository.refresh()

        assertTrue(result is ApiResult.Success)
        val cached = dao.getAll()
        assertEquals(2, cached.size)
        assertEquals("A", cached.first { it.id == "1" }.name)
        // syncedAt carimbado
        assertTrue(cached.all { it.syncedAt != null })
    }

    @Test
    fun refreshReservaLimitesCalculadosPeloBackendSemRecalcular() = runTest {
        // Backend manda committed=250.50 / available=749.50 — repository copia
        coEvery { api.list(any()) } returns Response.success(
            listOf(dto(id = "1", committed = "250.50", available = "749.50"))
        )

        repository.refresh()

        val e = dao.getById("1")!!
        assertEquals("250.50", e.committedLimit)
        assertEquals("749.50", e.availableLimit)
        assertEquals("1000.00", e.totalLimit)
    }

    @Test
    fun refreshRemoveLocaisQueNaoExistemNoServidor() = runTest {
        dao.upsertAll(listOf(
            br.com.equilibra.mobile.data.local.creditcard.CreditCardEntity(
                id = "old", name = "Old", institution = "B", nickname = null,
                totalLimit = "1.00", committedLimit = "0.00", reservedLimit = "0.00", availableLimit = "1.00",
                closingDay = 1, dueDay = 2, timezone = "UTC", status = "ACTIVE",
                createdAt = "2026-10-22T00:00:00Z", updatedAt = "2026-10-22T00:00:00Z", syncedAt = null
            )
        ))
        coEvery { api.list(any()) } returns Response.success(listOf(dto(id = "1")))

        repository.refresh()

        val ids = dao.getAll().map { it.id }
        assertEquals(listOf("1"), ids)
    }

    @Test
    fun refreshNaoApagaCacheEmErroDeRede() = runTest {
        dao.upsert(
            br.com.equilibra.mobile.data.local.creditcard.CreditCardEntity(
                id = "local", name = "Local", institution = "B", nickname = null,
                totalLimit = "1.00", committedLimit = "0.00", reservedLimit = "0.00", availableLimit = "1.00",
                closingDay = 1, dueDay = 2, timezone = "UTC", status = "ACTIVE",
                createdAt = "2026-10-22T00:00:00Z", updatedAt = "2026-10-22T00:00:00Z", syncedAt = null
            )
        )
        coEvery { api.list(any()) } throws java.io.IOException("offline")

        val result = repository.refresh()

        assertTrue(result is ApiResult.NetworkError)
        val cached = dao.getAll()
        assertEquals(1, cached.size)
        assertEquals("Local", cached[0].name)
    }

    @Test
    fun refreshNaoApagaCacheEmHttpError() = runTest {
        dao.upsert(
            br.com.equilibra.mobile.data.local.creditcard.CreditCardEntity(
                id = "local", name = "Local", institution = "B", nickname = null,
                totalLimit = "1.00", committedLimit = "0.00", reservedLimit = "0.00", availableLimit = "1.00",
                closingDay = 1, dueDay = 2, timezone = "UTC", status = "ACTIVE",
                createdAt = "2026-10-22T00:00:00Z", updatedAt = "2026-10-22T00:00:00Z", syncedAt = null
            )
        )
        coEvery { api.list(any()) } returns Response.error(500, "err".toResponseBody())

        val result = repository.refresh()

        assertTrue(result is ApiResult.HttpError)
        assertEquals(1, dao.getAll().size)
    }

    @Test
    fun refreshByIdPersisteNoRoom() = runTest {
        coEvery { api.get("7") } returns Response.success(dto(id = "7", name = "Sétimo"))

        val result = repository.refreshById("7")

        assertTrue(result is ApiResult.Success)
        assertEquals("Sétimo", dao.getById("7")!!.name)
    }

    @Test
    fun refreshByIdEmErroNaoTocaCache() = runTest {
        dao.upsert(
            br.com.equilibra.mobile.data.local.creditcard.CreditCardEntity(
                id = "7", name = "Cache", institution = "B", nickname = null,
                totalLimit = "1.00", committedLimit = "0.00", reservedLimit = "0.00", availableLimit = "1.00",
                closingDay = 1, dueDay = 2, timezone = "UTC", status = "ACTIVE",
                createdAt = "2026-10-22T00:00:00Z", updatedAt = "2026-10-22T00:00:00Z", syncedAt = null
            )
        )
        coEvery { api.get("7") } returns Response.error(404, "nf".toResponseBody())

        val result = repository.refreshById("7")

        assertTrue(result is ApiResult.HttpError)
        assertEquals("Cache", dao.getById("7")!!.name)
    }

    // Observação cache-first

    @Test
    fun observeActiveCardsEmiteCacheAtual() = runTest {
        dao.upsert(entity(id = "a1", status = "ACTIVE"))
        dao.upsert(entity(id = "a2", status = "INACTIVE"))

        val flow = repository.observeActiveCards().first()

        assertEquals(listOf("a1"), flow.map { it.id })
    }

    // create — só grava após sucesso remoto

    @Test
    fun createPersisteSomenteAposSucessoRemoto() = runTest {
        coEvery { api.create(any()) } returns Response.success(dto(id = "novo", name = "Novo"))

        val result = repository.create("Novo", "Banco", null, "100.00", 20, 5, null)

        assertTrue(result is ApiResult.Success)
        assertEquals("Novo", dao.getById("novo")!!.name)
        coVerify {
            api.create(CreateCreditCardRequestDto(name = "Novo", institution = "Banco", nickname = null, totalLimit = "100.00", closingDay = 20, dueDay = 5, timezone = null))
        }
    }

    @Test
    fun createNaoPersisteEmHttpError() = runTest {
        coEvery { api.create(any()) } returns Response.error(400, "bad".toResponseBody())

        val result = repository.create("X", "Y", null, "1.00", 1, 2, null)

        assertTrue(result is ApiResult.HttpError)
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun createNaoPersisteEmErroDeRede() = runTest {
        coEvery { api.create(any()) } throws java.io.IOException("offline")

        val result = repository.create("X", "Y", null, "1.00", 1, 2, null)

        assertTrue(result is ApiResult.NetworkError)
        assertTrue(dao.getAll().isEmpty())
    }

    // update

    @Test
    fun updatePersisteSomenteAposSucessoRemoto() = runTest {
        dao.upsert(entity(id = "u1", name = "Antigo"))
        // Resposta do backend reflete a atualização pedida
        coEvery { api.update("u1", any()) } returns Response.success(
            dto(id = "u1", name = "Novo").copy(nickname = "apelido", totalLimit = "500.00", timezone = "UTC")
        )

        val result = repository.update("u1", "Novo", "Banco", "apelido", "500.00", 10, 15, "UTC")

        assertTrue(result is ApiResult.Success)
        val got = dao.getById("u1")!!
        assertEquals("Novo", got.name)
        assertEquals("apelido", got.nickname)
        assertEquals("500.00", got.totalLimit)
        assertEquals("UTC", got.timezone)
    }

    @Test
    fun updateNaoPersisteEmHttpError() = runTest {
        dao.upsert(entity(id = "u1", name = "Antigo"))
        coEvery { api.update("u1", any()) } returns Response.error(404, "nf".toResponseBody())

        val result = repository.update("u1", "Novo", "Banco", null, "5.00", 10, 15, null)

        assertTrue(result is ApiResult.HttpError)
        assertEquals("Antigo", dao.getById("u1")!!.name)
    }

    // activate / deactivate — nunca apagam dados locais

    @Test
    fun deactivatePersisteSomenteAposSucessoRemoto() = runTest {
        dao.upsert(entity(id = "d1", status = "ACTIVE"))
        coEvery { api.deactivate("d1") } returns Response.success(dto(id = "d1", status = "INACTIVE"))

        val result = repository.deactivate("d1")

        assertTrue(result is ApiResult.Success)
        assertEquals("INACTIVE", dao.getById("d1")!!.status)
        // dado local foi atualizado, não apagado
        assertEquals(1, dao.getAll().size)
    }

    @Test
    fun deactivateNaoPersisteEmErroDeRede() = runTest {
        dao.upsert(entity(id = "d1", status = "ACTIVE"))
        coEvery { api.deactivate("d1") } throws java.io.IOException("offline")

        val result = repository.deactivate("d1")

        assertTrue(result is ApiResult.NetworkError)
        assertEquals("ACTIVE", dao.getById("d1")!!.status)
    }

    @Test
    fun activatePersisteSomenteAposSucessoRemoto() = runTest {
        dao.upsert(entity(id = "a1", status = "INACTIVE"))
        coEvery { api.activate("a1") } returns Response.success(dto(id = "a1", status = "ACTIVE"))

        val result = repository.activate("a1")

        assertTrue(result is ApiResult.Success)
        assertEquals("ACTIVE", dao.getById("a1")!!.status)
    }

    @Test
    fun activateEmHttpErrorNaoMudaStatusLocal() = runTest {
        dao.upsert(entity(id = "a1", status = "INACTIVE"))
        coEvery { api.activate("a1") } returns Response.error(409, "conflict".toResponseBody())

        val result = repository.activate("a1")

        assertTrue(result is ApiResult.HttpError)
        assertEquals("INACTIVE", dao.getById("a1")!!.status)
    }

    // compras/faturas preservadas: desativar não apaga nada do cache

    @Test
    fun desativarTodosOsCartoesNaoApagaCache() = runTest {
        dao.upsert(entity(id = "x1", status = "ACTIVE"))
        dao.upsert(entity(id = "x2", status = "INACTIVE"))
        coEvery { api.deactivate(any()) } returns Response.success(dto(id = "x1", status = "INACTIVE"))

        val result = repository.deactivate("x1")

        assertTrue(result is ApiResult.Success)
        // Cache continua com os 2 cartões; só o alvo mudou de status.
        // Desativar não apaga compras/faturas existentes associadas aos cartões.
        assertEquals(2, dao.getAll().size)
        assertEquals(setOf("x1", "x2"), dao.getAll().map { it.id }.toSet())
        assertEquals("INACTIVE", dao.getById("x1")!!.status)
        assertEquals("INACTIVE", dao.getById("x2")!!.status) // não tocado
    }
}
