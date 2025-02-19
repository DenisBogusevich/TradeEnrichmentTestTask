package com.testtask.tradeenrichmentservice.repository

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

/**
 * Repository for Redis operations.
 * Handles saving and retrieving product data.
 *
 * @property redisTemplate Template for Redis operations
 */
@Repository
class TradeRedisRepository(private val redisTemplate: StringRedisTemplate) {
    private val logger = LoggerFactory.getLogger(TradeRedisRepository::class.java)

    /**
     * Saves product data to Redis using pipelined operations.
     *
     * @param trades List of product ID and name pairs
     * @throws Exception if Redis operation fails
     */
    fun saveTrades(trades: List<Pair<String, String>>) {
        logger.info("Starting to save ${trades.size} trades to Redis")
        try {
            redisTemplate.executePipelined { connection ->
                trades.forEach { (key, value) ->
                    connection.stringCommands().set(key.toByteArray(), value.toByteArray())
                }
                null
            }
            logger.info("Successfully saved ${trades.size} trades to Redis")
        } catch (e: Exception) {
            logger.error("Error saving trades to Redis: ${e.message}", e)
            throw e
        }
    }

    /**
     * Retrieves product data from Redis.
     *
     * @param keys List of product IDs to retrieve
     * @return Flow<Pair<String, String>> Stream of product ID and name pairs
     * @throws Exception if Redis operation fails
     */
    fun getTrades(keys: List<String>): Flow<Pair<String, String>> {
        logger.info("Retrieving ${keys.size} trades from Redis")
        try {
            val results = redisTemplate.executePipelined { connection ->
                keys.forEach { key ->
                    connection.stringCommands().get(key.toByteArray())
                }
                null
            }
            
            val foundCount = results.filterNotNull().size
            logger.info("Retrieved $foundCount trades from Redis")
            if (foundCount < keys.size) {
                logger.warn("${keys.size - foundCount} trades were not found in Redis")
            }

            return keys.zip(results.filterNotNull().map { it.toString() }).asFlow()
        } catch (e: Exception) {
            logger.error("Error retrieving trades from Redis: ${e.message}", e)
            throw e
        }
    }
}
