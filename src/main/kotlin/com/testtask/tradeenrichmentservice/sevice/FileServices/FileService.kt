package com.testtask.tradeenrichmentservice.sevice.FileServices

import com.testtask.tradeenrichmentservice.model.TradeRecord
import kotlinx.coroutines.flow.Flow
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream

sealed interface FileService {

    fun processProductFile(buffer: String): List<Pair<String,String>>


    fun processTradeFile(buffer: String): List<TradeRecord>
}