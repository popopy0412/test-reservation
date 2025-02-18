package com.feeeeel.testreservation;

import com.feeeeel.testreservation.domain.reservation.exception.ReservationException;
import com.feeeeel.testreservation.domain.reservation.repository.ReservationRepository;
import com.feeeeel.testreservation.domain.reservation.service.ReservationManager;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
public class ReservationTest {

    private static final Logger log = LoggerFactory.getLogger(ReservationTest.class);

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationManager ReservationManager;

    private static final int NUM_OF_USER = 100;
    private static final int NUM_OF_SUCCESS = Math.min(NUM_OF_USER, 15);
    private final ExecutorService executorService = Executors.newFixedThreadPool(NUM_OF_USER);

    @Test
    @Transactional
    @DisplayName("100명 시도, 15명 예매 잘 되는지 확인")
    void test() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(NUM_OF_USER);
        for (int i = 1; i <= NUM_OF_USER; i++) {
            int finalI = i;
            executorService.execute(() -> {
                try {
                    log.info("User ID: {} Reserve Start", finalI);
                    reservationService.reserve((long) finalI, 1L);
                    log.info("=======User ID: {} Reserve Success!=========", finalI);
                } catch (ReservationException e) {
                    log.info("User ID: {} Failed to reserve", finalI);
                }
                countDownLatch.countDown();
            });
        }

        countDownLatch.await();
        Thread.sleep(1000);

        CountDownLatch countDownLatch2 = new CountDownLatch(NUM_OF_USER);

        for (int i = 1; i <= NUM_OF_USER; i++) {
            int finalI = i;
            executorService.execute(() -> {
                try {
                    log.info("User ID: {} Confirm Start", finalI);
                    reservationService.confirm((long) finalI, 1L);
                    log.info("=======User ID: {} Confirm Success!=========", finalI);
                } catch (ReservationException e) {
                    assertNotEquals("X", e.getMessage());
                    log.info("User ID: {} Failed to confirm", finalI);
                }
                countDownLatch2.countDown();
            });
        }

        countDownLatch2.await();

        long reservedCount = reservationRepository.findAll().size();

        assertEquals(NUM_OF_SUCCESS, reservedCount);
    }
}
