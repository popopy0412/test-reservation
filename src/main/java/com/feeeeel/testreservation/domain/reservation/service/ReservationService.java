package com.feeeeel.testreservation.domain.reservation.service;

import com.feeeeel.testreservation.domain.reservation.entity.BusSchedule;
import com.feeeeel.testreservation.domain.reservation.exception.ReservationException;
import com.feeeeel.testreservation.domain.reservation.model.ReservationRequest;
import com.feeeeel.testreservation.domain.reservation.repository.BusScheduleRepository;
import com.feeeeel.testreservation.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final BusScheduleRepository busScheduleRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationQueueManager reservationQueueManager;

    @Transactional
    public void reserve(Long userId, Long busScheduleId) {
        BusSchedule busSchedule = busScheduleRepository.findById(busScheduleId)
                .orElseThrow(() -> new ReservationException("해당 버스 스케쥴이 없습니다."));

//        if (!busSchedule.issue()) throw new ReservationException("버스가 꽉찼습니다.");
//        busScheduleRepository.save(busSchedule);
//        reservationRepository.save(Reservation.builder()
//                .userId(userId)
//                .status(Status.PENDING)
//                .busSchedule(busSchedule)
//                .build());
        reservationQueueManager.addReservation(new ReservationRequest(userId, busScheduleId, LocalDateTime.now(), false));
    }

    @Transactional
    public void confirm(Long userId, Long busScheduleId) {
        reservationQueueManager.confirmReservation(userId, busScheduleId);
    }
}