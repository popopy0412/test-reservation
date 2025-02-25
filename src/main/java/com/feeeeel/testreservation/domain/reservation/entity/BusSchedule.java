package com.feeeeel.testreservation.domain.reservation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class BusSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false)
    private Integer count;

    @Version
    private Integer version;

    public boolean issue() {
        if (count >= 15) return false;
        count++;
        return true;
    }

    public boolean cancel() {
        if (count <= 0) return false;
        count--;
        return true;
    }

    public void init() {
        count = 0;
    }
}
