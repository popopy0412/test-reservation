package com.feeeeel.testreservation;

import com.feeeeel.testreservation.domain.reservation.entity.BusSchedule;
import com.feeeeel.testreservation.domain.reservation.repository.BusScheduleRepository;
import com.feeeeel.testreservation.domain.reservation.repository.ReservationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ApplicationInitializer {

    private final ReservationRepository reservationRepository;
    private final BusScheduleRepository busScheduleRepository;

    @PostConstruct
    @Transactional
    public void init() {
        reservationRepository.deleteAllInBatch();
        for (BusSchedule busSchedule : busScheduleRepository.findAll()) {
            busSchedule.setCount(0);
            busScheduleRepository.save(busSchedule);
        }
    }
}
