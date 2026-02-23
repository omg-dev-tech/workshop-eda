#!/bin/bash

##############################################################################
# Instana Release Marker Script
# 
# 이 스크립트는 Instana에 Release Marker를 생성합니다.
# 배포 성공 또는 롤백 시 호출되어 릴리스 이벤트를 기록합니다.
##############################################################################

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 기본값
API_TOKEN=""
BASE_URL=""
RELEASE_NAME=""
APPLICATIONS=""

# 사용법 출력
usage() {
    cat << EOF
Usage: $0 --api-token <token> --base-url <url> --release-name <name> [--applications <apps>]

Options:
    --api-token      Instana API Token (required)
    --base-url       Instana Base URL (required)
    --release-name   Release name (required)
    --applications   Comma-separated list of application names (optional)
                     Example: "workshop-eda" or "app1,app2,app3"

Examples:
    # Basic usage (no applications)
    $0 --api-token "abc123" --base-url "https://instana.example.com" --release-name "Deploy-abc123"
    
    # With single application
    $0 --api-token "abc123" --base-url "https://instana.example.com" --release-name "Deploy-abc123" --applications "workshop-eda"
    
    # With multiple applications
    $0 --api-token "abc123" --base-url "https://instana.example.com" --release-name "Deploy-abc123" --applications "app1,app2,app3"
EOF
    exit 1
}

# 파라미터 파싱
while [[ $# -gt 0 ]]; do
    case $1 in
        --api-token)
            API_TOKEN="$2"
            shift 2
            ;;
        --base-url)
            BASE_URL="$2"
            shift 2
            ;;
        --release-name)
            RELEASE_NAME="$2"
            shift 2
            ;;
        --applications)
            APPLICATIONS="$2"
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        *)
            echo -e "${RED}❌ Unknown option: $1${NC}"
            usage
            ;;
    esac
done

# 필수 파라미터 검증
if [ -z "$API_TOKEN" ] || [ -z "$BASE_URL" ] || [ -z "$RELEASE_NAME" ]; then
    echo -e "${RED}❌ Error: Missing required parameters${NC}"
    usage
fi

# URL 정리 (trailing slash 제거)
BASE_URL="${BASE_URL%/}"

echo -e "${YELLOW}=== Instana Release Marker ===${NC}"
echo "Base URL: $BASE_URL"
echo "Release Name: $RELEASE_NAME"
echo ""

# 현재 시간을 밀리초로 변환 (Unix timestamp * 1000)
TIMESTAMP=$(date +%s)000

echo -e "${YELLOW}Creating release marker...${NC}"
echo "Timestamp: $TIMESTAMP"

# applications 배열 JSON 생성
APPLICATIONS_JSON=""
if [ -n "$APPLICATIONS" ]; then
    echo "Applications: $APPLICATIONS"
    
    # 쉼표로 구분된 애플리케이션 이름을 배열로 변환
    IFS=',' read -ra APP_ARRAY <<< "$APPLICATIONS"
    
    # JSON 배열 생성
    APPLICATIONS_JSON="\"applications\": ["
    FIRST=true
    for app in "${APP_ARRAY[@]}"; do
        # 공백 제거
        app=$(echo "$app" | xargs)
        if [ "$FIRST" = true ]; then
            APPLICATIONS_JSON="${APPLICATIONS_JSON}{\"name\": \"${app}\"}"
            FIRST=false
        else
            APPLICATIONS_JSON="${APPLICATIONS_JSON}, {\"name\": \"${app}\"}"
        fi
    done
    APPLICATIONS_JSON="${APPLICATIONS_JSON}]"
fi
echo ""

# JSON 페이로드 생성
JSON_PAYLOAD="{
  \"name\": \"${RELEASE_NAME}\",
  \"start\": ${TIMESTAMP}"

if [ -n "$APPLICATIONS_JSON" ]; then
    JSON_PAYLOAD="${JSON_PAYLOAD},
  ${APPLICATIONS_JSON}"
fi

JSON_PAYLOAD="${JSON_PAYLOAD}
}"

echo "JSON Payload:"
echo "$JSON_PAYLOAD"
echo ""

# Release Marker 생성 API 호출
RESPONSE=$(curl -k -s -w "\n%{http_code}" --location --request POST "${BASE_URL}/api/releases" \
  --header "Authorization: apiToken ${API_TOKEN}" \
  --header "Content-Type: application/json" \
  --data "$JSON_PAYLOAD")

# HTTP 상태 코드 추출
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "HTTP Status Code: $HTTP_CODE"
echo "Response Body: $BODY"
echo ""

# 결과 확인
if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
    echo -e "${GREEN}✅ Release marker created successfully!${NC}"
    echo -e "${GREEN}   Release: $RELEASE_NAME${NC}"
    echo -e "${GREEN}   Time: $(date -u -d @${TIMESTAMP:0:-3} '+%Y-%m-%d %H:%M:%S UTC' 2>/dev/null || date -u -r ${TIMESTAMP:0:-3} '+%Y-%m-%d %H:%M:%S UTC' 2>/dev/null || echo 'N/A')${NC}"
    exit 0
else
    echo -e "${RED}❌ Failed to create release marker${NC}"
    echo -e "${RED}   HTTP Code: $HTTP_CODE${NC}"
    echo -e "${RED}   Response: $BODY${NC}"
    exit 1
fi

# Made with Bob
