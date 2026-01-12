# Transfer Service

간단한 이체 도메인을 중심으로 계좌 관리·입출금·이체 API를 제공하는 Spring Boot 멀티모듈 프로젝트입니다. `transfer-domain`에 비즈니스 규칙을 두고, `transfer-infra` 가 영속성 구현체를, `transfer-api` 가 REST 엔드포인트를 담당합니다.

## 기술 스택
- Java 21 (Amazon Corretto)
- Spring Boot 3.5.10-SNAPSHOT + Spring Data JPA
- Gradle 멀티모듈 구성 (`api`, `domain`, `infra`)
- MySQL 8.0 (Docker Compose) / 테스트용 H2

## 빠르게 실행하기
1. Docker Desktop과 Docker Compose가 설치되어 있어야 합니다.
2. MySQL 8.0 인프라 기동:
   ```bash
   docker compose -f docker/infra-compose.yml up -d
   ```
3. 애플리케이션 실행 (JDK 21 기준):
   ```bash
   ./gradlew :transfer-api:bootRun
   ```
4. 종료 시에는 `Ctrl+C` 후 `docker compose -f docker/infra-compose.yml down` 으로 리소스를 정리합니다.

## API 문서 / Swagger
- 애플리케이션이 기동되면 `http://localhost:8080/swagger-ui.html` 에서 OpenAPI 문서를 확인하고 바로 호출할 수 있습니다.

## 샘플 API 호출
```bash
# 1) 계좌 생성
curl -X POST http://localhost:8080/accounts \
  -H 'Content-Type: application/json' \
  -d '{"ownerName":"홍길동"}'

# 2) 특정 계좌 조회
curl http://localhost:8080/accounts/1

# 3) 입금
curl -X POST http://localhost:8080/accounts/1/deposit \
  -H 'Content-Type: application/json' \
  -d '{"amountWon":500000}'

# 4) 출금
curl -X POST http://localhost:8080/accounts/1/withdraw \
  -H 'Content-Type: application/json' \
  -d '{"amountWon":100000}'

# 5) 이체 (Idempotency-Key 필수)
curl -X POST http://localhost:8080/transfers \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: transfer-20240101-0001' \
  -d '{"fromAccountId":1,"toAccountId":2,"amountWon":200000}'

# 6) 거래내역 조회
curl http://localhost:8080/accounts/1/transactions
```

## 정책 요약
- 출금 일일 한도: `1,000,000원`
- 이체 일일 한도: `3,000,000원`
- 이체 수수료: `금액의 1% (원 단위 절사)`
- 모든 이체 요청은 `Idempotency-Key` 헤더를 강제해 중복 실행을 막습니다.

## 동시성 처리 방식
- **Pessimistic Lock:** `SELECT ... FOR UPDATE` 기반으로 계좌 레코드를 잠궈 잔액 정합성을 확보합니다.
- **데드락 방지:** 이체 시 두 계좌의 ID를 정렬해 “작은 ID → 큰 ID” 순으로 항상 락을 획득합니다.
- **테스트:** 동시 입출금/이체에 대한 통합 테스트(`transfer-infra` 모듈)와 API 단의 E2E 테스트로 락 전략을 지속 검증합니다.

## 테스트 실행
1. 로컬 JDK가 21로 지정돼야 합니다. (예: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`)
2. 전체 테스트: `./gradlew clean test`
   - 모듈별 실행: `./gradlew transfer-domain:test`, `./gradlew transfer-infra:test`, `./gradlew transfer-api:test`

## 프로젝트 문서
- `docs/01-requirements.md`: 기능 요구사항과 남은 TODO
- `docs/02-model.md`: ERD 및 스키마 상세
- `docs/03-architecture.md`: 모듈 구조, 락/정합성 전략, 테스트 전략

추가적인 설계/진행 상황은 `docs` 폴더를 참고하세요.
