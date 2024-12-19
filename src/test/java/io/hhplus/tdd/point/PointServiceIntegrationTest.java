package io.hhplus.tdd.point;

import io.hhplus.tdd.point.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class PointServiceIntegrationTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @DisplayName("getUserPoint: 유저포인트 조회에 성공한다.")
    @Test
    void getUserPoint_success() {
        // given
        long userId = 1L;
        long initialAmount = 1000L;
        userPointRepository.insertOrUpdate(userId, initialAmount);

        // when
        UserPoint result = pointService.getUserPoint(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.point()).isEqualTo(initialAmount);
    }

    @DisplayName("chargeUserPoint: 유저포인트 충전에 성공한다.")
    @Test
    void chargeUserPoint_success() {
        // given
        long userId = 1L;
        long initialAmount = 1000L;
        long chargeAmount = 1000L;
        userPointRepository.insertOrUpdate(userId, initialAmount);

        // when
        UserPoint result = pointService.chargeUserPoint(userId, chargeAmount);
        List<PointHistory> history = pointService.getPointHistory(userId);


        // then
        assertThat(result).isNotNull();
        assertThat(result.point()).isEqualTo(initialAmount + chargeAmount);
        assertThat(history.get(history.size()-1).amount()).isEqualTo(chargeAmount);
        assertThat(history.get(history.size()-1).type()).isEqualTo(TransactionType.CHARGE);
    }

    @DisplayName("useUserPoint: 유저포인트 사용에 성공한다.")
    @Test
    void useUserPoint_success() {
        // given
        long userId = 1L;
        long initialAmount = 10000L;
        long useAmount = 1000L;
        userPointRepository.insertOrUpdate(userId, initialAmount);

        // when
        UserPoint result = pointService.useUserPoint(userId, useAmount);
        List<PointHistory> history = pointService.getPointHistory(userId);


        // then
        assertThat(result).isNotNull();
        assertThat(result.point()).isEqualTo(initialAmount - useAmount);
        assertThat(history.get(history.size()-1).amount()).isEqualTo(useAmount);
        assertThat(history.get(history.size()-1).type()).isEqualTo(TransactionType.USE);
    }

    @DisplayName("getPointHistory: 유저포인트 내역 조회에 성공한다.")
    @Test
    void getPointHistory_success() {
        // given
        long userId = 1L;
        long[] chargeAmounts = {500L, 1000L, 2000L};
        for (long chargeAmount : chargeAmounts) {
            userPointRepository.insertOrUpdate(userId, chargeAmount);
            pointHistoryRepository.insert(userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());
        }

        // when
        List<PointHistory> result = pointService.getPointHistory(userId);

        // then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(chargeAmounts.length);
        assertThat(result.get(0).amount()).isEqualTo(chargeAmounts[0]);
    }


}
