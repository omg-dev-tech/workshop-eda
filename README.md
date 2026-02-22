# Demo E-commerce Application for Instana PoC

![Build and Push](https://github.com/omg-dev-tech/workshop-eda/actions/workflows/build-and-push.yml/badge.svg)

This is Demo Application for Instana PoC.

## Architecture
1. Event Driven Arch.
2. Micro Service Arch.

## Core Feature
1. Based OpenTelemetry
2. Including Custom Business Metrics
3. Error Rate Configuration
4. EUM possible
## Requirement

## Install

## Container Images

모든 마이크로서비스는 GitHub Container Registry(GHCR)에서 사용할 수 있습니다:

```bash
# 최신 이미지 Pull
docker pull ghcr.io/omg-dev-tech/workshop-eda/order-service:latest
docker pull ghcr.io/omg-dev-tech/workshop-eda/inventory-service:latest
docker pull ghcr.io/omg-dev-tech/workshop-eda/fulfillment-service:latest
docker pull ghcr.io/omg-dev-tech/workshop-eda/payment-adapter-ext:latest
docker pull ghcr.io/omg-dev-tech/workshop-eda/analytics-service:latest
docker pull ghcr.io/omg-dev-tech/workshop-eda/api-gateway:latest
docker pull ghcr.io/omg-dev-tech/workshop-eda/load-generator:latest

# 특정 커밋 버전 Pull (예시)
docker pull ghcr.io/omg-dev-tech/workshop-eda/order-service:abc1234
```

### 이미지 빌드 자동화

main 브랜치에 푸시하면 GitHub Actions가 자동으로:
- 8개 마이크로서비스 이미지를 빌드 (6개 서비스 + Web UI + Load Generator)
- GHCR에 `latest` 태그와 커밋 SHA 태그로 푸시
- 빌드 캐시를 활용하여 빌드 시간 최적화

## Start

## Stop

## Uninstall

## Load Generator

자동 부하 발생기를 사용하여 시스템에 부하를 생성할 수 있습니다.

### 주요 기능
- **주문 생성**: 설정 가능한 TPS로 랜덤 주문 생성
- **재고 보충**: 주기적으로 재고 확인 및 자동 보충
- **배송 처리**: SCHEDULED 상태의 배송을 자동으로 SHIPPED로 변경
- **통계 수집**: 성공/실패 통계 실시간 출력

### 환경변수 설정

```bash
API_GATEWAY_URL=http://api-gateway:8080  # API Gateway URL
TPS=10                                    # Transactions Per Second
DURATION=300                              # 실행 시간 (초, 0=무한)
WORKERS=10                                # 동시 실행 워커 수
REPLENISH_INTERVAL=30                     # 재고 보충 주기 (초)
SHIP_INTERVAL=20                          # 배송 처리 주기 (초)
REPLENISH_QTY=100                         # 재고 보충 수량
LOOP_MODE=false                           # 무한 루프 모드 (true/false)
```

### 로컬 실행

```bash
# Docker로 실행
docker run --rm \
  -e API_GATEWAY_URL=http://localhost:8080 \
  -e TPS=10 \
  -e DURATION=60 \
  ghcr.io/omg-dev-tech/workshop-eda/load-generator:latest

# Python으로 직접 실행
cd load-generator
pip install -r requirements.txt
python load_generator.py
```

### OCP/Kubernetes 배포

Helm Chart를 통해 Deployment, Job, CronJob으로 배포할 수 있습니다:

```yaml
# values.yaml
apps:
  load-generator:
    enabled: true
    type: "deployment"  # deployment, job, cronjob
    replicas: 1  # Deployment인 경우
    schedule: "*/10 * * * *"  # CronJob인 경우
    env:
      TPS: "20"
      DURATION: "300"
```

#### Deployment로 지속 실행 (권장)

```bash
# 기본 설정으로 배포 (TPS=10, 무한 실행)
helm upgrade --install workshop-eda ./helm/workshop-eda \
  --set apps.load-generator.enabled=true \
  --set apps.load-generator.type=deployment

# TPS 조정하여 배포
helm upgrade --install workshop-eda ./helm/workshop-eda \
  --set apps.load-generator.enabled=true \
  --set apps.load-generator.type=deployment \
  --set apps.load-generator.env.TPS=20

# 중지 (enabled=false)
helm upgrade --install workshop-eda ./helm/workshop-eda \
  --set apps.load-generator.enabled=false
```

#### Job으로 1회 실행

```bash
helm upgrade --install workshop-eda ./helm/workshop-eda \
  --set apps.load-generator.enabled=true \
  --set apps.load-generator.type=job \
  --set apps.load-generator.env.DURATION=300
```

#### CronJob으로 주기적 실행

```bash
# 10분마다 실행 (Kubernetes 최소 간격: 1분)
helm upgrade --install workshop-eda ./helm/workshop-eda \
  --set apps.load-generator.enabled=true \
  --set apps.load-generator.type=cronjob \
  --set apps.load-generator.schedule="*/10 * * * *"
```

> **참고**: Kubernetes CronJob은 최소 1분 간격으로만 스케줄링 가능합니다.
> 지속적인 부하 생성이 필요한 경우 **Deployment** 타입을 사용하세요.

### 로그 확인 및 디버깅

```bash
# Pod 이름 확인
kubectl get pods -l app=load-generator

# 실시간 로그 확인
kubectl logs -f deployment/load-generator

# 최근 로그 확인 (마지막 100줄)
kubectl logs deployment/load-generator --tail=100

# 특정 Pod 로그 확인
kubectl logs <pod-name>

# 이전 실행 로그 확인 (재시작된 경우)
kubectl logs deployment/load-generator --previous
```

**로그 출력 예시:**
```
=== Load Generator Started ===
API Gateway URL: http://api-gateway:8080
Target TPS: 10
Duration: Infinite
...
[10s] Orders: 100 (Success: 95, Fail: 5), TPS: 10.00
[Replenish] product-001: 45 -> 145
[Ship] Fulfillment #123 shipped (Order: order-456)
[Order Failed] Status: 500, Response: {"error":"Internal Server Error"}
```

## Local Testing

로컬 환경에서 전체 시스템을 테스트하는 방법은 [로컬 테스트 가이드](docs/LOCAL_TESTING_GUIDE.md)를 참조하세요.

## Deployment

GitHub 리포지토리 설정 및 배포 방법은 [GitHub 배포 가이드](docs/GITHUB_DEPLOYMENT_GUIDE.md)를 참조하세요.

