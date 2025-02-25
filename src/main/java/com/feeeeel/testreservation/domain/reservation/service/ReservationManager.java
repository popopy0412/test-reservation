package com.feeeeel.testreservation.domain.reservation.service;

import com.feeeeel.testreservation.domain.reservation.entity.BusSchedule;
import com.feeeeel.testreservation.domain.reservation.entity.Reservation;
import com.feeeeel.testreservation.domain.reservation.entity.Ticket;
import com.feeeeel.testreservation.domain.reservation.entity.vo.Status;
import com.feeeeel.testreservation.domain.reservation.exception.ReservationException;
import com.feeeeel.testreservation.domain.reservation.repository.BusScheduleRepository;
import com.feeeeel.testreservation.domain.reservation.repository.ReservationRepository;
import com.feeeeel.testreservation.domain.reservation.repository.TicketRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationManager {

    private final BusScheduleRepository busScheduleRepository;
    private final ReservationRepository reservationRepository;
    private final TicketRepository ticketRepository;
    private final Map<Long, Semaphore> busSemaphores = new ConcurrentHashMap<>();
    private final Map<Long, ReentrantLock> confirmLocks = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> pendingReservations = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final int MAX_SEATS = 15;

    /**
     * 버스 스케줄별 Semaphore 생성 및 관리
     */
    @PostConstruct
    private void init() {
        List<BusSchedule> busSchedules = busScheduleRepository.findAll();

        busSchedules.forEach(busSchedule -> {
            busSemaphores.putIfAbsent(busSchedule.getId(), new Semaphore(MAX_SEATS - busSchedule.getCount(), true));
            confirmLocks.putIfAbsent(busSchedule.getId(), new ReentrantLock(true));
            pendingReservations.putIfAbsent(busSchedule.getId(), ConcurrentHashMap.newKeySet());
        });
        log.info("Semaphores initialized");
        log.info("Semaphores count: {}", busSemaphores.size());
        log.info("Semaphores available: {}", busSemaphores.size());
        log.info("Confirm Locks: {}", confirmLocks.size());
    }

    private Semaphore getSemaphore(Long busScheduleId) {
        return busSemaphores.computeIfAbsent(busScheduleId, id -> {
            BusSchedule schedule = busScheduleRepository.findById(busScheduleId)
                    .orElseThrow(() -> new ReservationException("버스 스케쥴이 없습니다."));
            int maxCapacity = MAX_SEATS - schedule.getCount();
            return new Semaphore(maxCapacity, true);
        });
    }

    /**
     * 예매 요청 처리 (남은 좌석 만큼 동시 예약 가능)
     */
    public void addReservation(Long userId, Long busScheduleId)  {
        Semaphore semaphore = getSemaphore(busScheduleId);
        Set<Long> userSet = pendingReservations.get(busScheduleId);

        if (userSet.contains(userId)) {
            throw new ReservationException("이미 예약 진행 중인 사용자입니다.");
        }

        try {
            if (!semaphore.tryAcquire(0, TimeUnit.SECONDS)) {
//                log.info("좌석 없음. User ID: {}, Bus Schedule ID: {}", userId, busScheduleId);
                throw new ReservationException("예매 가능한 좌석이 없습니다.");
            }
        } catch (InterruptedException e) {
//            log.info("좌석 없음. User ID: {}, Bus Schedule ID: {}", userId, busScheduleId);
            throw new ReservationException("예매 가능한 좌석이 없습니다.");
        }

        userSet.add(userId);

        log.info("User ID: {} 예매 대기 목록 추가됨, Bus Schedule ID: {}", userId, busScheduleId);

        // 5분 후 자동 취소 (confirmReservation() 호출 없을 경우)
        scheduler.schedule(() -> {
            if (userSet.contains(userId)) {
                log.warn("User ID: {} 예약 시간 초과 - 자동 취소", userId);
                userSet.remove(userId);
                semaphore.release(); // 세마포어 해제
            }
        }, 3, TimeUnit.SECONDS);
    }

    /**
     * 예매 확정 (트랜잭션 처리 및 Semaphore 해제)
     */
    @Transactional
    public void confirmReservation(Long userId, Long busScheduleId) {
        ReentrantLock lock = confirmLocks.get(busScheduleId);

        Set<Long> userSet = pendingReservations.get(busScheduleId);
        if (userSet == null || !userSet.contains(userId)) {
            throw new ReservationException("예약 대기 목록에 없습니다.");
        }

        lock.lock();
        try {
            BusSchedule busSchedule = busScheduleRepository.findById(busScheduleId)
                    .orElseThrow(() -> new ReservationException("해당 버스 스케쥴이 없습니다."));
            if (busScheduleRepository.issueTicket(busScheduleId) == 0) {
                throw new ReservationException("X");
            }

            Reservation reservation = reservationRepository.save(new Reservation(null, userId, Status.CONFIRMED, busSchedule));
            ticketRepository.save(new Ticket(null, reservation.getId(), userId, busScheduleId));
            userSet.remove(userId);

            log.info("User ID: {} 예매 확정 완료, Bus Schedule ID: {}", userId, busScheduleId);

            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                            log.warn("롤백 발생 - User ID: {} 예매 실패, Bus Schedule ID: {}", userId, busScheduleId);
                        }
                    }
                });
            }

        } catch (Exception e) {
            log.error("결제 실패 - User ID: {}, Bus Schedule ID: {}", userId, busScheduleId);
            throw e;
        } finally {
            lock.unlock();
        }
    }
}