package com.testtask.tradeenrichmentservice.service.FileServices

import com.testtask.tradeenrichmentservice.model.TradeRecord
import com.testtask.tradeenrichmentservice.sevice.FileServices.CsvFileService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CsvFileServiceTest {
    private val csvFileService = CsvFileService()

    @Test
    fun `processProductFile should parse valid CSV product data`() {
        val csvContent = """
            1,Product A
            2,Product B
            3,Product C
        """.trimIndent()

        val result = csvFileService.processProductFile(csvContent)

        assertEquals(3, result.size)
        assertEquals(Pair("1", "Product A"), result[0])
        assertEquals(Pair("2", "Product B"), result[1])
        assertEquals(Pair("3", "Product C"), result[2])
    }

    @Test
    fun `processProductFile should handle empty input`() {
        val result = csvFileService.processProductFile("")
        assertEquals(0, result.size)
    }

    @Test
    fun `processProductFile should skip blank lines`() {
        val csvContent = """
            1,Product A
            
            2,Product B
            
            3,Product C
        """.trimIndent()

        val result = csvFileService.processProductFile(csvContent)
        assertEquals(3, result.size)
    }

    @Test
    fun `processProductFile should skip lines with too many columns`() {
        val csvContent = """
            1,Product A
            2,Product B,Extra Column
            3,Product C
        """.trimIndent()

        val result = csvFileService.processProductFile(csvContent)
        assertEquals(2, result.size)
    }

    @Test
    fun `processProductFile should skip lines with too few columns`() {
        val csvContent = """
            1,Product A
            2
            3,Product C
        """.trimIndent()

        val result = csvFileService.processProductFile(csvContent)
        assertEquals(2, result.size)
    }

    @Test
    fun `processTradeFile should parse valid CSV trade data`() {
        val csvContent = """
            20240315,1,USD,100.50
            20240315,2,EUR,200.75
        """.trimIndent()

        val result = csvFileService.processTradeFile(csvContent)

        assertEquals(2, result.size)
        with(result[0]) {
            assertEquals("20240315", dateStr)
            assertEquals("1", productIdOrName)
            assertEquals("USD", currency)
            assertEquals("100.50", price)
        }
    }

    @Test
    fun `processTradeFile should handle empty input`() {
        val result = csvFileService.processTradeFile("")
        assertEquals(0, result.size)
    }

    @Test
    fun `processTradeFile should skip invalid date format`() {
        val csvContent = """
            2024-03-15,1,USD,100.50
            20240315,2,EUR,200.75
        """.trimIndent()

        val result = csvFileService.processTradeFile(csvContent)
        assertEquals(1, result.size)
        assertEquals("20240315", result[0].dateStr)
    }

    @Test
    fun `processTradeFile should skip lines with missing fields`() {
        val csvContent = """
            20240315,1,USD,100.50
            20240315,2,EUR
            20240315,3
            20240315
        """.trimIndent()

        val result = csvFileService.processTradeFile(csvContent)
        assertEquals(1, result.size)
    }

    @Test
    fun `processTradeFile should handle different numeric formats`() {
        val csvContent = """
            20240315,1,USD,100.50
            20240315,2,EUR,200
            20240315,3,JPY,300.000
        """.trimIndent()

        val result = csvFileService.processTradeFile(csvContent)
        assertEquals(3, result.size)
        assertEquals("100.50", result[0].price)
        assertEquals("200", result[1].price)
        assertEquals("300.000", result[2].price)
    }

    @Test
    fun `processTradeFile should handle whitespace in fields`() {
        val csvContent = """
            20240315, 1 , USD , 100.50
            20240315,2,EUR,200.75
        """.trimIndent()

        val result = csvFileService.processTradeFile(csvContent)
        assertEquals(2, result.size)
        assertEquals("1", result[0].productIdOrName.trim())
        assertEquals("USD", result[0].currency.trim())
    }
} 