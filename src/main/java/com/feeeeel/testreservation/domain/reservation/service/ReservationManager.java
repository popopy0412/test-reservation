package com.feeeeel.testreservation.domain.reservation.service;

import com.feeeeel.testreservation.domain.reservation.entity.BusSchedule;
import com.feeeeel.testreservation.domain.reservation.entity.Reservation;
import com.feeeeel.testreservation.domain.reservation.exception.ReservationException;
import com.feeeeel.testreservation.domain.reservation.repository.BusScheduleRepository;
import com.feeeeel.testreservation.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationManager {
    private final BusScheduleRepository busScheduleRepository;
    private final ReservationRepository reservationRepository;

    private static final String NO_BUS_SCHEDULE = "해당 버스 스케쥴이 없습니다.";

    @Async
    @Transactional
    public void cancelReservation(Long userId, Long busScheduleId) {
        BusSchedule busSchedule = busScheduleRepository.findByIdForUpdate(busScheduleId)
                .orElseThrow(() -> new ReservationException(NO_BUS_SCHEDULE));
        if (!busSchedule.cancel()) throw new ReservationException("예매 취소에 실패했습니다.");

        Reservation reservation = reservationRepository.getReservation(userId, busScheduleId)
                .orElseThrow(() -> new ReservationException("해당 예매 내역이 없습니다."));

        reservation.cancel();

        log.info("예매 취소 완료, User ID: {}, Schedule ID: {}", userId, busScheduleId);
    }
}
