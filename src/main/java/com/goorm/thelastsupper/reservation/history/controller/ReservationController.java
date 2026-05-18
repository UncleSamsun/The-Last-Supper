package com.goorm.thelastsupper.reservation.history.controller;

import com.goorm.thelastsupper.reservation.history.dto.ReservationRequest;
import com.goorm.thelastsupper.reservation.history.dto.ReservationResponse;
import com.goorm.thelastsupper.reservation.history.service.ReservationService;
import com.goorm.thelastsupper.common.security.CustomPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reservation")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReservationResponse> registerReservation(@Valid @RequestBody ReservationRequest request,
        @AuthenticationPrincipal CustomPrincipal customPrincipal
    ){
        String accountId = customPrincipal.getId();

        log.info("예약 요청 수신 - slotId={}, accountId={}", request.slotId(), accountId);
        ReservationResponse reservationResponse = reservationService.registerReservation(accountId,request);

        return ResponseEntity.ok(reservationResponse);
    }

    @DeleteMapping("/{historyId}")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReservationResponse> cancelReservation(@PathVariable("historyId") String historyId,
                                                                 @RequestParam("slotId") String slotId,
                                                                 @AuthenticationPrincipal CustomPrincipal customPrincipal
    ){
        String accountId = customPrincipal.getId();
        log.info("예약 취소 요청 수신 - slotId={}, slotId={}, accountId={}", historyId, slotId, accountId);
        reservationService.cancelReservation(slotId, historyId, accountId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{historyId}")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReservationResponse> modifyReservation(@PathVariable("historyId") String historyId,
                                                                 @AuthenticationPrincipal CustomPrincipal customPrincipal,
                                                                 @Valid @RequestBody ReservationRequest request
    ){
        String accountId = customPrincipal.getId();
        log.info("예약 수정 요청 수신 - slotId={}, accountId={}, slotId={}", historyId, accountId, request.slotId());
        ReservationResponse reservationResponse = reservationService.modifyReservation(historyId, accountId, request);

        return ResponseEntity.ok(reservationResponse);
    }

}
