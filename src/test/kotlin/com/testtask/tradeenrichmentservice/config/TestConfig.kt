package com.testtask.tradeenrichmentservice.config

import com.testtask.tradeenrichmentservice.sevice.TradeEnrichmentService
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.web.reactive.function.client.WebClient

@TestConfiguration
class TestConfig {
    @Bean
    fun dataBufferFactory() = DefaultDataBufferFactory()

    @Bean
    fun webClient() = WebClient.builder().build()

    @Bean
    fun tradeEnrichmentService(): TradeEnrichmentService = mockk()
} 