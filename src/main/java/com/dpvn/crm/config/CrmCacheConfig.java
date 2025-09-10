package com.dpvn.crm.config;

import com.dpvn.shared.config.CacheConfig;
import com.dpvn.shared.config.CacheService;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@EnableCaching
class CrmCacheConfig {
  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
    return CacheConfig.createRedisTemplate(factory);
  }

  @Bean
  public CacheService cacheService(RedisTemplate<String, Object> redisTemplate) {
    return new CacheService(redisTemplate);
  }

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory factory) {
    return CacheConfig.createCacheManager(factory);
  }
}
