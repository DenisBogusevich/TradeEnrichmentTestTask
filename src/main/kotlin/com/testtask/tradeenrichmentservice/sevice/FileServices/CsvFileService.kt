package com.testtask.tradeenrichmentservice.sevice.FileServices

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

    override fun processFile(file: MultipartFile): List<String> {
        return file.inputStream.bufferedReader().useLines { it.toList() }.drop(1)

    }



}
