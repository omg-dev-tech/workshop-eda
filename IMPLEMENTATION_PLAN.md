# Instana 데모 애플리케이션 구현 계획

## 📋 프로젝트 개요

### 목표
Instana의 다양한 기능을 검증할 수 있는 EDA 기반 마이크로서비스 데모 애플리케이션 구현

### 핵심 요구사항
1. ✅ **EDA 기반 아키텍처** - Kafka를 통한 이벤트 기반 통신
2. ✅ **마이크로서비스 아키텍처** - 독립적으로 배포 가능한 서비스들
3. ✅ **Docker Compose** - 로컬 개발 환경
4. 🔄 **Kubernetes 배포** - K3s 기반 프로덕션 환경
5. 🔄 **OpenTelemetry 기반** - 표준 관측성 구현
6. 🔄 **비즈니스 메트릭** - Instana 비즈니스 메트릭 기능 검증
7. ✅ **메소드 레벨 서비스 격상** - @ServiceNode를 통한 가상 서비스 생성

---

## 🏗️ 현재 아키텍처 분석

### 구현 완료된 서비스
1. **api-gateway** ✅
   - Spring Cloud Gateway 기반
   - 모든 서비스의 진입점
   - OpenTelemetry 통합

2. **order-service** ✅
   - 주문 생성 및 상태 관리
   - Kafka 이벤트 발행/구독
   - @ServiceNode 어노테이션으로 메소드 레벨 추적
   - VirtualOtelFactory를 통한 가상 서비스 생성

3. **inventory-service** ✅
   - 재고 확인 및 예약
   - Kafka 이벤트 처리

4. **fulfillment-service** ✅
   - 배송 스케줄링
   - Kafka 이벤트 처리

5. **payment-adapter-ext** ✅
   - 외부 결제 시뮬레이터
   - 성공률 설정 가능

### 구현 필요한 서비스
1. **analytics-service** 🔄
   - 모든 이벤트 수집 및 분석
   - 비즈니스 메트릭 생성
   - 대시보드용 데이터 제공

2. **notification-service** 🔄
   - 이메일/SMS/푸시 알림 시뮬레이션
   - 다양한 알림 채널 지원

---

## 🎯 구현 전략

### Phase 1: 누락된 서비스 구현 (우선순위: 높음)

#### 1.1 Payment Simulator 설정
```bash
# payment-adapter-ext를 payment-simulator로 사용
cd /Users/hansol/Workspace/TXC/0903_LG/order
ln -s payment-adapter-ext payment-simulator
```
#### 1.1.1 TCP 소켓 기반 레거시 결제 게이트웨이 추가

**목적**: 다양한 통신 프로토콜 시연 (HTTP/JSON, Kafka, TCP 소켓)

**구현 방식**:
Payment Simulator에 TCP 소켓 서버 추가하여 레거시 전문 통신 지원

**기술 스택**:
- Java NIO (Non-blocking I/O)
- Netty 또는 Spring Integration TCP
- 고정 길이 전문 또는 구분자 기반 전문

**전문 포맷 예시**:
```
[Header: 20 bytes][Body: Variable]

Header Format:
- Message Type (4 bytes): "0100" (승인요청), "0110" (승인응답)
- Message Length (6 bytes): "000256" (전체 메시지 길이)
- Transaction ID (10 bytes): "0000000001"

Body Format (승인요청):
- Order ID (20 bytes)
- Amount (12 bytes, right-aligned, zero-padded)
- Currency (3 bytes): "KRW", "USD"
- Merchant ID (10 bytes)
- Timestamp (14 bytes): "YYYYMMDDHHmmss"

Body Format (승인응답):
- Response Code (4 bytes): "0000" (성공), "9999" (실패)
- Auth ID (20 bytes)
- Message (50 bytes)
```

**구현 컴포넌트**:

1. **TCP Server** (Payment Simulator 내부):
```java
@Component
public class LegacyPaymentTcpServer {
    private final int port = 9091;
    
    @PostConstruct
    public void start() {
        // Netty 기반 TCP 서버 시작
        // 전문 파싱 및 응답 처리
    }
    
    private String processPaymentMessage(String message) {
        // 전문 파싱
        // 결제 처리 (성공률 적용)
        // 응답 전문 생성
    }
}
```

2. **TCP Client** (Order Service 내부):
```java
@Component
public class LegacyPaymentClient {
    
    @ServiceNode("vs.legacy-payment-gateway")
    public PaymentResponse authorizeViaTcp(PaymentRequest req) {
        // TCP 소켓 연결
        // 전문 생성 및 전송
        // 응답 수신 및 파싱
        // OpenTelemetry span 생성
    }
}
```

