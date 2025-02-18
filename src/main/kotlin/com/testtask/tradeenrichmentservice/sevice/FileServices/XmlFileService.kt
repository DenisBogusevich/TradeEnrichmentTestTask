package com.testtask.tradeenrichmentservice.sevice.FileServices

import com.fasterxml.jackson.databind.DeserializationFeature
import kotlinx.coroutines.flow.Flow
import org.springframework.boot.convert.ApplicationConversionService.configure
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
/*
@Service
class XmlFileService : FileService {
    private val xmlMapper = XmlMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    override fun processFile(file: MultipartFile): Flow<String> = flow {
        file.inputStream.use { inputStream ->
            val root = xmlMapper.readValue(inputStream, XmlProducts::class.java)
            root.products.forEach { emit("${it.productId},${it.productName}") }
        }
    }
}*/