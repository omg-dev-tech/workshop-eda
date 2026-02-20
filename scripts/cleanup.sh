#!/bin/bash

echo "🧹 Cleaning up EDA Workshop"
echo "============================"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 확인
read -p "Are you sure you want to clean up all data? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cancelled."
    exit 1
fi

echo -e "${YELLOW}🛑 Stopping all services...${NC}"
docker-compose down

echo -e "${YELLOW}🗑️  Removing volumes...${NC}"
docker-compose down --volumes

echo -e "${YELLOW}🖼️  Removing images...${NC}"
docker-compose down --rmi local

echo -e "${YELLOW}🧹 Cleaning up dangling resources...${NC}"
docker system prune -f

echo -e "${GREEN}✅ Cleanup completed!${NC}"
echo ""
echo -e "${YELLOW}💡 To start fresh:${NC}"
echo "  ./scripts/start-all.sh"
