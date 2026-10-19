package br.com.equilibra.mobile.data.local.tag

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
class TagDaoTest {

    private lateinit var db: EquilibraDatabase
    private lateinit var dao: TagDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EquilibraDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.tagDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entity(
        id: String = "tag-$seq",
        name: String = "Tag $seq",
        active: Boolean = true
    ) = TagEntity(
        id = id, name = name, active = active,
        createdAt = "2024-01-01T00:00:00Z", updatedAt = "2024-01-01T00:00:00Z"
    )

    @Test
    fun upsertAndGetById() = runTest {
        dao.upsert(entity(id = "t1", name = "URGENTE"))

        val got = dao.getById("t1")
        assertNotNull(got)
        assertEquals("URGENTE", got!!.name)
        assertTrue(got.active)
    }

    @Test
    fun getAllOrdenadoPorNome() = runTest {
        dao.upsertAll(listOf(
            entity(id = "2", name = "Zebra"),
            entity(id = "1", name = "aniversario"),
            entity(id = "3", name = "Morango")
        ))

        // COLLATE NOCASE → ordem alfabética ignorando caixa
        assertEquals(listOf("aniversario", "Morango", "Zebra"), dao.getAll().map { it.name })
    }

    @Test
    fun observeAllEmiteOrdenado() = runTest {
        dao.upsert(entity(id = "1", name = "B"))
        dao.upsert(entity(id = "2", name = "A"))

        val list = dao.observeAll().first()
        assertEquals("A", list[0].name)
    }

    @Test
    fun observeByIdEmite() = runTest {
        dao.upsert(entity(id = "1", name = "Antes"))

        assertEquals("Antes", dao.observeById("1").first()!!.name)

        dao.upsert(entity(id = "1", name = "Depois"))
        assertEquals("Depois", dao.observeById("1").first()!!.name)
    }

    @Test
    fun upsertAllSubstituiExistentes() = runTest {
        dao.upsert(entity(id = "1", name = "Old"))
        dao.upsertAll(listOf(entity(id = "1", name = "New")))

        assertEquals("New", dao.getById("1")!!.name)
    }

    @Test
    fun activeFlagPersisteDesativacao() = runTest {
        dao.upsert(entity(id = "1", name = "Ativa", active = true))
        dao.upsert(dao.getById("1")!!.copy(active = false, updatedAt = "2024-02-01T00:00:00Z"))

        val got = dao.getById("1")!!
        assertEquals(false, got.active)
        assertEquals("2024-02-01T00:00:00Z", got.updatedAt)

        dao.upsert(got.copy(active = true))
        assertTrue(dao.getById("1")!!.active)
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
