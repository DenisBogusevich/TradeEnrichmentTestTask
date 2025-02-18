package com.testtask.tradeenrichmentservice.config

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedisConfig {

    @Bean
    fun redisClient(): RedisClient {
        return RedisClient.create("redis://localhost:6379")
    }

    @Bean
    fun redisConnection(redisClient: RedisClient): StatefulRedisConnection<String, String> {
        return redisClient.connect()
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    @Bean
    fun redisCoroutineCommands(redisConnection: StatefulRedisConnection<String, String>): RedisCoroutinesCommands<String, String> {
        return redisConnection.coroutines()
    }
}
