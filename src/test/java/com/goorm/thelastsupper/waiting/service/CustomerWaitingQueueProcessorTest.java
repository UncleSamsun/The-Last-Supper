package com.goorm.thelastsupper.waiting.service;

import com.goorm.thelastsupper.account.entity.Account;
import com.goorm.thelastsupper.waiting.entity.WaitingQueue;
import com.goorm.thelastsupper.waiting.entity.WaitingStatus;
import com.goorm.thelastsupper.waiting.exception.WaitingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerWaitingQueueProcessorTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private CustomerWaitingValidationService validationService;

    @Mock
    private CustomerWaitingRedisService redisService;

    private CustomerWaitingQueueProcessor customerWaitingQueueProcessor;

    @BeforeEach
    void setUp() {
        customerWaitingQueueProcessor = new CustomerWaitingQueueProcessor(
                redisTemplate,
                validationService,
                redisService
        );
    }

    @Test
    void processAsyncQueue_whenPayloadIsValidSavesWaitingAndReleasesPending() {
        Account account = Account.builder().build();
        account.setId("account-1");
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPop("waiting:queue")).thenReturn("account-1:4", (String) null);
        when(validationService.validateAccount("account-1")).thenReturn(account);
        when(validationService.findNextNumber()).thenReturn(7L);

        customerWaitingQueueProcessor.processAsyncQueue();

        ArgumentCaptor<WaitingQueue> captor = ArgumentCaptor.forClass(WaitingQueue.class);
        verify(validationService).waitingQueueSave(captor.capture());

        WaitingQueue saved = captor.getValue();
        assertEquals(account, saved.getAccount());
        assertEquals(4, saved.getHeadCount());
        assertEquals(WaitingStatus.WAITING, saved.getWaitingStatus());
        assertEquals(7L, saved.getNumber());
        verify(redisService).releasePending("account-1");
    }

    @Test
    void processAsyncQueue_whenValidationFailsMovesPayloadToDeadLetterAndReleasesPending() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPop("waiting:queue")).thenReturn("account-1:4", (String) null);
        when(validationService.validateAccount("account-1"))
                .thenThrow(new WaitingException.AccountNotFoundException());

        customerWaitingQueueProcessor.processAsyncQueue();

        verify(listOperations).rightPush("waiting:queue:dead-letter", "account-1:4");
        verify(redisService).releasePending("account-1");
    }

    @Test
    void processAsyncQueue_whenPayloadIsInvalidMovesPayloadToDeadLetter() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPop("waiting:queue")).thenReturn("invalid-payload", (String) null);

        customerWaitingQueueProcessor.processAsyncQueue();

        verify(listOperations).rightPush("waiting:queue:dead-letter", "invalid-payload");
    }
}
