# 로컬 Docker Compose 테스트 가이드

## 개요

이 문서는 로컬 환경에서 Docker Compose를 사용하여 전체 마이크로서비스 시스템을 테스트하는 방법을 설명합니다.

## 사전 요구사항

- Docker Desktop 또는 Docker Engine (20.10 이상)
- Docker Compose V2 (2.0 이상)
- 최소 8GB RAM (권장: 16GB)
- 최소 20GB 디스크 공간
- curl, jq (테스트 스크립트 실행 시)

## 시스템 구성

### 인프라 서비스

- **Kafka**: 이벤트 스트리밍 플랫폼 (KRaft 모드, 포트 9092)
- **PostgreSQL 데이터베이스**:
  - order-db (포트 5433)
  - inventory-db (포트 5434)
  - fulfillment-db (포트 5435)
  - analytics-db (포트 5436)

### 애플리케이션 서비스

- **api-gateway**: API 게이트웨이 (포트 8080)
- **order-service**: 주문 서비스 (포트 8081)
- **inventory-service**: 재고 서비스 (포트 8082)
- **fulfillment-service**: 배송 서비스 (포트 8083)
- **analytics-service**: 분석 서비스 (포트 8084)
- **payment-adapter-ext**: 결제 어댑터 (포트 9090)

## 환경 설정

### 1. 환경 변수 파일 확인

`.env` 파일이 이미 존재하며, 필요시 수정할 수 있습니다:

```bash
# .env 파일 확인
cat .env

# 필요시 .env.example에서 복사
cp .env.example .env
```

### 2. 주요 환경 변수

```bash
# 프로젝트 설정
COMPOSE_PROJECT_NAME=eda-workshop
NETWORK_NAME=eda-net

# Kafka 설정
KAFKA_BROKER=kafka:9092
EVENT_NS=orders.v1

# 서비스 포트
API_GATEWAY_PORT=8080
ORDER_SVC_PORT=8081
INV_SVC_PORT=8082
FUL_SVC_PORT=8083
ANALYTICS_SVC_PORT=8084
PAY_ADAPTER_PORT=9090
```

## 로컬 테스트 실행

### 자동 테스트 스크립트 사용 (권장)

전체 시스템을 자동으로 시작하고 테스트하는 스크립트를 제공합니다:

```bash
# 스크립트 실행
./scripts/local-test.sh
```

스크립트는 다음 작업을 수행합니다:
1. Docker Compose 환경 확인
2. 모든 서비스 시작
3. 인프라 서비스 헬스체크 (Kafka, PostgreSQL)
4. 애플리케이션 서비스 헬스체크
5. 주문 생성 API 테스트
6. 결과 요약 출력

### 수동 테스트

#### 1. 서비스 시작

```bash
# 모든 서비스 시작 (백그라운드)
docker compose up -d

# 로그 확인
docker compose logs -f

# 특정 서비스 로그 확인
docker compose logs -f order-service
```

#### 2. 서비스 상태 확인

```bash
# 실행 중인 컨테이너 확인
docker compose ps

# 헬스체크
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8081/actuator/health  # Order Service
curl http://localhost:8082/actuator/health  # Inventory Service
curl http://localhost:8083/actuator/health  # Fulfillment Service
curl http://localhost:8084/actuator/health  # Analytics Service
curl http://localhost:9090/actuator/health  # Payment Adapter
```

#### 3. Kafka 토픽 확인

```bash
# Kafka 컨테이너 접속
docker compose exec kafka bash

# 토픽 목록 확인
kafka-topics --bootstrap-server localhost:9092 --list

# 특정 토픽의 메시지 확인
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic orders.v1.created \
  --from-beginning
```

#### 4. 데이터베이스 확인

```bash
# Order DB 접속
docker compose exec order-db psql -U orders -d orders

# 테이블 확인
\dt

# 주문 데이터 조회
SELECT * FROM orders;
```

## API 테스트

### 주문 생성

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-001",
    "productId": "PROD-001",
    "quantity": 2,
    "totalAmount": 50000
  }'
```

### 주문 조회

```bash
# 전체 주문 목록
curl http://localhost:8080/api/orders

