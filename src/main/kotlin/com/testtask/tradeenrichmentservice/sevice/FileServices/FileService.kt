package com.testtask.tradeenrichmentservice.sevice.FileServices

import kotlinx.coroutines.flow.Flow
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream

sealed interface FileService {

    fun processFile(file: MultipartFile): List<String>
}