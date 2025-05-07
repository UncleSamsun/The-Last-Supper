package com.goorm.thelastsupper.waiting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CustomerWaitingRedisService {

    private static final String REDIS_WAITING_QUEUE_KEY = "waiting:queue";

    private final StringRedisTemplate redisTemplate;

    public void enqueue(String accountId, int headCount) {
        String payload = accountId + ":" + headCount;
        redisTemplate.opsForList().rightPush(REDIS_WAITING_QUEUE_KEY, payload);
    }
}