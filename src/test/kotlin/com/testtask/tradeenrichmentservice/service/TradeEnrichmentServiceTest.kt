package com.testtask.tradeenrichmentservice.service

import com.testtask.tradeenrichmentservice.model.TradeRecord
import com.testtask.tradeenrichmentservice.repository.TradeRedisRepository
import com.testtask.tradeenrichmentservice.sevice.FileServices.FileService
import com.testtask.tradeenrichmentservice.sevice.FileServices.FileServiceFactory
import com.testtask.tradeenrichmentservice.sevice.TradeEnrichmentService
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TradeEnrichmentServiceTest {
    private val fileServiceFactory = mockk<FileServiceFactory>()
    private val fileService = mockk<FileService>()
    private val tradeRedisRepository = mockk<TradeRedisRepository>()
    private val tradeEnrichmentService = TradeEnrichmentService(fileServiceFactory, tradeRedisRepository)

    @BeforeEach
    fun setup() {
        clearAllMocks()
        every { fileServiceFactory.getFileService(any()) } returns fileService
    }

    @Test
    fun `processFileStream should convert DataBuffer to string`() = runTest {
        val content = "test content"
        val buffer = DefaultDataBufferFactory().wrap(content.toByteArray())
        
        val result = tradeEnrichmentService.processFileStream(flowOf(buffer)).toList()
        
        assertEquals(listOf(content), result)
    }

    @Test
    fun `processProducts should handle successful processing`() = runTest {
        val products = listOf(
            Pair("1", "Product A"),
            Pair("2", "Product B")
        )
        every { fileService.processProductFile(any()) } returns products
        coEvery { tradeRedisRepository.saveTrades(any()) } just Runs

        tradeEnrichmentService.processProducts("content", "csv")

        coVerify { tradeRedisRepository.saveTrades(products) }
    }



    @Test
    fun `enrichTrade should handle missing product names`() = runTest {
        val trades = listOf(
            TradeRecord("20240315", "1", "USD", "100.50")
        )
        every { fileService.processTradeFile(any()) } returns trades
        coEvery { tradeRedisRepository.getTrades(any()) } returns flowOf()

        val result = tradeEnrichmentService.enrichTrade("content", "csv").toList()

        assertEquals(1, result.size)
        assertTrue(result[0].contains("Missing Product Name"))
    }



} 