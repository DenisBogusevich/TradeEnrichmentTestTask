package com.testtask.tradeenrichmentservice.sevice.FileServices

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FileServiceFactory(
    private val csvFileService: CsvFileService,
    private val xmlFileService: XmlFileService,
    private val jsonFileService: JsonFileService,
) {
    private val logger = LoggerFactory.getLogger(FileServiceFactory::class.java)

    fun getFileService(fileType: String): FileService {
        logger.debug("Requesting file service for type: $fileType")
        return try {
            when (fileType.lowercase()) {
                "csv" -> csvFileService
                "xml" -> xmlFileService
                "json" -> jsonFileService
                else -> throw IllegalArgumentException("Unsupported file type: $fileType")
            }.also {
                logger.debug("Returned file service: ${it.javaClass.simpleName}")
            }
        } catch (e: Exception) {
            logger.error("Error getting file service: ${e.message}", e)
            throw e
        }
    }
}
