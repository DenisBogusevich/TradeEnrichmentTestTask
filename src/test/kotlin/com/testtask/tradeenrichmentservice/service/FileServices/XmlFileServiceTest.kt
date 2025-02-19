package com.testtask.tradeenrichmentservice.service.FileServices

import com.testtask.tradeenrichmentservice.sevice.FileServices.XmlFileService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XmlFileServiceTest {
    private val xmlFileService = XmlFileService()

    @Test
    fun `processProductFile should parse valid XML product data`() {
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <products>
                <product>
                    <productId>1</productId>
                    <productName>Product A</productName>
                </product>
                <product>
                    <productId>2</productId>
                    <productName>Product B</productName>
                </product>
            </products>
        """.trimIndent()

        val result = xmlFileService.processProductFile(xmlContent)
        assertEquals(2, result.size)
        assertEquals(Pair("1", "Product A"), result[0])
    }

    @Test
    fun `processProductFile should handle empty products`() {
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <products></products>
        """.trimIndent()

        val result = xmlFileService.processProductFile(xmlContent)
        assertEquals(0, result.size)
    }

    @Test
    fun `processProductFile should skip products with missing fields`() {
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <products>
                <product>
                    <productId>1</productId>
                </product>
                <product>
                    <productId>2</productId>
                    <productName>Product B</productName>
                </product>
            </products>
        """.trimIndent()

        val result = xmlFileService.processProductFile(xmlContent)
        assertEquals(1, result.size)
        assertEquals("2", result[0].first)
    }

    @Test
    fun `processProductFile should handle empty fields`() {
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <products>
                <product>
                    <productId></productId>
                    <productName>Product A</productName>
                </product>
                <product>
                    <productId>2</productId>
                    <productName></productName>
                </product>
            </products>
        """.trimIndent()

        val result = xmlFileService.processProductFile(xmlContent)
        assertEquals(0, result.size)
    }

    @Test
    fun `processProductFile should throw on invalid XML`() {
        val invalidXml = "<invalid>xml</invalid"
        assertThrows<Exception> { 
            xmlFileService.processProductFile(invalidXml)
        }
    }

    @Test
    fun `processTradeFile should parse valid XML trade data`() {
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <trades>
                <trade>
                    <date>20240315</date>
                    <productId>1</productId>
                    <currency>USD</currency>
                    <price>100.50</price>
                </trade>
            </trades>
        """.trimIndent()

        val result = xmlFileService.processTradeFile(xmlContent)
        assertEquals(1, result.size)
        with(result[0]) {
            assertEquals("20240315", dateStr)
            assertEquals("1", productIdOrName)
            assertEquals("USD", currency)
        }
    }

    @Test
    fun `processTradeFile should handle empty trades`() {
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <trades></trades>
        """.trimIndent()

        val result = xmlFileService.processTradeFile(xmlContent)
        assertEquals(0, result.size)
    }

    @Test
    fun `processTradeFile should skip invalid dates`() {
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <trades>
                <trade>
                    <date>2024-03-15</date>
                    <productId>1</productId>
                    <currency>USD</currency>
                    <price>100.50</price>
                </trade>
                <trade>
                    <date>20240315</date>
                    <productId>2</productId>
                    <currency>EUR</currency>
                    <price>200.75</price>
                </trade>
            </trades>
        """.trimIndent()

        val result = xmlFileService.processTradeFile(xmlContent)
        assertEquals(1, result.size)
        assertEquals("20240315", result[0].dateStr)
    }

    @Test
    fun `processTradeFile should validate all required fields`() {
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <trades>
                <trade>
                    <date>20240315</date>
                    <productId>1</productId>
                    <price>100.50</price>
                </trade>
            </trades>
        """.trimIndent()

        val result = xmlFileService.processTradeFile(xmlContent)
        assertEquals(0, result.size)
    }
} 