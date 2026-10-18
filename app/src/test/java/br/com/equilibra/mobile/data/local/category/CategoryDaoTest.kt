package br.com.equilibra.mobile.data.local.category

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
class CategoryDaoTest {

    private lateinit var db: EquilibraDatabase
    private lateinit var dao: CategoryDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EquilibraDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.categoryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entity(
        id: String = "cat-$seq",
        name: String = "Cat $seq",
        applicability: String = "EXPENSE",
        active: Boolean = true
    ) = CategoryEntity(
        id = id, name = name, applicability = applicability, active = active,
        createdAt = "2024-01-01T00:00:00Z", updatedAt = "2024-01-01T00:00:00Z"
    )

    @Test
    fun upsertAndGetById() = runTest {
        dao.upsert(entity(id = "c1", name = "Alimentação"))

        val got = dao.getById("c1")
        assertNotNull(got)
        assertEquals("Alimentação", got!!.name)
        assertEquals("EXPENSE", got.applicability)
    }

    @Test
    fun getAllOrdenadoPorNome() = runTest {
        dao.upsertAll(listOf(
            entity(id = "2", name = "Zebra"),
            entity(id = "1", name = "alface"),
            entity(id = "3", name = "Morango")
        ))

        // COLLATE NOCASE → ordem alfabética ignorando caixa
        assertEquals(listOf("alface", "Morango", "Zebra"), dao.getAll().map { it.name })
    }

    @Test
    fun observeAllEmiteOrdenado() = runTest {
        dao.upsert(entity(id = "1", name = "B"))
        dao.upsert(entity(id = "2", name = "A"))

        val list = dao.observeAll().first()
        assertEquals("A", list[0].name)
    }

    @Test
    fun upsertAllSubstituiExistentes() = runTest {
        dao.upsert(entity(id = "1", name = "Old"))
        dao.upsertAll(listOf(entity(id = "1", name = "New")))

        assertEquals("New", dao.getById("1")!!.name)
    }

    @Test
    fun deleteNotInSafeComListaVaziaLimpaTudo() = runTest {
        dao.upsert(entity(id = "1"))
        dao.upsert(entity(id = "2"))

        dao.deleteNotInSafe(emptyList())

        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun deleteNotInSafeRemoveForaDaLista() = runTest {
        dao.upsert(entity(id = "1"))
        dao.upsert(entity(id = "2"))
        dao.upsert(entity(id = "3"))

        dao.deleteNotInSafe(listOf("1", "3"))

        assertEquals(setOf("1", "3"), dao.getAll().map { it.id }.toSet())
    }

    @Test
    fun deleteByIdEClearAll() = runTest {
        dao.upsert(entity(id = "1"))
        dao.upsert(entity(id = "2"))
        dao.deleteById("1")
        assertNull(dao.getById("1"))

        dao.clearAll()
        assertTrue(dao.getAll().isEmpty())
    }

    // Filtro de aplicabilidade (estilo backend)

    @Test
    fun observeByApplicabilityExpenseIncluiBoth() = runTest {
        dao.upsertAll(listOf(
            entity(id = "1", name = "Mercado", applicability = "EXPENSE"),
            entity(id = "2", name = "Salário", applicability = "INCOME"),
            entity(id = "3", name = "Flexível", applicability = "BOTH")
        ))

        val expense = dao.observeByApplicability("EXPENSE").first()
        assertEquals(setOf("Mercado", "Flexível"), expense.map { it.name }.toSet())
    }

    @Test
    fun observeByApplicabilityIncomeIncluiBoth() = runTest {
        dao.upsertAll(listOf(
            entity(id = "1", name = "Mercado", applicability = "EXPENSE"),
            entity(id = "2", name = "Salário", applicability = "INCOME"),
            entity(id = "3", name = "Flexível", applicability = "BOTH")
        ))

        val income = dao.observeByApplicability("INCOME").first()
        assertEquals(setOf("Salário", "Flexível"), income.map { it.name }.toSet())
    }

    @Test
    fun observeByApplicabilityBothSoOnlyBoth() = runTest {
        dao.upsertAll(listOf(
            entity(id = "1", name = "Mercado", applicability = "EXPENSE"),
            entity(id = "3", name = "Flexível", applicability = "BOTH")
        ))

        val both = dao.observeByApplicability("BOTH").first()
        assertEquals(listOf("Flexível"), both.map { it.name })
    }

    @Test
    fun observeByApplicabilityExcluiInativas() = runTest {
        dao.upsertAll(listOf(
            entity(id = "1", name = "Ativa", applicability = "EXPENSE", active = true),
            entity(id = "2", name = "Inativa", applicability = "EXPENSE", active = false)
        ))

        val list = dao.observeByApplicability("EXPENSE").first()
        assertEquals(listOf("Ativa"), list.map { it.name })
    }

    @Test
    fun syncedAtNullENullable() = runTest {
        dao.upsert(entity(id = "s1"))
        assertNull(dao.getById("s1")!!.syncedAt)

        val e = dao.getById("s1")!!
        dao.upsert(e.copy(syncedAt = 1_700_000_000_000L))
        assertEquals(1_700_000_000_000L, dao.getById("s1")!!.syncedAt)
    }

    companion object {
        private var seq = 0
    }
}
