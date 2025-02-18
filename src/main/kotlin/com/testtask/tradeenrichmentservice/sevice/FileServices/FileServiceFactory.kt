package com.testtask.tradeenrichmentservice.sevice.FileServices

import org.springframework.stereotype.Service

@Service
class FileServiceFactory(
    private val csvFileService: CsvFileService,
    private val xmlFileService: FileService,
    private val jsonFileService: FileService
) {
    fun getFileService(fileType: String): FileService {
        return when (fileType.lowercase()) {
            "csv" -> csvFileService
            "xml" -> xmlFileService
            "json" -> jsonFileService
            else -> throw IllegalArgumentException("Unsupported file type: $fileType")
        }
    }
}
