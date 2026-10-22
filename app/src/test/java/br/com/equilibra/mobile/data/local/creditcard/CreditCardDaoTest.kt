package br.com.equilibra.mobile.data.local.creditcard

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
class CreditCardDaoTest {

    private lateinit var db: EquilibraDatabase
    private lateinit var dao: CreditCardDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EquilibraDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.creditCardDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entity(
        id: String,
        name: String = "Cartão $id",
        status: String = "ACTIVE",
        totalLimit: String = "1000.00",
        committedLimit: String = "0.00",
        availableLimit: String = "1000.00"
    ) = CreditCardEntity(
        id = id,
        name = name,
        institution = "Banco",
        nickname = null,
        totalLimit = totalLimit,
        committedLimit = committedLimit,
        reservedLimit = "0.00",
        availableLimit = availableLimit,
        closingDay = 20,
        dueDay = 5,
        timezone = "America/Sao_Paulo",
        status = status,
        createdAt = "2026-10-22T00:00:00Z",
        updatedAt = "2026-10-22T00:00:00Z",
        syncedAt = null
    )

    @Test
    fun upsertAndGetById() = runTest {
        val e = entity(id = "c1", name = "Nubank")
        dao.upsert(e)

        val got = dao.getById("c1")
        assertNotNull(got)
        assertEquals("Nubank", got!!.name)
        assertEquals("c1", got.id)
        // Decimal em String, sem perda
        assertEquals("1000.00", got.totalLimit)
    }

    @Test
    fun upsertReplacesByNameKey() = runTest {
        dao.upsert(entity(id = "c1", name = "Cartão A", totalLimit = "100.00"))
        dao.upsert(entity(id = "c1", name = "Cartão A", totalLimit = "200.00"))

        val all = dao.getAll()
        assertEquals(1, all.size)
        assertEquals("200.00", all[0].totalLimit)
    }

    @Test
    fun getAllReturnsOrderedByName() = runTest {
        dao.upsert(entity(id = "2", name = "Zeta"))
        dao.upsert(entity(id = "1", name = "Alpha"))
        dao.upsert(entity(id = "3", name = "Meio"))

        val all = dao.getAll()
        assertEquals(listOf("Alpha", "Meio", "Zeta"), all.map { it.name })
    }

    @Test
    fun observeAllEmitsOrdered() = runTest {
        dao.upsert(entity(id = "2", name = "Zeta"))
        dao.upsert(entity(id = "1", name = "Alpha"))

        val flow = dao.observeAll().first()
        assertEquals(listOf("Alpha", "Zeta"), flow.map { it.name })
    }

    @Test
    fun observeActiveFiltersInactive() = runTest {
        dao.upsert(entity(id = "1", name = "Ativo", status = "ACTIVE"))
        dao.upsert(entity(id = "2", name = "Inativo", status = "INACTIVE"))

        val active = dao.getActive()
        assertEquals(listOf("Ativo"), active.map { it.name })

        val flowActive = dao.observeActive().first()
        assertEquals(listOf("Ativo"), flowActive.map { it.name })
    }

    @Test
    fun getByIdUnknownReturnsNull() = runTest {
        assertNull(dao.getById("inexiste"))
    }

    @Test
    fun observeByIdReturnsEntity() = runTest {
        dao.upsert(entity(id = "c9", name = "Inter"))
        val got = dao.observeById("c9").first()
        assertNotNull(got)
        assertEquals("Inter", got!!.name)
    }

    @Test
    fun upsertAllAddsAndRefreshesWithoutDeletingUnsent() = runTest {
        // REPLACE não apaga o que não foi reenviado: old1 sobrevive, new1/new2 entram.
        dao.upsert(entity(id = "old1"))
        dao.upsertAll(listOf(entity(id = "new1"), entity(id = "new2")))

        val all = dao.getAll()
        assertEquals(3, all.size)
        assertEquals(setOf("old1", "new1", "new2"), all.map { it.id }.toSet())
    }

    @Test
    fun deleteNotInSafeRemovesOutdated() = runTest {
        dao.upsert(entity(id = "k1"))
        dao.upsert(entity(id = "stale"))
        dao.deleteNotInSafe(listOf("k1"))

        val all = dao.getAll()
        assertEquals(listOf("k1"), all.map { it.id })
    }

    @Test
    fun deleteNotInSafeWithEmptyListClearsAll() = runTest {
        dao.upsert(entity(id = "k1"))
        dao.upsert(entity(id = "k2"))
        dao.deleteNotInSafe(emptyList())

        val all = dao.getAll()
        assertTrue(all.isEmpty())
    }

    @Test
    fun deleteByIdRemovesEntity() = runTest {
        dao.upsert(entity(id = "c1"))
        dao.deleteById("c1")
        assertNull(dao.getById("c1"))
    }

    @Test
    fun clearAllRemovesEverything() = runTest {
        dao.upsert(entity(id = "c1"))
        dao.upsert(entity(id = "c2"))
        dao.clearAll()
        assertTrue(dao.getAll().isEmpty())
    }
}
