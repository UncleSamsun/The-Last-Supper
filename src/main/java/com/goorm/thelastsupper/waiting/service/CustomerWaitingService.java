package com.goorm.thelastsupper.waiting.service;

import com.goorm.thelastsupper.waiting.dto.WaitingPositionResponse;
import com.goorm.thelastsupper.waiting.dto.WaitingResponse;
import com.goorm.thelastsupper.waiting.entity.WaitingQueue;
import com.goorm.thelastsupper.account.entity.Account;
import com.goorm.thelastsupper.waiting.exception.WaitingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerWaitingService {
    private final CustomerWaitingValidationService customerWaitingValidationService;
    private final CustomerWaitingRedisService customerWaitingRedisService;
    private final CustomerWaitingQueueProcessor customerWaitingQueueProcessor;

    public void createWaiting(String accountId, int headCount) {
        Account account = customerWaitingValidationService.validateAccount(accountId);
        customerWaitingValidationService.validateWaitingSetCategory();
        customerWaitingValidationService.validateAlreadyWaiting(account);

        if (!customerWaitingRedisService.enqueue(accountId, headCount)) {
            throw new WaitingException.AlreadyWaitingException();
        }

        customerWaitingQueueProcessor.processAsyncQueue();
    }

    public WaitingResponse cancelWaiting(String accountId) {
        // accountId 기반으로 Account 엔티티 조회, 없면 AccountNotFoundException throw
        Account account = customerWaitingValidationService.validateAccount(accountId);

        // Account 기반으로 WaitingQueue 엔티티 조회
        // WAITING 상태의 고객이 없으면 WaitingNotFoundException throw
        // 고객이 WAITING 중이면 WaitingStatus 를 CANCEL 로 변경
        WaitingQueue waitingQueue = customerWaitingValidationService.waitingQueueCancel(account);

        return WaitingResponse.toWaitingResponse(waitingQueue);
    }

    @Transactional
    public void delayWaiting(String accountId) {
        // accountId 기반으로 Account 엔티티 조회, 없으면 AccountNotFoundException throw
        Account account = customerWaitingValidationService.validateAccount(accountId);

        // WaitingSetCategory 가 OPEN 인지 조회, OPEN 이 아니라면 WaitingNotOpenException throw
        customerWaitingValidationService.validateWaitingSetCategory();

        // 현재 대기번호가 마지막 대기번호 인지 조회
        // 기존 WaitingStatus 를 Delay 로 변경, headCount 반환
        int headCount = customerWaitingValidationService.waitingQueueDelay(account);

        if (headCount <= 0) {
            return;
        }

        if (!customerWaitingRedisService.enqueue(accountId, headCount)) {
            throw new WaitingException.AlreadyWaitingException();
        }

        customerWaitingQueueProcessor.processAsyncQueue();
    }

    public WaitingPositionResponse getWaitingPosition(String accountId) {
        // accountId 기반으로 Account 엔티티 조회, 없면 AccountNotFoundException throw
        Account account = customerWaitingValidationService.validateAccount(accountId);

        // accountId 기반으로 WaitingQueue 엔티티 조회, WAITING 상태의 고객이 없면 WaitingNotFoundException throw
        WaitingQueue waitingQueue = customerWaitingValidationService.validateWaitingQueue(account);

        // WAITING 중인 고객 중 내 번호보다 아래인 고객 수
        int ahead = customerWaitingValidationService.findAheadNumber(waitingQueue);

        // 앞 사람 수 + 1 반환
        return WaitingPositionResponse.toWaitingPositionResponse(ahead + 1);
    }
}
