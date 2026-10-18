package br.com.equilibra.mobile.data.local.account

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
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
class AccountDaoTest {

    private lateinit var db: EquilibraDatabase
    private lateinit var dao: AccountDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EquilibraDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.accountDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entity(
        id: String = "uuid-$idCounter",
        name: String = "Conta $idCounter",
        type: String = "CHECKING",
        initialBalance: String = "123.45",
        currentBalance: String = "123.45",
        active: Boolean = true
    ) = AccountEntity(
        id = id,
        name = name,
        type = type,
        initialBalance = initialBalance,
        currentBalance = currentBalance,
        active = active,
        createdAt = "2024-01-01T12:00:00Z",
        updatedAt = "2024-01-01T12:00:00Z",
        syncedAt = null
    )

    @Test
    fun upsertAndGetById() = runTest {
        val e = entity(id = "a1", name = "Conta A")
        dao.upsert(e)

        val got = dao.getById("a1")
        assertNotNull(got)
        assertEquals("Conta A", got!!.name)
        assertEquals("a1", got.id)
    }

    @Test
    fun getAllReturnsOrderedByName() = runTest {
        dao.upsert(entity(id = "2", name = "Zebra"))
        dao.upsert(entity(id = "1", name = "Alpha"))
        dao.upsert(entity(id = "3", name = "Meio"))

        val all = dao.getAll()
        assertEquals(listOf("Alpha", "Meio", "Zebra"), all.map { it.name })
    }

    @Test
    fun observeAllEmitsOrdered() = runTest {
        dao.upsert(entity(id = "1", name = "B"))
        dao.upsert(entity(id = "2", name = "A"))

        val first = dao.observeAll().first()
        assertEquals("A", first[0].name)
        assertEquals("B", first[1].name)
    }

    @Test
    fun getByIdReturnsNullWhenMissing() = runTest {
        assertNull(dao.getById("missing"))
    }

    @Test
    fun upsertAllReplacesExisting() = runTest {
        val a1 = entity(id = "1", name = "Old", initialBalance = "10.00", currentBalance = "10.00")
        dao.upsert(a1)

        val updated = a1.copy(name = "New", initialBalance = "99.99", currentBalance = "99.99")
        dao.upsertAll(listOf(updated))

        val got = dao.getById("1")!!
        assertEquals("New", got.name)
        assertEquals("99.99", got.initialBalance)
        assertEquals("99.99", got.currentBalance)
    }

    @Test
    fun deleteNotInKeepsOnlyGivenIds() = runTest {
        dao.upsert(entity(id = "1"))
        dao.upsert(entity(id = "2"))
        dao.upsert(entity(id = "3"))

        dao.deleteNotIn(listOf("1", "3"))

        val all = dao.getAll()
        assertEquals(2, all.size)
        assertTrue(all.all { it.id == "1" || it.id == "3" })
    }

    @Test
    fun deleteByIdRemovesOne() = runTest {
        dao.upsert(entity(id = "1"))
        dao.upsert(entity(id = "2"))

        dao.deleteById("1")

        assertNull(dao.getById("1"))
        assertNotNull(dao.getById("2"))
    }

    @Test
    fun clearAllRemovesEverything() = runTest {
        dao.upsert(entity(id = "1"))
        dao.upsert(entity(id = "2"))

        dao.clearAll()

        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun observeByIdEmits() = runTest {
        val e = entity(id = "obs1", name = "Obs")
        dao.upsert(e)

        val v = dao.observeById("obs1").first()
        assertEquals("Obs", v!!.name)
    }

    @Test
    fun bigDecimalPrecisionPreserved() = runTest {
        // BigDecimal como String não perde precisão
        val balance = "123456789.12"
        val e = entity(id = "precise", initialBalance = balance, currentBalance = balance)
        dao.upsert(e)

        val got = dao.getById("precise")!!
        assertEquals(balance, got.initialBalance)
        assertEquals(balance, got.currentBalance)
        // Também verifica que parseia corretamente
        assertEquals(0, BigDecimal(balance).compareTo(BigDecimal(got.initialBalance)))
    }

    @Test
    fun syncedAtCanBeNullAndUpdated() = runTest {
        val e = entity(id = "sync1")
        dao.upsert(e)
        assertNull(dao.getById("sync1")!!.syncedAt)

        dao.upsert(e.copy(syncedAt = 1_700_000_000_000L))
        assertEquals(1_700_000_000_000L, dao.getById("sync1")!!.syncedAt)
    }

    companion object {
        private var idCounter = 0
    }
}
