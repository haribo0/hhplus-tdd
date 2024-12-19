package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
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




}