# 특정 주문 조회
curl http://localhost:8080/api/orders/{orderId}
```

### 주문 처리 흐름 확인

주문 생성 후 다음 이벤트가 순차적으로 발생합니다:

1. **OrderCreated** → `orders.v1.created` 토픽
2. **InventoryReserved** → `orders.v1.inventory_reserved` 토픽
3. **PaymentAuthorized** → `orders.v1.payment_authorized` 토픽
4. **FulfillmentScheduled** → `orders.v1.fulfillment_scheduled` 토픽

각 단계는 로그와 Kafka 토픽에서 확인할 수 있습니다.

## 문제 해결

### 서비스가 시작되지 않는 경우

```bash
# 로그 확인
docker compose logs [service-name]

# 컨테이너 재시작
docker compose restart [service-name]

# 전체 재시작
docker compose down
docker compose up -d
```

### 포트 충돌

다른 애플리케이션이 포트를 사용 중인 경우 `.env` 파일에서 포트를 변경:

```bash
API_GATEWAY_PORT=8090  # 8080 대신 8090 사용
```

### 데이터베이스 초기화

```bash
# 볼륨 포함 전체 삭제
docker compose down -v

# 재시작
docker compose up -d
```

### Kafka 연결 문제

```bash
# Kafka 헬스체크
docker compose exec kafka kafka-broker-api-versions \
  --bootstrap-server localhost:9092

# 토픽 재생성
docker compose restart topic-init
```

## 성능 테스트

### 부하 테스트 스크립트

```bash
# 기존 스크립트 사용
./scripts/generate-load.sh
```

### 수동 부하 생성

```bash
# 100개 주문 생성
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d "{
      \"customerId\": \"customer-$i\",
      \"productId\": \"PROD-001\",
      \"quantity\": 1,
      \"totalAmount\": 10000
    }" &
done
wait
```

## 정리

### 서비스 중지

```bash
# 서비스 중지 (데이터 유지)
docker compose stop

# 서비스 중지 및 컨테이너 삭제
docker compose down

# 볼륨 포함 전체 삭제
docker compose down -v
```

### 이미지 정리

```bash
# 사용하지 않는 이미지 삭제
docker image prune -a

# 빌드 캐시 정리
docker builder prune
```

## 발견된 문제점 및 수정 사항

### 1. analytics-service Dockerfile 문제
- **문제**: 빌드 스테이지가 없어 로컬에서 빌드 불가
- **수정**: 다른 서비스와 동일한 멀티 스테이지 빌드로 변경
- **파일**: `analytics-service/Dockerfile`

### 2. EVENT_NS 불일치
- **문제**: docker-compose.yml과 .env 파일의 EVENT_NS 값이 다름
  - docker-compose.yml: `orders.v1`
  - .env: `com.workshop`
- **수정**: 모든 파일에서 `orders.v1`로 통일
- **영향 파일**: `.env`, `.env.example`, `docker-compose.yml`

### 3. 환경 변수명 불일치
- **문제**: .env 파일의 포트 변수명이 docker-compose.yml과 다름
  - .env: `ORDER_SERVICE_PORT`, `INVENTORY_SERVICE_PORT` 등
  - docker-compose.yml: `ORDER_SVC_PORT`, `INV_SVC_PORT` 등
- **수정**: docker-compose.yml에 맞춰 변수명 통일
- **영향 파일**: `.env`, `.env.example`

### 4. analytics-service 설정 불일치
- **문제**: 다른 서비스와 설정 형식이 다름
- **수정**: 
  - topic-init 의존성 추가
  - 환경 변수를 .env 파일 기반으로 변경
  - OTEL 설정 추가
  - 포트 변수명 통일 (`ANALYTICS_SVC_PORT`)
- **파일**: `docker-compose.yml`

### 5. 포트 노출 설정
- **확인 사항**: 모든 서비스가 올바른 포트로 노출됨
  - api-gateway: 8080
  - order-service: 8081
  - inventory-service: 8082
  - fulfillment-service: 8083
  - analytics-service: 8084
  - payment-adapter-ext: 9090

## 추가 리소스

- [Docker Compose 문서](https://docs.docker.com/compose/)
- [Kafka 문서](https://kafka.apache.org/documentation/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

## 지원

문제가 발생하면 다음을 확인하세요:
1. Docker Desktop이 실행 중인지 확인
2. 충분한 메모리와 디스크 공간이 있는지 확인
3. 로그 파일에서 에러 메시지 확인
4. 포트 충돌이 없는지 확인

---

**마지막 업데이트**: 2026-02-19