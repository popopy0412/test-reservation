package com.feeeeel.testreservation.domain.reservation.repository;

import com.feeeeel.testreservation.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
//    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.userId = :userId AND r.busSchedule.id = :busScheduleId AND r.status = 'PENDING'")
    Optional<Reservation> getReservation(
            @Param("userId") Long userId,
            @Param("busScheduleId") Long busScheduleId
    );

    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.userId = :userId AND r.busSchedule.id = :busScheduleId AND r.status != 'CANCELED'")
    boolean existsReservation(
            @Param("userId") Long userId,
            @Param("busScheduleId") Long busScheduleId
    );
}
