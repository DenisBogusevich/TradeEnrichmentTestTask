package com.testtask.tradeenrichmentservice.controller

import com.testtask.tradeenrichmentservice.sevice.TradeEnrichmentService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/v1")
class FileController(private val tradeEnrichmentService: TradeEnrichmentService) {
    private val logger = LoggerFactory.getLogger(FileController::class.java)

    private val semaphore = Semaphore(12
    )

    @PostMapping("/product/{extension}", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    suspend fun uploadFile(@PathVariable extension: String, @RequestBody file: Flow<DataBuffer>): Unit =
        coroutineScope {
            file.collect { buffer ->
                val bytes = ByteArray(buffer.readableByteCount())
                buffer.read(bytes)
                DataBufferUtils.release(buffer)

                val content = String(bytes, StandardCharsets.UTF_8)
                launch(Dispatchers.IO) {
                    semaphore.withPermit {
                        tradeEnrichmentService.processProducts(content, extension)

                    }
                }
            }
        }


    @PostMapping(
        "/enrich/{extension}",
        consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE]
    )
    suspend fun enrichTrades(@PathVariable extension: String, @RequestBody file: Flow<DataBuffer>): Flow<List<String>> = channelFlow {
        val bufferSize = 1024 * 1024 * 0.5 // 1 МБ
        val accumulator = StringBuilder()
       // Ограничиваем до 5 параллельных обработок

        file.collect { buffer ->
            val bytes = ByteArray(buffer.readableByteCount())
            buffer.read(bytes)
            DataBufferUtils.release(buffer)
            accumulator.append(String(bytes, StandardCharsets.UTF_8))

            if (accumulator.length >= bufferSize) {
                val chunk = accumulator.toString()
                accumulator.clear()

                launch(Dispatchers.IO) { // Запускаем параллельно в фоновом потоке
                    semaphore.withPermit {
                        tradeEnrichmentService.enrichTrade(chunk, extension)
                            .collect { send(it) } // Отправляем в поток быстрее
                    }
                }
            }
        }

        if (accumulator.isNotEmpty()) {
            launch(Dispatchers.IO) {
                semaphore.withPermit {
                    tradeEnrichmentService.enrichTrade(accumulator.toString(), extension)
                        .collect {
                            send(it)
                            }
                }
            }
        }

        return@channelFlow
    }

}
