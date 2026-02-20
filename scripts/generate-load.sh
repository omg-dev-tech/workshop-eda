#!/bin/bash

echo "🔥 Generating Load for EDA Workshop"
echo "==================================="

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

API_GATEWAY_URL=${API_GATEWAY_URL:-http://localhost:8080}
LOAD_DURATION=${LOAD_DURATION:-60}
CONCURRENT_USERS=${CONCURRENT_USERS:-5}
ORDER_INTERVAL=${ORDER_INTERVAL:-2}

echo -e "${BLUE}📋 Load Test Configuration:${NC}"
echo "  🌐 API Gateway: $API_GATEWAY_URL"
echo "  ⏱️  Duration: ${LOAD_DURATION}s"
echo "  👥 Concurrent Users: $CONCURRENT_USERS"
echo "  📦 Order Interval: ${ORDER_INTERVAL}s"
echo ""

# 고객 데이터
customers=(
    "CUST-001:customer1@example.com:김철수"
    "CUST-002:customer2@example.com:이영희"
    "CUST-003:customer3@example.com:박민수"
    "CUST-004:customer4@example.com:최은정"
    "CUST-005:customer5@example.com:정한국"
)

# 상품 데이터
products=(
    "PROD-001:iPhone 15 Pro:1200000"
    "PROD-002:Samsung Galaxy S24:1100000"
    "PROD-003:MacBook Pro 14\":2500000"
    "PROD-004:iPad Air:800000"
    "PROD-005:AirPods Pro:300000"
    "PROD-006:Apple Watch Series 9:500000"
    "PROD-007:Sony WH-1000XM5:400000"
    "PROD-008:Nintendo Switch OLED:400000"
)

# 로드 생성 함수
generate_order() {
    local user_id=$1
    local order_count=$2
    
    # 랜덤 고객 선택
    customer_data=${customers[$RANDOM % ${#customers[@]}]}
    IFS=':' read -r customer_id email customer_name <<< "$customer_data"
    
    # 랜덤 상품들 선택 (1-3개)
    num_items=$((RANDOM % 3 + 1))
    items="["
    total_amount=0
    
    for ((i=0; i<num_items; i++)); do
        product_data=${products[$RANDOM % ${#products[@]}]}
        IFS=':' read -r product_id product_name unit_price <<< "$product_data"
        
        quantity=$((RANDOM % 3 + 1))
        item_total=$((unit_price * quantity))
        total_amount=$((total_amount + item_total))
        
        if [ $i -gt 0 ]; then
            items+=","
        fi
        
        items+="{\"productId\":\"$product_id\",\"productName\":\"$product_name\",\"quantity\":$quantity,\"unitPrice\":$unit_price,\"totalPrice\":$item_total}"
    done
    items+="]"
    
    # 주문 생성 요청
    order_payload=$(cat <<EOF
{
    "customerId": "$customer_id",
    "customerEmail": "$email",
    "customerName": "$customer_name",
    "items": $items,
    "totalAmount": $total_amount,
    "shippingAddress": "서울시 강남구 테헤란로 123, ${user_id}동 ${order_count}호",
    "paymentMethod": {
        "type": "CARD",
        "cardNumber": "1234-5678-9012-3456",
        "expiryMonth": "12",
        "expiryYear": "2025",
        "cvv": "123"
    }
}
EOF
    )
    
    echo -e "${YELLOW}[User-$user_id] Creating order #$order_count (Amount: ₩$(printf "%'d" $total_amount))${NC}"
    
    response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -H "X-User-ID: $customer_id" \
        -H "X-Correlation-ID: LOAD-$user_id-$order_count-$(date +%s)" \
        -d "$order_payload" \
        "$API_GATEWAY_URL/api/orders" \
        --max-time 30)
    
    if echo "$response" | jq -e '.orderId' > /dev/null 2>&1; then
        order_id=$(echo "$response" | jq -r '.orderId')
        echo -e "${GREEN}[User-$user_id] ✅ Order created: $order_id${NC}"
    else
        echo -e "❌ [User-$user_id] Failed to create order: $response"
    fi
}

# 사용자별 로드 생성
run_user_load() {
    local user_id=$1
    local start_time=$(date +%s)
    local end_time=$((start_time + LOAD_DURATION))
    local order_count=0
    
    echo -e "${BLUE}🚀 User-$user_id started${NC}"
    
    while [ $(date +%s) -lt $end_time ]; do
        order_count=$((order_count + 1))
        generate_order "$user_id" "$order_count"
        
        # 다음 주문까지 대기 (랜덤 지연)
        delay=$((RANDOM % ORDER_INTERVAL + 1))
        sleep $delay
    done
    
    echo -e "${GREEN}🏁 User-$user_id completed ($order_count orders)${NC}"
}

# API Gateway 상태 확인
echo -e "${YELLOW}🔍 Checking API Gateway...${NC}"
if ! curl -s -f "$API_GATEWAY_URL/actuator/health" > /dev/null; then
    echo -e "❌ API Gateway not accessible at $API_GATEWAY_URL"
    exit 1
fi
echo -e "${GREEN}✅ API Gateway is healthy${NC}"

# 로드 테스트 시작
echo ""
echo -e "${BLUE}🔥 Starting load test...${NC}"
start_time=$(date +%s)

# 백그라운드에서 여러 사용자 시뮬레이션
pids=()
for ((i=1; i<=CONCURRENT_USERS; i++)); do
    run_user_load $i &
    pids+=($!)
done

# 진행상황 모니터링
monitor_progress() {
    while [ ${#pids[@]} -gt 0 ]; do
        running_pids=()
        for pid in "${pids[@]}"; do
            if kill -0 "$pid" 2>/dev/null; then
                running_pids+=("$pid")
            fi
        done
        pids=("${running_pids[@]}")
        
        if [ ${#pids[@]} -gt 0 ]; then
            elapsed=$(($(date +%s) - start_time))
            echo -e "${YELLOW}⏳ Running... ${#pids[@]} users active, ${elapsed}s elapsed${NC}"
            sleep 5
        fi
    done
}

monitor_progress

# 결과 요약
end_time=$(date +%s)
duration=$((end_time - start_time))

echo ""
echo -e "${GREEN}🎉 Load test completed!${NC}"
echo "================================="
echo "  ⏱️  Total duration: ${duration}s"
echo "  👥 Concurrent users: $CONCURRENT_USERS"
echo ""
echo -e "${BLUE}📊 Check results:${NC}"
echo "  📝 Order Service logs:    docker-compose logs order-service"
echo "  📦 Inventory logs:        docker-compose logs inventory-service"
echo "  📧 Notification logs:     docker-compose logs notification-service"
echo "  📊 Analytics dashboard:   http://localhost:8084/dashboard"
echo "  📨 Kafka UI:             http://localhost:8090"
