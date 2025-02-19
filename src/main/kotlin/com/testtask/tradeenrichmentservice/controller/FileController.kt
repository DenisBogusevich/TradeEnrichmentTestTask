package com.testtask.tradeenrichmentservice.controller

import com.testtask.tradeenrichmentservice.sevice.TradeEnrichmentService
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import org.springframework.beans.factory.annotation.Value

@RestController
@RequestMapping("/api/v1")
class FileController(private val tradeEnrichmentService: TradeEnrichmentService) {
    private val logger = LoggerFactory.getLogger(FileController::class.java)
    private val semaphore = Semaphore(50)
    private val bufferFactory = DefaultDataBufferFactory()

    @Value("\${app.processing.chunk-size:5000}")
    private val chunkSize: Int = 5000

    @PostMapping("/product/{extension}", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    suspend fun uploadFile(@PathVariable extension: String, @RequestBody file: Flow<DataBuffer>): Unit =
        coroutineScope {

            val accumulator = StringBuilder()
            file.collect { buffer ->
                try {
                    val bytes = ByteArray(buffer.readableByteCount())
                    buffer.read(bytes)
                    DataBufferUtils.release(buffer)

                    accumulator.append(String(bytes, StandardCharsets.UTF_8))

                    // Обрабатываем данные, когда накопился достаточный объем
                    if (accumulator.length >= chunkSize) {
                        processProductChunk(accumulator.toString(), extension)
                        accumulator.clear()
                    }
                } catch (e: Exception) {
                    logger.error("Error processing buffer: ${e.message}", e)
                    throw e
                }
            }

            // Обрабатываем оставшиеся данные
            if (accumulator.isNotEmpty()) {
                processProductChunk(accumulator.toString(), extension)
            }
        }

    private suspend fun processProductChunk(content: String, extension: String) {
        withContext(Dispatchers.IO) {
            semaphore.withPermit {
                tradeEnrichmentService.processProducts(content, extension)
            }
        }
    }

    @PostMapping("/enrich/{extension}", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    suspend fun enrichTrades(@PathVariable extension: String, @RequestBody file: Flow<DataBuffer>): Flow<DataBuffer> =
        channelFlow {
            val accumulator = StringBuilder()
            val bufferSize = 1024 * 1024 * 10 // 15MB

            try {
                coroutineScope {
                    file.collect { buffer ->
                        val bytes = ByteArray(buffer.readableByteCount())
                        buffer.read(bytes)
                        DataBufferUtils.release(buffer)

                        accumulator.append(String(bytes, StandardCharsets.UTF_8))

                        if (accumulator.length >= bufferSize) {
                            processAndEmitTradeChunk(accumulator.toString(), extension, this@channelFlow)
                            accumulator.clear()
                        }
                    }

                    if (accumulator.isNotEmpty()) {
                        processAndEmitTradeChunk(accumulator.toString(), extension, this@channelFlow)
                    }
                }
            } catch (e: Exception) {
                logger.error("Error processing trades: ${e.message}", e)
                throw e
            }
        }

    private suspend inline fun processAndEmitTradeChunk(
        chunk: String,
        extension: String,
        channel: ProducerScope<DataBuffer>
    ) {
        withContext(Dispatchers.IO) {
            semaphore.withPermit {
                tradeEnrichmentService.enrichTrade(chunk, extension)
                    .map { it.toByteArray() }
                    .map { bufferFactory.wrap(it) }
                    .collect { channel.send(it) }
            }
        }
    }



}
