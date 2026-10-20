package br.com.equilibra.mobile.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.equilibra.mobile.data.local.EquilibraDatabase
import br.com.equilibra.mobile.data.local.transaction.TransactionDao
import br.com.equilibra.mobile.data.local.transaction.TransactionEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.ExpenseApi
import br.com.equilibra.mobile.data.remote.IncomeApi
import br.com.equilibra.mobile.data.remote.TransferApi
import br.com.equilibra.mobile.data.remote.dto.transaction.CreateExpenseRequestDto
import br.com.equilibra.mobile.data.remote.dto.transaction.CreateIncomeRequestDto
import br.com.equilibra.mobile.data.remote.dto.transaction.CreateTransferRequestDto
import br.com.equilibra.mobile.data.remote.dto.transaction.ExpenseDto
import br.com.equilibra.mobile.data.remote.dto.transaction.ExpensePageResponseDto
import br.com.equilibra.mobile.data.remote.dto.transaction.IncomeDto
import br.com.equilibra.mobile.data.remote.dto.transaction.IncomePageResponseDto
import br.com.equilibra.mobile.data.remote.dto.transaction.TagSummaryDto
import br.com.equilibra.mobile.data.remote.dto.transaction.TransferDto
import br.com.equilibra.mobile.data.remote.dto.transaction.TransferPageResponseDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class TransactionRepositoryTest {

    private lateinit var db: EquilibraDatabase
    private lateinit var dao: TransactionDao
    private lateinit var expenseApi: ExpenseApi
    private lateinit var incomeApi: IncomeApi
    private lateinit var transferApi: TransferApi
    private lateinit var repository: TransactionRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EquilibraDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.transactionDao()
        expenseApi = mockk()
        incomeApi = mockk()
        transferApi = mockk()
        repository = TransactionRepository(expenseApi, incomeApi, transferApi, dao)
        // Padrão: APIs vazias com sucesso — testes que precisam de dado sobrescrevem
        coEvery { expenseApi.list(any(), any(), any()) } returns Response.success(emptyPage())
        coEvery { incomeApi.list(any(), any(), any()) } returns Response.success(emptyIncomePage())
        coEvery { transferApi.list(any(), any(), any()) } returns Response.success(emptyTransferPage())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun emptyPage() = ExpensePageResponseDto(emptyList(), 0, 20, 0, 0)
    private fun emptyIncomePage() = IncomePageResponseDto(emptyList(), 0, 20, 0, 0)
    private fun emptyTransferPage() = TransferPageResponseDto(emptyList(), 0, 20, 0, 0)

    private fun expenseDto(
        id: String = "e-$seq",
        description: String = "Despesa $seq",
        occurredAt: String = "2026-10-01T12:00:00Z",
        amount: String = "10.50",
        status: String = "ACTIVE"
    ) = ExpenseDto(
        id = id, description = description, occurredAt = occurredAt,
        accountId = "acc-1", amount = amount, categoryId = "cat-1", notes = null,
        paymentMethod = "BANK_ACCOUNT", creditCardId = null, installmentCount = null,
        cycleMonth = null, purchaseAt = null, status = status,
        createdAt = occurredAt, updatedAt = occurredAt, tags = emptyList()
    )

    private fun incomeDto(
        id: String = "i-$seq",
        description: String = "Receita $seq",
        occurredAt: String = "2026-10-02T12:00:00Z",
        amount: String = "100.00",
        status: String = "ACTIVE"
    ) = IncomeDto(
        id = id, description = description, occurredAt = occurredAt,
        accountId = "acc-2", amount = amount, categoryId = "cat-2", notes = null,
        status = status, createdAt = occurredAt, updatedAt = occurredAt, tags = emptyList()
    )

    private fun transferDto(
        id: String = "tr-$seq",
        description: String = "Transferência $seq",
        occurredAt: String = "2026-10-03T12:00:00Z",
        amount: String = "50.00",
        status: String = "ACTIVE"
    ) = TransferDto(
        id = id, description = description, occurredAt = occurredAt,
        sourceAccountId = "acc-1", destinationAccountId = "acc-2",
        amount = amount, categoryId = "cat-1", notes = null, status = status,
        createdAt = occurredAt, updatedAt = occurredAt, tags = emptyList()
    )

    // ---------- refresh — sincronização determinística ----------

    @Test
    fun refreshSincronizaOsTresTiposNoCache() = runTest {
        coEvery { expenseApi.list(any(), any(), any()) } returns
            Response.success(ExpensePageResponseDto(listOf(expenseDto(id = "e1")), 0, 100, 1, 1))
        coEvery { incomeApi.list(any(), any(), any()) } returns
            Response.success(IncomePageResponseDto(listOf(incomeDto(id = "i1")), 0, 100, 1, 1))
        coEvery { transferApi.list(any(), any(), any()) } returns
            Response.success(TransferPageResponseDto(listOf(transferDto(id = "tr1")), 0, 100, 1, 1))

        val result = repository.refresh()

        assertTrue(result is ApiResult.Success)
        val all = dao.getAll()
        assertEquals(3, all.size)
        // Mapeamento: expense → sourceAccountId; income → destinationAccountId; transfer → ambos
        assertEquals("acc-1", dao.getById("e1")!!.sourceAccountId)
        assertEquals(null, dao.getById("e1")!!.destinationAccountId)
        assertEquals(null, dao.getById("i1")!!.sourceAccountId)
        assertEquals("acc-2", dao.getById("i1")!!.destinationAccountId)
        assertEquals("acc-1", dao.getById("tr1")!!.sourceAccountId)
        assertEquals("acc-2", dao.getById("tr1")!!.destinationAccountId)
    }

    @Test
    fun refreshRemoveLocaisQueNaoExistemMaisNoServidor() = runTest {
        dao.upsertAll(listOf(
            TransactionEntity("e-old", "EXPENSE", "ACTIVE", "Antiga", "2026-09-01T00:00:00Z", "1.00",
                createdAt = "2026-09-01T00:00:00Z", updatedAt = "2026-09-01T00:00:00Z")
        ))
        coEvery { expenseApi.list(any(), any(), any()) } returns
            Response.success(ExpensePageResponseDto(listOf(expenseDto(id = "e-new")), 0, 100, 1, 1))

        repository.refresh("EXPENSE")

        assertEquals(listOf("e-new"), dao.getAll().map { it.id })
    }

    @Test
    fun refreshPorTipoNaoTocaOutrosTipos() = runTest {
        dao.upsertAll(listOf(
            TransactionEntity("i-keep", "INCOME", "ACTIVE", "Receita", "2026-09-01T00:00:00Z", "1.00",
                createdAt = "2026-09-01T00:00:00Z", updatedAt = "2026-09-01T00:00:00Z")
        ))
        coEvery { expenseApi.list(any(), any(), any()) } returns
            Response.success(ExpensePageResponseDto(listOf(expenseDto(id = "e1")), 0, 100, 1, 1))

        repository.refresh("EXPENSE")

        assertNotNull(dao.getById("i-keep"))
        assertNotNull(dao.getById("e1"))
    }

    @Test
    fun refreshNaoApagaCacheEmErroDeRede() = runTest {
        dao.upsert(TransactionEntity("e-local", "EXPENSE", "ACTIVE", "Local", "2026-09-01T00:00:00Z", "1.00",
            createdAt = "2026-09-01T00:00:00Z", updatedAt = "2026-09-01T00:00:00Z"))
        coEvery { expenseApi.list(any(), any(), any()) } throws IOException("offline")

        val result = repository.refresh("EXPENSE")

        assertTrue(result is ApiResult.NetworkError)
        assertEquals("Local", dao.getById("e-local")!!.description)
    }

    @Test
    fun refreshNaoApagaCacheEmHttpError() = runTest {
        dao.upsert(TransactionEntity("e-local", "EXPENSE", "ACTIVE", "Local", "2026-09-01T00:00:00Z", "1.00",
            createdAt = "2026-09-01T00:00:00Z", updatedAt = "2026-09-01T00:00:00Z"))
        coEvery { expenseApi.list(any(), any(), any()) } returns Response.error(500, "err".toResponseBody())

        val result = repository.refresh("EXPENSE")

        assertTrue(result is ApiResult.HttpError)
        assertEquals(1, dao.getAll().size)
    }

    @Test
    fun refreshParcialSucessoMantémOSincronizadoEReportaErro() = runTest {
        coEvery { expenseApi.list(any(), any(), any()) } returns
            Response.success(ExpensePageResponseDto(listOf(expenseDto(id = "e1")), 0, 100, 1, 1))
        coEvery { incomeApi.list(any(), any(), any()) } throws IOException("offline")

        val result = repository.refresh()

        // Primeiro erro reportado, mas o que sincronizou permanece
        assertTrue(result is ApiResult.NetworkError)
        assertNotNull(dao.getById("e1"))
    }

    @Test
    fun refreshAplicaTrimParaLimiteDoCache() = runTest {
        // 30 despesas + 30 receitas + 30 transferências = 90 → ok; depois forçar > 100
        val many = (1..40).map { expenseDto(id = "e-%03d".format(it), occurredAt = "2026-10-01T%02d:00:00Z".format(it % 24)) }
        coEvery { expenseApi.list(any(), any(), any()) } returns
            Response.success(ExpensePageResponseDto(many, 0, 100, 40, 1))
        val manyIncome = (1..40).map { incomeDto(id = "i-%03d".format(it), occurredAt = "2026-10-02T%02d:00:00Z".format(it % 24)) }
        coEvery { incomeApi.list(any(), any(), any()) } returns
            Response.success(IncomePageResponseDto(manyIncome, 0, 100, 40, 1))
        val manyTransfer = (1..40).map { transferDto(id = "tr-%03d".format(it), occurredAt = "2026-10-03T%02d:00:00Z".format(it % 24)) }
        coEvery { transferApi.list(any(), any(), any()) } returns
            Response.success(TransferPageResponseDto(manyTransfer, 0, 100, 40, 1))

        repository.refresh()

        assertEquals(100, dao.getAll().size)
    }

    @Test
    fun observeRecentEmiteCacheMesmoOffline() = runTest {
        dao.upsert(TransactionEntity("1", "EXPENSE", "ACTIVE", "Cache", "2026-09-01T00:00:00Z", "1.00",
            createdAt = "2026-09-01T00:00:00Z", updatedAt = "2026-09-01T00:00:00Z"))

        val list = repository.observeRecent().first()

        assertEquals(1, list.size)
        assertEquals("Cache", list[0].description)
    }

    // ---------- escritas — Room só após sucesso remoto ----------

    @Test
    fun createExpensePersisteNoRoomAposSucesso() = runTest {
        coEvery { expenseApi.create(any()) } returns Response.success(expenseDto(id = "new", description = "Nova"))

        val result = repository.createExpense(
            description = "Nova", occurredAt = "2026-10-01T12:00:00Z",
            accountId = "acc-1", amount = "10.50", categoryId = "cat-1"
        )

        assertTrue(result is ApiResult.Success)
        val entity = dao.getById("new")!!
        assertEquals("Nova", entity.description)
        assertEquals("EXPENSE", entity.type)
        assertEquals("acc-1", entity.sourceAccountId)
    }

    @Test
    fun createExpenseEnviaContratoEsperado() = runTest {
        coEvery { expenseApi.create(any()) } returns Response.success(expenseDto(id = "new"))

        repository.createExpense(
            description = "Nova", occurredAt = "2026-10-01T12:00:00Z",
            accountId = "acc-1", amount = "10.50", categoryId = "cat-1",
            notes = "obs", tagIds = listOf("t1")
        )

        val slot = slot<CreateExpenseRequestDto>()
        coVerify { expenseApi.create(capture(slot)) }
        assertEquals("Nova", slot.captured.description)
        assertEquals("acc-1", slot.captured.accountId)
        assertEquals("10.50", slot.captured.amount)
        assertEquals(listOf("t1"), slot.captured.tagIds)
    }

    @Test
    fun createExpenseNaoPersisteEmErroHttp() = runTest {
        coEvery { expenseApi.create(any()) } returns Response.error(400, "bad".toResponseBody())

        val result = repository.createExpense("X", "2026-10-01T00:00:00Z", "acc-1", "10.00", "cat-1")

        assertTrue(result is ApiResult.HttpError)
        assertEquals(0, dao.getAll().size)
    }

    @Test
    fun createExpenseNaoPersisteEmNetworkError() = runTest {
        coEvery { expenseApi.create(any()) } throws IOException("offline")

        val result = repository.createExpense("X", "2026-10-01T00:00:00Z", "acc-1", "10.00", "cat-1")

        assertTrue(result is ApiResult.NetworkError)
        assertEquals(0, dao.getAll().size)
    }

    @Test
    fun updateExpensePersisteNoRoomAposSucesso() = runTest {
        dao.upsert(TransactionEntity("1", "EXPENSE", "ACTIVE", "Old", "2026-10-01T00:00:00Z", "1.00",
            createdAt = "2026-10-01T00:00:00Z", updatedAt = "2026-10-01T00:00:00Z"))
        coEvery { expenseApi.update(any(), any()) } returns
            Response.success(expenseDto(id = "1", description = "New", amount = "20.00"))

        val result = repository.updateExpense(
            id = "1", description = "New", occurredAt = "2026-10-01T00:00:00Z",
            accountId = "acc-1", amount = "20.00", categoryId = "cat-1"
        )

        assertTrue(result is ApiResult.Success)
        assertEquals("New", dao.getById("1")!!.description)
        assertEquals("20.00", dao.getById("1")!!.amount)
    }

    @Test
    fun updateExpenseNaoPersisteEmErro() = runTest {
        dao.upsert(TransactionEntity("1", "EXPENSE", "ACTIVE", "Old", "2026-10-01T00:00:00Z", "1.00",
            createdAt = "2026-10-01T00:00:00Z", updatedAt = "2026-10-01T00:00:00Z"))
        coEvery { expenseApi.update(any(), any()) } returns Response.error(500, "err".toResponseBody())

        val result = repository.updateExpense(
            id = "1", description = "New", occurredAt = "2026-10-01T00:00:00Z",
            accountId = "acc-1", amount = "20.00", categoryId = "cat-1"
        )

        assertTrue(result is ApiResult.HttpError)
        assertEquals("Old", dao.getById("1")!!.description)
    }

    @Test
    fun cancelExpenseMarcaCanceladaNoRoomAposSucesso() = runTest {
        dao.upsert(TransactionEntity("1", "EXPENSE", "ACTIVE", "A", "2026-10-01T00:00:00Z", "1.00",
            createdAt = "2026-10-01T00:00:00Z", updatedAt = "2026-10-01T00:00:00Z"))
        coEvery { expenseApi.cancel("1") } returns
            Response.success(expenseDto(id = "1", status = "CANCELLED"))

        val result = repository.cancelExpense("1")

        assertTrue(result is ApiResult.Success)
        assertEquals("CANCELLED", dao.getById("1")!!.status)
    }

    @Test
    fun cancelExpenseNaoPersisteEmErro() = runTest {
        dao.upsert(TransactionEntity("1", "EXPENSE", "ACTIVE", "A", "2026-10-01T00:00:00Z", "1.00",
            createdAt = "2026-10-01T00:00:00Z", updatedAt = "2026-10-01T00:00:00Z"))
        coEvery { expenseApi.cancel("1") } returns Response.error(409, "err".toResponseBody())

        val result = repository.cancelExpense("1")

        assertTrue(result is ApiResult.HttpError)
        assertEquals("ACTIVE", dao.getById("1")!!.status)
    }

    @Test
    fun createIncomePersisteNoRoomAposSucesso() = runTest {
        coEvery { incomeApi.create(any()) } returns Response.success(incomeDto(id = "i-new", description = "Salário"))

        val result = repository.createIncome(
            description = "Salário", occurredAt = "2026-10-05T00:00:00Z",
            accountId = "acc-2", amount = "5000.00", categoryId = "cat-2"
        )

        assertTrue(result is ApiResult.Success)
        val entity = dao.getById("i-new")!!
        assertEquals("INCOME", entity.type)
        assertEquals("acc-2", entity.destinationAccountId)
    }

    @Test
    fun createIncomeEnviaContratoEsperado() = runTest {
        coEvery { incomeApi.create(any()) } returns Response.success(incomeDto(id = "i-new"))

        repository.createIncome("Salário", "2026-10-05T00:00:00Z", "acc-2", "5000.00", "cat-2")

        val slot = slot<CreateIncomeRequestDto>()
        coVerify { incomeApi.create(capture(slot)) }
        assertEquals("acc-2", slot.captured.accountId)
        assertEquals("5000.00", slot.captured.amount)
    }

    @Test
    fun createTransferPersisteNoRoomAposSucesso() = runTest {
        coEvery { transferApi.create(any()) } returns Response.success(transferDto(id = "tr-new", description = "Poupança"))

        val result = repository.createTransfer(
            description = "Poupança", occurredAt = "2026-10-10T00:00:00Z",
            sourceAccountId = "acc-1", destinationAccountId = "acc-2",
            amount = "250.75", categoryId = "cat-1"
        )

        assertTrue(result is ApiResult.Success)
        val entity = dao.getById("tr-new")!!
        assertEquals("TRANSFER", entity.type)
        assertEquals("acc-1", entity.sourceAccountId)
        assertEquals("acc-2", entity.destinationAccountId)
    }

    @Test
    fun createTransferEnviaOrigemEDestino() = runTest {
        coEvery { transferApi.create(any()) } returns Response.success(transferDto(id = "tr-new"))

        repository.createTransfer("Poupança", "2026-10-10T00:00:00Z", "acc-1", "acc-2", "250.75", "cat-1")

        val slot = slot<CreateTransferRequestDto>()
        coVerify { transferApi.create(capture(slot)) }
        assertEquals("acc-1", slot.captured.sourceAccountId)
        assertEquals("acc-2", slot.captured.destinationAccountId)
    }

    @Test
    fun cancelIncomeECancelTransferRefletemNoRoom() = runTest {
        dao.upsertAll(listOf(
            TransactionEntity("i1", "INCOME", "ACTIVE", "R", "2026-10-01T00:00:00Z", "1.00",
                createdAt = "2026-10-01T00:00:00Z", updatedAt = "2026-10-01T00:00:00Z"),
            TransactionEntity("tr1", "TRANSFER", "ACTIVE", "T", "2026-10-01T00:00:00Z", "1.00",
                createdAt = "2026-10-01T00:00:00Z", updatedAt = "2026-10-01T00:00:00Z")
        ))
        coEvery { incomeApi.cancel("i1") } returns Response.success(incomeDto(id = "i1", status = "CANCELLED"))
        coEvery { transferApi.cancel("tr1") } returns Response.success(transferDto(id = "tr1", status = "CANCELLED"))

        repository.cancelIncome("i1")
        repository.cancelTransfer("tr1")

        assertEquals("CANCELLED", dao.getById("i1")!!.status)
        assertEquals("CANCELLED", dao.getById("tr1")!!.status)
    }

    @Test
    fun refreshByIdAtualizaCacheDoItem() = runTest {
        coEvery { expenseApi.get("e1") } returns Response.success(expenseDto(id = "e1", description = "Detalhe"))

        val result = repository.refreshById("EXPENSE", "e1")

        assertTrue(result is ApiResult.Success)
        assertEquals("Detalhe", dao.getById("e1")!!.description)
    }

    @Test
    fun refreshByIdRetornaErroSemMexerNoCache() = runTest {
        dao.upsert(TransactionEntity("e1", "EXPENSE", "ACTIVE", "Cache", "2026-10-01T00:00:00Z", "1.00",
            createdAt = "2026-10-01T00:00:00Z", updatedAt = "2026-10-01T00:00:00Z"))
        coEvery { expenseApi.get("e1") } throws IOException("offline")

        val result = repository.refreshById("EXPENSE", "e1")

        assertTrue(result is ApiResult.NetworkError)
        assertEquals("Cache", dao.getById("e1")!!.description)
    }

    // ---------- paginação (lista de despesas — Task 4.5) ----------

    @Test
    fun listExpensesPaginadoRetornaMapeadoSemPersistir() = runTest {
        coEvery { expenseApi.list(1, 20, false) } returns Response.success(
            ExpensePageResponseDto(
                content = listOf(expenseDto(id = "p1", description = "Página 1")),
                page = 1, size = 20, totalElements = 45, totalPages = 3
            )
        )

        val result = repository.listExpenses(page = 1, includeCancelled = false)

        assertTrue(result is ApiResult.Success)
        val txPage = (result as ApiResult.Success).data
        assertEquals(1, txPage.page)
        assertEquals(3, txPage.totalPages)
        assertEquals(45L, txPage.totalElements)
        assertEquals("Página 1", txPage.items[0].description)
        assertEquals("EXPENSE", txPage.items[0].type)
        assertEquals("acc-1", txPage.items[0].sourceAccountId)
        // Paginação NÃO escreve no cache
        assertEquals(0, dao.getAll().size)
    }

    @Test
    fun listExpensesRepasaIncludeCancelled() = runTest {
        coEvery { expenseApi.list(0, 20, true) } returns Response.success(
            ExpensePageResponseDto(emptyList(), 0, 20, 0, 0)
        )

        val result = repository.listExpenses(page = 0, includeCancelled = true)

        assertTrue(result is ApiResult.Success)
        coVerify { expenseApi.list(0, 20, true) }
    }

    @Test
    fun listExpensesErroDeRedeNAoMexeNoCache() = runTest {
        dao.upsert(TransactionEntity("e1", "EXPENSE", "ACTIVE", "Cache", "2026-10-01T00:00:00Z", "1.00",
            createdAt = "2026-10-01T00:00:00Z", updatedAt = "2026-10-01T00:00:00Z"))
        coEvery { expenseApi.list(any(), any(), any()) } throws IOException("offline")

        val result = repository.listExpenses(page = 0, includeCancelled = false)

        assertTrue(result is ApiResult.NetworkError)
        assertEquals(1, dao.getAll().size)
    }

    @Test
    fun listExpensesErroHttpPropagaCodigo() = runTest {
        coEvery { expenseApi.list(any(), any(), any()) } returns Response.error(500, "err".toResponseBody())

        val result = repository.listExpenses(page = 0, includeCancelled = false)

        assertTrue(result is ApiResult.HttpError)
        assertEquals(500, (result as ApiResult.HttpError).code)
    }

    // ---------- paginação (lista de receitas — Task 4.7) ----------

    @Test
    fun listIncomesPaginadoRetornaMapeadoSemPersistir() = runTest {
        coEvery { incomeApi.list(1, 20, true) } returns Response.success(
            IncomePageResponseDto(
                content = listOf(incomeDto(id = "p1", description = "Salário")),
                page = 1, size = 20, totalElements = 33, totalPages = 2
            )
        )

        val result = repository.listIncomes(page = 1, includeCancelled = true)

        assertTrue(result is ApiResult.Success)
        val txPage = (result as ApiResult.Success).data
        assertEquals(1, txPage.page)
        assertEquals(2, txPage.totalPages)
        assertEquals(33L, txPage.totalElements)
        assertEquals("INCOME", txPage.items[0].type)
        // Receita: conta = destino
        assertEquals("acc-2", txPage.items[0].destinationAccountId)
        assertEquals(null, txPage.items[0].sourceAccountId)
        assertEquals(0, dao.getAll().size) // paginação não escreve no cache
        coVerify { incomeApi.list(1, 20, true) }
    }

    @Test
    fun listIncomesErroNaoMexeNoCache() = runTest {
        coEvery { incomeApi.list(any(), any(), any()) } throws IOException("offline")

        val result = repository.listIncomes(page = 0, includeCancelled = false)

        assertTrue(result is ApiResult.NetworkError)
        assertEquals(0, dao.getAll().size)
    }

    // ---------- detalhe por ID (Task 4.12) ----------

    @Test
    fun getDetailDespesaUsaEndpointDoTipoEMapeiaOrigem() = runTest {
        coEvery { expenseApi.get("e1") } returns Response.success(
            expenseDto(id = "e1", description = "Mercado", amount = "42.90")
        )

        val result = repository.getDetail("EXPENSE", "e1")

        assertTrue(result is ApiResult.Success)
        val detail = (result as ApiResult.Success).data
        assertEquals("e1", detail.id)
        assertEquals("EXPENSE", detail.type)
        assertEquals("Mercado", detail.description)
        assertEquals("42.90", detail.amount)
        assertEquals("acc-1", detail.sourceAccountId)
        assertEquals(null, detail.destinationAccountId)
        assertEquals("cat-1", detail.categoryId)
        assertEquals("ACTIVE", detail.status)
        assertEquals(0, dao.getAll().size) // detalhe não é cache
    }

    @Test
    fun getDetailReceitaUsaDestinoComoConta() = runTest {
        coEvery { incomeApi.get("i1") } returns Response.success(
            incomeDto(id = "i1", description = "Salário")
        )

        val result = repository.getDetail("INCOME", "i1")

        assertTrue(result is ApiResult.Success)
        val detail = (result as ApiResult.Success).data
        assertEquals("INCOME", detail.type)
        assertEquals("acc-2", detail.destinationAccountId)
        assertEquals(null, detail.sourceAccountId)
    }

    @Test
    fun getDetailTransferenciaTrazOrigemEDestino() = runTest {
        coEvery { transferApi.get("tr1") } returns Response.success(
            transferDto(id = "tr1", description = "Poupança")
        )

        val result = repository.getDetail("TRANSFER", "tr1")

        assertTrue(result is ApiResult.Success)
        val detail = (result as ApiResult.Success).data
        assertEquals("acc-1", detail.sourceAccountId)
        assertEquals("acc-2", detail.destinationAccountId)
    }

    @Test
    fun getDetailMapeiaTagsComoNomesOrdenados() = runTest {
        coEvery { expenseApi.get("e1") } returns Response.success(
            expenseDto(id = "e1").copy(
                tags = listOf(TagSummaryDto("t1", "mercado", true), TagSummaryDto("t2", "cartao", true))
            )
        )

        val result = repository.getDetail("EXPENSE", "e1")

        val detail = (result as ApiResult.Success).data
        assertEquals(listOf("mercado", "cartao"), detail.tagNames)
    }

    @Test
    fun getDetailCanceladaRefleteStatusDoBackend() = runTest {
        coEvery { expenseApi.get("e1") } returns Response.success(
            expenseDto(id = "e1", status = "CANCELLED")
        )

        val result = repository.getDetail("EXPENSE", "e1")

        val detail = (result as ApiResult.Success).data
        assertEquals("CANCELLED", detail.status)
        assertTrue(!detail.canEdit)
        assertTrue(!detail.canCancel)
    }

    @Test
    fun getDetailTipoSemEndpointRetornaErroSemChamarApi() = runTest {
        coEvery { expenseApi.get(any()) } returns Response.success(expenseDto(id = "e1"))
        coEvery { incomeApi.get(any()) } returns Response.success(incomeDto(id = "i1"))
        coEvery { transferApi.get(any()) } returns Response.success(transferDto(id = "tr1"))

        val result = repository.getDetail("STATEMENT_PAYMENT", "sp1")

        assertTrue(result is ApiResult.UnknownError)
        coVerify(exactly = 0) { expenseApi.get(any()) }
        coVerify(exactly = 0) { incomeApi.get(any()) }
        coVerify(exactly = 0) { transferApi.get(any()) }
    }

    @Test
    fun getDetailHttpErrorNaoEscreveNoCache() = runTest {
        coEvery { expenseApi.get("e1") } returns Response.error(404, "not found".toResponseBody())

        val result = repository.getDetail("EXPENSE", "e1")

        assertTrue(result is ApiResult.HttpError)
        assertEquals(404, (result as ApiResult.HttpError).code)
        assertEquals(0, dao.getAll().size)
    }

    @Test
    fun getDetailRedeForaRetornaNetworkError() = runTest {
        coEvery { expenseApi.get("e1") } throws IOException("offline")

        val result = repository.getDetail("EXPENSE", "e1")

        assertTrue(result is ApiResult.NetworkError)
    }

    companion object {
        private var seq = 0
    }
}
