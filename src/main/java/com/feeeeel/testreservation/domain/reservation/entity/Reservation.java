package com.feeeeel.testreservation.domain.reservation.entity;

import com.feeeeel.testreservation.domain.reservation.entity.vo.Status;
import com.feeeeel.testreservation.domain.reservation.exception.ReservationException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Reservation extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bus_schedule_id", nullable = false)
    private BusSchedule busSchedule;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    public void cancel() {
        if (status == Status.CANCELED) {
            throw new ReservationException("이미 취소한 예매입니다.");
        }
        this.status = Status.CANCELED;
    }

    public void confirm() {
        if (status == Status.CONFIRMED) {
            throw new ReservationException("이미 확정한 예매입니다.");
        }
        this.status = Status.CONFIRMED;
    }
}
