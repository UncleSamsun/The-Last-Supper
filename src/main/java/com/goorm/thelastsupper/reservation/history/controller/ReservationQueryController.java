package com.goorm.thelastsupper.reservation.history.controller;

import com.goorm.thelastsupper.common.security.CustomPrincipal;
import com.goorm.thelastsupper.reservation.history.dto.ReservationResponse;
import com.goorm.thelastsupper.reservation.history.service.ReservationQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reservation")
public class ReservationQueryController {

    private final ReservationQueryService reservationQueryService;

    @GetMapping("/me")
    public ResponseEntity<ReservationResponse> getMyReservationsByDate(@AuthenticationPrincipal CustomPrincipal customPrincipal,
                                                                       @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                                       @RequestParam("time") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time

                                                                            ){
        String accountId = customPrincipal.getId();
        //Date : yyyy-MM-dd
        //Time : HH:mm:ss
        log.info("조회 요청 수신 - accountId={}, reservedDate={}, reservedTime={}", accountId, date,time);

        return ResponseEntity.ok(reservationQueryService.getMyReservationsByDate(accountId, date, time));
    }

    @GetMapping("/me/all")
    public ResponseEntity<List<ReservationResponse>> getAllMyReservation(@AuthenticationPrincipal CustomPrincipal customPrincipal){
        String accountId = customPrincipal.getId();

        log.info("조회 요청 수신 - accountId={}",accountId);
        return ResponseEntity.ok(reservationQueryService.getAllMyReservation(accountId));
    }
}
