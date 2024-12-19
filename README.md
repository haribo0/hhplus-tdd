
### 동시성 제어에 대한 분석 보고서

---

## 1. 개요

사용자 포인트 충전 및 사용 기능은 동시에 여러 요청이 들어오는 상황에서 데이터의 무결성과 일관성을 유지하는 것이 중요합니다. 본 보고서는 **Java 동시성 제어 메커니즘**을 활용한 사용자 포인트 시스템 구현을 다루며, 주요 문제와 해결 방안을 설명합니다. 특히, `synchronized`와 `ReentrantLock` + `ConcurrentHashMap` 방식의 성능 비교 결과와 동작 원리를 중심으로 분석합니다.

---

## 2. 동시성 문제의 정의

### 2.1 문제 설명
사용자 포인트 충전 및 사용 작업은 다수의 스레드가 동시에 동일한 사용자 계정을 수정할 수 있는 상황에서 동시성 문제가 발생할 수 있습니다.
예를 들어:
- 하나의 스레드가 포인트를 충전하는 동안, 다른 스레드가 충전된 포인트를 사용하려고 시도할 경우.
- 동시 실행 중 포인트 잔액이 예상치와 달라질 수 있음.

### 2.2 데이터 무결성 위반의 사례
- `Thread A`: 사용자 포인트를 1000 충전.
- `Thread B`: 동일한 사용자 포인트를 500 사용.
- 두 스레드가 동시에 접근할 경우, 최종 잔액이 예상치와 다를 가능성 있음.

---

## 3. 동시성 제어 방법 비교

### 3.1 `synchronized` 방식
`synchronized`는 간단한 동기화 방법으로, 특정 코드 블록 또는 메서드에 대해 한 번에 하나의 스레드만 접근할 수 있도록 보장합니다.

#### 장점
- 사용이 간단하며, 코드가 직관적.
- JDK에서 제공하는 기본 동기화 방식으로 추가 라이브러리 없이 사용 가능.

#### 단점
- **경합이 심할 경우 성능 저하**: 락 해제가 완료될 때까지 대기해야 함.
- 세밀한 제어가 어려움: 락 해제를 명시적으로 처리할 수 없음.

---

### 3.2 `ReentrantLock` 방식
각 사용자별로 고유한 락(`ReentrantLock`)을 생성하고, 락 객체를 `ConcurrentHashMap`에서 관리합니다. 이 방식은 사용자별 동기화 범위를 제한하여 성능을 개선합니다.

#### 장점
- **명시적 제어**: 락을 명시적으로 획득하고 해제할 수 있어, 복잡한 시나리오에서도 유연하게 처리 가능.
- **공정 모드 지원**: `ReentrantLock`의 `fair` 옵션을 통해 특정 스레드가 락을 독점하지 않도록 공정하게 분배.
- **세밀한 제어 가능**: 사용자 단위로 동기화 범위를 제한하여 불필요한 락 경합을 방지.

#### 단점
- 락 해제를 잊을 경우 데드락 위험 존재.

#### 공정 모드 (`fair`) 사용
- `ReentrantLock`의 공정 모드(`fair = true`)는 락 요청 순서를 유지하려고 시도하며, 특정 스레드가 락을 독점하지 않도록 합니다.
- 이는 다수의 스레드가 동시에 동일한 사용자 포인트를 수정하려고 할 때 안정적인 처리를 보장합니다.

---

## 4. 구현 방식 설명

### 4.1 사용자별 락 관리
사용자별로 고유한 락을 생성하고, 이를 `ConcurrentHashMap`에서 관리하여 특정 사용자에 대해서만 동기화하도록 구현:
```java
private final ConcurrentHashMap<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();

private ReentrantLock getLockForUser(Long userId) {
    return userLocks.computeIfAbsent(userId, id -> new ReentrantLock(true));
}
```

### 4.2 충전 및 사용 로직
- **충전**: 사용자의 현재 잔액과 요청된 충전 금액을 더한 뒤, 결과를 검증하고 저장.
- **사용**: 사용자의 현재 잔액에서 요청된 사용 금액을 차감 후, 결과를 검증하고 저장.

#### 예시 코드
```java
public UserPoint chargeUserPoint(long userId, long chargeAmount) {
    if (userId <= 0) throw new InvalidUserException();

    ReentrantLock lock = getLockForUser(userId);
    lock.lock();
    try {
        UserPoint userPoint = userPointRepository.selectById(userId);
        long newBalance = userPoint.point() + chargeAmount;
        if (newBalance > UserPoint.MAX_POINT_BALANCE) {
            throw new PointExceedMaxBalanceException("Max balance exceeded");
        }
        userPoint = userPointRepository.insertOrUpdate(userId, newBalance);
        pointHistoryRepository.insert(userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());
        return userPoint;
    } finally {
        lock.unlock();
    }
}
```

