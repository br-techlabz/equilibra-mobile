package br.com.equilibra.mobile.data.remote

import br.com.equilibra.mobile.data.remote.dto.statement.CloseStatementRequestDto
import br.com.equilibra.mobile.data.remote.dto.statement.StatementDto
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

/**
 * Task 7.1 — Rotas e parsing reais das faturas de cartão.
 *
 * Backend (CreditCardStatementController / StatementPaymentController):
 * - GET  /credit-cards/{cardId}/current-statement
 * - GET  /credit-cards/{cardId}/statements
 * - GET  /credit-card-statements/{id}
 * - POST /credit-cards/{cardId}/statements/close
 */
class StatementApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: StatementApi

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(StatementApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private val responseJson = """
        {
          "id": "s-1", "creditCardId": "c-1",
          "cycleStart": "2026-10-03", "cycleEnd": "2026-11-02",
          "closingAt": "2026-11-03T03:00:00Z", "dueDate": "2026-11-10",
          "status": "PARTIALLY_PAID",
          "totalAmount": 1234.56, "paidAmount": 200.00, "outstandingAmount": 1034.56,
          "items": [
            {"id": "i-1", "purchaseId": "p-1", "description": "Notebook", "sequenceNumber": 1, "totalInstallments": 3, "amount": 411.52, "cycleMonth": "2026-11-01"}
          ]
        }
    """.trimIndent()

    @Test
    fun currentUsaRotaDoBackend() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(responseJson))

        val result = api.current("c-1")

        assertEquals("/credit-cards/c-1/current-statement", server.takeRequest().path)
        assertEquals(200, result.code())
        assertEquals("s-1", result.body()!!.id)
        assertEquals("1034.56", result.body()!!.outstandingAmount)
    }

    @Test
    fun listByCardUsaRotaDoBackend() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[$responseJson]"))

        val result = api.listByCard("c-1")

        assertEquals("/credit-cards/c-1/statements", server.takeRequest().path)
        assertEquals(1, result.body()!!.size)
        assertEquals("PARTIALLY_PAID", result.body()!!.single().status)
        assertEquals("411.52", result.body()!!.single().items.single().amount)
    }

    @Test
    fun getUsaIdComoPathParam() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(responseJson))

        val result = api.get("s-1")

        assertEquals("/credit-card-statements/s-1", server.takeRequest().path)
        assertEquals("c-1", result.body()!!.creditCardId)
    }

    @Test
    fun closeEnviaPostComCycleMonth() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(responseJson.replace("\"PARTIALLY_PAID\"", "\"CLOSED\""))
        )

        val result = api.close(
            cardId = "c-1",
            body = CloseStatementRequestDto(cycleMonth = "2026-11-01")
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/credit-cards/c-1/statements/close", request.path)
        val sent = request.body.readUtf8()
        assertTrue(sent.contains("\"cycleMonth\":\"2026-11-01\""))
        assertEquals("CLOSED", result.body()!!.status)
    }

    @Test
    fun decimalDaFaturaNaoPassaPorDouble() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(responseJson))

        val dto: StatementDto = api.get("s-1").body()!!

        assertEquals("1234.56", dto.totalAmount)
        assertEquals("200.00", dto.paidAmount)
        assertEquals("1034.56", dto.outstandingAmount)
    }
}
