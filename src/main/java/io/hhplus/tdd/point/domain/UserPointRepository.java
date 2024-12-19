package io.hhplus.tdd.point.domain;

public interface UserPointRepository {
    UserPoint selectById(long userId);
}
