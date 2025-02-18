package com.testtask.tradeenrichmentservice.sevice

import com.testtask.tradeenrichmentservice.model.TradeRecord
import com.testtask.tradeenrichmentservice.repository.TradeRedisRepository
import com.testtask.tradeenrichmentservice.sevice.FileServices.FileServiceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream



@Service
class TradeEnrichmentService(
    private val fileServiceFactory: FileServiceFactory,
    private val tradeRedisRepository: TradeRedisRepository
) {

    // Declare the record class once (avoids per-chunk inner class allocation)
    data class ParsedRecord(
        val dateStr: String,
        var productIdOrName: String,
        val currency: String,
        val price: String
    )

    suspend fun processProducts(file: MultipartFile) {
        val ext = file.extension
        val fileService = fileServiceFactory.getFileService(ext)
        // Use a sequence to avoid holding all lines in memory
        val lines = fileService.processFile(file).asSequence().drop(1)
        processChunkSequence(lines, 100) { chunk ->
            // Manually parse to avoid split() allocations (for 2-column CSV)
            val parsedData = ArrayList<Pair<String, String>>(chunk.size)
            for (line in chunk) {
                val idx = line.indexOf(',')
                if (idx != -1) {
                    // Avoid extra object creation by using substring directly
                    parsedData.add(line.substring(0, idx) to line.substring(idx + 1))
                }
            }
            tradeRedisRepository.saveTrades(parsedData)
        }
    }

    fun enrichTrade(file: MultipartFile): Flow<String> = channelFlow {
        val ext = file.extension
        val fileService = fileServiceFactory.getFileService(ext)
        val lines = fileService.processFile(file).asSequence().drop(1)
        processChunkSequence(lines, 10_000) { chunk ->
            // Manually parse each CSV line (format: date,productId,currency,price)
            val parsedRecords = ArrayList<ParsedRecord>(chunk.size)
            for (line in chunk) {
                // Instead of split(), use indexOf to locate commas
                if(line == null) continue
                val idx1 = line.indexOf(',')
                if (idx1 == -1) continue
                val idx2 = line.indexOf(',', idx1 + 1)
                if (idx2 == -1) continue
                val idx3 = line.indexOf(',', idx2 + 1)
                if (idx3 == -1) continue

                val dateStr = line.substring(0, idx1)
                val prodId = line.substring(idx1 + 1, idx2)
                val currency = line.substring(idx2 + 1, idx3)
                val price = line.substring(idx3 + 1)
                parsedRecords.add(ParsedRecord(dateStr, prodId, currency, price))
            }

            // Gather unique product IDs manually to minimize allocations
            val uniqueProductIds = HashSet<String>(parsedRecords.size)
            for (rec in parsedRecords) {
                uniqueProductIds.add(rec.productIdOrName)
            }

            val productNames = tradeRedisRepository.getTrades(uniqueProductIds.toList()).toMap()
            // Update records in place
            for (rec in parsedRecords) {
                rec.productIdOrName = productNames[rec.productIdOrName] ?: "Unknown"
            }

            // Use a single reusable StringBuilder to build output batches
            val sb = StringBuilder()
            var count = 0
            for (rec in parsedRecords) {
                sb.append(rec.dateStr)
                    .append(',')
                    .append(rec.productIdOrName)
                    .append(',')
                    .append(rec.currency)
                    .append(',')
                    .append(rec.price)
                    .append('\n')
                count++
                if (count % 1000 == 0) {
                    send(sb.toString())
                    sb.setLength(0)
                }
            }
            if (sb.isNotEmpty()) {
                send(sb.toString())
            }
        }
    }

    /**
     * Processes a sequence of lines in chunks.
     * This version builds each chunk manually from the iterator,
     * avoiding the creation of an intermediate list for the entire file.
     */
    private suspend inline fun processChunkSequence(
        lines: Sequence<String>,
        chunkSize: Int,
        crossinline operation: suspend (List<String>) -> Unit
    ) {
        coroutineScope {
            val iterator = lines.iterator()
            val chunk = ArrayList<String>(chunkSize)
            while (iterator.hasNext()) {
                chunk.add(iterator.next())
                if (chunk.size == chunkSize) {
                    // Create an immutable copy of the chunk before launching.
                    launch(Dispatchers.IO) { operation(chunk.toList()) }
                    chunk.clear()
                }
            }
            if (chunk.isNotEmpty()) {
                launch(Dispatchers.IO) { operation(chunk.toList()) }
            }
        }
    }


    // Extension property to safely extract file extension
    private val MultipartFile.extension: String
        get() = originalFilename?.substringAfterLast('.', "") ?: ""
}



