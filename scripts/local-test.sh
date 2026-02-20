#!/bin/bash

# =================================================================
# 로컬 Docker Compose 환경 테스트 스크립트
# =================================================================

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수
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

# 환경 변수 로드
if [ -f .env ]; then
    log_info ".env 파일을 로드합니다..."
    export $(cat .env | grep -v '^#' | xargs)
else
    log_warning ".env 파일이 없습니다. 기본값을 사용합니다."
fi

# 기본 포트 설정
API_GATEWAY_PORT=${API_GATEWAY_PORT:-8080}
ORDER_SVC_PORT=${ORDER_SERVICE_PORT:-8081}
INV_SVC_PORT=${INVENTORY_SERVICE_PORT:-8082}
FUL_SVC_PORT=${FULFILLMENT_SERVICE_PORT:-8083}
ANALYTICS_SVC_PORT=${ANALYTICS_SERVICE_PORT:-8084}
PAY_ADAPTER_PORT=${PAY_SIMULATOR_PORT:-9090}

# 서비스 헬스체크 함수
check_health() {
    local service_name=$1
    local url=$2
    local max_attempts=${3:-30}
    local attempt=1

    log_info "${service_name} 헬스체크 시작 (${url})..."

    while [ $attempt -le $max_attempts ]; do
        if curl -sf "${url}" > /dev/null 2>&1; then
            log_success "${service_name} 정상 응답 (시도: ${attempt}/${max_attempts})"
            return 0
        fi
        
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done

    log_error "${service_name} 헬스체크 실패 (${max_attempts}회 시도)"
    return 1
}

# Docker Compose 상태 확인
check_docker_compose() {
    log_info "Docker Compose 상태 확인..."
    
    if ! docker compose version > /dev/null 2>&1; then
        log_error "Docker Compose가 설치되어 있지 않습니다."
        exit 1
    fi
    
    log_success "Docker Compose 사용 가능"
}

# 서비스 시작
start_services() {
    log_info "Docker Compose 서비스를 시작합니다..."
    docker compose up -d
    
    log_info "서비스 시작 대기 중..."
    sleep 10
}

# 서비스 상태 확인
check_services_status() {
    log_info "실행 중인 서비스 확인..."
    docker compose ps
}

# 인프라 서비스 헬스체크
check_infrastructure() {
    log_info "=== 인프라 서비스 헬스체크 ==="
    
    # Kafka 확인
    log_info "Kafka 상태 확인..."
    if docker compose exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
        log_success "Kafka 정상"
    else
        log_error "Kafka 비정상"
        return 1
    fi
    
    # 데이터베이스 확인
    log_info "데이터베이스 상태 확인..."
    for db in order-db inventory-db fulfillment-db analytics-db; do
        if docker compose exec -T ${db} pg_isready > /dev/null 2>&1; then
            log_success "${db} 정상"
        else
            log_error "${db} 비정상"
            return 1
        fi
    done
}

# 애플리케이션 서비스 헬스체크
check_application_services() {
    log_info "=== 애플리케이션 서비스 헬스체크 ==="
    
    local failed=0
    
    # API Gateway
    if ! check_health "API Gateway" "http://localhost:${API_GATEWAY_PORT}/actuator/health" 30; then
        failed=$((failed + 1))
    fi
    
    # Order Service
    if ! check_health "Order Service" "http://localhost:${ORDER_SVC_PORT}/actuator/health" 30; then
        failed=$((failed + 1))
    fi
    
    # Inventory Service
    if ! check_health "Inventory Service" "http://localhost:${INV_SVC_PORT}/actuator/health" 30; then
        failed=$((failed + 1))
    fi
    
    # Fulfillment Service
    if ! check_health "Fulfillment Service" "http://localhost:${FUL_SVC_PORT}/actuator/health" 30; then
        failed=$((failed + 1))
    fi
    
    # Analytics Service
    if ! check_health "Analytics Service" "http://localhost:${ANALYTICS_SVC_PORT}/actuator/health" 30; then
        failed=$((failed + 1))
    fi
    
    # Payment Adapter
    if ! check_health "Payment Adapter" "http://localhost:${PAY_ADAPTER_PORT}/actuator/health" 30; then
        failed=$((failed + 1))
    fi
    
    if [ $failed -gt 0 ]; then
        log_error "${failed}개 서비스 헬스체크 실패"
        return 1
    fi
    
    log_success "모든 애플리케이션 서비스 정상"
}