---

## 5. 테스트 및 성능 비교

### 5.1 테스트 설정
- **환경**:
    - 동일 사용자 및 각기 다른 사용자에 대해 충전 및 사용 요청을 병렬로 수행.
    - `ExecutorService`를 활용하여 스레드를 생성하고 동시성을 시뮬레이션.
- **테스트 대상 메서드**:
    - `chargeUserPoint()`
    - `useUserPoint()`

---

### 5.2 테스트 결과: 동일 사용자에 대한 동시 요청

**테스트 시나리오**
- 동일한 사용자 ID(`userId = 1L`)에 대해 10개의 충전 요청(각 1000 포인트)과 10개의 사용 요청(각 500 포인트)을 병렬로 실행.
- 예상 결과:
    - 총 충전 금액 = `10 * 1000 = 10,000` 포인트
    - 총 사용 금액 = `10 * 500 = 5,000` 포인트
    - 최종 포인트 = `10,000 - 5,000 = 5,000` 포인트

**결과 비교**
- 동기화 미적용
  - 실패
  - 데이터 무결성 위반으로 잔액이 예상과 맞지 않으며, 충전 및 사용 내역이 불일치 발생.
- synchronized 방식
  - 성공
  - 충전 및 사용 내역이 예상 결과와 일치. 데이터 무결성은 유지되었으나, 성능 저하 발생 가능.
- ReentrantLock 방식
  - 성공
  - 데이터 무결성과 성능 모두 만족. 공정 모드(fair=true)를 사용하여 안정적이고 효율적인 처리 가능.
---

### 5.3 테스트 결과: 각기 다른 사용자에 대한 동시 요청

**테스트 시나리오**
- 50명의 서로 다른 사용자(`userId = 1L ~ 50L`)에 대해 충전 요청(각 1000 포인트)을 병렬로 실행.
- 예상 결과:
    - 각 사용자 최종 포인트 = `1,000` 포인트

**결과 비교**
- synchronized 방식
  - 실행 시간: 21.239초
  - 결과 분석:
    - 모든 요청이 전역적으로 동기화 처리됨. 
    - 동일 사용자뿐만 아니라 다른 사용자 요청도 대기 상태로 전환, 성능 저하 발생.
- ReentrantLock 방식
  - 실행 시간: 0.851초
  - 결과 분석:
    - 사용자별로 개별 락을 관리하여 병렬 처리 가능. 
    - 동기화 범위를 최소화하여 성능이 크게 개선됨.
---

### 5.4 분석 및 결론

1. **동일 사용자 요청 처리**
    - `ReentrantLock`과 `synchronized` 방식 모두 데이터 무결성을 유지하며 테스트 성공.
    - 동기화를 적용하지 않을 경우 데이터 무결성이 위반되어 잔액이 맞지 않아 테스트 실패.

2. **각기 다른 사용자 요청 처리**
    - `synchronized` 방식은 모든 요청을 전역적으로 동기화 처리하여 성능 저하 발생.
    - `ReentrantLock` 방식은 사용자별로 개별 락을 관리하여 동기화 범위를 최소화, 성능 대폭 개선.

3. **최종 선택**
    - **ReentrantLock** 방식을 선택.
        - 사용자별 락 관리로 동기화 범위를 최소화하여 높은 성능 제공.
        - 공정 모드(`fair = true`)를 통해 락 요청 순서를 보장, 특정 스레드 독점 방지.

---

### 5.5 최종 테스트 결과 요약

- **동일 사용자 요청**: 데이터 무결성을 유지하며 예상 결과와 일치했습니다.
- **각기 다른 사용자 요청**: 유저 수가 증가할수록 ReentrantLock 방식이 synchronized 방식에 비해 성능 우위 대폭 상승하는 것을 확인했습니다.


---

## 6. 향후 개선 방향

1. **락 객체 관리 최적화**: 사용자가 많아질 경우 `ConcurrentHashMap`의 크기가 증가하므로, 오래된 락 객체를 제거하는 로직 추가해 볼 수 있다.
2. **분산 환경 대응**: 분산 시스템에서의 동기화를 위해 `Redis`와 같은 외부 락 메커니즘 고려해볼 수 있다.

--- 

