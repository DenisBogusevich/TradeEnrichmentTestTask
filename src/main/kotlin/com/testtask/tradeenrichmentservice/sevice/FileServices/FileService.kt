package com.testtask.tradeenrichmentservice.sevice.FileServices

import com.testtask.tradeenrichmentservice.model.TradeRecord
import kotlinx.coroutines.flow.Flow
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream

/**
 * Interface for processing files of various formats.
 * Defines methods for handling product and trade data files.
 */
sealed interface FileService {

    /**
     * Processes product data file.
     *
     * @param buffer File content as string
     * @return List<Pair<String, String>> List of product ID and name pairs
     */
    fun processProductFile(buffer: String): List<Pair<String,String>>

    /**
     * Processes trade data file.
     *
     * @param buffer File content as string
     * @return List<TradeRecord> List of trade records
     */
    fun processTradeFile(buffer: String): List<TradeRecord>
}