package com.feeeeel.testreservation.domain.reservation.repository;

import com.feeeeel.testreservation.domain.reservation.entity.BusSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BusScheduleRepository extends JpaRepository<BusSchedule, Long> {
//    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BusSchedule b WHERE b.id = :id")
    Optional<BusSchedule> findByIdForUpdate(@Param("id") Long id);
}
