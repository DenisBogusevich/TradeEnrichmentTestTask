package com.testtask.tradeenrichmentservice.service.FileServices

import com.testtask.tradeenrichmentservice.sevice.FileServices.JsonFileService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonFileServiceTest {
    private val jsonFileService = JsonFileService()

    @Test
    fun `processProductFile should parse valid JSON product data`() {
        val jsonContent = """
            [
                {"productId": "1", "productName": "Product A"},
                {"productId": "2", "productName": "Product B"}
            ]
        """.trimIndent()

        val result = jsonFileService.processProductFile(jsonContent)
        assertEquals(2, result.size)
        assertEquals(Pair("1", "Product A"), result[0])
    }

    @Test
    fun `processProductFile should handle empty array`() {
        val jsonContent = "[]"
        val result = jsonFileService.processProductFile(jsonContent)
        assertEquals(0, result.size)
    }

    @Test
    fun `processProductFile should skip invalid entries`() {
        val jsonContent = """
            [
                {"productId": "1", "productName": "Product A"},
                {"invalidKey": "value"},
                {"productId": "2", "productName": "Product B"}
            ]
        """.trimIndent()

        val result = jsonFileService.processProductFile(jsonContent)
        assertEquals(2, result.size)
    }

    @Test
    fun `processProductFile should handle null values`() {
        val jsonContent = """
            [
                {"productId": "1", "productName": null},
                {"productId": null, "productName": "Product B"},
                {"productId": "3", "productName": "Product C"}
            ]
        """.trimIndent()

        val result = jsonFileService.processProductFile(jsonContent)
        assertEquals(1, result.size)
        assertEquals("3", result[0].first)
    }

    @Test
    fun `processProductFile should throw on invalid JSON`() {
        val invalidJson = "{ invalid json }"
        assertThrows<Exception> { 
            jsonFileService.processProductFile(invalidJson)
        }
    }

    @Test
    fun `processTradeFile should parse valid JSON trade data`() {
        val jsonContent = """
            [
                {
                    "date": "20240315",
                    "productId": "1",
                    "currency": "USD",
                    "price": "100.50"
                }
            ]
        """.trimIndent()

        val result = jsonFileService.processTradeFile(jsonContent)
        assertEquals(1, result.size)
        with(result[0]) {
            assertEquals("20240315", dateStr)
            assertEquals("1", productIdOrName)
        }
    }

    @Test
    fun `processTradeFile should skip invalid dates`() {
        val jsonContent = """
            [
                {
                    "date": "2024-03-15",
                    "productId": "1",
                    "currency": "USD",
                    "price": "100.50"
                },
                {
                    "date": "20240315",
                    "productId": "2",
                    "currency": "EUR",
                    "price": "200.75"
                }
            ]
        """.trimIndent()

        val result = jsonFileService.processTradeFile(jsonContent)
        assertEquals(1, result.size)
        assertEquals("20240315", result[0].dateStr)
    }

    @Test
    fun `processTradeFile should handle missing fields`() {
        val jsonContent = """
            [
                {
                    "date": "20240315",
                    "productId": "1",
                    "currency": "USD"
                }
            ]
        """.trimIndent()

        val result = jsonFileService.processTradeFile(jsonContent)
        assertEquals(0, result.size)
    }

    @Test
    fun `processTradeFile should validate date format`() {
        val jsonContent = """
            [
                {
                    "date": "20240315",
                    "productId": "1",
                    "currency": "USD",
                    "price": "100.50"
                },
                {
                    "date": "invalid",
                    "productId": "2",
                    "currency": "EUR",
                    "price": "200.75"
                }
            ]
        """.trimIndent()

        val result = jsonFileService.processTradeFile(jsonContent)
        assertEquals(1, result.size)
    }
} 