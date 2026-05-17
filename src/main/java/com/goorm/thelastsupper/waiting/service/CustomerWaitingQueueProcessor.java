package com.goorm.thelastsupper.waiting.service;

import com.goorm.thelastsupper.account.entity.Account;
import com.goorm.thelastsupper.waiting.entity.WaitingQueue;
import com.goorm.thelastsupper.waiting.entity.WaitingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerWaitingQueueProcessor {

    private final StringRedisTemplate redisTemplate;
    private final CustomerWaitingValidationService validationService;
    private final CustomerWaitingRedisService redisService;

    private static final String REDIS_WAITING_QUEUE_KEY = "waiting:queue";
    private static final String REDIS_WAITING_DEAD_LETTER_QUEUE_KEY = "waiting:queue:dead-letter";

    @Async("waitingExecutor")
    public void processAsyncQueue() {
        while (true) {
            String payload = redisTemplate.opsForList().leftPop(REDIS_WAITING_QUEUE_KEY);
            if (payload == null) break;

            String accountId = null;

            try {
                String[] parts = payload.split(":", 2);
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid waiting queue payload");
                }

                accountId = parts[0];
                int headCount = Integer.parseInt(parts[1]);

                // 서비스에서 쓰는 검증 및 등록 로직 재사용
                Account account = validationService.validateAccount(accountId);
                validationService.validateWaitingSetCategory();
                validationService.validateAlreadyWaiting(account);

                registerWaiting(account, headCount);
            } catch (Exception e) {
                log.error("비동기 처리 실패: {}", payload, e);
                redisTemplate.opsForList().rightPush(REDIS_WAITING_DEAD_LETTER_QUEUE_KEY, payload);
            } finally {
                if (accountId != null) {
                    redisService.releasePending(accountId);
                }
            }
        }
    }

    private void registerWaiting(Account account, int headCount) {
        Long nextNumber = validationService.findNextNumber();

        WaitingQueue waitingQueue = WaitingQueue.builder()
                .account(account)
                .headCount(headCount)
                .waitingStatus(WaitingStatus.WAITING)
                .number(nextNumber)
                .build();

        validationService.waitingQueueSave(waitingQueue);
    }
}
