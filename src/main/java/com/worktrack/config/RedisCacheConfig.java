package com.worktrack.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.worktrack.infra.cache.CacheNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig {
    private final Logger logger = LoggerFactory.getLogger(RedisCacheConfig.class);

    @Bean
    @ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
    public CacheManager cacheManager(RedisConnectionFactory cf, ObjectMapper globalMapper) {

        ObjectMapper redisMapper = globalMapper.copy();
        var ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.worktrack")
                .build();

        redisMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

        var serializer = new GenericJackson2JsonRedisSerializer(redisMapper);

        var baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues()
                .computePrefixWith(name -> "worktrack::" + name + "::");

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                CacheNames.USERS_BY_USERNAME, baseConfig.entryTtl(Duration.ofMinutes(10))
        );

        return RedisCacheManager.builder(cf)
                .cacheDefaults(baseConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "cache.enabled", havingValue = "false", matchIfMissing = true)
    public CacheManager noOpCacheManager() {
        logger.warn("Cache disabled — using NoOpCacheManager");
        return new NoOpCacheManager();
    }


    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new SimpleCacheErrorHandler() {

            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                logger.warn("Cache GET failed for cache={} key={} cause={}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                logger.warn("Cache PUT failed for cache={} key={} cause={}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                logger.warn("Cache EVICT failed for cache={} key={} cause={}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                logger.warn("Cache CLEAR failed for cache={} cause={}",
                        cache.getName(), exception.getMessage());
            }
        };
    }

}