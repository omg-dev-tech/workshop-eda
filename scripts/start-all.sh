#!/bin/bash
set -e

echo "🚀 Starting EDA Workshop..."
echo "=================================="

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# .env 파일 확인
if [ ! -f .env ]; then
    echo -e "${YELLOW}⚠️  .env file not found, copying from .env.example${NC}"
    if [ -f .env.example ]; then
        cp .env.example .env
        echo -e "${GREEN}✅ .env file created${NC}"
    else
        echo -e "${RED}❌ .env.example not found. Please create .env file manually${NC}"
        exit 1
    fi
fi

# Docker 실행 확인
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running. Please start Docker first.${NC}"
    exit 1
fi

echo -e "${BLUE}📦 Starting infrastructure services...${NC}"

# 1단계: 데이터베이스들 먼저 시작
echo -e "${YELLOW}🗄️  Starting databases...${NC}"
docker-compose up -d \
    order-db \
    inventory-db \
    fulfillment-db \
    analytics-db \
    notification-db

# 2단계: Kafka 시작
echo -e "${YELLOW}📨 Starting Kafka...${NC}"
docker-compose up -d kafka

# 3단계: 데이터베이스와 Kafka 헬스체크 대기
echo -e "${YELLOW}⏳ Waiting for services to be healthy...${NC}"
timeout=300
elapsed=0
interval=5

while [ $elapsed -lt $timeout ]; do
    if docker-compose ps --format json | jq -r '.[] | select(.Service | IN("order-db", "inventory-db", "fulfillment-db", "analytics-db", "notification-db", "kafka")) | .Health' | grep -v "healthy" > /dev/null; then
        echo -e "${YELLOW}⏳ Services still starting... (${elapsed}s/${timeout}s)${NC}"
        sleep $interval
        elapsed=$((elapsed + interval))
    else
        echo -e "${GREEN}✅ All infrastructure services are healthy${NC}"
        break
    fi
done

if [ $elapsed -ge $timeout ]; then
    echo -e "${RED}❌ Timeout waiting for services to be healthy${NC}"
    echo -e "${YELLOW}📋 Current service status:${NC}"
    docker-compose ps
    exit 1
fi

# 4단계: 토픽 초기화
echo -e "${YELLOW}📝 Initializing Kafka topics...${NC}"
docker-compose up topic-init
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Kafka topics initialized${NC}"
else
    echo -e "${RED}❌ Failed to initialize topics${NC}"
    exit 1
fi

# 5단계: Kafka UI 시작
echo -e "${YELLOW}🖥️  Starting Kafka UI...${NC}"
docker-compose up -d kafka-ui

# 6단계: 애플리케이션 서비스들 시작
echo -e "${BLUE}🎯 Starting application services...${NC}"

# Payment Simulator 먼저
echo -e "${YELLOW}💳 Starting Payment Simulator...${NC}"
docker-compose up -d payment-simulator

# 나머지 서비스들
echo -e "${YELLOW}🔧 Starting microservices...${NC}"
docker-compose up -d \
    order-service \
    inventory-service \
    fulfillment-service \
    analytics-service \
    notification-service

# API Gateway 마지막
echo -e "${YELLOW}🌐 Starting API Gateway...${NC}"
docker-compose up -d api-gateway

# 7단계: 전체 서비스 상태 확인
echo -e "${YELLOW}⏳ Waiting for application services to start...${NC}"
sleep 30

echo ""
echo -e "${GREEN}🎉 EDA Workshop is ready!${NC}"
echo "=================================="
echo ""
echo -e "${BLUE}📊 Access Points:${NC}"
echo "  🌐 API Gateway:        http://localhost:8080"
echo "  📋 Order Service:      http://localhost:8081"
echo "  📦 Inventory Service:  http://localhost:8082"
echo "  🚚 Fulfillment Service: http://localhost:8083"
echo "  📊 Analytics Service:  http://localhost:8084"
echo "  📧 Notification Service: http://localhost:8085"
echo "  💳 Payment Simulator:  http://localhost:9090"
echo "  📨 Kafka UI:          http://localhost:8090"
echo ""
echo -e "${BLUE}🗄️  Database Connections:${NC}"
echo "  📋 Order DB:      localhost:5433"
echo "  📦 Inventory DB:  localhost:5434"
echo "  🚚 Fulfillment DB: localhost:5435"
echo "  📊 Analytics DB:  localhost:5436"
echo "  📧 Notification DB: localhost:5437"
echo ""
echo -e "${YELLOW}💡 Helpful Commands:${NC}"
echo "  📊 Check status:     docker-compose ps"
echo "  📝 View logs:        docker-compose logs -f [service-name]"
echo "  🛑 Stop all:         docker-compose down"
echo "  🔄 Restart service:  docker-compose restart [service-name]"
echo ""
echo -e "${GREEN}✨ Happy coding!${NC}"
