package io.hhplus.tdd.point.domain;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.exception.InsufficientBalanceException;
import io.hhplus.tdd.point.exception.InvalidUserException;
import io.hhplus.tdd.point.exception.PointExceedMaxBalanceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    // 포인트 조회
    public UserPoint getUserPoint(long userId) {
        if(userId<=0) throw new InvalidUserException();

        UserPoint userPoint = userPointRepository.selectById(userId);

        return userPoint;
    }



}
