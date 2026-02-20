#!/bin/bash

echo "🔄 Restarting EDA Workshop"
echo "=========================="

# 색상 정의
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
NC='\033[0m'

echo -e "${YELLOW}🛑 Stopping services...${NC}"
./scripts/stop-all.sh

echo ""
echo -e "${YELLOW}🚀 Starting services...${NC}"
./scripts/start-all.sh

echo ""
echo -e "${GREEN}✅ Restart completed!${NC}"
