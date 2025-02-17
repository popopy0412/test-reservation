package com.feeeeel.testreservation.domain.reservation.controller.dto;

import lombok.Data;

@Data
public class RequestReservationDto {
    private Long userId;
    private Long busScheduleId;
}
