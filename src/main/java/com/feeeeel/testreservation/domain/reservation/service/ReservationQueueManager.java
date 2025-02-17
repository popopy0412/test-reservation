package com.feeeeel.testreservation.domain.reservation.service;

import com.feeeeel.testreservation.domain.reservation.exception.ReservationException;
import com.feeeeel.testreservation.domain.reservation.model.ReservationRequest;
import com.feeeeel.testreservation.domain.reservation.repository.BusScheduleRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@Component
public class ReservationQueueManager {
    private final Map<Long, ConcurrentLinkedQueue<Long>> reservationQueues = new ConcurrentHashMap<>();
    private final Map<Long, Integer> remainingSeatsMap = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledExecutorService> schedulers = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> reservatedUserIds = new ConcurrentHashMap<>();
    private final BusScheduleRepository busScheduleRepository;


    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final int MAX_QUEUE_SIZE = 15; // 최대 15개 요청만 저장

    public ReservationQueueManager(BusScheduleRepository busScheduleRepository) {
        this.busScheduleRepository = busScheduleRepository;
    }

    // 예매 요청 추가 (큐 크기 제한 적용)
    public void addReservation(ReservationRequest request) {
        Long busScheduleId = request.getBusScheduleId();
        Long userId = request.getUserId();
        if (!reservationQueues.containsKey(busScheduleId)) {
            reservationQueues.put(busScheduleId, new ConcurrentLinkedQueue<>());
            remainingSeatsMap.put(busScheduleId, busScheduleRepository.findById(busScheduleId).get().getCount());
            reservatedUserIds.put(busScheduleId, ConcurrentHashMap.newKeySet());
        }
        ConcurrentLinkedQueue<Long> queue = reservationQueues.get(busScheduleId);
        Integer remainingSeats = remainingSeatsMap.get(busScheduleId);
        if (queue.size() < Math.min(MAX_QUEUE_SIZE, remainingSeats)) {
            queue.offer(userId);
            startSchedulerForBusSchedule(busScheduleId); // 스케줄러 동적 실행
        } else {
            throw new ReservationException("현재 예매 가능한 좌석이 없습니다.");
        }
    }

    public void confirmReservation(Long userId, Long busScheduleId) {
        if(reservatedUserIds.containsKey(busScheduleId)) {
            Set<Long> reservationRequests = reservatedUserIds.get(busScheduleId);
            reservationRequests.add(userId);
        }
    }

    // 버스 스케줄별 개별 처리 스케줄러 실행
    private synchronized void startSchedulerForBusSchedule(Long busScheduleId) {
        if (schedulers.containsKey(busScheduleId)) return; // 이미 실행 중이면 return

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        schedulers.put(busScheduleId, scheduler);

        scheduler.scheduleAtFixedRate(() -> processReservations(busScheduleId), 0, 5, TimeUnit.SECONDS);
    }

    // 버스 스케줄별 예매 요청 처리 로직
    private void processReservations(Long busScheduleId) {
        ConcurrentLinkedQueue<Long> queue = reservationQueues.get(busScheduleId);
        Set<Long> reservatedUsers = reservatedUserIds.get(busScheduleId);
        if (queue == null || queue.isEmpty()) {
            stopSchedulerForBusSchedule(busScheduleId);
            return;
        }

        Long userId = queue.poll();
        if (userId != null) {
            if (reservatedUsers.contains(userId)) {
                //TODO 싱글 스레드 방식 Queue를 순회하며 예매했는지 안했는지 확인
                remainingSeatsMap.put(busScheduleId, remainingSeatsMap.get(busScheduleId)-1);
            }
            else {
                queue.offer(userId);
            }
        }
    }

    // 요청이 없으면 스케줄러 종료
    private synchronized void stopSchedulerForBusSchedule(Long busScheduleId) {
        ScheduledExecutorService scheduler = schedulers.remove(busScheduleId);
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}