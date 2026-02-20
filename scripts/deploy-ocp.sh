#!/bin/bash

################################################################################
# OpenShift(OCP) 배포 스크립트
# 
# 이 스크립트는 Workshop EDA 애플리케이션을 OpenShift에 배포합니다.
# 
# 사용법:
#   ./scripts/deploy-ocp.sh [options]
#
# 옵션:
#   --project <name>        프로젝트 이름 (기본값: workshop-eda)
#   --skip-infra           인프라 서비스 배포 건너뛰기
#   --skip-kafka           Kafka 배포 건너뛰기
#   --skip-postgres        PostgreSQL 배포 건너뛰기
#   --image-tag <tag>      이미지 태그 (기본값: latest)
#   --dry-run              실제 배포 없이 테스트만 수행
#   --help                 도움말 표시
#
################################################################################

set -e  # 오류 발생 시 스크립트 중단

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 기본 설정
PROJECT_NAME="${OCP_PROJECT:-workshop-eda}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
SKIP_INFRA=false
SKIP_KAFKA=false
SKIP_POSTGRES=false
DRY_RUN=false
HELM_RELEASE_NAME="workshop-eda"

# 스크립트 디렉토리
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
HELM_CHART_DIR="${PROJECT_ROOT}/helm/workshop-eda"

################################################################################
# 함수 정의
################################################################################

# 로그 출력 함수
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 도움말 표시
show_help() {
    cat << EOF
OpenShift(OCP) 배포 스크립트

사용법:
  $0 [options]

옵션:
  --project <name>        프로젝트 이름 (기본값: workshop-eda)
  --skip-infra           인프라 서비스 배포 건너뛰기
  --skip-kafka           Kafka 배포 건너뛰기
  --skip-postgres        PostgreSQL 배포 건너뛰기
  --image-tag <tag>      이미지 태그 (기본값: latest)
  --dry-run              실제 배포 없이 테스트만 수행
  --help                 도움말 표시

환경 변수:
  OCP_PROJECT            프로젝트 이름
  IMAGE_TAG              이미지 태그
  KAFKA_BOOTSTRAP        Kafka 부트스트랩 서버
  DB_HOST                PostgreSQL 호스트
  DB_PASSWORD            PostgreSQL 비밀번호

예시:
  # 기본 배포
  $0

  # 특정 프로젝트에 배포
  $0 --project my-workshop

  # 인프라 없이 애플리케이션만 배포
  $0 --skip-infra

  # Dry-run 모드
  $0 --dry-run

EOF
}

# 명령어 존재 확인
check_command() {
    if ! command -v "$1" &> /dev/null; then
        log_error "$1 명령어를 찾을 수 없습니다. 설치해주세요."
        exit 1
    fi
}

# 필수 도구 확인
check_prerequisites() {
    log_info "필수 도구 확인 중..."
    
    check_command "oc"
    check_command "helm"
    
    # oc 로그인 확인
    if ! oc whoami &> /dev/null; then
        log_error "OpenShift에 로그인되어 있지 않습니다."
        log_info "다음 명령어로 로그인하세요: oc login --token=<token> --server=<server>"
        exit 1
    fi
    
    log_success "필수 도구 확인 완료"
    log_info "현재 사용자: $(oc whoami)"
    log_info "현재 서버: $(oc whoami --show-server)"
}

# 프로젝트 생성 또는 전환
setup_project() {
    log_info "프로젝트 설정 중: ${PROJECT_NAME}"
    
    if oc get project "${PROJECT_NAME}" &> /dev/null; then
        log_info "기존 프로젝트 사용: ${PROJECT_NAME}"
        oc project "${PROJECT_NAME}"
    else
        log_info "새 프로젝트 생성: ${PROJECT_NAME}"
        if [ "$DRY_RUN" = false ]; then
            oc new-project "${PROJECT_NAME}" \
                --display-name="Workshop EDA" \
                --description="Event-Driven Architecture Workshop"
        else
            log_info "[DRY-RUN] oc new-project ${PROJECT_NAME}"
        fi
    fi
    
    log_success "프로젝트 설정 완료"
}

