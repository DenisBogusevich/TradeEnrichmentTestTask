package com.testtask.tradeenrichmentservice.sevice.FileServices

import com.testtask.tradeenrichmentservice.model.TradeRecord
import com.testtask.tradeenrichmentservice.repository.TradeRedisRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant
import kotlin.math.min

/**
 * Service for processing CSV format files.
 * Implements FileService interface for CSV-specific processing.
 */
@Service
class CsvFileService() : FileService {
    private val logger = LoggerFactory.getLogger(CsvFileService::class.java)

    /**
     * Processes CSV product file.
     * Expects two columns: product ID and product name.
     *
     * @param buffer CSV file content
     * @return List of product ID and name pairs
     */
    override fun processProductFile(buffer: String): List<Pair<String, String>> {
        logger.info("Starting to process product CSV file")
        var lineCount = 0
        var successCount = 0

        return buffer.lineSequence().mapNotNull { line ->
            lineCount++
            val parts = line.split(",")
            if (parts.size == 2) {
                try {
                    successCount++
                    Pair(parts[0], parts[1])
                } catch (e: Exception) {
                    logger.error("Error processing line $lineCount: ${e.message}", e)
                    null
                }
            } else {
                logger.warn("Invalid line format at line $lineCount: expected 2 parts, got ${parts.size}")
                null
            }
        }.toList().also {
            logger.info("CSV processing completed. Processed $lineCount lines, successfully parsed $successCount products")
        }
    }

    /**
     * Processes CSV trade file.
     * Expects four columns: date, product ID, currency, and price.
     *
     * @param buffer CSV file content
     * @return List of trade records
     */
    override fun processTradeFile(buffer: String): List<TradeRecord> {
        logger.info("Starting to process trade CSV file")
        var lineCount = 0
        var successCount = 0

        return buffer.lineSequence().mapNotNull { line ->
            lineCount++
            val parts = line.split(",")
            if (parts.size == 4) {
                try {
                    successCount++
                    TradeRecord(
                        dateStr = parts[0],
                        productIdOrName = parts[1],
                        currency = parts[2],
                        price = parts[3]
                    )
                } catch (e: Exception) {
                    logger.error("Error processing trade at line $lineCount: ${e.message}", e)
                    null
                }
            } else {
                logger.warn("Invalid trade line format at line $lineCount: expected 4 parts, got ${parts.size}")
                null
            }
        }.toList().also {
            logger.info("Trade CSV processing completed. Processed $lineCount lines, successfully parsed $successCount trades")
        }
    }
}
