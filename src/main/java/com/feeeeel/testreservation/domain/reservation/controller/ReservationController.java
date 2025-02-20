package com.feeeeel.testreservation.domain.reservation.controller;

import com.feeeeel.testreservation.domain.reservation.controller.dto.RequestReservationDto;
import com.feeeeel.testreservation.domain.reservation.exception.ReservationException;
import com.feeeeel.testreservation.domain.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservation")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<Void> reserve(
            @RequestBody RequestReservationDto dto
    ) {
        try {
            reservationService.reserve(dto.getUserId(), dto.getBusScheduleId());
        } catch (ReservationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(
            @RequestBody RequestReservationDto dto
    ) {
        try {
            reservationService.confirm(dto.getUserId(), dto.getBusScheduleId());
        } catch (ReservationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
