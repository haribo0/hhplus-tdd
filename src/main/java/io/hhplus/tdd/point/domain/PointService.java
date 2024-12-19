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

    private final ConcurrentHashMap<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    // 유저 락 획득
    private ReentrantLock getLockForUser(Long userId) {
        return userLocks.computeIfAbsent(userId, id -> new ReentrantLock(true));
    }

    // 포인트 조회
    public UserPoint getUserPoint(long userId) {
        if(userId<=0) throw new InvalidUserException();

        UserPoint userPoint = userPointRepository.selectById(userId);

        return userPoint;
    }

    // 유저 포인트 충전
    public UserPoint chargeUserPoint(long userId, long chargeAmount) {
        if(userId<=0) throw new InvalidUserException();

        if (chargeAmount < UserPoint.MIN_CHARGE_AMOUNT || chargeAmount > UserPoint.MAX_CHARGE_AMOUNT) {
            throw new IllegalArgumentException("Invalid charge amount");
        }

        ReentrantLock lock = getLockForUser(userId);
        lock.lock();
        try {
            UserPoint userPoint = userPointRepository.selectById(userId);
            if (userPoint.point() == 0) {
                if (chargeAmount > UserPoint.MAX_POINT_BALANCE) {
                    throw new PointExceedMaxBalanceException("Max balance exceeded");
                }
                userPoint = userPointRepository.insertOrUpdate(userId, chargeAmount);
            } else {
                long newBalance = userPoint.point() + chargeAmount;
                if (newBalance > UserPoint.MAX_POINT_BALANCE) {
                    throw new PointExceedMaxBalanceException("Max balance exceeded");
                }
                userPoint = userPointRepository.insertOrUpdate(userId, newBalance);
            }

            pointHistoryRepository.insert(userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());
            return userPoint;
        } finally {
            lock.unlock();
        }
    }

    // 유저 포인트 사용
    public UserPoint useUserPoint(long userId, long useAmount) {
        if(userId<=0) throw new InvalidUserException();

        ReentrantLock lock = getLockForUser(userId);
        lock.lock();
        try {
            UserPoint userPoint = userPointRepository.selectById(userId);
            if (userPoint.point() < useAmount) {
                throw new InsufficientBalanceException("Insufficient Balance");
            }
            long newBalance = userPoint.point() - useAmount;
            UserPoint updatedUserPoint = userPointRepository.insertOrUpdate(userId, newBalance);

            pointHistoryRepository.insert(userId, useAmount, TransactionType.USE, System.currentTimeMillis());
            return updatedUserPoint;
        } finally {
            lock.unlock();
        }
    }

    // 포인트 내역 조회
    public List<PointHistory> getPointHistory(long userId) {
        List<PointHistory> pointHistoryList = pointHistoryRepository.selectAllByUserId(userId);
        if (pointHistoryList.isEmpty()) {
            throw new IllegalStateException("Point History not found");
        }
        return pointHistoryList;
    }

}
