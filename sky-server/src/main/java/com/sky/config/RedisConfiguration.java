package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfiguration {
    /**
     *
     * @param redisConnectionFactory
     * @return
     */
    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("开始创建redis模版对象");
        RedisTemplate redisTemplate = new RedisTemplate();
        // 自动管理 Redis 的连接（连接池、创建、释放）
        // 将 Redis 的原生命令（如 SET, GET, HSET, LPUSH）封装成了 Java 方法。
        // 把 Java 对象（如 User 对象）转换成 Redis 能存的二进制或字符串（序列化），取出来时再转回 Java 对象（反序列化）

        // 设置redis连接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 设置redis key 序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        // 建议补充的部分：设置 Value 的序列化器为 JSON
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        // 4. 设置 Hash Key (Field) 的序列化器 (解决你图片中 Key 列的乱码)
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        // 5. 设置 Hash Value 的序列化器 (解决你图片中 Value 列的乱码)
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());


        return redisTemplate;
    }
}
