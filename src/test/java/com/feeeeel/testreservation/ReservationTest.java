package com.feeeeel.testreservation;

import com.feeeeel.testreservation.domain.reservation.exception.ReservationException;
import com.feeeeel.testreservation.domain.reservation.repository.ReservationRepository;
import com.feeeeel.testreservation.domain.reservation.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ReservationTest {

    private static final Logger log = LoggerFactory.getLogger(ReservationTest.class);

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationService reservationService;

    private static final int NUM_OF_USER = 100;
    private static final int NUM_OF_SUCCESS = 15;
    private final ExecutorService executorService = Executors.newFixedThreadPool(NUM_OF_USER);

    @Test
    @Transactional
    @DisplayName("100명 시도, 15명 예매 잘 되는지 확인")
    void test() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        for (int i = 1; i <= NUM_OF_USER; i++) {
            int finalI = i;
            executorService.execute(() -> {
                try {
//                    countDownLatch.await();
                    log.info("User ID: {} Start", finalI);
                    reservationService.reserve((long) finalI, 1L);
                    success.incrementAndGet();
                    reservationService.confirm((long) finalI, 1L);
                    log.info("=======User ID: {} Success!=========", finalI);
                } catch (ReservationException e) {
                    log.info("User ID: {} Failed to reserve", finalI);
                    fail.incrementAndGet();
                }
            });
            Thread.sleep(5);
        }

//        countDownLatch.countDown();
        Thread.sleep(1000);

        long reservedCount = reservationRepository.findAll().size();

        assertEquals(NUM_OF_SUCCESS, success.get());
        assertEquals(NUM_OF_USER - NUM_OF_SUCCESS, fail.get());
    }
}
