package io.hhplus.tdd.point.interfaces;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserPointResponse {
    private long id;
    private long point;
    private long updateMillis;
}