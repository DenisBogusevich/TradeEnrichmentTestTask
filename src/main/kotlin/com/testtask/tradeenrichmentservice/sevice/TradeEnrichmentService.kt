package com.testtask.tradeenrichmentservice.sevice

import com.testtask.tradeenrichmentservice.model.TradeRecord
import com.testtask.tradeenrichmentservice.repository.TradeRedisRepository
import com.testtask.tradeenrichmentservice.sevice.FileServices.FileServiceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.nio.charset.StandardCharsets


@Service
class TradeEnrichmentService(
    private val fileServiceFactory: FileServiceFactory,
    private val tradeRedisRepository: TradeRedisRepository
) {


    // Преобразует поток DataBuffer в поток строк
    fun processFileStream(dataBufferFlow: Flow<DataBuffer>): Flow<String> {
        return dataBufferFlow.map { dataBuffer ->
            val bytes = ByteArray(dataBuffer.readableByteCount())
            dataBuffer.read(bytes)
            String(bytes, StandardCharsets.UTF_8)
        }
    }

    // Обработка файла для сохранения трейдов в Redis
    suspend fun processProducts(buffer: String, extension: String) {

        val fileService = fileServiceFactory.getFileService(extension)

        tradeRedisRepository.saveTrades(fileService.processProductFile(buffer))

    }

    fun enrichTrade(buffer: String, extension: String): Flow<String> = channelFlow {
        val fileService = fileServiceFactory.getFileService(extension)

        val parsedRecords = fileService.processTradeFile(buffer)
        println(parsedRecords.size)

        val uniqueProductIds = parsedRecords.map { it.productIdOrName }.toSet().toList()
        println(uniqueProductIds)
        val productNamesMap: Map<String, String> = tradeRedisRepository.getTrades(uniqueProductIds)
            .toList() // Преобразуем Flow<Pair<String, String>> в List<Pair<String, String>>
            .toMap()  // Преобразуем List в Map

        parsedRecords.forEach {
           val data = it.copy(productIdOrName =productNamesMap[it.productIdOrName] ?:"Unnown ")
            send(data.toString())
        }


    }
}
