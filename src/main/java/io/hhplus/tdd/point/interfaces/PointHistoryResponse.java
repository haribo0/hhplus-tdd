package io.hhplus.tdd.point.interfaces;

import io.hhplus.tdd.point.domain.PointHistory;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PointHistoryResponse {
    private long id;
    private long userId;
    private long amount;
    private String transactionType;
    private long updateMillis;

    public static PointHistoryResponse fromPointHistory(PointHistory history) {
        return new PointHistoryResponse(
                history.id(),
                history.userId(),
                history.amount(),
                history.type().name(),
                history.updateMillis()
        );
    }
}
