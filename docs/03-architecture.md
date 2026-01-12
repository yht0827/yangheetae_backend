# 시스템 아키텍쳐

## 1. 동시성 처리 및 락 전략

### 1.1 기본 전략: Pessimistic Lock

잔액 업데이트는 경쟁이 발생할 수 있으므로, 계좌 row에 대해 `SELECT ... FOR UPDATE` 기반의 비관적 락을 사용한다.
이 방식은 충돌 시 재시도 로직이 필요 없고, 잔액 정합성을 단순하게 보장할 수 있다.


### 1.2 데드락 방지: 락 획득 순서 고정

이체는 두 계좌를 동시에 업데이트하므로, 항상 “작은 ID → 큰 ID” 순서로 락을 잡는다.
이를 통해 서로 반대 방향 이체가 동시에 발생해도 데드락을 예방한다.

```java
Long firstId = Math.min(fromAccountId, toAccountId);
Long secondId = Math.max(fromAccountId, toAccountId);

Account first = accountRepo.findByIdWithLock(firstId);
Account second = accountRepo.findByIdWithLock(secondId);
```

## 2. 모듈 구조
- `transfer-api`: Controller, DTO, 전역 예외 처리 등 API 레이어
- `transfer-domain`: Entity, Service, Policy, Repository Interface 등 핵심 비즈니스
- `transfer-infra`: JPA 구현체, 영속성/설정 등 인프라 레이어

의존성 방향:
- `transfer-api` → `transfer-domain`
- `transfer-infra` → `transfer-domain`
- Domain은 Infra를 직접 참조하지 않고 Repository 인터페이스만 의존한다.

### 2.1 모듈별 패키지 예시
```text
com.example.transfer
├── api
│   ├── controller
│   ├── dto
│   │   ├── request
│   │   └── response
│   └── exception
├── domain
│   ├── entity
│   ├── service
│   ├── policy
│   ├── repository
│   └── exception
└── infra
    ├── repository
    └── config
```

## 3. API 응답 규약

### 3.1 성공 응답

```json
{
  "id": 1,
  "ownerName": "홍길동",
  "balanceWon": 100000,
  "status": "ACTIVE",
  "createdAt": "2025-01-09T10:30:00"
}
```

```json
{
  "transferId": "550e8400-e29b-41d4-a716-446655440000",
  "amountWon": 100000,
  "feeWon": 1000,
  "occurredAt": "2025-01-09T10:30:00"
}
```

```json
[
  {
    "id": 1,
    "accountId": 1,
    "type": "DEPOSIT",
    "amountWon": 100000,
    "counterpartyAccountId": null,
    "relatedTransferId": null,
    "occurredAt": "2025-01-09T10:30:00"
  }
]
```

### 3.2 실패 응답

```json
{
  "code": "INSUFFICIENT_BALANCE",
  "message": "잔액이 부족합니다",
  "timestamp": "2025-01-09T10:30:00"
}
```
### 3.3 에러 코드 정의

| HTTP Status | 에러 코드 | 설명 | 발생 상황 |
|-------------|-----------|------|----------|
| 400 | INVALID_AMOUNT | 금액은 0보다 커야 합니다 | amount ≤ 0 |
| 400 | SAME_ACCOUNT_TRANSFER | 동일 계좌로 이체할 수 없습니다 | from == to |
| 404 | ACCOUNT_NOT_FOUND | 계좌를 찾을 수 없습니다 | 없거나 삭제된 계좌 |
| 409 | INSUFFICIENT_BALANCE | 잔액이 부족합니다 | 잔액 < 요청금액 |
| 409 | DAILY_LIMIT_EXCEEDED | 일일 한도를 초과했습니다 | 출금 100만 / 이체 300만 초과 |
| 409 | CONCURRENT_MODIFICATION | 동시 요청으로 처리에 실패했습니다 | 낙관적 락 충돌 |