3. **설정 옵션**:
```yaml
# application.properties
payment.protocol=http  # http, tcp, both
payment.tcp.host=payment-simulator
payment.tcp.port=9091
payment.tcp.timeout=5000
payment.tcp.pool.size=10
```

**OpenTelemetry 통합**:
```java
// TCP 통신에 대한 커스텀 span 생성
Span span = tracer.spanBuilder("tcp.payment.authorize")
    .setSpanKind(SpanKind.CLIENT)
    .setAttribute("net.peer.name", "payment-simulator")
    .setAttribute("net.peer.port", 9091)
    .setAttribute("rpc.system", "tcp")
    .setAttribute("rpc.service", "legacy-payment-gateway")
    .startSpan();
```

**Docker Compose 설정**:
```yaml
payment-simulator:
  ports:
    - "9090:9090"  # HTTP
    - "9091:9091"  # TCP
  environment:
    TCP_SERVER_ENABLED: "true"
    TCP_SERVER_PORT: 9091
```

**장점**:
1. 레거시 시스템 통합 시나리오 시연
2. 다양한 프로토콜 추적 능력 검증
3. TCP 레벨 네트워크 메트릭 수집
4. Instana의 프로토콜 무관 추적 능력 시연


#### 1.2 Analytics Service 구현
**목적**: 모든 비즈니스 이벤트를 수집하고 메트릭 생성

**기술 스택**:
- Spring Boot 3.3.2
- Spring Kafka (Consumer)
- PostgreSQL (분석 데이터 저장)
- OpenTelemetry (메트릭 및 트레이싱)

**핵심 기능**:
1. **이벤트 수집**
   - 모든 도메인 이벤트 구독 (order.*, inventory.*, fulfillment.*)
   - 이벤트 타입별 카운터 및 히스토그램

2. **비즈니스 메트릭**
   ```java
   // OpenTelemetry Metrics
   - order.created.count (Counter)
   - order.completed.count (Counter)
   - order.failed.count (Counter)
   - order.processing.duration (Histogram)
   - order.amount.total (Counter)
   - inventory.reservation.success.rate (Gauge)
   - payment.authorization.success.rate (Gauge)
   ```

3. **집계 및 분석**
   - 시간대별 주문 통계
   - 상품별 판매 통계
   - 고객별 구매 패턴

**데이터베이스 스키마**:
```sql
-- 이벤트 로그
CREATE TABLE event_log (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) UNIQUE,
    event_type VARCHAR(100),
    aggregate_id VARCHAR(255),
    payload JSONB,
    timestamp BIGINT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 주문 통계
CREATE TABLE order_metrics (
    id BIGSERIAL PRIMARY KEY,
    date DATE,
    hour INT,
    total_orders INT,
    completed_orders INT,
    failed_orders INT,
    total_amount BIGINT,
    avg_processing_time_ms BIGINT
);

-- 상품 통계
CREATE TABLE product_metrics (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(100),
    date DATE,
    total_sold INT,
    total_revenue BIGINT
);
```

#### 1.3 Notification Service 구현
**목적**: 주문 프로세스의 각 단계에서 고객에게 알림 전송

**기술 스택**:
- Spring Boot 3.3.2
- Spring Kafka (Consumer)
- PostgreSQL (알림 이력 저장)
- OpenTelemetry

**핵심 기능**:
1. **알림 채널**
   - Email 시뮬레이션 (로그 출력 + DB 저장)
   - SMS 시뮬레이션 (로그 출력 + DB 저장)
   - Push 시뮬레이션 (로그 출력 + DB 저장)

2. **알림 트리거**
   - 주문 생성 시
   - 재고 예약 완료 시
   - 결제 승인 시
   - 배송 스케줄링 시
   - 주문 완료 시
   - 주문 실패 시

3. **비즈니스 메트릭**
   ```java
   - notification.email.sent.count (Counter)
   - notification.sms.sent.count (Counter)
   - notification.push.sent.count (Counter)
   - notification.failed.count (Counter)
   - notification.delivery.duration (Histogram)
   ```

**데이터베이스 스키마**:
```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    notification_id VARCHAR(255) UNIQUE,
    order_id VARCHAR(255),
    customer_id VARCHAR(255),
    channel VARCHAR(50), -- EMAIL, SMS, PUSH
    template VARCHAR(100),
    status VARCHAR(50), -- PENDING, SENT, FAILED
    sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

### Phase 2: 비즈니스 메트릭 구현 (우선순위: 높음)

#### 2.1 OpenTelemetry Metrics 구현 전략

**공통 메트릭 유틸리티 클래스**:
```java
@Component
public class BusinessMetrics {
    private final Meter meter;
    
