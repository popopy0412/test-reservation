package com.feeeeel.testreservation.domain.reservation.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReservationRequest {
    private final Long userId;
    private final Long busScheduleId;
    private final LocalDateTime createdTime;
    private boolean processed;

    public Long getUserId() { return userId; }
    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }

    // 5분 초과 시 만료 처리
    public boolean isExpired() {
        return Duration.between(LocalDateTime.now(), createdTime).toSeconds() > 300;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ReservationRequest
                && userId.equals(((ReservationRequest) o).userId)
                && (busScheduleId.equals(((ReservationRequest) o).busScheduleId));
    }
}