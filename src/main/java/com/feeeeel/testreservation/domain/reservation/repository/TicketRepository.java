package com.feeeeel.testreservation.domain.reservation.repository;

import com.feeeeel.testreservation.domain.reservation.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
}
