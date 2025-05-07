package com.goorm.thelastsupper.waiting.controller;

import com.goorm.thelastsupper.waiting.dto.WaitingPositionResponse;
import com.goorm.thelastsupper.waiting.dto.WaitingRequest;
import com.goorm.thelastsupper.waiting.dto.WaitingResponse;
import com.goorm.thelastsupper.waiting.service.CustomerWaitingQueueProcessor;
import com.goorm.thelastsupper.waiting.service.CustomerWaitingRedisService;
import com.goorm.thelastsupper.waiting.service.CustomerWaitingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/waiting")
public class CustomerWaitingController {
    private final CustomerWaitingService customerWaitingService;
    private final CustomerWaitingRedisService customerWaitingRedisService;
    private final CustomerWaitingQueueProcessor customerWaitingQueueProcessor;

    @PostMapping
    public ResponseEntity<WaitingResponse> createWaiting(@RequestParam String accountId,
                                                         @Valid @RequestBody WaitingRequest request) {
        log.info("accountId={}, headCount={}", accountId, request.headCount());
        customerWaitingRedisService.enqueue(accountId, request.headCount());
        customerWaitingQueueProcessor.processAsyncQueue();

        return ResponseEntity.accepted().build();
    }


    @PostMapping("/cancel")
    public ResponseEntity<WaitingResponse> cancelWaiting(@RequestParam String accountId) {
        WaitingResponse waitingResponse = customerWaitingService.cancelWaiting(accountId);

        return ResponseEntity.ok(waitingResponse);
    }

    @PostMapping("/delay")
    public ResponseEntity<WaitingResponse> delayWaiting(@RequestParam String accountId) {
        customerWaitingService.delayWaiting(accountId);

        return ResponseEntity.accepted().build();
    }

    @GetMapping("/position")
    public ResponseEntity<WaitingPositionResponse> getWaitingPosition(@RequestParam String accountId) {
        WaitingPositionResponse waitingPositionResponse = customerWaitingService.getWaitingPosition(accountId);
        return ResponseEntity.ok(waitingPositionResponse);
    }
}
