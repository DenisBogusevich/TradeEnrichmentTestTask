package com.testtask.tradeenrichmentservice.controller

import com.testtask.tradeenrichmentservice.sevice.TradeEnrichmentService
import kotlinx.coroutines.Dispatchers
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

    private val semaphore = Semaphore(50)

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
    suspend fun enrichTrades(@PathVariable extension: String, @RequestBody file: Flow<DataBuffer>): Flow<String> =
        channelFlow  {


                val bufferSize = 1024 * 1024 // 1 МБ
                val accumulator = StringBuilder()

                file.collect { buffer ->
                    val bytes = ByteArray(buffer.readableByteCount())
                    buffer.read(bytes)
                    DataBufferUtils.release(buffer)
                    accumulator.append(String(bytes, StandardCharsets.UTF_8))

                    if (accumulator.length >= bufferSize) {
                        val chunk = accumulator.toString()
                        accumulator.clear()

                        launch(Dispatchers.IO) { // Запускаем обработку чанка асинхронно
                                tradeEnrichmentService.enrichTrade(chunk, extension)
                                    .collect { enrichedRecord -> send(enrichedRecord) } // Используем send() в channelFlow

                        }


                    }
                }

                // Отправляем оставшиеся данные, если что-то накопилось
                if (accumulator.isNotEmpty()) {
                    tradeEnrichmentService.enrichTrade(accumulator.toString(), extension)
                        .collect { enrichedRecord -> send(enrichedRecord) }
                }


            }

}
















  /*  @PostMapping(
        "/product",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    suspend fun loadProducts(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {
        if (!file.originalFilename?.endsWith(".csv", ignoreCase = true)!!) {
            return ResponseEntity.badRequest().body("Only CSV files are supported")
        }

        return try {
            tradeEnrichmentService.processProducts(file)
            ResponseEntity.ok("Products loaded successfully")
        } catch (e: Exception) {
            logger.error("Error processing products file", e)
            ResponseEntity.internalServerError().body("Error processing file: ${e.message}")
        }
    }
*/
   /* @PostMapping(
        "/enrich",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun enrichTrades(@RequestParam("file") file: MultipartFile): Flow<String> = flow {
        if (!file.originalFilename?.endsWith(".csv", ignoreCase = true)!!) {
            throw IllegalArgumentException("Only CSV files are supported")
        }

        // Читаем файл построчно и эмитим каждую строку
        BufferedReader(InputStreamReader(file.inputStream, StandardCharsets.UTF_8)).use { reader ->
            // Читаем заголовок
            val header = reader.readLine()
            emit(header)

            // Читаем и обрабатываем данные построчно
            reader.lineSequence().forEach { line ->
                try {
                 //   val enrichedLine = tradeEnrichmentService.enrichTradeLine(line)
                    emit(line)
                } catch (e: Exception) {
                    logger.error("Error processing line: $line", e)
                }
            }
        }
    }*/

   /*@PostMapping(
        "/stream",
        consumes = [MediaType.TEXT_PLAIN_VALUE],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun processCsvStream(@RequestBody content: String): Flow<String> = flow {
        content.lineSequence().forEach { line ->
            try {
                //val processedLine = tradeEnrichmentService.enrichTradeLine(line)
                emit(line)
            } catch (e: Exception) {
                logger.error("Error processing line: $line", e)
            }
        }
    }*/

