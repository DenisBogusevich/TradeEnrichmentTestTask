package com.testtask.tradeenrichmentservice.sevice.FileServices

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.testtask.tradeenrichmentservice.model.TradeRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for processing JSON format files.
 * Implements FileService interface for JSON-specific processing.
 */
@Service
class JsonFileService : FileService {
    private val logger = LoggerFactory.getLogger(JsonFileService::class.java)
    private val objectMapper = jacksonObjectMapper()

    /**
     * Processes JSON product file.
     * Expects JSON array with objects containing 'productId' and 'productName' fields.
     *
     * @param buffer JSON file content
     * @return List of product ID and name pairs
     * @throws com.fasterxml.jackson.core.JsonParseException if JSON is malformed
     * @throws com.fasterxml.jackson.databind.JsonMappingException if JSON structure doesn't match expected format
     */
    override fun processProductFile(buffer: String): List<Pair<String, String>> {
        logger.info("Starting to process product JSON file")
        var successCount = 0

        return try {
            val products: List<Map<String, Any>> = objectMapper.readValue(buffer)
            logger.debug("Parsed ${products.size} products from JSON")

            products.mapNotNull { product ->
                try {
                    val productId = product["productId"]?.toString()
                    val productName = product["productName"]?.toString()

                    if (productId != null && productName != null) {
                        successCount++
                        Pair(productId, productName)
                    } else {
                        logger.warn("Invalid product data: missing productId or productName")
                        null
                    }
                } catch (e: Exception) {
                    logger.error("Error processing product: ${e.message}", e)
                    null
                }
            }.also {
                logger.info("JSON processing completed. Successfully parsed $successCount products")
            }
        } catch (e: Exception) {
            logger.error("Error parsing JSON file: ${e.message}", e)
            throw e
        }
    }

    /**
     * Processes JSON trade file.
     * Expects JSON array with objects containing 'date', 'productId', 'currency', and 'price' fields.
     *
     * @param buffer JSON file content
     * @return List of trade records
     * @throws com.fasterxml.jackson.core.JsonParseException if JSON is malformed
     * @throws com.fasterxml.jackson.databind.JsonMappingException if JSON structure doesn't match expected format
     */
    override fun processTradeFile(buffer: String): List<TradeRecord> {
        logger.info("Starting to process trade JSON file")
        var successCount = 0

        return try {
            val trades: List<Map<String, Any>> = objectMapper.readValue(buffer)
            logger.debug("Parsed ${trades.size} trades from JSON")

            trades.mapNotNull { trade ->
                try {
                    val date = trade["date"]?.toString()
                    val productId = trade["productId"]?.toString()
                    val currency = trade["currency"]?.toString()
                    val price = trade["price"]?.toString()

                    if (date != null && productId != null && currency != null && price != null) {
                        successCount++
                        TradeRecord(
                            dateStr = date,
                            productIdOrName = productId,
                            currency = currency,
                            price = price
                        )
                    } else {
                        logger.warn("Invalid trade data: missing required fields")
                        null
                    }
                } catch (e: Exception) {
                    logger.error("Error processing trade: ${e.message}", e)
                    null
                }
            }.also {
                logger.info("JSON trade processing completed. Successfully parsed $successCount trades")
            }
        } catch (e: Exception) {
            logger.error("Error parsing JSON trade file: ${e.message}", e)
            throw e
        }
    }
}