    // Counters
    private final LongCounter orderCreatedCounter;
    private final LongCounter orderCompletedCounter;
    private final LongCounter orderFailedCounter;
    private final LongCounter paymentAuthorizedCounter;
    private final LongCounter paymentFailedCounter;
    
    // Histograms
    private final LongHistogram orderProcessingDuration;
    private final LongHistogram paymentProcessingDuration;
    
    // UpDownCounters (for gauges)
    private final LongUpDownCounter activeOrdersCounter;
    
    public BusinessMetrics(OpenTelemetry openTelemetry) {
        this.meter = openTelemetry.getMeter("business-metrics");
        
        // Initialize metrics
        this.orderCreatedCounter = meter
            .counterBuilder("order.created")
            .setDescription("Total number of orders created")
            .setUnit("orders")
            .build();
            
        // ... more metrics
    }
    
    public void recordOrderCreated(String customerId, long amount) {
        orderCreatedCounter.add(1, 
            Attributes.of(
                AttributeKey.stringKey("customer.id"), customerId,
                AttributeKey.longKey("order.amount"), amount
            ));
    }
}
```

#### 2.2 각 서비스별 메트릭 추가

**Order Service**:
- `order.created.count` - 주문 생성 수
- `order.completed.count` - 주문 완료 수
- `order.failed.count` - 주문 실패 수
- `order.processing.duration` - 주문 처리 시간
- `order.amount.total` - 총 주문 금액

**Inventory Service**:
- `inventory.check.count` - 재고 확인 수
- `inventory.reserved.count` - 재고 예약 성공 수
- `inventory.rejected.count` - 재고 부족 수
- `inventory.level` - 현재 재고 수준

**Payment Service**:
- `payment.authorization.count` - 결제 시도 수
- `payment.authorized.count` - 결제 승인 수
- `payment.failed.count` - 결제 실패 수
- `payment.amount.total` - 총 결제 금액

**Fulfillment Service**:
- `fulfillment.scheduled.count` - 배송 스케줄링 수
- `fulfillment.completed.count` - 배송 완료 수
- `fulfillment.processing.duration` - 배송 처리 시간

---

### Phase 3: @ServiceNode 확장 적용 (우선순위: 중간)

#### 3.1 현재 구현 상태
- ✅ `@ServiceNode` 어노테이션 정의
- ✅ `VirtualOtelFactory` - 가상 서비스별 Tracer 생성
- ✅ `OTelServiceNodeAspect` - AOP 기반 자동 span 생성
- ✅ Order Service의 taskA, taskB, taskC에 적용

#### 3.2 확장 적용 대상

**Order Service**:
```java
@ServiceNode("vs.order.validation")
private void validateOrder(CreateOrderReq req) { ... }

@ServiceNode("vs.order.persistence")
private void persistOrder(OrderEntity order) { ... }

@ServiceNode("vs.payment.request", mode = Mode.CLIENT_SERVER)
private PaymentResponse requestPayment(PaymentRequest req) { ... }
```

**Inventory Service**:
```java
@ServiceNode("vs.inventory.check")
private boolean checkInventory(String sku, int qty) { ... }

@ServiceNode("vs.inventory.reserve")
private void reserveInventory(String orderId, List<Item> items) { ... }
```

**Fulfillment Service**:
```java
@ServiceNode("vs.fulfillment.planning")
private String planShipment(String orderId) { ... }

@ServiceNode("vs.fulfillment.scheduling")
private void scheduleShipment(String shippingId) { ... }
```

---

### Phase 4: Kubernetes 배포 구성 (우선순위: 중간)

#### 4.1 Helm Chart 구조
```
helm/workshop-eda/
├── Chart.yaml
├── values.yaml
├── values-dev.yaml
├── values-prod.yaml
└── templates/
    ├── _helpers.tpl
    ├── namespace.yaml
    ├── configmap.yaml
    ├── secrets.yaml
    ├── kafka/
    │   ├── statefulset.yaml
    │   └── service.yaml
    ├── postgres/
    │   ├── statefulset.yaml
    │   └── service.yaml
    ├── api-gateway/
    │   ├── deployment.yaml
    │   ├── service.yaml
    │   └── ingress.yaml
    ├── order-service/
    │   ├── deployment.yaml
    │   └── service.yaml
    ├── inventory-service/
    │   ├── deployment.yaml
    │   └── service.yaml
    ├── fulfillment-service/
    │   ├── deployment.yaml
    │   └── service.yaml
    ├── analytics-service/
    │   ├── deployment.yaml
    │   └── service.yaml
    ├── notification-service/
    │   ├── deployment.yaml
    │   └── service.yaml
    └── payment-simulator/
        ├── deployment.yaml
        └── service.yaml
