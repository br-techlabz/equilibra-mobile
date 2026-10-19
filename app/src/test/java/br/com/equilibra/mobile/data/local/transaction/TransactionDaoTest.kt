package br.com.equilibra.mobile.data.local.transaction

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.equilibra.mobile.data.local.EquilibraDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TransactionDaoTest {

    private lateinit var db: EquilibraDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EquilibraDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.transactionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entity(
        id: String,
        type: String = "EXPENSE",
        status: String = "ACTIVE",
        description: String = "Transação $id",
        occurredAt: String = "2026-10-01T12:00:00Z",
        amount: String = "10.50",
        categoryId: String? = "cat-1",
        sourceAccountId: String? = "acc-1",
        destinationAccountId: String? = null,
        notes: String? = null
    ) = TransactionEntity(
        id = id,
        type = type,
        status = status,
        description = description,
        occurredAt = occurredAt,
        amount = amount,
        categoryId = categoryId,
        sourceAccountId = sourceAccountId,
        destinationAccountId = destinationAccountId,
        notes = notes,
        createdAt = occurredAt,
        updatedAt = occurredAt
    )

    @Test
    fun upsertAndGetById() = runTest {
        dao.upsert(entity(id = "t1", description = "Mercado"))

        val got = dao.getById("t1")
        assertNotNull(got)
        assertEquals("Mercado", got!!.description)
        assertEquals("EXPENSE", got.type)
        assertEquals("ACTIVE", got.status)
    }

    @Test
    fun observeRecentOrdenaPorDataDescERespeitaLimit() = runTest {
        dao.upsertAll(listOf(
            entity(id = "1", occurredAt = "2026-10-01T00:00:00Z"),
            entity(id = "3", occurredAt = "2026-10-03T00:00:00Z"),
            entity(id = "2", occurredAt = "2026-10-02T00:00:00Z"),
            entity(id = "4", occurredAt = "2026-10-04T00:00:00Z")
        ))

        val recent = dao.observeRecent(limit = 3).first()

        assertEquals(listOf("4", "3", "2"), recent.map { it.id })
    }

    @Test
    fun observeByTypeFiltraPorTipo() = runTest {
        dao.upsertAll(listOf(
            entity(id = "e1", type = "EXPENSE"),
            entity(id = "i1", type = "INCOME"),
            entity(id = "t1", type = "TRANSFER"),
            entity(id = "e2", type = "EXPENSE")
        ))

        assertEquals(listOf("e1", "e2"), dao.observeByType("EXPENSE").first().map { it.id })
        assertEquals(listOf("i1"), dao.observeByType("INCOME").first().map { it.id })
        assertEquals(listOf("t1"), dao.observeByType("TRANSFER").first().map { it.id })
    }

    @Test
    fun observeByTypeOrdenaPorDataDesc() = runTest {
        dao.upsertAll(listOf(
            entity(id = "1", type = "EXPENSE", occurredAt = "2026-10-01T00:00:00Z"),
            entity(id = "2", type = "EXPENSE", occurredAt = "2026-10-05T00:00:00Z"),
            entity(id = "3", type = "EXPENSE", occurredAt = "2026-10-03T00:00:00Z")
        ))

        assertEquals(listOf("2", "3", "1"), dao.observeByType("EXPENSE").first().map { it.id })
    }

    @Test
    fun getAllRetornaTudo() = runTest {
        dao.upsertAll(listOf(entity(id = "1"), entity(id = "2")))
        assertEquals(2, dao.getAll().size)
    }

    @Test
    fun upsertAllSubstituiExistentes() = runTest {
        dao.upsert(entity(id = "1", description = "Old"))
        dao.upsertAll(listOf(entity(id = "1", description = "New")))

        assertEquals("New", dao.getById("1")!!.description)
    }

    @Test
    fun trimToRecentMantemApenasOs100MaisRecentes() = runTest {
        // 105 transações com datas únicas e crescentes: id maior = mais recente
        val entities = (1..105).map { i ->
            entity(
                id = "id-%03d".format(i),
                occurredAt = java.time.Instant.ofEpochSecond(1_700_000_000L + i * 3600L).toString()
            )
        }
        dao.upsertAll(entities)
        dao.trimToRecent(limit = 100)

        val all = dao.getAll()
        assertEquals(100, all.size)
        // As 5 removidas são as mais antigas (ids menores)
        val remainingIds = all.map { it.id }.toSet()
        assertTrue(remainingIds.contains("id-105"))
        assertTrue(!remainingIds.contains("id-001"))
        assertTrue(!remainingIds.contains("id-005"))
        assertTrue(remainingIds.contains("id-006"))
    }

    @Test
    fun camposOpcionaisAceitamNulo() = runTest {
        dao.upsert(
            entity(
                id = "1",
                categoryId = null,
                sourceAccountId = null,
                destinationAccountId = null,
                notes = null
            )
        )

        val got = dao.getById("1")!!
        assertNull(got.categoryId)
        assertNull(got.sourceAccountId)
        assertNull(got.destinationAccountId)
        assertNull(got.notes)
        assertNull(got.syncedAt)
    }

    @Test
    fun amountStringSemPerdaDePrecisao() = runTest {
        dao.upsert(entity(id = "1", amount = "1234567890.123456789"))
        dao.upsert(entity(id = "2", amount = "0.10"))

        assertEquals("1234567890.123456789", dao.getById("1")!!.amount)
        assertEquals("0.10", dao.getById("2")!!.amount)
    }

    @Test
    fun deleteNotInSafeComListaVaziaLimpaTudo() = runTest {
        dao.upsertAll(listOf(entity(id = "1"), entity(id = "2")))

        dao.deleteNotInSafe(emptyList())

        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun deleteNotInSafeRemoveForaDaLista() = runTest {
        dao.upsertAll(listOf(entity(id = "1"), entity(id = "2"), entity(id = "3")))

        dao.deleteNotInSafe(listOf("1", "3"))

        assertEquals(setOf("1", "3"), dao.getAll().map { it.id }.toSet())
    }

    @Test
    fun deleteByIdEClearAll() = runTest {
        dao.upsertAll(listOf(entity(id = "1"), entity(id = "2")))

        dao.deleteById("1")
        assertNull(dao.getById("1"))

        dao.clearAll()
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun syncedAtAtualizavel() = runTest {
        dao.upsert(entity(id = "1"))
        assertNull(dao.getById("1")!!.syncedAt)

        dao.upsert(dao.getById("1")!!.copy(syncedAt = 1_700_000_000_000L))
        assertEquals(1_700_000_000_000L, dao.getById("1")!!.syncedAt)
    }
}
