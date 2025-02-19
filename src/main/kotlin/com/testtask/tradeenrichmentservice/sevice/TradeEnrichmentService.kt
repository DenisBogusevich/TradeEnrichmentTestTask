package com.testtask.tradeenrichmentservice.sevice

import com.testtask.tradeenrichmentservice.model.TradeRecord
import com.testtask.tradeenrichmentservice.repository.TradeRedisRepository
import com.testtask.tradeenrichmentservice.sevice.FileServices.FileServiceFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * Service for enriching trade data.
 * Handles processing of product data files and trade operations,
 * enriching them with additional information.
 *
 * @property fileServiceFactory Factory for creating file processing services for different formats
 * @property tradeRedisRepository Repository for Redis operations
 */
@Service
class TradeEnrichmentService(
    private val fileServiceFactory: FileServiceFactory,
    private val tradeRedisRepository: TradeRedisRepository
) {
    private val logger = LoggerFactory.getLogger(TradeEnrichmentService::class.java)

    /**
     * Converts DataBuffer stream to string stream.
     *
     * @param dataBufferFlow Input stream of data buffers
     * @return Flow<String> Stream of strings
     */
    fun processFileStream(dataBufferFlow: Flow<DataBuffer>): Flow<String> {
        logger.debug("Starting to process file stream")
        return dataBufferFlow.map { dataBuffer ->
            val bytes = ByteArray(dataBuffer.readableByteCount())
            dataBuffer.read(bytes)
            String(bytes, StandardCharsets.UTF_8)
        }.also {
            logger.debug("File stream processing completed")
        }
    }

    /**
     * Processes product file and saves data to Redis.
     * 
     * @param buffer File content as string
     * @param extension File extension determining the data format
     * @throws Exception if processing or saving fails
     */
    suspend fun processProducts(buffer: String, extension: String) {
        logger.info("Processing products file with extension: $extension")
        try {
            val fileService = fileServiceFactory.getFileService(extension)
            val products = fileService.processProductFile(buffer)
            logger.info("Successfully processed ${products.size} products")
            tradeRedisRepository.saveTrades(products)
            logger.info("Products saved to Redis")
        } catch (e: Exception) {
            logger.error("Error processing products: ${e.message}", e)
            throw e
        }
    }

    /**
     * Enriches trade data with product information.
     * Processes trade records in batches for efficient memory usage.
     *
     * @param buffer Trade file content
     * @param extension File extension determining the data format
     * @return Flow<List<String>> Stream of enriched data split into batches
     */
    fun enrichTrade(buffer: String, extension: String): Flow<String> = flow {
        logger.info("Starting trade enrichment for file with extension: $extension")
        try {
            val fileService = fileServiceFactory.getFileService(extension)
            
            // Разделяем входной буфер на чанки
            val chunks = buffer.lineSequence()
                .chunked(5000)
                .map { it.joinToString("\n") }
                .toList()

            // Параллельно обрабатываем чанки
            val parsedRecords = withContext(Dispatchers.IO) {
                chunks.map { chunk ->
                    async {
                        fileService.processTradeFile(chunk)
                    }
                }.awaitAll().flatten()
            }
            
            logger.info("Parsed ${parsedRecords.size} trade records")

            val uniqueProductIds = parsedRecords.asSequence()
                .map { it.productIdOrName }
                .distinct()
                .toList()
                
            val productNamesMap = withContext(Dispatchers.IO) {
                tradeRedisRepository.getTrades(uniqueProductIds)
                    .toList()
                    .associate { it.first to it.second }
            }

            // Обрабатываем результаты
            parsedRecords.asSequence()
                .chunked(1000)
                .forEach { chunk ->
                    val enrichedChunk = chunk.joinToString("") { record ->
                        val productName = productNamesMap[record.productIdOrName] ?: "Missing Product Name"
                        "${record.dateStr}, $productName, ${record.currency}, ${record.price}\n"
                    }
                    emit(enrichedChunk)
                }

            logger.info("Trade enrichment completed successfully")
        } catch (e: Exception) {
            logger.error("Error during trade enrichment: ${e.message}", e)
            throw e
        }
    }
}
