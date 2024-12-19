package io.hhplus.tdd.point.exception;

public class PointExceedMaxBalanceException extends RuntimeException {
    public PointExceedMaxBalanceException(String message) {
        super(message);
    }
}