package br.com.equilibra.mobile.data.remote.dto.account

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class AssetAccountDtoTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    @Test
    fun responseDeserializesFromBackendJson() {
        val raw = """
            {
              "id": "550e8400-e29b-41d4-a716-446655440000",
              "name": "Nubank",
              "type": "CHECKING",
              "initialBalance": "1000.00",
              "currentBalance": "1234.56",
              "active": true,
              "createdAt": "2024-01-01T12:00:00Z",
              "updatedAt": "2024-01-01T12:00:00Z"
            }
        """.trimIndent()

        val dto = json.decodeFromString<AssetAccountDto>(raw)

        assertEquals("550e8400-e29b-41d4-a716-446655440000", dto.id)
        assertEquals("Nubank", dto.name)
        assertEquals("CHECKING", dto.type)
        assertEquals("1000.00", dto.initialBalance)
        assertEquals("1234.56", dto.currentBalance)
        assertEquals(true, dto.active)
    }

    @Test
    fun responseDeserializesNumericBigDecimal() {
        // Backend serializa BigDecimal como JSON number, não string — deve deserializar para String
        val raw = """
            {
              "id": "id-1",
              "name": "Caixa",
              "type": "SAVINGS",
              "initialBalance": 1000.00,
              "currentBalance": 0,
              "active": false,
              "createdAt": "2024-01-01T00:00:00Z",
              "updatedAt": "2024-01-01T00:00:00Z"
            }
        """.trimIndent()

        val dto = json.decodeFromString<AssetAccountDto>(raw)

        // Serializador aceita number e expõe como String sem perda
        assertEquals("1000.00", dto.initialBalance)
        assertEquals("0", dto.currentBalance)
    }

    @Test
    fun listDeserializes() {
        val raw = """[{"id":"1","name":"A","type":"CASH","initialBalance":"10.00","currentBalance":"10.00","active":true,"createdAt":"2024-01-01T00:00:00Z","updatedAt":"2024-01-01T00:00:00Z"}]"""
        val list = json.decodeFromString<List<AssetAccountDto>>(raw)
        assertEquals(1, list.size)
        assertEquals("A", list[0].name)
    }

    @Test
    fun createRequestSerializes() {
        val req = CreateAssetAccountRequestDto(name = "Conta X", type = "DIGITAL", initialBalance = "2500.12")
        val out = json.encodeToString(CreateAssetAccountRequestDto.serializer(), req)
        assertEquals(true, out.contains("\"name\":\"Conta X\""))
        assertEquals(true, out.contains("\"type\":\"DIGITAL\""))
    }

    @Test
    fun updateRequestSerializes() {
        val req = UpdateAssetAccountRequestDto(name = "Atualizada", type = "OTHER", initialBalance = "0.00")
        val out = json.encodeToString(UpdateAssetAccountRequestDto.serializer(), req)
        assertEquals(true, out.contains("\"name\":\"Atualizada\""))
    }

    @Test
    fun reconcileRequestSerializes() {
        val req = ReconcileAccountRequestDto(
            reconciliationDate = "2024-01-15",
            reconciliationTime = "14:30",
            informedBalance = "5000.00"
        )
        val out = json.encodeToString(ReconcileAccountRequestDto.serializer(), req)
        assertEquals(true, out.contains("2024-01-15"))
        assertEquals(true, out.contains("14:30"))
    }

    @Test
    fun reconcileResultDeserializes() {
        val raw = """
            {
              "accountId": "acc-1",
              "reconciliationDate": "2024-01-15",
              "calculatedBalance": "1000.00",
              "informedBalance": "900.00",
              "difference": "-100.00",
              "adjustmentType": "EXPENSE",
              "createdTransactionId": "tx-1"
            }
        """.trimIndent()
        val dto = json.decodeFromString<ReconciliationResultDto>(raw)
        assertEquals("acc-1", dto.accountId)
        assertEquals("-100.00", dto.difference)
        assertEquals("EXPENSE", dto.adjustmentType)
        assertEquals("tx-1", dto.createdTransactionId)
    }

    @Test
    fun reconcileResultWithoutAdjustmentDeserializes() {
        val raw = """
            {
              "accountId": "acc-1",
              "reconciliationDate": "2024-01-15",
              "calculatedBalance": "1000.00",
              "informedBalance": "1000.00",
              "difference": "0.00"
            }
        """.trimIndent()
        val dto = json.decodeFromString<ReconciliationResultDto>(raw)
        assertEquals("0.00", dto.difference)
        assertEquals(null, dto.adjustmentType)
        assertEquals(null, dto.createdTransactionId)
    }

    @Test
    fun assetAccountTypesHavePtBrLabels() {
        assertEquals("Conta corrente", AssetAccountTypeDto.labelPtBr("CHECKING"))
        assertEquals("Poupança", AssetAccountTypeDto.labelPtBr("SAVINGS"))
        assertEquals("Dinheiro", AssetAccountTypeDto.labelPtBr("CASH"))
        assertEquals("Investimento", AssetAccountTypeDto.labelPtBr("INVESTMENT"))
        assertEquals("Digital", AssetAccountTypeDto.labelPtBr("DIGITAL"))
        assertEquals("Outro", AssetAccountTypeDto.labelPtBr("OTHER"))
    }

    @Test
    fun allAccountTypesContainsSix() {
        assertEquals(6, AssetAccountTypeDto.all.size)
    }
}
