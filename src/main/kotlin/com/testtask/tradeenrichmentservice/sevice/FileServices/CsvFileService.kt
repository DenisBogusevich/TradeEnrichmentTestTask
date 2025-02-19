package com.testtask.tradeenrichmentservice.sevice.FileServices

import com.testtask.tradeenrichmentservice.model.TradeRecord
import com.testtask.tradeenrichmentservice.repository.TradeRedisRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant
import kotlin.math.min

@Service
class CsvFileService() : FileService {
    override fun processProductFile(buffer: String): List<Pair<String, String>> {

        return  buffer.lineSequence().mapNotNull { line ->
            val parts = line.split(",")
            if (parts.size == 2) {
                try {
                    Pair(parts[0], parts[1])
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
            .toList()

    }

    override fun processTradeFile(buffer: String): List<TradeRecord> {

        return  buffer.lineSequence().mapNotNull { line ->
            val parts = line.split(",")
            if (parts.size == 4) {
                try {
                    TradeRecord(
                        dateStr = parts[0],
                        productIdOrName = parts[1],
                        currency = parts[2],
                        price = parts[3]
                    )
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
            .toList()

    }


}
