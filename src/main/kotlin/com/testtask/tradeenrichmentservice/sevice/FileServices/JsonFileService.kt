package com.testtask.tradeenrichmentservice.sevice.FileServices

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

/*@Service
class JsonFileService : FileService {
    private val objectMapper = jacksonObjectMapper()

    override fun processFile(file: MultipartFile): Flow<Pair<String, String>> = flow {
        file.inputStream.use { inputStream ->
            val products: List<Map<String, Any>> = objectMapper.readValue(inputStream)
            products.forEach { emit("${it["productId"]},${it["productName"]}") }
        }
    }
}
*/