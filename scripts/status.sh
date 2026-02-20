#!/bin/bash

echo "📊 EDA Workshop Status"
echo "====================="

# 색상 정의
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 서비스 상태 확인
echo -e "${BLUE}🐳 Docker Services:${NC}"
docker-compose ps --format "table {{.Service}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo -e "${BLUE}🏥 Health Checks:${NC}"

services=("order-db:5433" "inventory-db:5434" "fulfillment-db:5435" "analytics-db:5436" "notification-db:5437" "kafka:29092")

for service in "${services[@]}"; do
    IFS=':' read -r name port <<< "$service"
    if nc -z localhost "$port" 2>/dev/null; then
        echo -e "  ✅ ${name}: ${GREEN}Healthy${NC} (port $port)"
    else
        echo -e "  ❌ ${name}: ${RED}Unhealthy${NC} (port $port)"
    fi
done

echo ""
echo -e "${BLUE}📨 Kafka Topics:${NC}"
if nc -z localhost 29092 2>/dev/null; then
    docker-compose exec -T kafka kafka-topics.sh \
        --bootstrap-server localhost:9092 \
        --list | sort
else
    echo -e "  ${RED}❌ Kafka not accessible${NC}"
fi

echo ""
echo -e "${BLUE}🌐 API Endpoints:${NC}"

endpoints=(
    "api-gateway:8080:/actuator/health"
    "order-service:8081:/actuator/health"
    "inventory-service:8082:/actuator/health"
    "fulfillment-service:8083:/actuator/health"
    "analytics-service:8084:/actuator/health"
    "notification-service:8085:/actuator/health"
    "payment-simulator:9090:/actuator/health"
)

for endpoint in "${endpoints[@]}"; do
    IFS=':' read -r service port path <<< "$endpoint"
    if curl -s -f "http://localhost:${port}${path}" > /dev/null 2>&1; then
        echo -e "  ✅ ${service}: ${GREEN}Healthy${NC} (http://localhost:${port})"
    else
        echo -e "  ❌ ${service}: ${RED}Unhealthy${NC} (http://localhost:${port})"
    fi
done

echo ""
echo -e "${YELLOW}💡 Quick Actions:${NC}"
echo "  🔄 Restart all:       ./scripts/restart-all.sh"
echo "  📊 Generate load:     ./scripts/generate-loa