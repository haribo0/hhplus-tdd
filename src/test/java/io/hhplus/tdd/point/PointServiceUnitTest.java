package io.hhplus.tdd.point;

import io.hhplus.tdd.point.domain.*;
import io.hhplus.tdd.point.exception.InsufficientBalanceException;
import io.hhplus.tdd.point.exception.InvalidUserException;
import io.hhplus.tdd.point.exception.PointExceedMaxBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;


public class PointServiceUnitTest {

    @Mock
    private UserPointRepository userPointRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    private PointService pointService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @DisplayName("getUserPoint: 유저포인트 조회 시 유저아이디가 음수일 경우 InvalidUserException 발생한다.")
    @Test
    void getUserPoint_InvalidUser_ShouldThrowException() {
        // given
        long userId = -1L;
        when(userPointRepository.selectById(userId)).thenReturn(UserPoint.empty(userId));

        // when, then
        assertThatThrownBy(() -> pointService.getUserPoint(userId))
                .isInstanceOf(InvalidUserException.class);
    }

    @DisplayName("getUserPoint: 유저포인트 조회 시 정상적으로 조회된다.")
    @Test
    void getUserPoint_Success() {
        // given
        long userId = 1L;
        UserPoint expectedUserPoint = new UserPoint(1L, 1000L, System.currentTimeMillis());
        when(userPointRepository.selectById(userId)).thenReturn(expectedUserPoint);

        // when
        UserPoint result = pointService.getUserPoint(userId);

        // then
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(expectedUserPoint.point());
        verify(userPointRepository, times(1)).selectById(userId);
    }

    @DisplayName("getUserPoint: 유저포인트 조회 시 유저아이디가 음수일 경우 InvalidUserException 발생한다.")
    @Test
    void chargeUserPoint_InvalidUser_ShouldThrowException() {
        // given
        long userId = -1L;
        when(userPointRepository.selectById(userId)).thenReturn(UserPoint.empty(userId));

        // when, then
        assertThatThrownBy(() -> pointService.getUserPoint(userId))
                .isInstanceOf(InvalidUserException.class);
    }
    @DisplayName("chargeUserPoint: 포인트 충전 시 포인트가 정상적으로 충전된다.")
    @Test
    void chargeUserPoint_PointsCreated_ShouldCharge() {
        // given
        long userId = 1L;
        long chargeAmount = 1000L;
        when(userPointRepository.selectById(userId)).thenReturn(UserPoint.empty(userId));
        when(userPointRepository.insertOrUpdate(userId, chargeAmount)).thenReturn(new UserPoint(userId, chargeAmount, System.currentTimeMillis()));

        // when
        UserPoint updatedUserPoint = pointService.chargeUserPoint(userId, chargeAmount);

        // then
        assertThat(updatedUserPoint).isNotNull();
        assertThat(updatedUserPoint.point()).isEqualTo(chargeAmount);

        verify(pointHistoryRepository, times(1))
                .insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong());
    }



    @DisplayName("chargeUserPoint: 포인트 충전 시 -1000포인트 충전하면 예외를 발생시킨다.")
    @Test
    void chargeUserPoint_NegativePoint_ShouldThrowException() {
        // given
        long userId = 1L;
        long chargeAmount = -1000L;
        when(userPointRepository.selectById(userId)).thenReturn(UserPoint.empty(userId));

        // when & then
        assertThatThrownBy(() -> pointService.chargeUserPoint(userId, chargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid charge amount");
    }


    @DisplayName("chargeUserPoint: 포인트 충전 시, 최대 충전 금액을 초과할 경우 PointExceedMaxBalanceException 예외를 발생시킨다.")
    @Test
    void chargeUserPoint_ExceedsMaxBalance_ShouldThrowException() {
        // given
        long userId = 1L;
        long initialPoint = 900000L; // 초기 포인트 잔액
        long chargeAmount = 200000L; // 충전 금액 (총합 1100000 > MAX_POINT_BALANCE)
        when(userPointRepository.selectById(userId)).thenReturn(new UserPoint(userId, initialPoint, System.currentTimeMillis()));

        // when, then
        assertThatThrownBy(() -> pointService.chargeUserPoint(userId, chargeAmount))
                .isInstanceOf(PointExceedMaxBalanceException.class);
    }

    @DisplayName("useUserPoint: 포인트 사용시 보유 포인트가 부족할 경우 InsufficientBalanceException 예외를 발생시킨다.")
    @Test
    void useUserPoint_InsufficientBalance_ShouldThrowException() {
        // given
        long userId = 1L;
        long useAmount = 500L;
        UserPoint userPoint = new UserPoint(userId, 0L, System.currentTimeMillis());
        when(userPointRepository.selectById(userId)).thenReturn(userPoint);

        // when, then
        assertThatThrownBy(() -> pointService.useUserPoint(userId, useAmount))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @DisplayName("useUserPoint: 포인트 사용 후 보유포인트가 이전 보유포인트에서 사용포인트 차감한 것과 동일하다.")
    @Test
    void useUserPoint_Success() {
        // given
        long userId = 1L;
        long initialPoint = 1000L; // 초기 포인트 잔액
        long useAmount = 500L;    // 사용할 포인트 금액
        long expectedBalance = 500L; // 차감 후 잔액

        UserPoint initialUserPoint = new UserPoint(userId, initialPoint, System.currentTimeMillis());
        UserPoint updatedUserPoint = new UserPoint(userId, expectedBalance, System.currentTimeMillis());

        when(userPointRepository.selectById(userId)).thenReturn(initialUserPoint);
        when(userPointRepository.insertOrUpdate(userId, expectedBalance)).thenReturn(updatedUserPoint);

        // when
        UserPoint result = pointService.useUserPoint(userId, useAmount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.point()).isEqualTo(expectedBalance);
    }

}
