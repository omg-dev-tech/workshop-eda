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

# 특정 커밋 버전 Pull (예시)
docker pull ghcr.io/omg-dev-tech/workshop-eda/order-service:abc1234
```

### 이미지 빌드 자동화

main 브랜치에 푸시하면 GitHub Actions가 자동으로:
- 6개 마이크로서비스 이미지를 빌드
- GHCR에 `latest` 태그와 커밋 SHA 태그로 푸시
- 빌드 캐시를 활용하여 빌드 시간 최적화

## Start

## Stop

## Uninstall

## Local Testing

로컬 환경에서 전체 시스템을 테스트하는 방법은 [로컬 테스트 가이드](docs/LOCAL_TESTING_GUIDE.md)를 참조하세요.

## Deployment

GitHub 리포지토리 설정 및 배포 방법은 [GitHub 배포 가이드](docs/GITHUB_DEPLOYMENT_GUIDE.md)를 참조하세요.

