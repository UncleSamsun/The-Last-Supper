package com.goorm.thelastsupper.waiting.service;

import com.goorm.thelastsupper.account.entity.Account;
import com.goorm.thelastsupper.waiting.exception.WaitingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerWaitingServiceTest {

    @Mock
    private CustomerWaitingValidationService customerWaitingValidationService;

    @Mock
    private CustomerWaitingRedisService customerWaitingRedisService;

    @Mock
    private CustomerWaitingQueueProcessor customerWaitingQueueProcessor;

    @InjectMocks
    private CustomerWaitingService customerWaitingService;

    @Test
    void createWaiting_validatesBeforeEnqueueAndStartsProcessor() {
        Account account = Account.builder().build();
        when(customerWaitingValidationService.validateAccount("account-1")).thenReturn(account);
        when(customerWaitingRedisService.enqueue("account-1", 2)).thenReturn(true);

        customerWaitingService.createWaiting("account-1", 2);

        InOrder inOrder = inOrder(
                customerWaitingValidationService,
                customerWaitingRedisService,
                customerWaitingQueueProcessor
        );
        inOrder.verify(customerWaitingValidationService).validateAccount("account-1");
        inOrder.verify(customerWaitingValidationService).validateWaitingSetCategory();
        inOrder.verify(customerWaitingValidationService).validateAlreadyWaiting(account);
        inOrder.verify(customerWaitingRedisService).enqueue("account-1", 2);
        inOrder.verify(customerWaitingQueueProcessor).processAsyncQueue();
    }

    @Test
    void createWaiting_whenPendingAlreadyExistsThrowsAlreadyWaiting() {
        Account account = Account.builder().build();
        when(customerWaitingValidationService.validateAccount("account-1")).thenReturn(account);
        when(customerWaitingRedisService.enqueue("account-1", 2)).thenReturn(false);

        assertThrows(
                WaitingException.AlreadyWaitingException.class,
                () -> customerWaitingService.createWaiting("account-1", 2)
        );

        verifyNoInteractions(customerWaitingQueueProcessor);
    }

    @Test
    void createWaiting_whenValidationFailsDoesNotEnqueue() {
        when(customerWaitingValidationService.validateAccount("missing-account"))
                .thenThrow(new WaitingException.AccountNotFoundException());

        assertThrows(
                WaitingException.AccountNotFoundException.class,
                () -> customerWaitingService.createWaiting("missing-account", 2)
        );

        verifyNoInteractions(customerWaitingRedisService, customerWaitingQueueProcessor);
    }
}
