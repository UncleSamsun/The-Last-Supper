package com.goorm.thelastsupper.waiting.controller;

import com.goorm.thelastsupper.common.security.CustomPrincipal;
import com.goorm.thelastsupper.waiting.dto.WaitingPositionResponse;
import com.goorm.thelastsupper.waiting.dto.WaitingRequest;
import com.goorm.thelastsupper.waiting.dto.WaitingResponse;
import com.goorm.thelastsupper.waiting.service.CustomerWaitingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/waiting")
public class CustomerWaitingController {
    private final CustomerWaitingService customerWaitingService;

    @PostMapping
    public ResponseEntity<WaitingResponse> createWaiting(@AuthenticationPrincipal CustomPrincipal customPrincipal,
                                                         @Valid @RequestBody WaitingRequest request) {
        String accountId = customPrincipal.getId();
        log.info("accountId={}, headCount={}", accountId, request.headCount());
        customerWaitingService.createWaiting(accountId, request.headCount());

        return ResponseEntity.accepted().build();
    }


    @PostMapping("/cancel")
    public ResponseEntity<WaitingResponse> cancelWaiting(@AuthenticationPrincipal CustomPrincipal customPrincipal) {
        String accountId = customPrincipal.getId();
        WaitingResponse waitingResponse = customerWaitingService.cancelWaiting(accountId);

        return ResponseEntity.ok(waitingResponse);
    }

    @PostMapping("/delay")
    public ResponseEntity<WaitingResponse> delayWaiting(@AuthenticationPrincipal CustomPrincipal customPrincipal) {
        String accountId = customPrincipal.getId();
        customerWaitingService.delayWaiting(accountId);

        return ResponseEntity.accepted().build();
    }

    @GetMapping("/position")
    public ResponseEntity<WaitingPositionResponse> getWaitingPosition(@AuthenticationPrincipal CustomPrincipal customPrincipal) {
        String accountId = customPrincipal.getId();
        WaitingPositionResponse waitingPositionResponse = customerWaitingService.getWaitingPosition(accountId);
        return ResponseEntity.ok(waitingPositionResponse);
    }
}