# 주문 생성 테스트
test_order_creation() {
    log_info "=== 주문 생성 API 테스트 ==="
    
    local order_payload='{
        "customerId": "test-customer-001",
        "amount": 50000,
        "currency": "KRW",
        "items": [
            {
                "sku": "product-001",
                "qty": 2
            }
        ]
    }'
    
    log_info "주문 생성 요청 전송..."
    local response=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -d "${order_payload}" \
        "http://localhost:${API_GATEWAY_PORT}/api/orders")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        log_success "주문 생성 성공 (HTTP ${http_code})"
        echo "응답: ${body}" | jq '.' 2>/dev/null || echo "${body}"
        
        # 주문 ID 추출
        local order_id=$(echo "${body}" | jq -r '.orderId' 2>/dev/null)
        if [ -n "$order_id" ] && [ "$order_id" != "null" ]; then
            log_info "생성된 주문 ID: ${order_id}"
            
            # 주문 조회 테스트
            log_info "주문 조회 테스트..."
            sleep 2
            local order_detail=$(curl -s "http://localhost:${API_GATEWAY_PORT}/api/orders/${order_id}")
            echo "주문 상세: ${order_detail}" | jq '.' 2>/dev/null || echo "${order_detail}"
        fi
        
        return 0
    else
        log_error "주문 생성 실패 (HTTP ${http_code})"
        echo "응답: ${body}"
        return 1
    fi
}

# 로그 확인
show_logs() {
    log_info "=== 최근 로그 확인 ==="
    docker compose logs --tail=50
}

# 정리 함수
cleanup() {
    log_info "=== 정리 작업 ==="
    read -p "서비스를 중지하시겠습니까? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        log_info "서비스 중지 중..."
        docker compose down
        log_success "서비스 중지 완료"
    fi
}

# 메인 실행
main() {
    log_info "=========================================="
    log_info "  로컬 Docker Compose 환경 테스트 시작"
    log_info "=========================================="
    echo
    
    # Docker Compose 확인
    check_docker_compose
    echo
    
    # 서비스 시작
    start_services
    echo
    
    # 서비스 상태 확인
    check_services_status
    echo
    
    # 인프라 헬스체크
    if ! check_infrastructure; then
        log_error "인프라 서비스 헬스체크 실패"
        show_logs
        exit 1
    fi
    echo
    
    # 애플리케이션 헬스체크
    if ! check_application_services; then
        log_error "애플리케이션 서비스 헬스체크 실패"
        show_logs
        exit 1
    fi
    echo
    
    # 주문 생성 테스트
    if ! test_order_creation; then
        log_warning "주문 생성 테스트 실패"
    fi
    echo
    
    log_success "=========================================="
    log_success "  모든 테스트 완료!"
    log_success "=========================================="
    echo
    
    log_info "서비스 접속 정보:"
    echo "  - API Gateway:        http://localhost:${API_GATEWAY_PORT}"
    echo "  - Order Service:      http://localhost:${ORDER_SVC_PORT}"
    echo "  - Inventory Service:  http://localhost:${INV_SVC_PORT}"
    echo "  - Fulfillment Service: http://localhost:${FUL_SVC_PORT}"
    echo "  - Analytics Service:  http://localhost:${ANALYTICS_SVC_PORT}"
    echo "  - Payment Adapter:    http://localhost:${PAY_ADAPTER_PORT}"
    echo
    
    log_info "로그 확인: docker compose logs -f [service-name]"
    log_info "서비스 중지: docker compose down"
    echo
}

# 스크립트 실행
main

# Ctrl+C 처리
trap cleanup EXIT

# Made with Bob
