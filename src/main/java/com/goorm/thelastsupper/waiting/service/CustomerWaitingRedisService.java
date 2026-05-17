package com.goorm.thelastsupper.waiting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@RequiredArgsConstructor
@Component
public class CustomerWaitingRedisService {

    private static final String REDIS_WAITING_QUEUE_KEY = "waiting:queue";
    private static final String REDIS_WAITING_PENDING_KEY_PREFIX = "waiting:pending:";
    private static final Duration PENDING_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    public boolean enqueue(String accountId, int headCount) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(pendingKey(accountId), "1", PENDING_TTL);

        if (!Boolean.TRUE.equals(acquired)) {
            return false;
        }

        String payload = accountId + ":" + headCount;
        try {
            redisTemplate.opsForList().rightPush(REDIS_WAITING_QUEUE_KEY, payload);
            return true;
        } catch (RuntimeException e) {
            releasePending(accountId);
            throw e;
        }
    }

    public void releasePending(String accountId) {
        redisTemplate.delete(pendingKey(accountId));
    }

    private String pendingKey(String accountId) {
        return REDIS_WAITING_PENDING_KEY_PREFIX + accountId;
    }
}
