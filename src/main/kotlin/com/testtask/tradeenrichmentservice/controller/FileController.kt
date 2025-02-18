package com.testtask.tradeenrichmentservice.controller

import com.testtask.tradeenrichmentservice.sevice.TradeEnrichmentService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File


@RestController
@RequestMapping("/api/v1")
class FileController(private val tradeEnrichmentService: TradeEnrichmentService) {

    @PostMapping(
        "/product",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    suspend fun loadProductsStream(@RequestPart("file") file: MultipartFile){

        return tradeEnrichmentService.processProducts(file)
    }


    @PostMapping(
        "/enrich",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE] // SSE
    )
    suspend fun enrichTradesStream(@RequestPart("file") file: MultipartFile): Flow<String> {

        val test = tradeEnrichmentService.enrichTrade(file)

        return test
    }


}
