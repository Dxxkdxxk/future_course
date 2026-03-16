package com.lzlz.springboot.security.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
@Slf4j
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.info("redis cache set success, key={}, ttlSeconds={}", key, ttl.getSeconds());
        } catch (Exception e) {
            log.warn("redis cache set failed, key={}", key, e);
        }
    }

    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                log.info("redis cache miss, key={}", key);
                return null;
            }
            log.info("redis cache hit, key={}", key);
            return objectMapper.convertValue(value, clazz);
        } catch (Exception e) {
            log.warn("redis cache get failed, key={}", key, e);
            return null;
        }
    }

    public <T> T get(String key, TypeReference<T> typeReference) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                log.info("redis cache miss, key={}", key);
                return null;
            }
            log.info("redis cache hit, key={}", key);
            return objectMapper.convertValue(value, typeReference);
        } catch (Exception e) {
            log.warn("redis cache get failed, key={}", key, e);
            return null;
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.info("redis cache delete, key={}", key);
        } catch (Exception e) {
            log.warn("redis cache delete failed, key={}", key, e);
        }
    }

    public void deleteByPrefix(String prefix) {
        try {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("redis cache delete by prefix, prefix={}, count={}", prefix, keys.size());
            }
        } catch (Exception e) {
            log.warn("redis cache delete by prefix failed, prefix={}", prefix, e);
        }
    }
}