```

#### 4.2 주요 설정

**ConfigMap** (공통 설정):
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  KAFKA_BOOTSTRAP_SERVERS: "kafka:9092"
  EVENT_VERSION: "v1"
  OTEL_EXPORTER_OTLP_ENDPOINT: "http://instana-agent:4317"
  OTEL_RESOURCE_ENV: "k3s"
```

**Deployment** (예: order-service):
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
      - name: order-service
        image: {{ .Values.image.registry }}/order-service:{{ .Values.image.tag }}
        env:
        - name: OTEL_SERVICE_NAME
          value: "order-service"
        - name: OTEL_EXPORTER_OTLP_ENDPOINT
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: OTEL_EXPORTER_OTLP_ENDPOINT
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
```

---

### Phase 5: 테스트 및 검증 (우선순위: 높음)

#### 5.1 부하 테스트 스크립트 개선

**현재**: `scripts/generate-load.sh`
**개선 사항**:
1. 다양한 시나리오 지원
2. 성공/실패 비율 조정
3. 동시성 제어
4. 메트릭 수집

```bash
#!/bin/bash
# Enhanced load generation script

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
CONCURRENT_USERS="${CONCURRENT_USERS:-10}"
DURATION_SECONDS="${DURATION_SECONDS:-300}"
SUCCESS_RATE="${SUCCESS_RATE:-90}"

# Scenarios
scenarios=(
  "normal_order"      # 정상 주문
  "out_of_stock"      # 재고 부족
  "payment_failure"   # 결제 실패
  "large_order"       # 대량 주문
)

