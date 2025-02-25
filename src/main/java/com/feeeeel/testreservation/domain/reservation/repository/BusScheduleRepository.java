package com.feeeeel.testreservation.domain.reservation.repository;

import com.feeeeel.testreservation.domain.reservation.entity.BusSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BusScheduleRepository extends JpaRepository<BusSchedule, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BusSchedule b WHERE b.id = :id")
    Optional<BusSchedule> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("UPDATE BusSchedule b SET b.count = b.count + 1 WHERE b.id = :busScheduleId AND b.count < 15")
    int issueTicket(@Param("busScheduleId") Long busScheduleId);
}
