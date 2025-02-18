package com.testtask.tradeenrichmentservice.repository

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.withContext
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

@Repository
class TradeRedisRepository(private val redisTemplate: StringRedisTemplate) {

    fun saveTrades(trades: List<Pair<String, String>>) {
        redisTemplate.executePipelined { connection ->
            trades.forEach { (key, value) ->
                connection.stringCommands().set(key.toByteArray(), value.toByteArray())
            }
            null // Возвращаем `null`, так как executePipelined требует `Any?`
        }
    }


    fun getTrades(keys: List<String>): List<Pair<String, String>> {
        val results = redisTemplate.executePipelined { connection ->

            keys.forEach { key ->
                connection.stringCommands().get(key.toByteArray())
            }
            null // Обязательно вернуть null, иначе executePipelined не завершится корректно
        }


        return keys.zip(results.filterNotNull().map { it.toString() })
    }

}
