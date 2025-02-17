package com.feeeeel.testreservation.domain.reservation.entity;

import com.feeeeel.testreservation.domain.reservation.entity.vo.Status;
import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne
    @JoinColumn(name = "bus_schedule_id", nullable = false)
    private BusSchedule busSchedule;
}