# PostgreSQL 배포
deploy_postgresql() {
    if [ "$SKIP_POSTGRES" = true ]; then
        log_warning "PostgreSQL 배포 건너뛰기"
        return
    fi
    
    log_info "PostgreSQL 배포 중..."
    
    # PostgreSQL이 이미 배포되어 있는지 확인
    if oc get deployment postgresql &> /dev/null; then
        log_info "PostgreSQL이 이미 배포되어 있습니다."
        return
    fi
    
    if [ "$DRY_RUN" = false ]; then
        # PostgreSQL 배포
        oc new-app postgresql-persistent \
            -p POSTGRESQL_USER=postgres \
            -p POSTGRESQL_PASSWORD="${DB_PASSWORD:-postgres}" \
            -p POSTGRESQL_DATABASE=postgres \
            -p VOLUME_CAPACITY=5Gi \
            -p POSTGRESQL_VERSION=15-el8 || true
        
        # PostgreSQL이 준비될 때까지 대기
        log_info "PostgreSQL이 준비될 때까지 대기 중..."
        oc wait --for=condition=Available deployment/postgresql --timeout=300s || true
        
        # 데이터베이스 생성
        sleep 10
        log_info "데이터베이스 생성 중..."
        POD_NAME=$(oc get pods -l name=postgresql -o jsonpath='{.items[0].metadata.name}')
        
        oc exec "${POD_NAME}" -- psql -U postgres -c "CREATE DATABASE orderdb;" || true
        oc exec "${POD_NAME}" -- psql -U postgres -c "CREATE DATABASE inventorydb;" || true
        oc exec "${POD_NAME}" -- psql -U postgres -c "CREATE DATABASE fulfillmentdb;" || true
        oc exec "${POD_NAME}" -- psql -U postgres -c "CREATE DATABASE analyticsdb;" || true
        
        log_success "PostgreSQL 배포 완료"
    else
        log_info "[DRY-RUN] PostgreSQL 배포 시뮬레이션"
    fi
}

# Kafka 배포
deploy_kafka() {
    if [ "$SKIP_KAFKA" = true ]; then
        log_warning "Kafka 배포 건너뛰기"
        return
    fi
    
    log_info "Kafka 배포 중..."
    
    # Kafka가 이미 배포되어 있는지 확인
    if oc get kafka kafka-cluster &> /dev/null; then
        log_info "Kafka가 이미 배포되어 있습니다."
        return
    fi
    
    if [ "$DRY_RUN" = false ]; then
        # Kafka 클러스터 생성
        cat <<EOF | oc apply -f -
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: kafka-cluster
  namespace: ${PROJECT_NAME}
spec:
  kafka:
    version: 3.6.0
    replicas: 3
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
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
        
        # Kafka가 준비될 때까지 대기
        log_info "Kafka가 준비될 때까지 대기 중... (최대 5분)"
        oc wait kafka/kafka-cluster --for=condition=Ready --timeout=300s || true
        
        # Kafka 토픽 생성
        log_info "Kafka 토픽 생성 중..."
        cat <<EOF | oc apply -f -
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: order-events
  namespace: ${PROJECT_NAME}
  labels:
    strimzi.io/cluster: kafka-cluster
spec:
  partitions: 3
  replicas: 3
---
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: inventory-events
  namespace: ${PROJECT_NAME}
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
  namespace: ${PROJECT_NAME}
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
  namespace: ${PROJECT_NAME}
  labels:
    strimzi.io/cluster: kafka-cluster
spec:
  partitions: 3
  replicas: 3
EOF
        
        log_success "Kafka 배포 완료"
    else
        log_info "[DRY-RUN] Kafka 배포 시뮬레이션"
    fi
}

# 인프라 서비스 배포
deploy_infrastructure() {
    if [ "$SKIP_INFRA" = true ]; then
        log_warning "인프라 서비스 배포 건너뛰기"
        return
    fi
    
    log_info "인프라 서비스 배포 시작..."
    
    deploy_postgresql
    deploy_kafka
    
    log_success "인프라 서비스 배포 완료"
}

# 애플리케이션 배포
deploy_application() {
    log_info "애플리케이션 배포 중..."
    
    # Helm 차트 디렉토리 확인
    if [ ! -d "${HELM_CHART_DIR}" ]; then
        log_error "Helm 차트를 찾을 수 없습니다: ${HELM_CHART_DIR}"
        exit 1
    fi
    
    cd "${PROJECT_ROOT}"
    
    # Helm 차트 검증
    log_info "Helm 차트 검증 중..."
    helm lint "${HELM_CHART_DIR}"
    
    if [ "$DRY_RUN" = true ]; then
        log_info "[DRY-RUN] Helm 차트 배포 시뮬레이션"
        helm install "${HELM_RELEASE_NAME}" "${HELM_CHART_DIR}" \
            --namespace "${PROJECT_NAME}" \
            --set global.imageTag="${IMAGE_TAG}" \
            --dry-run --debug
    else
        # Helm 릴리스가 이미 존재하는지 확인
        if helm list -n "${PROJECT_NAME}" | grep -q "${HELM_RELEASE_NAME}"; then
            log_info "기존 Helm 릴리스 업그레이드 중..."
            helm upgrade "${HELM_RELEASE_NAME}" "${HELM_CHART_DIR}" \
                --namespace "${PROJECT_NAME}" \
                --set global.imageTag="${IMAGE_TAG}" \
                --wait --timeout=10m
        else
            log_info "새 Helm 릴리스 설치 중..."
            helm install "${HELM_RELEASE_NAME}" "${HELM_CHART_DIR}" \
                --namespace "${PROJECT_NAME}" \
                --set global.imageTag="${IMAGE_TAG}" \
                --wait --timeout=10m
        fi
        
        log_success "애플리케이션 배포 완료"
    fi
}

