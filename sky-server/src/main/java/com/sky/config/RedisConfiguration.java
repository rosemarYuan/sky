//package com.sky.config;
//
//import com.fasterxml.jackson.annotation.JsonAutoDetect;
//import com.fasterxml.jackson.annotation.PropertyAccessor;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//
//@Configuration
//@Slf4j
//public class RedisConfiguration {
//    /**
//     *
//     * @param redisConnectionFactory
//     * @return
//     */
//    @Bean
//    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
//        log.info("开始创建redis模版对象");
//        RedisTemplate redisTemplate = new RedisTemplate();
//        // 自动管理 Redis 的连接（连接池、创建、释放）
//        // 将 Redis 的原生命令（如 SET, GET, HSET, LPUSH）封装成了 Java 方法。
//        // 把 Java 对象（如 User 对象）转换成 Redis 能存的二进制或字符串（序列化），取出来时再转回 Java 对象（反序列化）
//
//        // 设置redis连接工厂对象
//        redisTemplate.setConnectionFactory(redisConnectionFactory);
//
//        /* 配置 ObjectMapper，注册 JavaTimeModule 模块，
//           Jackson 才能正确处理 LocalDateTime
//        */
//        ObjectMapper objectMapper = new ObjectMapper();
//        // 注册 Java 8 时间模块
//        objectMapper.registerModule(new JavaTimeModule());
//        // 设置可视度
//        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//        // 启动默认的类型处理（解决反序列化时不知道是哪个类的问题）
//        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
//
//
//        // 设置redis key 序列化器
//        redisTemplate.setKeySerializer(new StringRedisSerializer());
//        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
//
//        return redisTemplate;
//    }
//}

package com.sky.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching; // 确保这里开启了缓存，或者在启动类开启
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration; // 用于设置过期时间

@Configuration
@Slf4j
public class RedisConfiguration {

    /**
     * RedisTemplate 配置
     * 用于手动操作 Redis (redisTemplate.opsForValue().set...)
     */
    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("开始创建redis模版对象");
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 复用下面的 ObjectMapper 配置逻辑
        ObjectMapper objectMapper = createObjectMapper();

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));

        return redisTemplate;
    }

    /**
     * RedisCacheManager 配置
     * 用于 @Cacheable, @CacheEvict 等注解
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 1. 创建 JSON 序列化器
        ObjectMapper objectMapper = createObjectMapper();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 2. 配置缓存规则
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // Key 使用 String 序列化
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // Value 使用 JSON 序列化
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                // (可选) 设置缓存默认过期时间，例如 1 天，防止缓存无限积压
                .entryTtl(Duration.ofDays(1))
                // 不缓存 null 值
                .disableCachingNullValues();

        // 3. 构建 CacheManager
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * 提取公共的 ObjectMapper 配置
     * 作用：让 Redis 能够将 Java 对象(包括时间)转成 JSON 字符串
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 注册 Java 8 时间模块 (解决 LocalDateTime 序列化问题)
        objectMapper.registerModule(new JavaTimeModule());
        // 设置可视度
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 启动默认的类型处理
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        return objectMapper;
    }
}