package br.com.equilibra.mobile.data.remote

import br.com.equilibra.mobile.data.remote.dto.purchase.CreatePurchaseRequestDto
import br.com.equilibra.mobile.data.remote.dto.purchase.PurchaseDto
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

/**
 * Task 6.6 — Rotas e parsing reais de /credit-card-purchases.
 *
 * O teste usa MockWebServer para travar método/rota/path param e o shape
 * exato que o backend devolve (CreditCardPurchaseResponse).
 */
class PurchaseApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: PurchaseApi

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PurchaseApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private val responseJson = """
        {
          "id": "p-1", "creditCardId": "c-1", "categoryId": "cat-1",
          "description": "Notebook", "totalAmount": 100.00,
          "purchaseDate": "2026-10-15", "purchaseAt": "2026-10-15T14:30:00Z",
          "installmentCount": 2, "cycleMonth": "2026-11-01", "status": "ACTIVE",
          "installments": [
            {"sequenceNumber": 1, "totalInstallments": 2, "principalAmount": 50.01, "cycleMonth": "2026-11-01"},
            {"sequenceNumber": 2, "totalInstallments": 2, "principalAmount": 49.99, "cycleMonth": "2026-12-01"}
          ]
        }
    """.trimIndent()

    @Test
    fun createEnviaPostEParseiaResposta() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody(responseJson))

        val result = api.create(
            CreatePurchaseRequestDto(
                description = "Notebook",
                purchaseDate = "2026-10-15",
                purchaseAt = "2026-10-15T14:30:00Z",
                totalAmount = "100.00",
                installmentCount = 2,
                creditCardId = "c-1",
                categoryId = "cat-1"
            )
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/credit-card-purchases", request.path)
        val sent = request.body.readUtf8()
        assertTrue(sent.contains("\"creditCardId\":\"c-1\""))
        assertTrue(sent.contains("\"installmentCount\":2"))

        assertEquals(201, result.code())
        assertEquals("50.01", result.body()!!.installments[0].principalAmount)
        assertEquals("49.99", result.body()!!.installments[1].principalAmount)
    }

    @Test
    fun listByCardUsaRotaDoBackend() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[$responseJson]"))

        val result = api.listByCard("c-1")

        assertEquals("/credit-card-purchases/card/c-1", server.takeRequest().path)
        assertEquals(1, result.body()!!.size)
        assertEquals("Notebook", result.body()!!.single().description)
    }

    @Test
    fun getUsaIdComoPathParam() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(responseJson))

        val result = api.get("p-1")

        assertEquals("/credit-card-purchases/p-1", server.takeRequest().path)
        assertEquals("p-1", result.body()!!.id)
    }

    @Test
    fun cancelUsaPostNaSubRotaCancel() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(responseJson.replace("\"status\": \"ACTIVE\"", "\"status\": \"CANCELLED\""))
        )

        val result = api.cancel("p-1")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/credit-card-purchases/p-1/cancel", request.path)
        assertEquals("CANCELLED", result.body()!!.status)
    }
}
