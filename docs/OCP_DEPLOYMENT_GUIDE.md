# OpenShift(OCP) 배포 가이드

이 문서는 Workshop EDA 애플리케이션을 OpenShift Container Platform(OCP)에 배포하는 방법을 설명합니다.

## 목차
1. [사전 요구사항](#사전-요구사항)
2. [OCP 로그인](#ocp-로그인)
3. [프로젝트 생성](#프로젝트-생성)
4. [인프라 서비스 배포](#인프라-서비스-배포)
5. [애플리케이션 배포](#애플리케이션-배포)
6. [서비스 확인](#서비스-확인)
7. [외부 접근 설정](#외부-접근-설정)
8. [모니터링 및 로깅](#모니터링-및-로깅)
9. [문제 해결](#문제-해결)
10. [배포 제거](#배포-제거)

---

## 사전 요구사항

### 필수 도구
- **oc CLI**: OpenShift 명령줄 도구
  ```bash
  # macOS
  brew install openshift-cli
  
  # Linux
  wget https://mirror.openshift.com/pub/openshift-v4/clients/ocp/latest/openshift-client-linux.tar.gz
  tar -xvf openshift-client-linux.tar.gz
  sudo mv oc /usr/local/bin/
  ```

- **Helm 3**: Kubernetes 패키지 매니저
  ```bash
  # macOS
  brew install helm
  
  # Linux
  curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
  ```

### 접근 권한
- OCP 클러스터 접근 권한
- 프로젝트 생성 권한
- 이미지 Pull 권한 (GHCR public 이미지 사용)

---

## OCP 로그인

### 1. 웹 콘솔을 통한 로그인 토큰 획득
1. OCP 웹 콘솔에 접속
2. 우측 상단 사용자 이름 클릭
3. "Copy login command" 선택
4. "Display Token" 클릭
5. 로그인 명령어 복사

### 2. CLI 로그인
```bash
# 복사한 명령어 실행 (예시)
oc login --token=sha256~XXXXX --server=https://api.your-cluster.com:6443

# 로그인 확인
oc whoami
oc cluster-info
```

---

## 프로젝트 생성

### 새 프로젝트 생성
```bash
# 프로젝트 생성
oc new-project workshop-eda --display-name="Workshop EDA" --description="Event-Driven Architecture Workshop"

# 현재 프로젝트 확인
oc project

# 프로젝트 목록 확인
oc projects
```

### 기존 프로젝트 사용
```bash
# 프로젝트 전환
oc project workshop-eda
```

---

## 인프라 서비스 배포

애플리케이션 실행에 필요한 인프라 서비스를 먼저 배포합니다.

### 1. PostgreSQL 배포

#### Operator를 통한 배포 (권장)
```bash
# PostgreSQL Operator 설치 (관리자 권한 필요)
# OCP 웹 콘솔 > Operators > OperatorHub > PostgreSQL 검색 및 설치

# 또는 템플릿 사용
oc new-app postgresql-persistent \
  -p POSTGRESQL_USER=postgres \
  -p POSTGRESQL_PASSWORD=postgres \
  -p POSTGRESQL_DATABASE=postgres \
  -p VOLUME_CAPACITY=5Gi
```

#### 개별 데이터베이스 생성
```bash
# PostgreSQL Pod에 접속
POD_NAME=$(oc get pods -l name=postgresql -o jsonpath='{.items[0].metadata.name}')
oc rsh $POD_NAME

# 데이터베이스 생성
psql -U postgres -c "CREATE DATABASE orderdb;"
psql -U postgres -c "CREATE DATABASE inventorydb;"
psql -U postgres -c "CREATE DATABASE fulfillmentdb;"
psql -U postgres -c "CREATE DATABASE analyticsdb;"

# 초기 데이터 로드 (선택사항)
# 로컬에서 SQL 파일 복사 후 실행
oc cp infra/sql/order-init.sql $POD_NAME:/tmp/
oc rsh $POD_NAME psql -U postgres -d orderdb -f /tmp/order-init.sql
```

### 2. Apache Kafka 배포

#### Strimzi Operator를 통한 배포 (권장)
```bash
# Strimzi Operator 설치
# OCP 웹 콘솔 > Operators > OperatorHub > Strimzi 검색 및 설치

# Kafka 클러스터 생성
cat <<EOF | oc apply -f -
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: kafka-cluster
  namespace: workshop-eda
spec:
  kafka:
    version: 3.6.0
    replicas: 3
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
      - name: tls
        port: 9093
        type: internal
        tls: true
    config:
      offsets.topic.replication.factor: 3
      transaction.state.log.replication.factor: 3
      transaction.state.log.min.isr: 2
      default.replication.factor: 3
      min.insync.replicas: 2
    storage:
      type: ephemeral
  zookeeper:
    replicas: 3
    storage:
      type: ephemeral
  entityOperator:
    topicOperator: {}
    userOperator: {}
EOF

# Kafka 클러스터 상태 확인
oc get kafka kafka-cluster -o yaml
oc wait kafka/kafka-cluster --for=condition=Ready --timeout=300s
```

#### Kafka 토픽 생성
```bash
# 필요한 토픽 생성
cat <<EOF | oc apply -f -
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: order-events
  namespace: workshop-eda
  labels:
    strimzi.io/cluster: kafka-cluster
spec:
  partitions: 3
  replicas: 3
  config:
    retention.ms: 604800000
    segment.bytes: 1073741824
---
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: inventory-events
  namespace: workshop-eda
  labels:
    strimzi.io/cluster: kafka-cluster
spec:
  partitions: 3
  replicas: 3
---
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: payment-events
  namespace: workshop-eda
  labels:
    strimzi.io/cluster: kafka-cluster
spec:
  partitions: 3
  replicas: 3
---
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: fulfillment-events
  namespace: workshop-eda
  labels:
    strimzi.io/cluster: kafka-cluster
spec:
  partitions: 3
  replicas: 3
EOF

# 토픽 확인
oc get kafkatopic
```

### 3. 인프라 서비스 확인
```bash
# 모든 Pod 상태 확인
oc get pods

# 서비스 확인
oc get svc

# 대기 (모든 Pod가 Running 상태가 될 때까지)
oc wait --for=condition=Ready pods --all --timeout=300s
```

---

## 애플리케이션 배포

### 1. GHCR 이미지 Pull Secret 생성 (Private 저장소인 경우)

GHCR이 public이면 이 단계는 건너뛰세요.

```bash
# GitHub Personal Access Token 생성 필요
# Settings > Developer settings > Personal access tokens > Generate new token
# 권한: read:packages

# Secret 생성
oc create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=YOUR_GITHUB_USERNAME \
  --docker-password=YOUR_GITHUB_TOKEN \
  --docker-email=YOUR_EMAIL

# Secret 확인
oc get secret ghcr-secret
```

### 2. Helm 차트로 애플리케이션 배포

#### values.yaml 환경 설정 확인
배포 전에 `helm/workshop-eda/values.yaml` 파일에서 다음 설정을 확인하세요:

```yaml
global:
  imageRegistry: "ghcr.io/omg-dev-tech/workshop-eda"
  imageTag: "latest"
  imagePullSecret: ""  # Private 저장소면 "ghcr-secret"로 변경
  kafkaBootstrap: "kafka-cluster-kafka-bootstrap:9092"
```

#### Helm 차트 배포
```bash
# 프로젝트 루트 디렉토리에서 실행
cd /path/to/workshop-eda

# Helm 차트 검증
helm lint helm/workshop-eda

# Dry-run으로 배포 테스트
helm install workshop-eda helm/workshop-eda \
  --namespace workshop-eda \
  --dry-run --debug

# 실제 배포
helm install workshop-eda helm/workshop-eda \
  --namespace workshop-eda \
  --create-namespace

# 배포 상태 확인
helm list -n workshop-eda
helm status workshop-eda -n workshop-eda
```

#### 특정 값 오버라이드하여 배포
```bash
# 명령줄에서 값 오버라이드
helm install workshop-eda helm/workshop-eda \
  --namespace workshop-eda \
  --set global.imageTag=v1.0.0 \
  --set global.kafkaBootstrap=kafka-cluster-kafka-bootstrap:9092

# 또는 별도 values 파일 사용
helm install workshop-eda helm/workshop-eda \
  --namespace workshop-eda \
  --values helm/workshop-eda/values-prod.yaml
```

### 3. 배포 업그레이드
```bash
# 설정 변경 후 업그레이드
helm upgrade workshop-eda helm/workshop-eda \
  --namespace workshop-eda \
  --reuse-values

# 새 이미지 버전으로 업그레이드
helm upgrade workshop-eda helm/workshop-eda \
  --namespace workshop-eda \
  --set global.imageTag=v1.1.0
```

---

## 서비스 확인

### 1. Pod 상태 확인
```bash
# 모든 Pod 확인
oc get pods -n workshop-eda

# 특정 서비스 Pod 확인
oc get pods -l app.kubernetes.io/name=order-service

# Pod 상세 정보
oc describe pod <pod-name>

# Pod 로그 확인
oc logs -f <pod-name>

# 이전 컨테이너 로그 확인 (재시작된 경우)
oc logs <pod-name> --previous
```

### 2. 서비스 확인
```bash
# 모든 서비스 확인
oc get svc -n workshop-eda

# 서비스 상세 정보
oc describe svc order-service

# 엔드포인트 확인
oc get endpoints
```

### 3. 애플리케이션 Health Check
```bash
# Pod 내부에서 Health Check
oc exec -it <pod-name> -- curl http://localhost:8080/actuator/health

# 또는 포트 포워딩으로 로컬에서 확인
oc port-forward svc/order-service 8080:8080
curl http://localhost:8080/actuator/health
```

---

## 외부 접근 설정

### 1. Route 생성 (API Gateway)

OpenShift의 Route를 사용하여 외부에서 API Gateway에 접근할 수 있도록 설정합니다.

```bash
# HTTP Route 생성
oc expose svc/api-gateway --name=api-gateway-route

# Route 확인
oc get route api-gateway-route

# Route URL 확인
ROUTE_URL=$(oc get route api-gateway-route -o jsonpath='{.spec.host}')
echo "API Gateway URL: http://$ROUTE_URL"

# API 테스트
curl http://$ROUTE_URL/actuator/health
```

### 2. HTTPS Route 생성 (TLS 적용)
```bash
# Edge TLS 종료를 사용한 HTTPS Route
oc create route edge api-gateway-secure \
  --service=api-gateway \
  --insecure-policy=Redirect

# Route 확인
oc get route api-gateway-secure

# HTTPS URL 확인
SECURE_URL=$(oc get route api-gateway-secure -o jsonpath='{.spec.host}')
echo "Secure API Gateway URL: https://$SECURE_URL"

# HTTPS API 테스트
curl https://$SECURE_URL/actuator/health
```

### 3. 주문 생성 테스트
```bash
# 주문 생성 API 호출
curl -X POST https://$SECURE_URL/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-001",
    "productId": "product-001",
    "quantity": 2,
    "price": 29.99
  }'

# 주문 조회
curl https://$SECURE_URL/api/orders
```

### 4. 개별 서비스 Route 생성 (선택사항)
```bash
# 각 서비스별 Route 생성
oc expose svc/order-service
oc expose svc/inventory-service
oc expose svc/fulfillment-service
oc expose svc/analytics-service

# 모든 Route 확인
oc get routes
```

---

## 모니터링 및 로깅

### 1. OCP 웹 콘솔 모니터링
1. OCP 웹 콘솔 접속
2. **Workloads > Pods** 메뉴에서 Pod 상태 확인
3. **Monitoring > Metrics** 메뉴에서 메트릭 확인
4. **Monitoring > Dashboards** 메뉴에서 대시보드 확인

### 2. 로그 확인
```bash
# 실시간 로그 스트리밍
oc logs -f deployment/order-service

# 여러 Pod의 로그 동시 확인
oc logs -f -l app.kubernetes.io/name=order-service

# 최근 로그 확인
oc logs --tail=100 deployment/order-service

# 특정 시간 이후 로그
oc logs --since=1h deployment/order-service
```

### 3. 이벤트 확인
```bash
# 프로젝트 이벤트 확인
oc get events --sort-by='.lastTimestamp'

# 특정 리소스 이벤트
oc describe pod <pod-name> | grep -A 10 Events
```

### 4. 리소스 사용량 확인
```bash
# Pod 리소스 사용량
oc adm top pods

# Node 리소스 사용량
oc adm top nodes

# 특정 Pod의 상세 리소스 사용량
oc adm top pod <pod-name> --containers
```

### 5. Kafka 모니터링
```bash
# Kafka 클러스터 상태
oc get kafka kafka-cluster -o yaml

# Kafka 토픽 상태
oc get kafkatopic

# Kafka Pod 로그
oc logs -f kafka-cluster-kafka-0
```

---

## 문제 해결

### 1. Pod가 시작되지 않는 경우

#### ImagePullBackOff 오류
```bash
# Pod 상태 확인
oc describe pod <pod-name>

# 원인:
# - 이미지 이름 오류
# - 이미지 Pull Secret 누락 (Private 저장소)
# - 네트워크 문제

# 해결:
# 1. 이미지 이름 확인
oc get deployment <deployment-name> -o yaml | grep image:

# 2. Pull Secret 확인 및 추가
oc get secret ghcr-secret
oc set serviceaccount deployment/<deployment-name> default
oc secrets link default ghcr-secret --for=pull
```

#### CrashLoopBackOff 오류
```bash
# Pod 로그 확인
oc logs <pod-name>
oc logs <pod-name> --previous

# 원인:
# - 애플리케이션 설정 오류
# - 의존 서비스 미연결 (DB, Kafka)
# - 리소스 부족

# 해결:
# 1. 환경 변수 확인
oc set env deployment/<deployment-name> --list

# 2. 의존 서비스 확인
oc get svc postgresql kafka-cluster-kafka-bootstrap

# 3. 리소스 제한 조정
oc set resources deployment/<deployment-name> \
  --limits=cpu=1000m,memory=1Gi \
  --requests=cpu=200m,memory=512Mi
```

### 2. 서비스 연결 문제

#### 서비스 간 통신 실패
```bash
# DNS 확인
oc exec -it <pod-name> -- nslookup order-service

# 서비스 엔드포인트 확인
oc get endpoints order-service

# 네트워크 정책 확인
oc get networkpolicy

# 포트 확인
oc exec -it <pod-name> -- curl http://order-service:8080/actuator/health
```

#### Kafka 연결 문제
```bash
# Kafka 서비스 확인
oc get svc kafka-cluster-kafka-bootstrap

# Kafka 연결 테스트
oc run kafka-test --image=confluentinc/cp-kafka:latest --rm -it --restart=Never -- \
  kafka-topics --bootstrap-server kafka-cluster-kafka-bootstrap:9092 --list

# 환경 변수 확인
oc set env deployment/order-service --list | grep KAFKA
```

### 3. 데이터베이스 연결 문제
```bash
# PostgreSQL 서비스 확인
oc get svc postgresql

# PostgreSQL 연결 테스트
oc run psql-test --image=postgres:15 --rm -it --restart=Never -- \
  psql -h postgresql -U postgres -d orderdb -c "SELECT 1"

# 데이터베이스 존재 확인
oc rsh deployment/postgresql
psql -U postgres -l
```

### 4. 성능 문제

#### Pod 리소스 부족
```bash
# 리소스 사용량 확인
oc adm top pods

# 리소스 제한 증가
oc set resources deployment/order-service \
  --limits=cpu=2000m,memory=2Gi \
  --requests=cpu=500m,memory=1Gi

# HPA (Horizontal Pod Autoscaler) 설정
oc autoscale deployment/order-service \
  --min=2 --max=10 --cpu-percent=70
```

#### 느린 응답 시간
```bash
# Readiness/Liveness Probe 조정
oc set probe deployment/order-service \
  --readiness --initial-delay-seconds=120 --period-seconds=60

# 로그에서 성능 문제 확인
oc logs deployment/order-service | grep -i "slow\|timeout\|error"
```

### 5. 일반적인 디버깅 명령어
```bash
# Pod 내부 쉘 접속
oc rsh <pod-name>

# 임시 디버그 Pod 실행
oc debug deployment/order-service

# 파일 복사
oc cp <pod-name>:/path/to/file ./local-file
oc cp ./local-file <pod-name>:/path/to/file

# 포트 포워딩
oc port-forward pod/<pod-name> 8080:8080

# 리소스 상태 감시
watch oc get pods
```

---

## 배포 제거

### 1. Helm 차트 제거
```bash
# Helm 릴리스 제거
helm uninstall workshop-eda -n workshop-eda

# 제거 확인
helm list -n workshop-eda
oc get all -n workshop-eda
```

### 2. 인프라 서비스 제거
```bash
# Kafka 클러스터 제거
oc delete kafka kafka-cluster

# PostgreSQL 제거
oc delete all -l app=postgresql

# PVC 제거 (데이터 영구 삭제)
oc delete pvc --all
```

### 3. 프로젝트 제거
```bash
# 프로젝트 전체 제거 (주의: 모든 리소스 삭제됨)
oc delete project workshop-eda

# 제거 확인
oc get projects | grep workshop-eda
```

---

## 추가 리소스

### 공식 문서
- [OpenShift Documentation](https://docs.openshift.com/)
- [Helm Documentation](https://helm.sh/docs/)
- [Strimzi Kafka Operator](https://strimzi.io/docs/operators/latest/overview.html)

### 유용한 명령어 치트시트
```bash
# 빠른 상태 확인
oc get all -n workshop-eda

# 모든 리소스 상세 정보
oc get all,cm,secret,pvc,route -n workshop-eda

# 특정 라벨의 리소스 확인
oc get all -l app.kubernetes.io/name=order-service

# YAML 출력
oc get deployment order-service -o yaml

# JSON 경로 쿼리
oc get pods -o jsonpath='{.items[*].metadata.name}'

# 리소스 편집
oc edit deployment order-service

# 스케일 조정
oc scale deployment order-service --replicas=3

# 롤아웃 상태 확인
oc rollout status deployment/order-service

# 롤아웃 히스토리
oc rollout history deployment/order-service

# 이전 버전으로 롤백
oc rollout undo deployment/order-service
```

---

## 보안 고려사항

### 1. Secret 관리
```bash
# Secret 생성
oc create secret generic db-credentials \
  --from-literal=username=postgres \
  --from-literal=password=secure-password

# Secret을 환경 변수로 사용
oc set env deployment/order-service \
  --from=secret/db-credentials
```

### 2. Network Policy
```bash
# 네트워크 정책 예시
cat <<EOF | oc apply -f -
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-from-same-namespace
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector: {}
EOF
```

### 3. Security Context Constraints (SCC)
```bash
# SCC 확인
oc get scc

# Pod의 SCC 확인
oc get pod <pod-name> -o yaml | grep scc
```

---

## 다음 단계

1. **모니터링 설정**: Prometheus, Grafana 통합
2. **로깅 설정**: EFK (Elasticsearch, Fluentd, Kibana) 스택
3. **CI/CD 파이프라인**: Tekton, Jenkins 통합
4. **백업 및 복구**: Velero 설정
5. **성능 튜닝**: 리소스 최적화 및 오토스케일링

---

## 지원 및 문의

문제가 발생하거나 질문이 있으시면:
- GitHub Issues: [프로젝트 저장소 이슈 페이지]
- 내부 Slack 채널: #workshop-eda-support

---

**마지막 업데이트**: 2026-02-20