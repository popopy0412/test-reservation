package com.feeeeel.testreservation.domain.reservation.repository;

import com.feeeeel.testreservation.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.busSchedule.id = :busScheduleId AND r.userId = :userId")
    boolean existsByBusScheduleIdAndUserId(
            @Param("userId") Long userId,
            @Param("busScheduleId") Long busScheduleId
    );
}
