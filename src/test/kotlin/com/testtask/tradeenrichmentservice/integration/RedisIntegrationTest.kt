package com.testtask.tradeenrichmentservice.integration

import com.redis.testcontainers.RedisContainer
import com.testtask.tradeenrichmentservice.repository.TradeRedisRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@Testcontainers
class RedisIntegrationTest {
    companion object {
        @Container
        val redis = RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG))

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.firstMappedPort }
        }
    }

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @Autowired
    private lateinit var tradeRedisRepository: TradeRedisRepository

    @BeforeEach
    fun setup() {
        redisTemplate.connectionFactory?.connection?.flushAll()
    }

    @Test
    fun `should save and retrieve single trade`() = runBlocking {
        val trade = "1" to "Product A"

        tradeRedisRepository.saveTrades(listOf(trade))

        val result = tradeRedisRepository.getTrades(listOf("1")).toList()
        assertEquals(1, result.size)
        assertEquals(trade, result[0])
    }

    @Test
    fun `should save and retrieve multiple trades`() = runBlocking {
        val trades = listOf(
            "1" to "Product A",
            "2" to "Product B",
            "3" to "Product C"
        )

        tradeRedisRepository.saveTrades(trades)

        val result = tradeRedisRepository.getTrades(trades.map { it.first }).toList()
        assertEquals(3, result.size)
        assertTrue(result.containsAll(trades))
    }

    @Test
    fun `should handle non-existent keys`() = runBlocking {
        val trades = listOf("1" to "Product A")
        tradeRedisRepository.saveTrades(trades)

        val result = tradeRedisRepository.getTrades(listOf("1", "2", "3")).toList()
        assertEquals(1, result.size)
        assertEquals(trades[0], result[0])
    }

    @Test
    fun `should handle empty input`() = runBlocking {
        val result = tradeRedisRepository.getTrades(emptyList()).toList()
        assertEquals(0, result.size)
    }

    @Test
    fun `should handle large number of trades`() = runBlocking {
        val trades = (1..10000).map { 
            it.toString() to "Product $it" 
        }

        tradeRedisRepository.saveTrades(trades)

        val result = tradeRedisRepository.getTrades(trades.map { it.first }).toList()
        assertEquals(10000, result.size)
        assertEquals(trades.toSet(), result.toSet())
    }

    @Test
    fun `should overwrite existing trades`() = runBlocking {
        val trade = "1" to "Product A"
        val updatedTrade = "1" to "Updated Product A"

        tradeRedisRepository.saveTrades(listOf(trade))
        tradeRedisRepository.saveTrades(listOf(updatedTrade))

        val result = tradeRedisRepository.getTrades(listOf("1")).toList()
        assertEquals(1, result.size)
        assertEquals(updatedTrade, result[0])
    }
} 