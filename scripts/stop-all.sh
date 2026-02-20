#!/bin/bash
set -e

echo "🛑 Stopping EDA Workshop..."

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}🔄 Stopping all services...${NC}"
docker-compose down

echo -e "${YELLOW}🧹 Cleaning up...${NC}"
docker-compose down --volumes --remove-orphans

echo -e "${GREEN}✅ EDA Workshop stopped${NC}"
echo ""
echo -e "${YELLOW}💡 To remove all data (including databases):${NC}"
echo "  docker-compose down --volumes"
echo ""
echo -e "${YELLOW}💡 To remove all images:${NC}"
echo "  docker-compose down --rmi all"
