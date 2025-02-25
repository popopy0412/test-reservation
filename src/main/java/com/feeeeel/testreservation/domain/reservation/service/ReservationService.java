package com.feeeeel.testreservation.domain.reservation.service;

import com.feeeeel.testreservation.domain.reservation.entity.BusSchedule;
import com.feeeeel.testreservation.domain.reservation.entity.Reservation;
import com.feeeeel.testreservation.domain.reservation.entity.vo.Status;
import com.feeeeel.testreservation.domain.reservation.exception.ReservationException;
import com.feeeeel.testreservation.domain.reservation.repository.BusScheduleRepository;
import com.feeeeel.testreservation.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {
    private final BusScheduleRepository busScheduleRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationManager reservationManager;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private static final String NO_BUS_SCHEDULE = "해당 버스 스케쥴이 없습니다.";

    @Transactional
    public void reserve(Long userId, Long busScheduleId) {
        BusSchedule busSchedule = busScheduleRepository.findByIdForUpdate(busScheduleId)
                .orElseThrow(() -> new ReservationException(NO_BUS_SCHEDULE));

        if (reservationRepository.existsReservation(userId, busScheduleId)) {
            throw new ReservationException("이미 예매한 버스 스케쥴입니다.");
        }
        if (!busSchedule.issue()) {
            throw new ReservationException("버스가 꽉찼습니다.");
        }
        reservationRepository.save(new Reservation(null, busSchedule, userId, Status.PENDING));

        scheduledExecutorService.schedule(() ->
                 reservationManager.cancelReservation(userId, busScheduleId), 5, TimeUnit.MINUTES);
    }

    @Transactional
    public void confirm(Long userId, Long busScheduleId) {
        if (!busScheduleRepository.existsById(busScheduleId)) {
            throw new ReservationException(NO_BUS_SCHEDULE);
        }
        LocalDateTime now = LocalDateTime.now();

        Reservation reservation = reservationRepository.getReservation(userId, busScheduleId)
                .orElseThrow(() -> new ReservationException("해당 예매 내역이 없습니다."));

        if (reservation.getStatus().equals(Status.CANCELED)) {
            throw new ReservationException("예매할 수 있는 기간이 만료되었습니다.");
        }

        reservation.confirm();
    }
}