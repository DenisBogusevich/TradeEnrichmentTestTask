package com.testtask.tradeenrichmentservice.controller

import com.testtask.tradeenrichmentservice.config.TestConfig
import com.testtask.tradeenrichmentservice.sevice.TradeEnrichmentService
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import java.nio.charset.StandardCharsets

@WebFluxTest(controllers = [FileController::class])
@Import(TestConfig::class)
class FileControllerTest {
    private lateinit var webClient: WebTestClient
    private val tradeEnrichmentService = mockk<TradeEnrichmentService>()
    private val fileController = FileController(tradeEnrichmentService)
    private val bufferFactory = DefaultDataBufferFactory()

    @BeforeEach
    fun setup() {
        clearAllMocks()
        webClient = WebTestClient.bindToController(fileController).build()
    }

    private fun createDataBuffer(content: String): DataBuffer {
        return bufferFactory.wrap(content.toByteArray(StandardCharsets.UTF_8))
    }

    @Test
    fun `should process CSV product upload`() = runTest {
        val content = "1,Product A\n2,Product B"
        coEvery { tradeEnrichmentService.processProducts(content, "csv") } just Runs

        webClient.post()
            .uri("/api/v1/product/csv")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .bodyValue(createDataBuffer(content))
            .exchange()
            .expectStatus().isOk

        coVerify { tradeEnrichmentService.processProducts(content, "csv") }
    }

    @Test
    fun `should process JSON product upload`() = runTest {
        val content = """[{"productId":"1","productName":"Product A"}]"""
        coEvery { tradeEnrichmentService.processProducts(content, "json") } just Runs

        webClient.post()
            .uri("/api/v1/product/json")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .bodyValue(createDataBuffer(content))
            .exchange()
            .expectStatus().isOk

        coVerify { tradeEnrichmentService.processProducts(content, "json") }
    }

    @Test
    fun `should process XML product upload`() = runTest {
        val content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <products>
                <product>
                    <productId>1</productId>
                    <productName>Product A</productName>
                </product>
            </products>
        """.trimIndent()
        coEvery { tradeEnrichmentService.processProducts(content, "xml") } just Runs

        webClient.post()
            .uri("/api/v1/product/xml")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .bodyValue(createDataBuffer(content))
            .exchange()
            .expectStatus().isOk

        coVerify { tradeEnrichmentService.processProducts(content, "xml") }
    }


    @Test
    fun `should handle large file upload in chunks`() = runTest {
        val largeContent = "1,Product A\n".repeat(1024)
        coEvery { tradeEnrichmentService.processProducts(any(), any()) } just Runs

        val dataBuffers = Flux.just(
            createDataBuffer(largeContent.substring(0, 2048)),
            createDataBuffer(largeContent.substring(2048))
        )

        webClient.post()
            .uri("/api/v1/product/csv")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(dataBuffers, DataBuffer::class.java)
            .exchange()
            .expectStatus().isOk

        coVerify { tradeEnrichmentService.processProducts(any(), eq("csv")) }
    }

    @Test
    fun `should handle empty file upload`() = runTest {
        val content = ""
        coEvery { tradeEnrichmentService.processProducts(content, "csv") } just Runs

        webClient.post()
            .uri("/api/v1/product/csv")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .bodyValue(createDataBuffer(content))
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `should handle service errors`() = runTest {
        val content = "invalid content"
        coEvery { tradeEnrichmentService.processProducts(content, "csv") } throws RuntimeException("Processing error")

        webClient.post()
            .uri("/api/v1/product/csv")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .bodyValue(createDataBuffer(content))
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    fun `should handle concurrent requests within semaphore limit`() = runTest {
        val content = "test content"
        coEvery { tradeEnrichmentService.processProducts(any(), any()) } just Runs

        // Send multiple concurrent requests
        val requests = (1..15).map {
            webClient.post()
                .uri("/api/v1/product/csv")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(createDataBuffer(content))
                .exchange()
        }

        requests.forEach { it.expectStatus().isOk }
    }
} 