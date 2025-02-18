package com.feeeeel.testreservation.domain.reservation.service;

import com.feeeeel.testreservation.domain.reservation.exception.ReservationException;
import com.feeeeel.testreservation.domain.reservation.repository.BusScheduleRepository;
import com.feeeeel.testreservation.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final BusScheduleRepository busScheduleRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationQueueManager reservationQueueManager;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public void reserve(Long userId, Long busScheduleId) {
        if (reservationRepository.existsByBusScheduleIdAndUserId(userId, busScheduleId)) {
            throw new ReservationException("이미 예약했습니다.");
        }
        try {
            reservationQueueManager.addReservation(userId, busScheduleId);
        } catch (Exception e) {
            throw new ReservationException("예매에 실패했습니다.");
        }
    }

    @Transactional
    public void confirm(Long userId, Long busScheduleId) {
        reservationQueueManager.confirmReservation(userId, busScheduleId);
    }
}