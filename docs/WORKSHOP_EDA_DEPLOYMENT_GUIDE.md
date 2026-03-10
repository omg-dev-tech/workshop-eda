
# Workshop EDA 종합 배포 가이드

이 문서는 Event-Driven Architecture 워크샵 프로젝트를 `workshop-eda` 네임스페이스에 배포하는 전체 과정을 단계별로 안내합니다.

## 목차
1. [개요](#개요)
2. [사전 준비](#사전-준비)
3. [단계별 배포 가이드](#단계별-배포-가이드)
4. [배포 검증](#배포-검증)
5. [트러블슈팅](#트러블슈팅)
6. [정리 (Clean Up)](#정리-clean-up)
7. [참고 자료](#참고-자료)
8. [부록](#부록)

---

## 개요

### 프로젝트 소개
Workshop EDA는 이벤트 기반 아키텍처(Event-Driven Architecture)를 학습하기 위한 마이크로서비스 기반 주문 처리 시스템입니다.

### 배포 아키텍처

**마이크로서비스 (6개)**
- `api-gateway`: API 게이트웨이 (포트: 8080)
- `order-service`: 주문 관리 서비스 (포트: 8081)
- `inventory-service`: 재고 관리 서비스 (포트: 8082)
- `fulfillment-service`: 배송 관리 서비스 (포트: 8083)
- `analytics-service`: 분석 서비스 (포트: 8084)
- `payment-adapter-ext`: 결제 어댑터 (포트: 8085)

**인프라 구성요소**
- **Kafka 클러스터**: 3개 브로커, 3개 Zookeeper (Strimzi Operator 사용)
- **PostgreSQL**: 4개 데이터베이스 (orderdb, inventorydb, fulfillmentdb, analyticsdb)
- **Kafka 토픽**: 6개 이벤트 토픽

### 사전 요구사항

#### 필수 권한
- OpenShift/Kubernetes 클러스터 접근 권한
- 네임스페이스 생성 권한
- Strimzi Kafka Operator 설치 권한 (또는 이미 설치됨)

#### 필수 CLI 도구
```bash
# oc CLI (OpenShift)
oc version

# kubectl (Kubernetes)
kubectl version --client

# helm (Kubernetes 패키지 매니저)
helm version
```

#### 리소스 요구사항
- **CPU**: 최소 8 vCPU
- **메모리**: 최소 16GB RAM
- **스토리지**: 최소 50GB (PVC용)

---

## 사전 준비

### 1. 클러스터 접근 확인

```bash
# OpenShift 로그인
oc login --token=<your-token> --server=<your-server>

# 또는 Kubernetes 컨텍스트 확인
kubectl config current-context

# 클러스터 정보 확인
oc cluster-info
# 또는
kubectl cluster-info
```

### 2. Strimzi Kafka Operator 설치 확인

```bash
# Operator 설치 확인
oc get csv -n openshift-operators | grep strimzi
# 또는
kubectl get pods -n strimzi-system

# Operator가 없다면 설치 (관리자 권한 필요)
# OpenShift: OperatorHub에서 "Strimzi" 검색 후 설치
# Kubernetes: https://strimzi.io/docs/operators/latest/quickstart.html 참조
```

### 3. 네임스페이스 생성

```bash
# OpenShift
oc new-project workshop-eda --display-name="Workshop EDA" --description="Event-Driven Architecture Workshop"

# Kubernetes
kubectl create namespace workshop-eda

# 네임스페이스 확인
oc project workshop-eda
# 또는
kubectl config set-context --current --namespace=workshop-eda

# 생성 확인
oc get project workshop-eda
# 또는
kubectl get namespace workshop-eda
```

---

## 단계별 배포 가이드

### Step 1: Kafka 클러스터 배포

Strimzi Operator를 사용하여 Kafka 클러스터를 배포합니다.

```bash
# Kafka 클러스터 배포
kubectl apply -f infra/k8s/kafka-cluster.yaml

# 배포 확인 (약 5-10분 소요)
kubectl get kafka -n workshop-eda

# Ready 상태 대기
kubectl wait kafka/my-cluster --for=condition=Ready --timeout=600s -n workshop-eda
```

**예상 출력:**
```
NAME         DESIRED KAFKA REPLICAS   DESIRED ZK REPLICAS   READY   WARNINGS
my-cluster   3                        3                     True
```

**배포되는 리소스:**
- Kafka 브로커 Pod: `my-cluster-kafka-0`, `my-cluster-kafka-1`, `my-cluster-kafka-2`
- Zookeeper Pod: `my-cluster-zookeeper-0`, `my-cluster-zookeeper-1`, `my-cluster-zookeeper-2`
- Entity Operator Pod: `my-cluster-entity-operator-*`
- Kafka Exporter Pod: `my-cluster-kafka-exporter-*`

**Bootstrap Servers:**
```
my-cluster-kafka-bootstrap:9092  (내부 plain 연결)
my-cluster-kafka-bootstrap:9093  (내부 TLS 연결)
```

**상세 상태 확인:**
```bash
# Pod 상태 확인
kubectl get pods -n workshop-eda -l strimzi.io/cluster=my-cluster

# Kafka 클러스터 상세 정보
kubectl describe kafka my-cluster -n workshop-eda

# Kafka 브로커 로그 확인
kubectl logs -f my-cluster-kafka-0 -n workshop-eda
```

---

### Step 2: Kafka 토픽 생성

이벤트 기반 통신을 위한 6개의 Kafka 토픽을 생성합니다.

```bash
# Kafka 토픽 생성
kubectl apply -f infra/k8s/kafka-topics.yaml

# 토픽 생성 확인
kubectl get kafkatopic -n workshop-eda
```

**예상 출력:**
```
NAME                              CLUSTER      PARTITIONS   REPLICATION FACTOR   READY
orders.v1.created                 my-cluster   3            3                    True
orders.v1.inventory-reserved      my-cluster   3            3                    True
orders.v1.inventory-rejected      my-cluster   3            3                    True
orders.v1.payment-authorized      my-cluster   3            3                    True
orders.v1.payment-failed          my-cluster   3            3                    True
orders.v1.fulfillment-scheduled   my-cluster   3            3                    True
```

**생성되는 토픽:**
1. `orders.v1.created` - 주문 생성 이벤트
2. `orders.v1.inventory_reserved` - 재고 예약 성공 이벤트
3. `orders.v1.inventory_rejected` - 재고 예약 실패 이벤트
4. `orders.v1.payment_authorized` - 결제 승인 이벤트
5. `orders.v1.payment_failed` - 결제 실패 이벤트
6. `orders.v1.fulfillment_scheduled` - 배송 스케줄링 이벤트

**토픽 상세 정보 확인:**
```bash
# 특정 토픽 상세 정보
kubectl describe kafkatopic orders.v1.created -n workshop-eda

# 토픽 설정 확인
kubectl get kafkatopic orders.v1.created -n workshop-eda -o yaml
```

---

### Step 3: PostgreSQL 데이터베이스 배포

4개의 마이크로서비스를 위한 PostgreSQL 데이터베이스를 배포합니다.

#### 3.1 Secret 생성

```bash
# PostgreSQL 비밀번호 Secret 생성
kubectl apply -f infra/k8s/postgresql-secret.yaml

# Secret 확인
kubectl get secret postgresql-secret -n workshop-eda
```

#### 3.2 ConfigMap 생성

```bash
# 데이터베이스 초기화 스크립트 ConfigMap 생성
kubectl apply -f infra/k8s/postgresql-configmap.yaml

# ConfigMap 확인
kubectl get configmap postgresql-initdb -n workshop-eda
```

#### 3.3 StatefulSet 배포

```bash
# PostgreSQL StatefulSet 배포
kubectl apply -f infra/k8s/postgresql-statefulset.yaml

# Pod 상태 확인 (약 2-3분 소요)
kubectl get pods -n workshop-eda -l app=postgresql

# Pod Ready 대기
kubectl wait --for=condition=Ready pod/postgresql-0 --timeout=300s -n workshop-eda
```

**예상 출력:**
```
NAME           READY   STATUS    RESTARTS   AGE
postgresql-0   1/1     Running   0          2m
```

#### 3.4 데이터베이스 확인

```bash
# PostgreSQL Pod에 접속하여 데이터베이스 확인
kubectl exec -it postgresql-0 -n workshop-eda -- psql -U postgres -c "\l"
```

**예상 출력:**
```
                                  List of databases
     Name      |  Owner   | Encoding |  Collate   |   Ctype    |   Access privileges   
---------------+----------+----------+------------+------------+-----------------------
 analyticsdb   | postgres | UTF8     | en_US.utf8 | en_US.utf8 | 
 fulfillmentdb | postgres | UTF8     | en_US.utf8 | en_US.utf8 | 
 inventorydb   | postgres | UTF8     | en_US.utf8 | en_US.utf8 | 
 orderdb       | postgres | UTF8     | en_US.utf8 | en_US.utf8 | 
 postgres      | postgres | UTF8     | en_US.utf8 | en_US.utf8 | 
```

**생성되는 서비스:**
- `postgresql` (Headless Service)
- `order-db` (ClusterIP)
- `inventory-db` (ClusterIP)
- `fulfillment-db` (ClusterIP)
- `analytics-db` (ClusterIP)

**서비스 확인:**
```bash
kubectl get svc -n workshop-eda -l app=postgresql
```

---

### Step 4: 애플리케이션 서비스 배포

6개의 마이크로서비스를 배포합니다.

#### 방법 1: Helm 차트 사용 (권장)

```bash
# 프로젝트 루트 디렉토리에서 실행
cd /path/to/workshop-eda

# Helm 차트 검증
helm lint helm/workshop-eda

# Dry-run으로 배포 테스트
helm install workshop-eda ./helm/workshop-eda \
  -n workshop-eda \
  --dry-run --debug

# 실제 배포
helm install workshop-eda ./helm/workshop-eda -n workshop-eda

# 배포 상태 확인
helm list -n workshop-eda
helm status workshop-eda -n workshop-eda
```

**예상 출력:**
```
NAME            NAMESPACE       REVISION    UPDATED                                 STATUS      CHART               APP VERSION
workshop-eda    workshop-eda    1           2026-03-10 16:20:00.000000 +0900 KST    deployed    workshop-eda-1.0.0  1.0.0
```

#### 방법 2: kubectl 직접 사용

```bash
# 애플리케이션 배포
kubectl apply -f helm/workshop-eda/templates/apps.yaml -n workshop-eda

# 배포 확인
kubectl get deployments -n workshop-eda
```

#### 배포되는 서비스

| 서비스 | 포트 | 설명 |
|--------|------|------|
| api-gateway | 8080 | API 게이트웨이 |
| order-service | 8081 | 주문 관리 |
| inventory-service | 8082 | 재고 관리 |
| fulfillment-service | 8083 | 배송 관리 |
| analytics-service | 8084 | 분석 서비스 |
| payment-adapter-ext | 8085 | 결제 어댑터 |

#### Pod 상태 확인

```bash
# 모든 애플리케이션 Pod 확인
kubectl get pods -n workshop-eda -l app.kubernetes.io/part-of=workshop-eda

# Pod Ready 대기 (약 3-5분 소요)
kubectl wait --for=condition=Ready pods -l app.kubernetes.io/part-of=workshop-eda --timeout=600s -n workshop-eda
```

**예상 출력:**
```
NAME                                   READY   STATUS    RESTARTS   AGE
api-gateway-xxxxxxxxxx-xxxxx           1/1     Running   0          3m
order-service-xxxxxxxxxx-xxxxx         1/1     Running   0          3m
inventory-service-xxxxxxxxxx-xxxxx     1/1     Running   0          3m
fulfillment-service-xxxxxxxxxx-xxxxx   1/1     Running   0          3m
analytics-service-xxxxxxxxxx-xxxxx     1/1     Running   0          3m
payment-adapter-ext-xxxxxxxxxx-xxxxx   1/1     Running   0          3m
```

---

## 배포 검증

### 4.1 모든 리소스 확인

```bash
# 모든 Pod 상태 확인
kubectl get pods -n workshop-eda

# 모든 Service 확인
kubectl get svc -n workshop-eda

# 모든 Deployment 확인
kubectl get deployments -n workshop-eda

# Kafka 클러스터 확인
kubectl get kafka -n workshop-eda

# Kafka 토픽 확인
kubectl get kafkatopic -n workshop-eda

# StatefulSet 확인
kubectl get statefulset -n workshop-eda

# PVC 확인
kubectl get pvc -n workshop-eda
```

**전체 리소스 한 번에 확인:**
```bash
kubectl get all,kafka,kafkatopic,pvc -n workshop-eda
```

### 4.2 API 테스트

#### OpenShift Route 생성 (OpenShift 환경)

```bash
# API Gateway Route 생성
oc expose svc/api-gateway -n workshop-eda

# Route URL 확인
export API_GATEWAY=$(oc get route api-gateway -n workshop-eda -o jsonpath='{.spec.host}')
echo "API Gateway URL: http://$API_GATEWAY"
```

#### Kubernetes Ingress 또는 Port Forward (Kubernetes 환경)

```bash
# Port Forward 사용
kubectl port-forward svc/api-gateway 8080:8080 -n workshop-eda

# 다른 터미널에서 테스트
export API_GATEWAY=localhost:8080
```

#### Health Check

```bash
# API Gateway Health Check
curl http://$API_GATEWAY/actuator/health

# 각 서비스 Health Check
kubectl exec -it deployment/order-service -n workshop-eda -- \
  curl http://localhost:8081/actuator/health
```

**예상 출력:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "kafka": {
      "status": "UP"
    }
  }
}
```

#### 주문 생성 테스트

```bash
# 주문 생성
curl -X POST http://$API_GATEWAY/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-001",
    "productId": "product-001",
    "quantity": 2,
    "price": 29.99
  }'
```

**예상 출력:**
```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "customer-001",
  "productId": "product-001",
  "quantity": 2,
  "price": 29.99,
  "status": "CREATED",
  "createdAt": "2026-03-10T16:30:00Z"
}
```

#### 주문 조회

```bash
# 주문 ID로 조회
ORDER_ID="550e8400-e29b-41d4-a716-446655440000"
curl http://$API_GATEWAY/api/orders/$ORDER_ID

# 모든 주문 조회
curl http://$API_GATEWAY/api/orders
```

### 4.3 로그 확인

```bash
# 특정 서비스 로그 확인
kubectl logs -f deployment/order-service -n workshop-eda

# 모든 애플리케이션 로그 확인
kubectl logs -f -l app.kubernetes.io/part-of=workshop-eda -n workshop-eda

# 최근 100줄 로그 확인
kubectl logs --tail=100 deployment/order-service -n workshop-eda

# 특정 시간 이후 로그
kubectl logs --since=1h deployment/order-service -n workshop-eda

# 이전 컨테이너 로그 (재시작된 경우)
kubectl logs deployment/order-service --previous -n workshop-eda
```

### 4.4 이벤트 플로우 확인

```bash
# Order Service 로그에서 이벤트 발행 확인
```bash
kubectl logs deployment/order-service -n workshop-eda | grep "Published event"

# Inventory Service 로그에서 이벤트 수신 확인
kubectl logs deployment/inventory-service -n workshop-eda | grep "Received event"

# Analytics Service 로그에서 이벤트 처리 확인
kubectl logs deployment/analytics-service -n workshop-eda | grep "Processing event"

# Kafka Consumer Group 확인
kubectl exec -it my-cluster-kafka-0 -n workshop-eda -- \
  bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

# 특정 Consumer Group 상세 정보
kubectl exec -it my-cluster-kafka-0 -n workshop-eda -- \
  bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group order-service-group --describe
```

### 4.5 데이터베이스 데이터 확인

```bash
# Order 데이터베이스 확인
kubectl exec -it postgresql-0 -n workshop-eda -- \
  psql -U postgres -d orderdb -c "SELECT * FROM orders LIMIT 10;"

# Inventory 데이터베이스 확인
kubectl exec -it postgresql-0 -n workshop-eda -- \
  psql -U postgres -d inventorydb -c "SELECT * FROM inventory LIMIT 10;"

# Analytics 데이터베이스 확인
kubectl exec -it postgresql-0 -n workshop-eda -- \
  psql -U postgres -d analyticsdb -c "SELECT * FROM event_logs ORDER BY created_at DESC LIMIT 10;"

# Fulfillment 데이터베이스 확인
kubectl exec -it postgresql-0 -n workshop-eda -- \
  psql -U postgres -d fulfillmentdb -c "SELECT * FROM fulfillments LIMIT 10;"
```

---

## 5. 트러블슈팅

### 5.1 Kafka 클러스터가 Ready 상태가 안 될 때

#### 증상
```bash
kubectl get kafka my-cluster -n workshop-eda
# READY 컬럼이 False로 표시됨
```

#### 원인 및 해결방법

**1. Strimzi Operator 상태 확인**
```bash
# Operator Pod 확인
kubectl get pods -n openshift-operators | grep strimzi
# 또는
kubectl get pods -n strimzi-system

# Operator 로그 확인
kubectl logs -f deployment/strimzi-cluster-operator -n openshift-operators
```

**해결:** Operator가 없거나 오류 상태면 재설치 필요

**2. PVC 생성 확인**
```bash
# PVC 상태 확인
kubectl get pvc -n workshop-eda

# PVC가 Pending 상태인 경우
kubectl describe pvc <pvc-name> -n workshop-eda
```

**해결:** 
- StorageClass 확인: `kubectl get storageclass`
- 충분한 스토리지 용량 확인
- 필요시 PVC 크기 조정

**3. 리소스 부족 확인**
```bash
# Node 리소스 확인
kubectl top nodes

# Pod 이벤트 확인
kubectl get events -n workshop-eda --sort-by='.lastTimestamp'
```

**해결:**
- Kafka 리소스 요청량 줄이기
- Node 추가 또는 리소스 증설

**4. Kafka Pod 로그 확인**
```bash
# Kafka 브로커 로그
kubectl logs my-cluster-kafka-0 -n workshop-eda

# Zookeeper 로그
kubectl logs my-cluster-zookeeper-0 -n workshop-eda

# Entity Operator 로그
kubectl logs deployment/my-cluster-entity-operator -n workshop-eda
```

---

### 5.2 PostgreSQL Pod가 시작 안 될 때

#### 증상
```bash
kubectl get pods -n workshop-eda -l app=postgresql
# STATUS가 CrashLoopBackOff, Error, Pending 등
```

#### 원인 및 해결방법

**1. PVC 바인딩 확인**
```bash
# PVC 상태 확인
kubectl get pvc -n workshop-eda | grep postgresql

# PVC 상세 정보
kubectl describe pvc postgresql-storage-postgresql-0 -n workshop-eda
```

**해결:**
- PVC가 Pending이면 StorageClass 확인
- 수동으로 PV 생성 필요한 경우 PV 생성

**2. ConfigMap 마운트 확인**
```bash
# ConfigMap 존재 확인
kubectl get configmap postgresql-initdb -n workshop-eda

# ConfigMap 내용 확인
kubectl describe configmap postgresql-initdb -n workshop-eda

# Pod에서 마운트 확인
kubectl describe pod postgresql-0 -n workshop-eda | grep -A 5 Mounts
```

**해결:**
- ConfigMap이 없으면 재생성: `kubectl apply -f infra/k8s/postgresql-configmap.yaml`

**3. 초기화 스크립트 로그 확인**
```bash
# Pod 로그에서 초기화 과정 확인
kubectl logs postgresql-0 -n workshop-eda | grep -i "database\|error\|failed"

# 초기화 스크립트 실행 확인
kubectl logs postgresql-0 -n workshop-eda | grep "init"
```

**해결:**
- SQL 스크립트 오류 수정
- ConfigMap 업데이트 후 Pod 재시작

**4. Secret 확인**
```bash
# Secret 존재 확인
kubectl get secret postgresql-secret -n workshop-eda

# Secret 값 확인 (base64 디코딩)
kubectl get secret postgresql-secret -n workshop-eda -o jsonpath='{.data.postgres-password}' | base64 -d
```

**해결:**
- Secret이 없으면 생성: `kubectl apply -f infra/k8s/postgresql-secret.yaml`

---

### 5.3 애플리케이션 Pod가 CrashLoopBackOff 상태일 때

#### 증상
```bash
kubectl get pods -n workshop-eda
# 애플리케이션 Pod가 CrashLoopBackOff 상태
```

#### 원인 및 해결방법

**1. 데이터베이스 연결 확인**
```bash
# Pod 로그에서 DB 연결 오류 확인
kubectl logs deployment/order-service -n workshop-eda | grep -i "database\|connection\|jdbc"

# DB Service 확인
kubectl get svc -n workshop-eda | grep db

# DB 연결 테스트
kubectl exec -it deployment/order-service -n workshop-eda -- \
  nc -zv order-db 5432
```

**해결:**
- PostgreSQL Pod가 Running 상태인지 확인
- Service 이름이 올바른지 확인 (order-db, inventory-db 등)
- 환경 변수 확인: `kubectl describe deployment order-service -n workshop-eda | grep -A 10 Environment`

**2. Kafka 연결 확인**
```bash
# Kafka 연결 오류 확인
kubectl logs deployment/order-service -n workshop-eda | grep -i "kafka\|bootstrap"

# Kafka Service 확인
kubectl get svc -n workshop-eda | grep kafka

# Kafka 연결 테스트
kubectl exec -it deployment/order-service -n workshop-eda -- \
  nc -zv my-cluster-kafka-bootstrap 9092
```

**해결:**
- Kafka 클러스터가 Ready 상태인지 확인
- Bootstrap servers 주소 확인: `my-cluster-kafka-bootstrap:9092`
- 환경 변수 SPRING_KAFKA_BOOTSTRAP_SERVERS 확인

**3. 환경 변수 확인**
```bash
# Deployment의 환경 변수 확인
kubectl get deployment order-service -n workshop-eda -o yaml | grep -A 20 env:

# Pod에서 환경 변수 확인
kubectl exec -it deployment/order-service -n workshop-eda -- env | grep -i "spring\|kafka\|db"
```

**해결:**
- values.yaml에서 환경 변수 설정 확인
- Helm 차트 재배포: `helm upgrade workshop-eda ./helm/workshop-eda -n workshop-eda`

**4. 애플리케이션 로그 상세 확인**
```bash
# 전체 로그 확인
kubectl logs deployment/order-service -n workshop-eda --tail=200

# 이전 컨테이너 로그 (재시작된 경우)
kubectl logs deployment/order-service -n workshop-eda --previous

# 실시간 로그 스트리밍
kubectl logs -f deployment/order-service -n workshop-eda
```

**일반적인 오류 패턴:**
- `Connection refused`: 대상 서비스가 실행 중이 아님
- `Unknown host`: DNS 해석 실패, Service 이름 오류
- `Authentication failed`: 비밀번호 또는 인증 정보 오류
- `Timeout`: 네트워크 문제 또는 리소스 부족

---

### 5.4 데이터베이스 연결 오류

#### 증상
```
org.postgresql.util.PSQLException: Connection refused
```

#### 해결방법

**1. Service 이름 확인**
```bash
# Service 목록 확인
kubectl get svc -n workshop-eda -l app=postgresql

# 예상되는 Service 이름:
# - order-db
# - inventory-db
# - fulfillment-db
# - analytics-db
```

**2. Service 엔드포인트 확인**
```bash
# 엔드포인트 확인
kubectl get endpoints -n workshop-eda | grep db

# Service 상세 정보
kubectl describe svc order-db -n workshop-eda
```

**3. PostgreSQL Pod 상태 확인**
```bash
# Pod 상태
kubectl get pods -n workshop-eda -l app=postgresql

# Pod가 Running이 아니면 로그 확인
kubectl logs postgresql-0 -n workshop-eda
```

**4. 연결 정보 확인**
```bash
# 애플리케이션의 DB 연결 설정 확인
kubectl exec -it deployment/order-service -n workshop-eda -- \
  env | grep -i "datasource\|jdbc"

# 올바른 연결 정보:
# SPRING_DATASOURCE_URL=jdbc:postgresql://order-db:5432/orderdb
# SPRING_DATASOURCE_USERNAME=postgres
# SPRING_DATASOURCE_PASSWORD=postgres
```

**5. 네트워크 정책 확인**
```bash
# NetworkPolicy 확인
kubectl get networkpolicy -n workshop-eda

# NetworkPolicy가 있다면 상세 확인
kubectl describe networkpolicy <policy-name> -n workshop-eda
```

**해결:**
- NetworkPolicy가 DB 접근을 차단하는 경우 정책 수정

---

### 5.5 Kafka 연결 오류

#### 증상
```
org.apache.kafka.common.errors.TimeoutException: Failed to update metadata
```

#### 해결방법

**1. Bootstrap servers 확인**
```bash
# Kafka Service 확인
kubectl get svc -n workshop-eda | grep kafka-bootstrap

# 올바른 주소: my-cluster-kafka-bootstrap:9092
```

**2. Kafka 클러스터 상태 확인**
```bash
# Kafka 리소스 확인
kubectl get kafka my-cluster -n workshop-eda

# READY가 True인지 확인
# False이면 5.1 섹션 참조
```

**3. 토픽 생성 확인**
```bash
# 토픽 목록 확인
kubectl get kafkatopic -n workshop-eda

# 6개 토픽이 모두 Ready 상태인지 확인
```

**4. Kafka 브로커 로그 확인**
```bash
# 브로커 로그
kubectl logs my-cluster-kafka-0 -n workshop-eda | tail -100

# 연결 오류 확인
kubectl logs my-cluster-kafka-0 -n workshop-eda | grep -i "error\|exception"
```

**5. Consumer group 확인**
```bash
# Consumer group 목록
kubectl exec -it my-cluster-kafka-0 -n workshop-eda -- \
  bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

# 특정 group 상세 정보
kubectl exec -it my-cluster-kafka-0 -n workshop-eda -- \
  bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group order-service-group --describe
```

**6. 애플리케이션 Kafka 설정 확인**
```bash
# Kafka 관련 환경 변수 확인
kubectl exec -it deployment/order-service -n workshop-eda -- \
  env | grep KAFKA

# 예상 값:
# SPRING_KAFKA_BOOTSTRAP_SERVERS=my-cluster-kafka-bootstrap:9092
```

---

### 5.6 이미지 Pull 오류

#### 증상
```
ImagePullBackOff
ErrImagePull
```

#### 해결방법

**1. 이미지 이름 확인**
```bash
# Deployment의 이미지 확인
kubectl get deployment order-service -n workshop-eda -o yaml | grep image:

# 올바른 형식: ghcr.io/your-org/order-service:latest
```

**2. 이미지 Pull Secret 확인 (Private 저장소)**
```bash
# Secret 확인
kubectl get secret -n workshop-eda | grep pull

# Secret이 없으면 생성
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=YOUR_GITHUB_USERNAME \
  --docker-password=YOUR_GITHUB_TOKEN \
  --docker-email=YOUR_EMAIL \
  -n workshop-eda

# Deployment에 Secret 연결
kubectl patch serviceaccount default -n workshop-eda \
  -p '{"imagePullSecrets": [{"name": "ghcr-secret"}]}'
```

**3. 이미지 존재 확인**
```bash
# 로컬에서 이미지 pull 테스트
docker pull ghcr.io/your-org/order-service:latest

# 이미지가 없으면 빌드 및 푸시 필요
```

**4. Pod 이벤트 확인**
```bash
kubectl describe pod <pod-name> -n workshop-eda | grep -A 10 Events
```

---

### 5.7 리소스 부족 문제

#### 증상
```
Insufficient cpu
Insufficient memory
```

#### 해결방법

**1. Node 리소스 확인**
```bash
# Node 리소스 사용량
kubectl top nodes

# Node 상세 정보
kubectl describe node <node-name>
```

**2. Pod 리소스 요청량 확인**
```bash
# Pod 리소스 설정 확인
kubectl get deployment order-service -n workshop-eda -o yaml | grep -A 10 resources:
```

**3. 리소스 제한 조정**
```bash
# Helm values.yaml 수정 후 재배포
# 또는 직접 리소스 조정
kubectl set resources deployment/order-service -n workshop-eda \
  --limits=cpu=500m,memory=512Mi \
  --requests=cpu=100m,memory=256Mi
```

**4. 불필요한 Pod 정리**
```bash
# 사용하지 않는 리소스 정리
kubectl delete pod <unused-pod> -n workshop-eda
```

---

### 5.8 일반적인 디버깅 명령어

```bash
# Pod 내부 쉘 접속
kubectl exec -it <pod-name> -n workshop-eda -- /bin/sh

# 임시 디버그 Pod 실행
kubectl run debug --image=busybox -it --rm --restart=Never -n workshop-eda -- sh

# 네트워크 연결 테스트
kubectl run nettest --image=nicolaka/netshoot -it --rm --restart=Never -n workshop-eda -- sh

# DNS 테스트
kubectl exec -it <pod-name> -n workshop-eda -- nslookup order-db

# 포트 연결 테스트
kubectl exec -it <pod-name> -n workshop-eda -- nc -zv order-db 5432

# 파일 복사 (Pod -> 로컬)
kubectl cp workshop-eda/<pod-name>:/path/to/file ./local-file

# 파일 복사 (로컬 -> Pod)
kubectl cp ./local-file workshop-eda/<pod-name>:/path/to/file

# 포트 포워딩
kubectl port-forward svc/order-service 8081:8081 -n workshop-eda

# 리소스 상태 감시
watch kubectl get pods -n workshop-eda

# 모든 리소스 한 번에 확인
kubectl get all,pvc,configmap,secret -n workshop-eda
```

---

### 5.9 로그 수집 및 분석

```bash
# 모든 Pod 로그 수집
for pod in $(kubectl get pods -n workshop-eda -o name); do
  echo "=== $pod ===" >> all-logs.txt
  kubectl logs $pod -n workshop-eda >> all-logs.txt 2>&1
done

# 특정 시간대 로그 수집
kubectl logs deployment/order-service -n workshop-eda \
  --since-time='2026-03-10T10:00:00Z' > order-service-logs.txt

# 에러 로그만 필터링
kubectl logs deployment/order-service -n workshop-eda | grep -i "error\|exception\|failed"

# 여러 Pod의 로그 동시 확인 (stern 사용)
# stern 설치: brew install stern
stern order-service -n workshop-eda
```

---
## 6. 정리 (Clean Up)

배포된 리소스를 제거하는 방법입니다.

### 6.1 Helm으로 배포한 경우

```bash
# Helm 릴리스 제거
helm uninstall workshop-eda -n workshop-eda

# 제거 확인
helm list -n workshop-eda
kubectl get all -n workshop-eda
```

### 6.2 kubectl로 배포한 경우

```bash
# 애플리케이션 제거
kubectl delete -f helm/workshop-eda/templates/apps.yaml -n workshop-eda

# 배포 확인
kubectl get deployments -n workshop-eda
```

### 6.3 인프라 서비스 제거

```bash
# PostgreSQL 제거
kubectl delete -f infra/k8s/postgresql-statefulset.yaml
kubectl delete -f infra/k8s/postgresql-configmap.yaml
kubectl delete -f infra/k8s/postgresql-secret.yaml

# Kafka 토픽 제거
kubectl delete -f infra/k8s/kafka-topics.yaml

# Kafka 클러스터 제거
kubectl delete -f infra/k8s/kafka-cluster.yaml

# 제거 확인
kubectl get kafka,kafkatopic -n workshop-eda
kubectl get statefulset -n workshop-eda
```

### 6.4 PVC 제거 (데이터 영구 삭제)

⚠️ **주의**: PVC를 삭제하면 모든 데이터가 영구적으로 삭제됩니다.

```bash
# PVC 목록 확인
kubectl get pvc -n workshop-eda

# 모든 PVC 삭제
kubectl delete pvc --all -n workshop-eda

# 특정 PVC만 삭제
kubectl delete pvc postgresql-storage-postgresql-0 -n workshop-eda
kubectl delete pvc data-my-cluster-kafka-0 -n workshop-eda
kubectl delete pvc data-my-cluster-zookeeper-0 -n workshop-eda
```

### 6.5 네임스페이스 제거

⚠️ **주의**: 네임스페이스를 삭제하면 해당 네임스페이스의 모든 리소스가 삭제됩니다.

```bash
# 네임스페이스 삭제 (모든 리소스 포함)
kubectl delete namespace workshop-eda

# 삭제 확인
kubectl get namespace workshop-eda
```

### 6.6 단계별 정리 스크립트

```bash
#!/bin/bash
# cleanup-workshop-eda.sh

NAMESPACE="workshop-eda"

echo "=== Workshop EDA 리소스 정리 시작 ==="

# 1. Helm 릴리스 제거
echo "1. Helm 릴리스 제거..."
helm uninstall workshop-eda -n $NAMESPACE 2>/dev/null || echo "Helm 릴리스 없음"

# 2. 애플리케이션 제거
echo "2. 애플리케이션 제거..."
kubectl delete deployment --all -n $NAMESPACE 2>/dev/null || echo "Deployment 없음"
kubectl delete service --all -n $NAMESPACE 2>/dev/null || echo "Service 없음"

# 3. Kafka 리소스 제거
echo "3. Kafka 리소스 제거..."
kubectl delete kafkatopic --all -n $NAMESPACE 2>/dev/null || echo "KafkaTopic 없음"
kubectl delete kafka --all -n $NAMESPACE 2>/dev/null || echo "Kafka 없음"

# 4. PostgreSQL 제거
echo "4. PostgreSQL 제거..."
kubectl delete statefulset postgresql -n $NAMESPACE 2>/dev/null || echo "StatefulSet 없음"
kubectl delete configmap postgresql-initdb -n $NAMESPACE 2>/dev/null || echo "ConfigMap 없음"
kubectl delete secret postgresql-secret -n $NAMESPACE 2>/dev/null || echo "Secret 없음"

# 5. PVC 제거 확인
echo "5. PVC 확인..."
kubectl get pvc -n $NAMESPACE

read -p "PVC를 삭제하시겠습니까? (모든 데이터가 삭제됩니다) [y/N]: " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    kubectl delete pvc --all -n $NAMESPACE
    echo "PVC 삭제 완료"
else
    echo "PVC 유지"
fi

# 6. 네임스페이스 제거 확인
echo "6. 네임스페이스 제거 확인..."
read -p "네임스페이스를 삭제하시겠습니까? [y/N]: " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    kubectl delete namespace $NAMESPACE
    echo "네임스페이스 삭제 완료"
else
    echo "네임스페이스 유지"
fi

echo "=== 정리 완료 ==="
```

**스크립트 실행:**
```bash
chmod +x cleanup-workshop-eda.sh
./cleanup-workshop-eda.sh
```

---

## 7. 참고 자료

### 7.1 관련 문서

- [기존 OCP 배포 가이드](./OCP_DEPLOYMENT_GUIDE.md) - OpenShift 특화 배포 가이드
- [프로젝트 README](../README.md) - 프로젝트 개요 및 시작 가이드
- [아키텍처 문서](../arch.md) - 시스템 아키텍처 상세 설명
- [로컬 테스팅 가이드](./LOCAL_TESTING_GUIDE.md) - 로컬 환경 테스트 방법
- [트러블슈팅 가이드](./TROUBLESHOOTING_GUIDE.md) - 일반적인 문제 해결
- [CI/CD 설정 가이드](./CICD_SETUP_GUIDE.md) - 자동화 파이프라인 구성

### 7.2 공식 문서

- [Kubernetes 공식 문서](https://kubernetes.io/docs/)
- [OpenShift 공식 문서](https://docs.openshift.com/)
- [Helm 공식 문서](https://helm.sh/docs/)
- [Strimzi Kafka Operator](https://strimzi.io/docs/operators/latest/overview.html)
- [PostgreSQL 공식 문서](https://www.postgresql.org/docs/)
- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [Spring Kafka 공식 문서](https://spring.io/projects/spring-kafka)

### 7.3 유용한 도구

- **kubectl**: Kubernetes CLI
- **oc**: OpenShift CLI
- **helm**: Kubernetes 패키지 매니저
- **stern**: 다중 Pod 로그 스트리밍 도구
- **k9s**: Kubernetes 터미널 UI
- **kubectx/kubens**: 컨텍스트/네임스페이스 전환 도구

---

## 8. 부록

### 8.1 주요 연결 정보

#### Kafka 연결 정보
```yaml
Bootstrap Servers: my-cluster-kafka-bootstrap:9092
Bootstrap Servers (TLS): my-cluster-kafka-bootstrap:9093
Zookeeper: my-cluster-zookeeper-client:2181
```

#### PostgreSQL 연결 정보

**Order Service Database:**
```yaml
Host: order-db.workshop-eda.svc.cluster.local
Port: 5432
Database: orderdb
Username: postgres
Password: postgres (from secret)
JDBC URL: jdbc:postgresql://order-db:5432/orderdb
```

**Inventory Service Database:**
```yaml
Host: inventory-db.workshop-eda.svc.cluster.local
Port: 5432
Database: inventorydb
Username: postgres
Password: postgres (from secret)
JDBC URL: jdbc:postgresql://inventory-db:5432/inventorydb
```

**Fulfillment Service Database:**
```yaml
Host: fulfillment-db.workshop-eda.svc.cluster.local
Port: 5432
Database: fulfillmentdb
Username: postgres
Password: postgres (from secret)
JDBC URL: jdbc:postgresql://fulfillment-db:5432/fulfillmentdb
```

**Analytics Service Database:**
```yaml
Host: analytics-db.workshop-eda.svc.cluster.local
Port: 5432
Database: analyticsdb
Username: postgres
Password: postgres (from secret)
JDBC URL: jdbc:postgresql://analytics-db:5432/analyticsdb
```

#### 서비스 엔드포인트

| 서비스 | 내부 URL | 포트 |
|--------|----------|------|
| API Gateway | http://api-gateway:8080 | 8080 |
| Order Service | http://order-service:8081 | 8081 |
| Inventory Service | http://inventory-service:8082 | 8082 |
| Fulfillment Service | http://fulfillment-service:8083 | 8083 |
| Analytics Service | http://analytics-service:8084 | 8084 |
| Payment Adapter | http://payment-adapter-ext:8085 | 8085 |

---

### 8.2 이벤트 플로우

#### 정상 주문 처리 플로우

```
1. 주문 생성
   ├─ API Gateway → Order Service
   └─ Order Service → Kafka Topic: orders.v1.created

2. 재고 확인
   ├─ Inventory Service ← Kafka Topic: orders.v1.created
   └─ Inventory Service → Kafka Topic: orders.v1.inventory_reserved

3. 결제 처리
   ├─ Order Service ← Kafka Topic: orders.v1.inventory_reserved
   ├─ Order Service → Payment Adapter (HTTP)
   └─ Order Service → Kafka Topic: orders.v1.payment_authorized

4. 배송 스케줄링
   ├─ Fulfillment Service ← Kafka Topic: orders.v1.payment_authorized
   └─ Fulfillment Service → Kafka Topic: orders.v1.fulfillment_scheduled

5. 주문 완료
   └─ Order Service ← Kafka Topic: orders.v1.fulfillment_scheduled

6. 분석 데이터 수집 (병렬)
   └─ Analytics Service ← 모든 이벤트 토픽
```

#### 재고 부족 시나리오

```
1. 주문 생성
   └─ Order Service → Kafka Topic: orders.v1.created

2. 재고 확인 실패
   ├─ Inventory Service ← Kafka Topic: orders.v1.created
   └─ Inventory Service → Kafka Topic: orders.v1.inventory_rejected

3. 주문 취소
   └─ Order Service ← Kafka Topic: orders.v1.inventory_rejected
```

#### 결제 실패 시나리오

```
1-2. 주문 생성 및 재고 예약 (정상)

3. 결제 실패
   ├─ Order Service → Payment Adapter (HTTP)
   └─ Order Service → Kafka Topic: orders.v1.payment_failed

4. 재고 복원
   ├─ Inventory Service ← Kafka Topic: orders.v1.payment_failed
   └─ Inventory Service: 예약 취소 및 재고 복원

5. 주문 취소
   └─ Order Service: 주문 상태 업데이트 (PAYMENT_FAILED)
```

---

### 8.3 Kafka 토픽 상세 정보

| 토픽 이름 | Producer | Consumer | 파티션 | 복제본 | 보관 기간 |
|-----------|----------|----------|--------|--------|-----------|
| orders.v1.created | order-service | inventory-service, analytics-service | 3 | 3 | 7일 |
| orders.v1.inventory_reserved | inventory-service | order-service, analytics-service | 3 | 3 | 7일 |
| orders.v1.inventory_rejected | inventory-service | order-service, analytics-service | 3 | 3 | 7일 |
| orders.v1.payment_authorized | order-service | fulfillment-service, analytics-service | 3 | 3 | 7일 |
| orders.v1.payment_failed | order-service | order-service, inventory-service, analytics-service | 3 | 3 | 7일 |
| orders.v1.fulfillment_scheduled | fulfillment-service | order-service, analytics-service | 3 | 3 | 7일 |

---

### 8.4 데이터베이스 스키마

#### Order Database (orderdb)

```sql
-- orders 테이블
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
```

#### Inventory Database (inventorydb)

```sql
-- inventory 테이블
CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    product_id VARCHAR(255) UNIQUE NOT NULL,
    quantity INTEGER NOT NULL,
    reserved_quantity INTEGER DEFAULT 0,
    updated_at TIMESTAMP NOT NULL
);

-- reservations 테이블
CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_reservations_order_id ON reservations(order_id);
CREATE INDEX idx_reservations_status ON reservations(status);
```

#### Fulfillment Database (fulfillmentdb)

```sql
-- fulfillments 테이블
CREATE TABLE fulfillments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    scheduled_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_fulfillments_order_id ON fulfillments(order_id);
CREATE INDEX idx_fulfillments_status ON fulfillments(status);
```

#### Analytics Database (analyticsdb)

```sql
-- event_logs 테이블
CREATE TABLE event_logs (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- order_metrics 테이블
CREATE TABLE order_metrics (
    id BIGSERIAL PRIMARY KEY,
    order_id UUID NOT NULL,
    customer_id VARCHAR(255),
    product_id VARCHAR(255),
    quantity INTEGER,
    price DECIMAL(10, 2),
    status VARCHAR(50),
    created_at TIMESTAMP NOT NULL
);

-- product_metrics 테이블
CREATE TABLE product_metrics (
    id BIGSERIAL PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL,
    total_orders INTEGER DEFAULT 0,
    total_quantity INTEGER DEFAULT 0,
    total_revenue DECIMAL(12, 2) DEFAULT 0,
    updated_at TIMESTAMP NOT NULL
);

-- event_count_summary 테이블
CREATE TABLE event_count_summary (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    count BIGINT DEFAULT 0,
    last_updated TIMESTAMP NOT NULL
);

CREATE INDEX idx_event_logs_type ON event_logs(event_type);
CREATE INDEX idx_event_logs_created_at ON event_logs(created_at);
CREATE INDEX idx_order_metrics_order_id ON order_metrics(order_id);
CREATE INDEX idx_product_metrics_product_id ON product_metrics(product_id);
CREATE UNIQUE INDEX idx_event_count_summary_type ON event_count_summary(event_type);
```

---

### 8.5 환경 변수 참조

#### Order Service

```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://order-db:5432/orderdb
SPRING_DATASOURCE_USERNAME: postgres
SPRING_DATASOURCE_PASSWORD: postgres
SPRING_KAFKA_BOOTSTRAP_SERVERS: my-cluster-kafka-bootstrap:9092
PAYMENT_SERVICE_URL: http://payment-adapter-ext:8085
SERVER_PORT: 8081
```

#### Inventory Service

```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://inventory-db:5432/inventorydb
SPRING_DATASOURCE_USERNAME: postgres
SPRING_DATASOURCE_PASSWORD: postgres
SPRING_KAFKA_BOOTSTRAP_SERVERS: my-cluster-kafka-bootstrap:9092
SERVER_PORT: 8082
```

#### Fulfillment Service

```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://fulfillment-db:5432/fulfillmentdb
SPRING_DATASOURCE_USERNAME: postgres
SPRING_DATASOURCE_PASSWORD: postgres
SPRING_KAFKA_BOOTSTRAP_SERVERS: my-cluster-kafka-bootstrap:9092
SERVER_PORT: 8083
```

#### Analytics Service

```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://analytics-db:5432/analyticsdb
SPRING_DATASOURCE_USERNAME: postgres
SPRING_DATASOURCE_PASSWORD: postgres
SPRING_KAFKA_BOOTSTRAP_SERVERS: my-cluster-kafka-bootstrap:9092
SERVER_PORT: 8084
```

#### API Gateway

```yaml
ORDER_SERVICE_URL: http://order-service:8081
SERVER_PORT: 8080
```

#### Payment Adapter

```yaml
SERVER_PORT: 8085
PAYMENT_SUCCESS_RATE: 0.9
```

---

### 8.6 유용한 명령어 치트시트

#### 빠른 상태 확인

```bash
# 모든 리소스 확인
kubectl get all -n workshop-eda

# Pod 상태 확인
kubectl get pods -n workshop-eda -o wide

# Service 확인
kubectl get svc -n workshop-eda

# Kafka 리소스 확인
kubectl get kafka,kafkatopic -n workshop-eda

# PVC 확인
kubectl get pvc -n workshop-eda

# 이벤트 확인
kubectl get events -n workshop-eda --sort-by='.lastTimestamp'
```

#### 로그 확인

```bash
# 실시간 로그
kubectl logs -f deployment/order-service -n workshop-eda

# 최근 100줄
kubectl logs --tail=100 deployment/order-service -n workshop-eda

# 여러 Pod 동시 확인
kubectl logs -f -l app.kubernetes.io/name=order-service -n workshop-eda

# 에러만 필터링
kubectl logs deployment/order-service -n workshop-eda | grep -i error
```

#### 디버깅

```bash
# Pod 내부 접속
kubectl exec -it deployment/order-service -n workshop-eda -- /bin/sh

# 포트 포워딩
kubectl port-forward svc/order-service 8081:8081 -n workshop-eda

# 리소스 사용량
kubectl top pods -n workshop-eda
kubectl top nodes

# 상세 정보
kubectl describe pod <pod-name> -n workshop-eda
kubectl describe svc order-service -n workshop-eda
```

#### 스케일링

```bash
# 수동 스케일링
kubectl scale deployment order-service --replicas=3 -n workshop-eda

# 오토스케일링 설정
kubectl autoscale deployment order-service \
  --min=2 --max=10 --cpu-percent=70 -n workshop-eda

# HPA 확인
kubectl get hpa -n workshop-eda
```

#### 롤아웃 관리

```bash
# 롤아웃 상태
kubectl rollout status deployment/order-service -n workshop-eda

# 롤아웃 히스토리
kubectl rollout history deployment/order-service -n workshop-eda

# 롤백
kubectl rollout undo deployment/order-service -n workshop-eda

# 특정 리비전으로 롤백
kubectl rollout undo deployment/order-service --to-revision=2 -n workshop-eda
```

---

### 8.7 성능 튜닝 팁

#### Kafka 성능 최적화

```yaml
# kafka-cluster.yaml에서 조정
config:
  num.network.threads: 8  # 네트워크 스레드 증가
  num.io.threads: 16      # I/O 스레드 증가
  socket.send.buffer.bytes: 1048576
  socket.receive.buffer.bytes: 1048576
  compression.type: lz4   # 압축 알고리즘 변경
```

#### PostgreSQL 성능 최적화

```yaml
# postgresql-configmap.yaml에서 조정
shared_buffers: 256MB
effective_cache_size: 1GB
maintenance_work_mem: 64MB
checkpoint_completion_target: 0.9
wal_buffers: 16MB
default_statistics_target: 100
random_page_cost: 1.1
effective_io_concurrency: 200
```

#### 애플리케이션 리소스 최적화

```yaml
# Helm values.yaml에서 조정
resources:
  requests:
    cpu: 200m      # 초기 요청량
    memory: 512Mi
  limits:
    cpu: 1000m     # 최대 사용량
    memory: 1Gi
```

---

### 8.8 보안 권장사항

1. **Secret 관리**
   - 프로덕션 환경에서는 외부 Secret 관리 도구 사용 (Vault, Sealed Secrets)
   - 기본 비밀번호 변경

2. **네트워크 정책**
   - NetworkPolicy로 Pod 간 통신 제한
   - 필요한 통신만 허용

3. **RBAC 설정**
   - 최소 권한 원칙 적용
   - ServiceAccount별 권한 분리

4. **이미지 보안**
   - 신뢰할 수 있는 레지스트리 사용
   - 이미지 스캔 도구 활용
   - 최신 베이스 이미지 사용

5. **TLS/SSL**
   - Kafka TLS 리스너 사용
   - Ingress/Route에 TLS 적용
   - 서비스 간 통신 암호화

---

### 8.9 모니터링 및 관찰성

#### Prometheus 메트릭 수집

```yaml
# ServiceMonitor 예시
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: order-service
  namespace: workshop-eda
spec:
  selector:
    matchLabels:
      app.kubernetes.io/name: order-service
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
```

#### 주요 메트릭

- **애플리케이션 메트릭**: JVM, HTTP 요청, 데이터베이스 연결
- **Kafka 메트릭**: 처리량, 지연시간, Consumer Lag
- **데이터베이스 메트릭**: 연결 수, 쿼리 성능, 트랜잭션