package com.testtask.tradeenrichmentservice.sevice.FileServices

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.testtask.tradeenrichmentservice.model.TradeRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for processing XML format files.
 * Implements FileService interface for XML-specific processing.
 */
@Service
class XmlFileService : FileService {
    private val logger = LoggerFactory.getLogger(XmlFileService::class.java)
    private val xmlMapper = XmlMapper().registerKotlinModule()

    /**
     * Data class for XML product structure
     */
    @JacksonXmlRootElement(localName = "products")
    data class ProductsXml(
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "product")
        val products: List<ProductXml> = emptyList()
    )

    data class ProductXml(
        @JacksonXmlProperty(localName = "productId")
        val productId: String = "",
        @JacksonXmlProperty(localName = "productName")
        val productName: String = ""
    )

    /**
     * Data class for XML trade structure
     */
    @JacksonXmlRootElement(localName = "trades")
    data class TradesXml(
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "trade")
        val trades: List<TradeXml> = emptyList()
    )

    data class TradeXml(
        @JacksonXmlProperty(localName = "date")
        val date: String = "",
        @JacksonXmlProperty(localName = "productId")
        val productId: String = "",
        @JacksonXmlProperty(localName = "currency")
        val currency: String = "",
        @JacksonXmlProperty(localName = "price")
        val price: String = ""
    )

    /**
     * Processes XML product file.
     * Expects XML structure with root element 'products' containing 'product' elements
     * with 'productId' and 'productName' fields.
     *
     * @param buffer XML file content
     * @return List of product ID and name pairs
     * @throws com.fasterxml.jackson.core.JsonParseException if XML is malformed
     * @throws com.fasterxml.jackson.databind.JsonMappingException if XML structure doesn't match expected format
     */
    override fun processProductFile(buffer: String): List<Pair<String, String>> {
        logger.info("Starting to process product XML file")
        var successCount = 0

        return try {
            val productsXml = xmlMapper.readValue(buffer, ProductsXml::class.java)
            logger.debug("Parsed ${productsXml.products.size} products from XML")

            productsXml.products.mapNotNull { product ->
                try {
                    if (product.productId.isNotBlank() && product.productName.isNotBlank()) {
                        successCount++
                        Pair(product.productId, product.productName)
                    } else {
                        logger.warn("Invalid product data: empty productId or productName")
                        null
                    }
                } catch (e: Exception) {
                    logger.error("Error processing product: ${e.message}", e)
                    null
                }
            }.also {
                logger.info("XML processing completed. Successfully parsed $successCount products")
            }
        } catch (e: Exception) {
            logger.error("Error parsing XML file: ${e.message}", e)
            throw e
        }
    }

    /**
     * Processes XML trade file.
     * Expects XML structure with root element 'trades' containing 'trade' elements
     * with 'date', 'productId', 'currency', and 'price' fields.
     *
     * @param buffer XML file content
     * @return List of trade records
     * @throws com.fasterxml.jackson.core.JsonParseException if XML is malformed
     * @throws com.fasterxml.jackson.databind.JsonMappingException if XML structure doesn't match expected format
     */
    override fun processTradeFile(buffer: String): List<TradeRecord> {
        logger.info("Starting to process trade XML file")
        var successCount = 0

        return try {
            val tradesXml = xmlMapper.readValue(buffer, TradesXml::class.java)
            logger.debug("Parsed ${tradesXml.trades.size} trades from XML")

            tradesXml.trades.mapNotNull { trade ->
                try {
                    if (trade.date.isNotBlank() && trade.productId.isNotBlank() &&
                        trade.currency.isNotBlank() && trade.price.isNotBlank()
                    ) {
                        successCount++
                        TradeRecord(
                            dateStr = trade.date,
                            productIdOrName = trade.productId,
                            currency = trade.currency,
                            price = trade.price
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
                logger.info("XML trade processing completed. Successfully parsed $successCount trades")
            }
        } catch (e: Exception) {
            logger.error("Error parsing XML trade file: ${e.message}", e)
            throw e
        }
    }
}