package io.hhplus.tdd.point.interfaces;

import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.PointService;
import io.hhplus.tdd.point.domain.UserPoint;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/point")
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);
    private final PointService pointService;

    /**
     * 포인트 조회
     */
    @GetMapping("{id}")
    public UserPointResponse point(
            @PathVariable long id
    ) {
        UserPoint userPoint = pointService.getUserPoint(id);
        return new UserPointResponse(userPoint.id(), userPoint.point(), userPoint.updateMillis());
    }

    /**
     * TODO: 포인트 내역 조회
     */
    @GetMapping("{id}/histories")
    public List<PointHistory> history(
            @PathVariable long id
    ) {
        return List.of();
    }

    /**
     * 포인트 충전
     */
    @PatchMapping("{id}/charge")
    public UserPointResponse charge(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        UserPoint userPoint = pointService.chargeUserPoint(id, amount);
        return new UserPointResponse(userPoint.id(), userPoint.point(), userPoint.updateMillis());
    }

    /**
     * 포인트 사용
     */
    @PatchMapping("{id}/use")
    public UserPointResponse use(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        UserPoint userPoint = pointService.useUserPoint(id, amount);
        return new UserPointResponse(userPoint.id(), userPoint.point(), userPoint.updateMillis());
    }
}