/*
@Service
class TradeEnrichmentService(
    private val fileServiceFactory: FileServiceFactory,
    private val tradeRedisRepository: TradeRedisRepository
) {

    suspend fun processProducts(file: MultipartFile) {
        val fileService =
            fileServiceFactory.getFileService(file.originalFilename?.substringAfterLast('.', "").orEmpty())

        /*processChunk(fileService.processFile(file).drop(1), 100) { chunk ->
            val parsedData = chunk
                .mapNotNull { line ->
                    val parts = line.split(",")
                    if (parts.size == 2) parts[0] to parts[1] else null
                }
            tradeRedisRepository.saveTrades(parsedData)
        }*/
    }

    fun enrichTrade(file: MultipartFile): Flow<String> = channelFlow {
        val fileService = fileServiceFactory.getFileService(
            file.originalFilename?.substringAfterLast('.', "") ?: ""
        )
        val lines = fileService.processFile(file).asSequence().drop(1)
        processChunk(lines, 10_000) { chunk ->
            // Parse each record only once.
            val parsedRecords = chunk.mapNotNull { record ->
                val parts = record.split(',')
                if (parts.size < 4) null
                else TradeRecord(parts[0], parts[1], parts[2], parts[3])
            }.toList()

            // Extract unique product IDs.
            val uniqueProductIds = parsedRecords.asSequence()
                .map { it.productIdOrName }
                .distinct()
                .toList()

            // Retrieve product names once per chunk.
            val productNames = tradeRedisRepository.getTrades(uniqueProductIds).toMap()

            // Update records with the product names.
            parsedRecords.forEach { rec ->
                rec.productIdOrName = productNames[rec.productIdOrName] ?: "Unknown"
            }

            // Send the result in batches (avoiding a huge single string).
            parsedRecords.chunked(1_000).forEach { batch ->
                val batchString = batch.joinToString("\n") { "${it.dateStr},${it.productIdOrName},${it.currency},${it.price}" }
                send(batchString)
            }
        }
    }





    /*fun enrichTrade(file: MultipartFile): Flow<String> = channelFlow {
        val fileService = fileServiceFactory.getFileService(
            file.originalFilename?.substringAfterLast('.', "").orEmpty()
        )

        processChunk(fileService.processFile(file).drop(1), 10000) { chunk ->
            // Создаем класс для хранения разобранных данных строки
            data class ParsedRecord(val dateStr: String, var productIdOrName: String, val currency: String, val price: String)

            // Разбираем каждую строку единожды
            val parsedRecords = chunk.mapNotNull { record ->
                val parts = record.split(",")
                if (parts.size < 4) null
                else ParsedRecord(parts[0], parts[1], parts[2], parts[3])
            }

            // Извлекаем уникальные productId из уже распарсенных записей
            val uniqueProductIds = parsedRecords.asSequence()
                .map { it.productIdOrName }
                .distinct()
                .toList()

            // Получаем имена товаров по productId
            val productNames = tradeRedisRepository.getTrades(uniqueProductIds).toMap()

            // Формируем итоговые строки, используя данные из объекта ParsedRecord
            parsedRecords
                .asSequence()
                .map { rec ->

                    rec.productIdOrName=productNames[rec.productIdOrName] ?: "Unknown"

                    return@map rec

                // val productName = productNames[rec.productId] ?: "Unknown Product"
                    //"${rec.dateStr},$productName,${rec.currency},${rec.price}"
                }
                .chunked(1000) // Разбиваем на батчи по 1000 записей
                .forEach { batch ->
                    send(batch.joinToString("\n"))
                }
        }

    }*/


    /*private suspend inline fun processChunk(lines: List<String>, chunkSize: Int, crossinline operation: suspend (List<String>) -> Unit) {
        coroutineScope {
            val chunks = lines.chunked(chunkSize)
            chunks.mapIndexed { _, chunk ->
                launch(Dispatchers.IO) {
                    operation(chunk)
                }
            }
        }

    }*/

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend  fun processChunk(
        lines: Sequence<String>,
        chunkSize: Int,
         operation:  suspend (List<String>) -> Unit
    ) {
        coroutineScope {
            // Limit concurrency to avoid launching too many coroutines.
            val limitedDispatcher = Dispatchers.IO.limitedParallelism(10)
            lines.chunked(chunkSize).forEach { chunk ->
                launch(limitedDispatcher) {
                    operation(chunk)
                }
            }
        }
    }


}*/