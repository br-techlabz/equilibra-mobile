package br.com.equilibra.mobile.data.repository

import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.TransactionHistoryApi
import br.com.equilibra.mobile.data.remote.dto.transaction.HistoryItemDto
import br.com.equilibra.mobile.data.remote.dto.transaction.HistoryPageResponseDto
import br.com.equilibra.mobile.data.remote.dto.transaction.TagSummaryDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class TransactionHistoryRepositoryTest {

    private lateinit var api: TransactionHistoryApi
    private lateinit var repository: TransactionHistoryRepository

    @Before
    fun setUp() {
        api = mockk()
        repository = TransactionHistoryRepository(api)
    }

    private fun item(
        id: String = "h1",
        type: String = "EXPENSE",
        status: String = "ACTIVE",
        description: String = "Histórico $id"
    ) = HistoryItemDto(
        id = id, type = type, status = status, description = description,
        occurredAt = "2026-10-01T12:00:00Z", amount = "10.50",
        categoryId = "cat-1", sourceAccountId = "acc-1", destinationAccountId = null,
        notes = null, createdAt = "2026-10-01T12:00:00Z", updatedAt = "2026-10-01T12:00:00Z",
        tags = listOf(TagSummaryDto("t1", "URGENTE", true)), attachmentCount = 0
    )

    private fun page(items: List<HistoryItemDto>, page: Int = 0, totalPages: Int = 1, totalElements: Long = items.size.toLong()) =
        HistoryPageResponseDto(items, page, 20, totalElements, totalPages)

    @Test
    fun `sucesso mapeia para entidades sem persistir`() = runTest {
        coEvery { api.list(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            Response.success(page(listOf(item())))

        val result = repository.list(page = 0, filters = HistoryFilters.empty())

        assertTrue(result is ApiResult.Success)
        val txPage = (result as ApiResult.Success).data
        val entity = txPage.items[0]
        assertEquals("EXPENSE", entity.type)
        assertEquals("ACTIVE", entity.status)
        assertEquals("10.50", entity.amount)
        assertEquals("acc-1", entity.sourceAccountId)
        // Sem cache: o repositório não tem DAO — nada é persistido por definição
        assertNull(entity.syncedAt)
    }

    @Test
    fun `filtros combinados sao repassados integralmente`() = runTest {
        coEvery { api.list(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            Response.success(page(emptyList()))
        val filters = HistoryFilters(
            type = "EXPENSE",
            status = "ACTIVE",
            fromIso = "2026-10-01T00:00:00Z",
            toIso = "2026-10-31T23:59:59.999999999Z",
            accountId = "acc-1",
            categoryId = "cat-1",
            tagIds = listOf("t1", "t2")
        )

        repository.list(page = 2, filters = filters)

        coVerify(exactly = 1) {
            api.list(
                page = 2,
                size = 20,
                type = "EXPENSE",
                status = "ACTIVE",
                from = "2026-10-01T00:00:00Z",
                to = "2026-10-31T23:59:59.999999999Z",
                accountId = "acc-1",
                categoryId = "cat-1",
                tagIds = listOf("t1", "t2")
            )
        }
    }

    @Test
    fun `sem filtros omite parametros nulos`() = runTest {
        coEvery { api.list(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            Response.success(page(emptyList()))

        repository.list(page = 0, filters = HistoryFilters.empty())

        coVerify(exactly = 1) {
            api.list(
                page = 0,
                size = 20,
                type = null,
                status = null,
                from = null,
                to = null,
                accountId = null,
                categoryId = null,
                tagIds = null
            )
        }
    }

    @Test
    fun `erro de rede propaga sem tocar em cache`() = runTest {
        coEvery { api.list(any(), any(), any(), any(), any(), any(), any(), any(), any()) } throws
            IOException("offline")

        val result = repository.list(page = 0, filters = HistoryFilters.empty())

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `erro http propaga codigo`() = runTest {
        coEvery { api.list(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            Response.error(400, "bad".toResponseBody())

        val result = repository.list(page = 0, filters = HistoryFilters.empty())

        assertTrue(result is ApiResult.HttpError)
        assertEquals(400, (result as ApiResult.HttpError).code)
    }
}
