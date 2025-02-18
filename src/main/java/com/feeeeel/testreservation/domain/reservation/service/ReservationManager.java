package com.feeeeel.testreservation.domain.reservation.service;

import com.feeeeel.testreservation.domain.reservation.entity.BusSchedule;
import com.feeeeel.testreservation.domain.reservation.entity.Reservation;
import com.feeeeel.testreservation.domain.reservation.entity.Ticket;
import com.feeeeel.testreservation.domain.reservation.entity.vo.Status;
import com.feeeeel.testreservation.domain.reservation.exception.ReservationException;
import com.feeeeel.testreservation.domain.reservation.repository.BusScheduleRepository;
import com.feeeeel.testreservation.domain.reservation.repository.ReservationRepository;
import com.feeeeel.testreservation.domain.reservation.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationManager {

    private final BusScheduleRepository busScheduleRepository;
    private final ReservationRepository reservationRepository;
    private final Map<Long, Semaphore> busSemaphores = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> pendingReservations = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final int MAX_SEATS = 15;
    private final TicketRepository ticketRepository;

    /**
     * 버스 스케줄별 Semaphore 생성 및 관리
     */

    private Semaphore getSemaphore(Long busScheduleId) {
        return busSemaphores.computeIfAbsent(busScheduleId, id -> {
            BusSchedule schedule = busScheduleRepository.findById(busScheduleId)
                    .orElseThrow(() -> new ReservationException("버스 스케쥴이 없습니다."));
            int maxCapacity = MAX_SEATS - schedule.getCount();
            return new Semaphore(maxCapacity, true);
        });
    }

    /**
     * 예매 요청 처리 (최대 `MAX_CAPACITY` 만큼 동시 예약 가능)
     */
    public void addReservation(Long userId, Long busScheduleId) {
        Semaphore semaphore = getSemaphore(busScheduleId);

        pendingReservations.putIfAbsent(busScheduleId, ConcurrentHashMap.newKeySet());
        Set<Long> userSet = pendingReservations.get(busScheduleId);

        if (userSet.contains(userId)) {
            throw new ReservationException("이미 예약 진행 중인 사용자입니다.");
        }

        if (!semaphore.tryAcquire()) {
            throw new ReservationException("예매 가능한 좌석이 없습니다.");
        }

        userSet.add(userId);

        log.info("User ID: {} 예매 대기 목록 추가됨", userId);

        // 5분 후 자동 취소 (confirmReservation() 호출 없을 경우)
        scheduler.schedule(() -> {
            if (userSet.contains(userId)) {
                log.warn("User ID: {} 예약 시간 초과 - 자동 취소", userId);
                userSet.remove(userId);
                semaphore.release(); // 세마포어 해제
            }
        }, 5, TimeUnit.MINUTES);
    }

    /**
     * 예매 확정 (트랜잭션 처리 및 Semaphore 해제)
     */
    @Transactional
    public void confirmReservation(Long userId, Long busScheduleId) {
        BusSchedule busSchedule = getBusSchedule(busScheduleId);

        Set<Long> userSet = pendingReservations.get(busScheduleId);
        if (userSet == null || !userSet.contains(userId)) {
            throw new ReservationException("예약 대기 목록에 없습니다.");
        }

        if (!busSchedule.issue()) throw new ReservationException("X");
        busSchedule = busScheduleRepository.save(busSchedule);
        Reservation reservation = reservationRepository.save(new Reservation(null, userId, Status.CONFIRMED, busSchedule));
        ticketRepository.save(new Ticket(null, reservation.getId(), userId, busScheduleId));

        userSet.remove(userId);
        busSemaphores.get(busScheduleId).release();
        log.info("User ID: {} 예매 확정 완료", userId);
    }

    private BusSchedule getBusSchedule(Long busScheduleId) {
        return busScheduleRepository.findByIdForUpdate(busScheduleId)
                .orElseThrow(() -> new ReservationException("버스 스케쥴이 없습니다."));
    }
}