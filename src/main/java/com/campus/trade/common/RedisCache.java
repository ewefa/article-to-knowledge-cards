package com.campus.trade.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Redis 缓存工具：统一使用 JSON 字符串存储，避免序列化器不兼容问题
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCache {

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    public <T> T get(String key, Class<T> type) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("缓存反序列化失败，key={}", key, e);
            delete(key);
            return null;
        }
    }

    public <T> List<T> getList(String key, Class<T> type) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (Exception e) {
            log.warn("缓存反序列化失败，key={}", key, e);
            delete(key);
            return null;
        }
    }

    public <T> T get(String key, TypeReference<T> type) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("缓存反序列化失败，key={}", key, e);
            delete(key);
            return null;
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            log.warn("缓存写入失败，key={}", key, e);
        }
    }

    public void delete(String... keys) {
        for (String key : keys) {
            redisTemplate.delete(key);
        }
    }

    public boolean has(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
