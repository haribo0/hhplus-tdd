package io.hhplus.tdd;

import io.hhplus.tdd.point.exception.InsufficientBalanceException;
import io.hhplus.tdd.point.exception.InvalidUserException;
import io.hhplus.tdd.point.exception.PointExceedMaxBalanceException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ApiControllerAdvice extends ResponseEntityExceptionHandler {
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.status(500).body(new ErrorResponse("500", "에러가 발생했습니다."));
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleException(IllegalArgumentException e) {
        return ResponseEntity.status(400).body(new ErrorResponse("400", e.getMessage()));
    }

    @ExceptionHandler(value = InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleException(InsufficientBalanceException e) {
        return ResponseEntity.status(400).body(new ErrorResponse("400", e.getMessage()));
    }

    @ExceptionHandler(PointExceedMaxBalanceException.class)
    public ResponseEntity<ErrorResponse> handleException(PointExceedMaxBalanceException e) {
        return ResponseEntity.status(400).body(new ErrorResponse("400", e.getMessage()));
    }

    @ExceptionHandler(InvalidUserException.class)
    public ResponseEntity<ErrorResponse> handleException(InvalidUserException e) {
        return ResponseEntity.status(400).body(new ErrorResponse("400", e.getMessage()));
    }
}
