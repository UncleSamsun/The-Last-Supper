package com.goorm.thelastsupper.waiting.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerWaitingRedisServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ListOperations<String, String> listOperations;

    private CustomerWaitingRedisService customerWaitingRedisService;

    @BeforeEach
    void setUp() {
        customerWaitingRedisService = new CustomerWaitingRedisService(redisTemplate);
    }

    @Test
    void enqueue_whenPendingLockAcquiredPushesPayload() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(valueOperations.setIfAbsent("waiting:pending:account-1", "1", Duration.ofMinutes(10)))
                .thenReturn(true);

        boolean result = customerWaitingRedisService.enqueue("account-1", 3);

        assertTrue(result);
        verify(listOperations).rightPush("waiting:queue", "account-1:3");
    }

    @Test
    void enqueue_whenPendingLockExistsReturnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("waiting:pending:account-1", "1", Duration.ofMinutes(10)))
                .thenReturn(false);

        boolean result = customerWaitingRedisService.enqueue("account-1", 3);

        assertFalse(result);
        verifyNoInteractions(listOperations);
    }

    @Test
    void enqueue_whenPushFailsReleasesPendingLock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(valueOperations.setIfAbsent("waiting:pending:account-1", "1", Duration.ofMinutes(10)))
                .thenReturn(true);
        when(listOperations.rightPush("waiting:queue", "account-1:3"))
                .thenThrow(new IllegalStateException("redis down"));

        assertThrows(
                IllegalStateException.class,
                () -> customerWaitingRedisService.enqueue("account-1", 3)
        );

        verify(redisTemplate).delete("waiting:pending:account-1");
    }

    @Test
    void releasePendingDeletesPendingKey() {
        customerWaitingRedisService.releasePending("account-1");

        verify(redisTemplate).delete("waiting:pending:account-1");
    }
}
