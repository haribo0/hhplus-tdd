package io.hhplus.tdd.point.infra;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.domain.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserPointRepositoryImpl implements UserPointRepository {

    private final UserPointTable userPointTable;

    @Override
    public UserPoint selectById(long userId) {
        return userPointTable.selectById(userId);
    }

    @Override
    public UserPoint insertOrUpdate(long userId, long amount) {
        return userPointTable.insertOrUpdate(userId, amount);
    }
}