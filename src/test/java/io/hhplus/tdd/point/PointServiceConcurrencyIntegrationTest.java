package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PointServiceConcurrencyIntegrationTest {

    @Autowired
    private PointService pointService;
    @Autowired
    private UserPointTable userPointTable;
    @Autowired
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        // 초기 유저 포인트 설정(0으로 시작)
        userPointTable.insertOrUpdate(1L, 0L);
    }

    @DisplayName("chargeUserPoint: 유저가 동시에 충전 요청 했을 경우 작업이 끝난 후 보유 포인트가 매 충전포인트 * 요청횟수와 동일하다.  ")
    @Test
    void testConcurrentCharge() throws InterruptedException {

        // given
        long userId = 1L;
        int threadCount = 10;
        long chargeAmount = 1000L;


        //when
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        // 충전 스레드 생성
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드 대기
                    pointService.chargeUserPoint(userId, chargeAmount);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // 모든 스레드가 준비되었으므로 시작 (동시에 시작)
        startLatch.countDown();
        // 모든 스레드 작업 완료 대기
        doneLatch.await();
        executor.shutdown();


        // then
        long totalCharged = threadCount * chargeAmount;
        UserPoint finalPoint = pointService.getUserPoint(userId);
        // 최종 포인트는 총 충전량과 동일한지 확인
        assertThat(finalPoint.point()).isEqualTo(totalCharged);
        // 충전 기록 내역수 검증
        var histories = pointHistoryTable.selectAllByUserId(userId);
        long chargeHistoryCount = histories.stream()
                .filter(h -> h.type() == TransactionType.CHARGE)
                .count();
        assertThat(chargeHistoryCount).isEqualTo(threadCount);
    }

    @DisplayName("useUserPoint: 유저가 동시에 충전 요청 했을 경우 작업이 끝난 후 보유 포인트가 매 충전포인트 * 요청횟수와 동일하다.  ")
    @Test
    void testConcurrentUse() throws InterruptedException {

        //given
        long userId = 1L;
        int threadCount = 10;
        long useAmount = 500L;

        //when
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        // 사용 스레드 생성
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드 대기
                    pointService.useUserPoint(userId, useAmount);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // 모든 스레드가 준비되었으므로 시작 (동시에 시작)
        startLatch.countDown();
        // 모든 스레드 작업 완료 대기
        doneLatch.await();
        executor.shutdown();


        // then
        long totalUsed = threadCount * useAmount;
        UserPoint finalPoint = pointService.getUserPoint(userId);
        // 최종 포인트는 충전량 - 총 사용량이어야 한다.
        assertThat(finalPoint.point()).isEqualTo(totalUsed);
        // 사용 기록 검증
        var histories = pointHistoryTable.selectAllByUserId(userId);
        long useHistoryCount = histories.stream()
                .filter(h -> h.type() == TransactionType.USE)
                .count();
        assertThat(useHistoryCount).isEqualTo(threadCount);
    }

    @DisplayName("동시에 충전/사용 요청 들어올 경우 최종 보유 포인트와 사용내역이 기대값과 동일하다.  ")
    @Test
    void testConcurrentChargeAndUseWithFairLock() throws InterruptedException {
        int threadCount = 20;
        int chargeThreadCount = 10;
        int useThreadCount = 10;

        // 충전 및 사용 금액 설정
        long chargeAmount = 1000L;
        long useAmount = 500L;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger chargeCompleted = new AtomicInteger(0);
        AtomicInteger useCompleted = new AtomicInteger(0);

        // 충전 스레드 5개 생성
        for (int i = 0; i < chargeThreadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드 대기
                    pointService.chargeUserPoint(1L, chargeAmount);
                    chargeCompleted.incrementAndGet();
                } catch (Exception e) {
                    // 예외 발생 시 출력(테스트 실패 요인)
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 사용 스레드 5개 생성
        for (int i = 0; i < useThreadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드 대기
                    pointService.useUserPoint(1L, useAmount);
                    useCompleted.incrementAndGet();
                } catch (Exception e) {
                    // 예외 발생 시 출력(테스트 실패 요인)
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 모든 스레드가 준비되었으므로 시작
        startLatch.countDown();

        // 모든 스레드 작업 완료 대기
        doneLatch.await();
        executor.shutdown();

        // 최종 결과 검증
        // 총 충전량: chargeThreadCount * chargeAmount
        long totalCharged = chargeThreadCount * chargeAmount;
        // 총 사용량: useThreadCount * useAmount
        long totalUsed = useThreadCount * useAmount;

        // 최종 포인트 조회
        UserPoint finalPoint = pointService.getUserPoint(1L);

        // 최종 포인트는 (총 충전량 - 총 사용량)이어야 한다.
        assertThat(finalPoint.point()).isEqualTo(totalCharged - totalUsed);

        // 모든 충전/사용 요청이 성공했는지 확인
        assertThat(chargeCompleted.get()).isEqualTo(chargeThreadCount);
        assertThat(useCompleted.get()).isEqualTo(useThreadCount);

        // 히스토리 검증 (개수만 확인)
        var histories = pointHistoryTable.selectAllByUserId(1L);
        // 충전 + 사용 총합
        assertThat(histories).hasSize(threadCount);

        // 충전/사용 트랜잭션 각각 count
        long chargeHistoryCount = histories.stream()
                .filter(h -> h.type() == TransactionType.CHARGE)
                .count();
        long useHistoryCount = histories.stream()
                .filter(h -> h.type() == TransactionType.USE)
                .count();

        assertThat(chargeHistoryCount).isEqualTo(chargeThreadCount);
        assertThat(useHistoryCount).isEqualTo(useThreadCount);
    }

}
