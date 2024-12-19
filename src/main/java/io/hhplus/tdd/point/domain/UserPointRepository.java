package io.hhplus.tdd.point.domain;

public interface UserPointRepository {
    UserPoint selectById(long userId);
    UserPoint insertOrUpdate(long userId, long amount);

}
