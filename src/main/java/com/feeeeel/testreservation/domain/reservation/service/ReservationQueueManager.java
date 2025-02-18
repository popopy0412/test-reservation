package com.feeeeel.testreservation.domain.reservation.service;

import com.feeeeel.testreservation.domain.reservation.entity.BusSchedule;
import com.feeeeel.testreservation.domain.reservation.entity.Reservation;
import com.feeeeel.testreservation.domain.reservation.entity.Ticket;
import com.feeeeel.testreservation.domain.reservation.entity.vo.Status;
import com.feeeeel.testreservation.domain.reservation.exception.ReservationException;
import com.feeeeel.testreservation.domain.reservation.model.ReservationRequest;
import com.feeeeel.testreservation.domain.reservation.repository.BusScheduleRepository;
import com.feeeeel.testreservation.domain.reservation.repository.ReservationRepository;
import com.feeeeel.testreservation.domain.reservation.repository.TicketRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationQueueManager {
    // 예매 요청을 저장하는 블로킹 큐 (FIFO)
    private final BlockingQueue<ReservationRequest> reservationQueue = new LinkedBlockingQueue<>();

    // 처리 중인 예약 (confirm 대기)
    private final Map<Long, Set<Long>> pendingReservations = new ConcurrentHashMap<>();

    // 싱글 스레드에서 예매 요청을 처리하는 Executor
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final BusScheduleRepository busScheduleRepository;
    private final ReservationRepository reservationRepository;
    private final TicketRepository ticketRepository;

    private static final int MAX_CAPACITY = 15; // 최대 예매 가능 좌석 수
    private static final int CONFIRM_TIMEOUT = 5; // 확인 대기 시간 (분)

    @PostConstruct
    public void startProcessing() {
        executorService.submit(() -> {
            while (true) {
                try {
                    ReservationRequest request = reservationQueue.take(); // 요청이 없으면 대기
                    processPendingReservation(request);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
    }

    // 예매 요청 추가 (좌석 수 체크 후 큐에 추가)
    public void addReservation(ReservationRequest request) {
        Long busScheduleId = request.getBusScheduleId();
        Long userId = request.getUserId();

        BusSchedule busSchedule = busScheduleRepository.findById(busScheduleId)
                .orElseThrow(() -> new ReservationException("버스 스케쥴이 없습니다."));

        int reservedSeats = busSchedule.getCount();
        int pendingSeats = pendingReservations.getOrDefault(busScheduleId, ConcurrentHashMap.newKeySet()).size();
        int totalSeats = reservedSeats + pendingSeats;
        log.info("현재 예매 + 대기 중인 좌석: {}", totalSeats);

        //대기열 + 기존 예약자 수 = 15면 거부
        if (totalSeats >= MAX_CAPACITY) {
            log.info("User ID: {} 예매 실패 - 대기열 포함 최대 좌석 수 초과", userId);
            throw new ReservationException("예매 가능한 좌석이 없습니다.");
        }

        // 이미 예약한 사용자인지 확인
        if (reservationRepository.existsByBusScheduleIdAndUserId(busScheduleId, userId)) {
            log.info("User ID: {} 이미 예약된 상태", userId);
            throw new ReservationException("이미 예약한 일정입니다.");
        }

        // 대기열 포함 15명 이하일 때만 큐에 추가
        if (!reservationQueue.offer(request)) {
            throw new ReservationException("현재 예매 가능한 좌석이 없습니다.");
        }

        // 대기 목록에 추가
        pendingReservations.putIfAbsent(busScheduleId, ConcurrentHashMap.newKeySet());

        Set<Long> userSet = pendingReservations.get(busScheduleId);

        // 사용자가 이미 존재하면 예외 발생
        if (!userSet.add(userId)) {
            throw new ReservationException("이미 예약 진행 중인 사용자입니다.");
        }

        log.info("User ID: {} 예매 요청 추가됨 (대기 포함 남은 좌석: {})", userId, MAX_CAPACITY - totalSeats - 1);
    }

    // 예매 보류 처리 (싱글 스레드에서 실행됨)
    private void processPendingReservation(ReservationRequest request) {
        Long busScheduleId = request.getBusScheduleId();
        Long userId = request.getUserId();

        log.info("Pending reservation - User ID: {}, Bus Schedule ID: {}", userId, busScheduleId);

        // 일정 시간이 지나도 confirm 없으면 제거
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            if (pendingReservations.getOrDefault(busScheduleId, ConcurrentHashMap.newKeySet()).contains(userId)) {
                log.info("User ID: {} 예약 확인 시간 초과 - 대기 목록에서 제거", userId);
                pendingReservations.get(busScheduleId).remove(userId);
            }
        }, CONFIRM_TIMEOUT, TimeUnit.MINUTES);
    }

    // 예약 확정 (confirm 요청이 오면 실행됨)
    @Transactional
    public synchronized void confirmReservation(Long userId, Long busScheduleId) {
        Set<Long> pendingUsers = pendingReservations.get(busScheduleId);
        if (pendingUsers == null || !pendingUsers.contains(userId)) {
            throw new ReservationException("예약 대기 목록에 없습니다. 다시 예약 요청하세요.");
        }

        BusSchedule busSchedule = busScheduleRepository.findById(busScheduleId)
                .orElseThrow(() -> new ReservationException("버스 스케쥴이 없습니다."));

        if (reservationRepository.existsByBusScheduleIdAndUserId(busScheduleId, userId)) {
            throw new ReservationException("이미 예약한 일정입니다.");
        }

        if (busSchedule.getCount() > MAX_CAPACITY) {
            throw new ReservationException("예매 가능한 좌석이 없습니다.");
        }

        // 예약 확정
        Reservation reservation = Reservation.builder()
                .userId(userId)
                .status(Status.CONFIRMED)
                .busSchedule(busSchedule)
                .build();

        busSchedule.issue();
        reservationRepository.save(reservation);
        busScheduleRepository.save(busSchedule);
        ticketRepository.save(new Ticket(null, reservation.getId(), userId, busSchedule.getId()));

        // 예약 확정 후 대기 목록에서 제거
        pendingUsers.remove(userId);
        log.info("User ID: {} 예매 확정 완료", userId);
    }
}