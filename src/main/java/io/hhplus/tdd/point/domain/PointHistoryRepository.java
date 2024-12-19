package io.hhplus.tdd.point.domain;

import java.util.List;

public interface PointHistoryRepository {
    void insert(long userId, long amount, TransactionType transactionType, long timestamp);
//    List<PointHistory> selectAllByUserId(long userId);
}