# Route 생성
create_routes() {
    log_info "Route 생성 중..."
    
    if [ "$DRY_RUN" = false ]; then
        # API Gateway Route 생성
        if ! oc get route api-gateway &> /dev/null; then
            oc expose svc/api-gateway
            log_success "API Gateway Route 생성 완료"
        else
            log_info "API Gateway Route가 이미 존재합니다."
        fi
        
        # Route URL 출력
        ROUTE_URL=$(oc get route api-gateway -o jsonpath='{.spec.host}')
        log_success "API Gateway URL: http://${ROUTE_URL}"
    else
        log_info "[DRY-RUN] Route 생성 시뮬레이션"
    fi
}

# 배포 상태 확인
check_deployment_status() {
    log_info "배포 상태 확인 중..."
    
    echo ""
    log_info "=== Pod 상태 ==="
    oc get pods -n "${PROJECT_NAME}"
    
    echo ""
    log_info "=== Service 상태 ==="
    oc get svc -n "${PROJECT_NAME}"
    
    echo ""
    log_info "=== Route 상태 ==="
    oc get route -n "${PROJECT_NAME}"
    
    echo ""
    log_info "=== Helm 릴리스 상태 ==="
    helm list -n "${PROJECT_NAME}"
}

# 배포 요약 출력
print_summary() {
    echo ""
    echo "========================================"
    log_success "배포 완료!"
    echo "========================================"
    echo ""
    log_info "프로젝트: ${PROJECT_NAME}"
    log_info "이미지 태그: ${IMAGE_TAG}"
    echo ""
    
    if [ "$DRY_RUN" = false ]; then
        # API Gateway URL
        if oc get route api-gateway &> /dev/null; then
            ROUTE_URL=$(oc get route api-gateway -o jsonpath='{.spec.host}')
            log_info "API Gateway URL: http://${ROUTE_URL}"
            echo ""
            log_info "테스트 명령어:"
            echo "  curl http://${ROUTE_URL}/actuator/health"
            echo "  curl -X POST http://${ROUTE_URL}/api/orders -H 'Content-Type: application/json' -d '{\"customerId\":\"customer-001\",\"productId\":\"product-001\",\"quantity\":2,\"price\":29.99}'"
        fi
        
        echo ""
        log_info "유용한 명령어:"
        echo "  oc get pods -n ${PROJECT_NAME}                    # Pod 상태 확인"
        echo "  oc logs -f deployment/order-service              # 로그 확인"
        echo "  oc port-forward svc/api-gateway 8080:8080       # 포트 포워딩"
        echo "  helm status ${HELM_RELEASE_NAME} -n ${PROJECT_NAME}  # Helm 상태 확인"
    else
        log_info "DRY-RUN 모드로 실행되었습니다. 실제 배포는 수행되지 않았습니다."
    fi
    
    echo ""
    log_info "자세한 내용은 docs/OCP_DEPLOYMENT_GUIDE.md를 참조하세요."
    echo ""
}

################################################################################
# 메인 실행
################################################################################

main() {
    # 명령줄 인자 파싱
    while [[ $# -gt 0 ]]; do
        case $1 in
            --project)
                PROJECT_NAME="$2"
                shift 2
                ;;
            --skip-infra)
                SKIP_INFRA=true
                shift
                ;;
            --skip-kafka)
                SKIP_KAFKA=true
                shift
                ;;
            --skip-postgres)
                SKIP_POSTGRES=true
                shift
                ;;
            --image-tag)
                IMAGE_TAG="$2"
                shift 2
                ;;
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            --help)
                show_help
                exit 0
                ;;
            *)
                log_error "알 수 없는 옵션: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # 배포 시작
    log_info "OpenShift 배포 시작..."
    echo ""
    
    # 1. 필수 도구 확인
    check_prerequisites
    echo ""
    
    # 2. 프로젝트 설정
    setup_project
    echo ""
    
    # 3. 인프라 서비스 배포
    deploy_infrastructure
    echo ""
    
    # 4. 애플리케이션 배포
    deploy_application
    echo ""
    
    # 5. Route 생성
    create_routes
    echo ""
    
    # 6. 배포 상태 확인
    if [ "$DRY_RUN" = false ]; then
        check_deployment_status
    fi
    
    # 7. 배포 요약 출력
    print_summary
}

# 스크립트 실행
main "$@"

# Made with Bob