# Generate load with different scenarios
for i in $(seq 1 $CONCURRENT_USERS); do
  (
    while true; do
      scenario=${scenarios[$RANDOM % ${#scenarios[@]}]}
      generate_order_for_scenario "$scenario"
      sleep $(( RANDOM % 5 + 1 ))
    done
  ) &
done

wait
```

#### 5.2 검증 체크리스트

**기능 검증**:
- [ ] 주문 생성 → 재고 확인 → 결제 → 배송 플로우
- [ ] 재고 부족 시 주문 거절
- [ ] 결제 실패 시 주문 취소
- [ ] 모든 이벤트가 Kafka를 통해 전파
- [ ] 알림이 각 단계에서 발송

**Instana 검증**:
- [ ] 모든 서비스가 Dependency Map에 표시
- [ ] @ServiceNode로 정의한 가상 서비스가 별도 노드로 표시
- [ ] 비즈니스 메트릭이 Instana에서 조회 가능
- [ ] 트레이스가 전체 플로우를 추적
- [ ] 에러 발생 시 Instana에서 감지

**성능 검증**:
- [ ] 초당 100 TPS 처리 가능
- [ ] P95 레이턴시 < 500ms
- [ ] 에러율 < 1%
- [ ] 리소스 사용률 정상 범위

---

## 📊 비즈니스 메트릭 상세 설계

### 메트릭 카테고리

#### 1. Order Metrics (주문 메트릭)
```java
// Counter
order.created.total              // 총 생성된 주문 수
order.completed.total            // 완료된 주문 수
order.failed.total               // 실패한 주문 수
order.cancelled.total            // 취소된 주문 수

// Histogram
order.processing.duration.ms     // 주문 처리 시간 (ms)
order.amount                     // 주문 금액 분포

// Gauge
order.active.count               // 현재 처리 중인 주문 수
order.pending.count              // 대기 중인 주문 수

// Attributes (Labels)
- customer.id
- order.status
- payment.method
- currency
```

#### 2. Inventory Metrics (재고 메트릭)
```java
// Counter
inventory.check.total            // 재고 확인 요청 수
inventory.reserved.total         // 재고 예약 성공 수
inventory.rejected.total         // 재고 부족으로 거절된 수
inventory.released.total         // 재고 해제 수

// Gauge
inventory.level                  // SKU별 현재 재고 수준
inventory.reservation.active     // 활성 예약 수

// Attributes
- sku
- warehouse.id
```

#### 3. Payment Metrics (결제 메트릭)
```java
// Counter
payment.authorization.total      // 결제 시도 수
payment.authorized.total         // 결제 승인 수
payment.failed.total             // 결제 실패 수
payment.amount.total             // 총 결제 금액

// Histogram
payment.processing.duration.ms   // 결제 처리 시간

// Attributes
- payment.method
- payment.provider
- currency
- failure.reason
```

#### 4. Fulfillment Metrics (배송 메트릭)
```java
// Counter
fulfillment.scheduled.total      // 배송 스케줄링 수
fulfillment.shipped.total        // 배송 시작 수
fulfillment.delivered.total      // 배송 완료 수
fulfillment.failed.total         // 배송 실패 수

// Histogram
fulfillment.processing.duration.ms  // 배송 처리 시간
fulfillment.delivery.duration.ms    // 배송 소요 시간

// Attributes
- shipping.method
- destination.region
```

#### 5. Notification Metrics (알림 메트릭)
```java
// Counter
notification.sent.total          // 발송된 알림 수
notification.failed.total        // 실패한 알림 수

// Histogram
notification.delivery.duration.ms  // 알림 전송 시간

// Attributes
- channel (email, sms, push)
- template
- status
```

---

## 🔧 기술 스택 상세

### Backend Services
- **Language**: Java 21
- **Framework**: Spring Boot 3.3.2
- **Build Tool**: Gradle 8.x
- **Database**: PostgreSQL 16
- **Message Broker**: Apache Kafka 3.7 (Bitnami)
- **Observability**: OpenTelemetry 1.44.1

### Infrastructure
- **Local Dev**: Docker Compose
- **Production**: Kubernetes (K3s)
- **Container Registry**: GitHub Container Registry
- **CI/CD**: GitLab CI

### Monitoring
- **APM**: Instana
- **Tracing**: OpenTelemetry → Instana
- **Metrics**: OpenTelemetry Metrics → Instana
- **Logs**: stdout → Instana

---

## 📅 구현 일정

### Week 1: 서비스 구현
- Day 1-2: Analytics Service 구현
- Day 3-4: Notification Service 구현
- Day 5: Payment Simulator 설정 및 테스트

### Week 2: 메트릭 및 추적
- Day 1-2: 비즈니스 메트릭 구현
- Day 3-4: @ServiceNode 확장 적용
- Day 5: 통합 테스트

### Week 3: 배포 및 검증
- Day 1-2: Kubernetes 매니페스트 작성
- Day 3: K3s 배포 및 테스트
- Day 4-5: Instana 통합 검증

---

## 🎯 성공 기준

### 기능적 요구사항
- ✅ 모든 서비스가 정상 동작
- ✅ 이벤트 기반 통신이 안정적으로 작동
- ✅ 에러 시나리오가 적절히 처리됨

### 비기능적 요구사항
- ✅ 초당 100 TPS 이상 처리
- ✅ P95 레이턴시 500ms 이하
- ✅ 에러율 1% 이하
- ✅ 리소스 효율적 사용

### Instana 검증 요구사항
- ✅ 모든 서비스가 Dependency Map에 표시
- ✅ @ServiceNode 가상 서비스가 별도 노드로 표시
- ✅ 비즈니스 메트릭이 Instana에서 조회 가능
- ✅ End-to-End 트레이싱 가능
- ✅ 에러 및 이상 징후 자동 감지

---

## 📚 참고 자료

### OpenTelemetry
- [OpenTelemetry Java SDK](https://opentelemetry.io/docs/instrumentation/java/)
- [OpenTelemetry Metrics API](https://opentelemetry.io/docs/specs/otel/metrics/api/)
- [OpenTelemetry Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/)

### Instana
- [Instana Java Trace SDK](https://www.ibm.com/docs/en/instana-observability/current?topic=apis-java-trace-sdk)
- [Instana Custom Metrics](https://www.ibm.com/docs/en/instana-observability/current?topic=references-custom-metrics)
- [Instana OpenTelemetry](https://www.ibm.com/docs/en/instana-observability/current?topic=technologies-opentelemetry)

### Spring Boot & Kafka
- [Spring Kafka Documentation](https://spring.io/projects/spring-kafka)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

---

## 🚀 다음 단계

1. **즉시 시작**: Analytics Service 구현
2. **병렬 진행**: Notification Service 구현
3. **통합**: 비즈니스 메트릭 추가
4. **배포**: Kubernetes 환경 구성
5. **검증**: Instana 통합 테스트

---

**작성일**: 2026-01-30
**작성자**: IBM Bob (Plan Mode)
**버전**: 1.0