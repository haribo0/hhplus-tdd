package io.hhplus.tdd.point.domain;

import lombok.Builder;

@Builder
public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    // 보유 가능한 최대 포인트
    public static final long MAX_POINT_BALANCE = 1_000_000;
    // 보유 가능한 최소 포인트
    private static final long MIN_POINT_BALANCE = 0;

    // 한번에 충전 가능한 포인트 최소/최대 금액
    public static final long MIN_CHARGE_AMOUNT = 1_000;
    public static final long MAX_CHARGE_AMOUNT = 1_000_000;

    // 사용 가능한 포인트 최소금액
    public static final long MIN_USE_AMOUNT = 1_000;

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }


}
