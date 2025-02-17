package com.feeeeel.testreservation.domain.reservation.service;

import com.feeeeel.testreservation.domain.reservation.entity.BusSchedule;
import com.feeeeel.testreservation.domain.reservation.entity.Reservation;
import com.feeeeel.testreservation.domain.reservation.exception.ReservationException;
import com.feeeeel.testreservation.domain.reservation.repository.BusScheduleRepository;
import com.feeeeel.testreservation.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final BusScheduleRepository busScheduleRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    @Async
    public void reserve(Long userId, Long busScheduleId) {
        BusSchedule busSchedule = busScheduleRepository.findById(busScheduleId)
                .orElseThrow(() -> new ReservationException("해당 버스 스케쥴이 없습니다."));

        if (!busSchedule.issue()) throw new ReservationException("버스가 꽉찼습니다.");
        busScheduleRepository.save(busSchedule);
        reservationRepository.save(new Reservation(null, busSchedule, userId));
    }
}