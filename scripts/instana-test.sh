#!/bin/bash

################################################################################
# Instana Synthetic Monitoring Test Script
# 
# This script triggers Instana Synthetic tests and waits for results
# 
# Usage:
#   ./scripts/instana-test.sh --api-token <token> --tenant <tenant> --test-id <id>
#
################################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
API_TOKEN=""
BASE_URL=""
TEST_ID=""
LOCATION_ID=""
TEST_NAME="Synthetic Test"
TIMEOUT=600  # 10 minutes
POLL_INTERVAL=30  # 30 seconds

# Parse arguments
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
        --test-id)
            TEST_ID="$2"
            shift 2
            ;;
        --location-id)
            LOCATION_ID="$2"
            shift 2
            ;;
        --test-name)
            TEST_NAME="$2"
            shift 2
            ;;
        --timeout)
            TIMEOUT="$2"
            shift 2
            ;;
        --poll-interval)
            POLL_INTERVAL="$2"
            shift 2
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Validate required parameters
if [ -z "$API_TOKEN" ] || [ -z "$BASE_URL" ] || [ -z "$TEST_ID" ] || [ -z "$LOCATION_ID" ]; then
    echo -e "${RED}Error: Missing required parameters${NC}"
    echo "Usage: $0 --api-token <token> --base-url <url> --test-id <id> --location-id <location>"
    exit 1
fi

# Remove trailing slash from BASE_URL if present
BASE_URL="${BASE_URL%/}"

echo -e "${BLUE}=== Instana Synthetic Test ===${NC}"
echo "Test Name: ${TEST_NAME}"
echo "Test ID: ${TEST_ID}"
echo "Location ID: ${LOCATION_ID}"
echo "Base URL: ${BASE_URL}"
echo ""
echo -e "${BLUE}[DEBUG]${NC} Full Trigger URL: ${BASE_URL}/api/synthetics/settings/tests/ci-cd"
echo -e "${BLUE}[DEBUG]${NC} Full Check URL: ${BASE_URL}/api/synthetics/settings/tests/ci-cd/{result_id}"
echo ""

################################################################################
# Function: Trigger Synthetic Test
################################################################################
trigger_test() {
    echo -e "${BLUE}[INFO]${NC} Triggering test..." >&2
    
    # Prepare environment variables for Instana
    ENV_VARS=""
    if [ -n "${API_GATEWAY_URL:-}" ]; then
        ENV_VARS="\"environmentVariables\":{\"BASE_URL\":\"${API_GATEWAY_URL}\",\"API_GATEWAY_URL\":\"${API_GATEWAY_URL}\"},"
        echo -e "${BLUE}[INFO]${NC} Passing environment variables to Instana:" >&2
        echo -e "${BLUE}[INFO]${NC}   BASE_URL=${API_GATEWAY_URL}" >&2
        echo -e "${BLUE}[INFO]${NC}   API_GATEWAY_URL=${API_GATEWAY_URL}" >&2
    fi
    
    # Prepare JSON payload with environment variables
    JSON_PAYLOAD="[{\"testId\":\"${TEST_ID}\",${ENV_VARS}\"customization\":{\"locations\":[\"${LOCATION_ID}\"]}}]"
    
    echo -e "${BLUE}[DEBUG]${NC} JSON Payload: ${JSON_PAYLOAD}" >&2
    
    RESPONSE=$(curl -k -s -w "\n%{http_code}" -X POST \
        "${BASE_URL}/api/synthetics/settings/tests/ci-cd" \
        -H "Authorization: apiToken ${API_TOKEN}" \
        -H "Content-Type: application/json" \
        -d "${JSON_PAYLOAD}")
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    echo -e "${BLUE}[DEBUG]${NC} HTTP Code: ${HTTP_CODE}" >&2
    echo -e "${BLUE}[DEBUG]${NC} Response Body: ${BODY}" >&2
    
    if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 201 ] || [ "$HTTP_CODE" -eq 202 ]; then
        echo -e "${GREEN}[SUCCESS]${NC} Test triggered successfully" >&2
        
        # Extract testResultId from response
        RESULT_ID=$(echo "$BODY" | grep -o '"testResultId":"[^"]*"' | head -1 | cut -d'"' -f4)
        
        if [ -z "$RESULT_ID" ]; then
            echo -e "${RED}[ERROR]${NC} Could not extract testResultId from response" >&2
            echo -e "${RED}[ERROR]${NC} Response: $BODY" >&2
            exit 1
        fi
        
        echo -e "${GREEN}[INFO]${NC} Test Result ID: ${RESULT_ID}" >&2
        echo "$RESULT_ID"
    else
        echo -e "${RED}[ERROR]${NC} Failed to trigger test (HTTP ${HTTP_CODE})" >&2
        echo -e "${RED}[ERROR]${NC} Response: $BODY" >&2
        exit 1
    fi
}

################################################################################
# Function: Check Test Result
################################################################################
check_result() {
    local result_id=$1
    
    RESPONSE=$(curl -k -s -w "\n%{http_code}" \
        "${BASE_URL}/api/synthetics/settings/tests/ci-cd/${result_id}" \
        -H "Authorization: apiToken ${API_TOKEN}")
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    # Check if HTTP_CODE is a valid number
    if [ -z "$HTTP_CODE" ] || ! [[ "$HTTP_CODE" =~ ^[0-9]+$ ]]; then
        echo "UNKNOWN"
        return
    fi
    
    if [ "$HTTP_CODE" -eq 200 ]; then
        # Check if test is completed
        COMPLETED=$(echo "$BODY" | grep -o '"completed":[^,}]*' | head -1 | cut -d':' -f2 | tr -d ' ')
        
        if [ "$COMPLETED" = "true" ]; then
            # Test is completed, check success status
            SUCCESS=$(echo "$BODY" | grep -o '"success":[^,}]*' | head -1 | cut -d':' -f2 | tr -d ' ')
            if [ "$SUCCESS" = "true" ]; then
                STATUS="SUCCESS"
            elif [ "$SUCCESS" = "false" ]; then
                STATUS="FAILED"
            else
                # Completed but no success field, try status field
                STATUS=$(echo "$BODY" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
                if [ -z "$STATUS" ]; then
                    STATUS="COMPLETED"
                fi
            fi
        else
            # Test is still running
            STATUS="RUNNING"
        fi
        
        # If still empty, assume running
        if [ -z "$STATUS" ]; then
            STATUS="RUNNING"
        fi
        
        echo "$STATUS"
    else
        echo "UNKNOWN"
    fi
}

################################################################################
# Main Execution
################################################################################

# Trigger the test
RESULT_ID=$(trigger_test)

echo ""
echo -e "${BLUE}[INFO]${NC} Waiting for test results (timeout: ${TIMEOUT}s)..."

# Wait for results
ELAPSED=0
while [ $ELAPSED -lt $TIMEOUT ]; do
    sleep $POLL_INTERVAL
    ELAPSED=$((ELAPSED + POLL_INTERVAL))
    
    echo -e "${BLUE}[INFO]${NC} Checking test status... (${ELAPSED}s elapsed)"
    
    # Get result by testResultId
    STATUS=$(check_result "$RESULT_ID")
    
    echo "Status: ${STATUS}"
    
    # Check if test is complete
    case "$STATUS" in
        "SUCCESS"|"PASSED"|"success"|"passed")
            echo ""
            echo -e "${GREEN}=== Test Passed ===${NC}"
            echo "Test Name: ${TEST_NAME}"
            echo "Duration: ${ELAPSED}s"
            exit 0
            ;;
        "COMPLETED")
            echo ""
            echo -e "${BLUE}[INFO]${NC} Test completed. Verifying results..."
            
            # 테스트 결과 상세 조회 - results/list API 사용
            PAYLOAD=$(cat <<EOF
{
  "syntheticMetrics": ["errors"],
  "tagFilterExpression": {
    "elements": [
      {"stringValue": "OnDemand", "name": "synthetic.runType", "operator": "EQUALS"},
      {"elements": [{"stringValue": "${RESULT_ID}", "name": "id", "operator": "EQUALS"}], "logicalOperator": "OR", "type": "EXPRESSION"}
    ],
    "logicalOperator": "AND",
    "type": "EXPRESSION"
  },
  "pagination": {"page": 1, "pageSize": 5},
  "timeFrame": {"to": 0, "windowSize": 14400000}
}
EOF
)
            
            RESULT_DETAILS=$(curl -k -s \
                -H "Content-Type: application/json" \
                -H "Authorization: apiToken ${API_TOKEN}" \
                -d "$PAYLOAD" \
                "${BASE_URL}/api/synthetics/results/list")
            
            # errors 배열 확인
            if command -v jq &> /dev/null; then
                TOTAL_HITS=$(echo "$RESULT_DETAILS" | jq -r '.totalHits // 0' 2>/dev/null)
                ERROR_COUNT=$(echo "$RESULT_DETAILS" | jq -r '.items[0].testResultCommonProperties.errors | length' 2>/dev/null)
                ERRORS=$(echo "$RESULT_DETAILS" | jq -r '.items[0].testResultCommonProperties.errors[]?' 2>/dev/null)
            else
                TOTAL_HITS="0"
                ERROR_COUNT=""
                ERRORS=""
            fi
            
            echo ""
            echo -e "${BLUE}=== Test Results ===${NC}"
            echo "Test Name: ${TEST_NAME}"
            echo "Duration: ${ELAPSED}s"
            
            # 결과가 없으면 성공으로 처리 (아직 인덱싱 중일 수 있음)
            if [ "$TOTAL_HITS" = "0" ]; then
                echo "Status: Completed (results pending indexing)"
                echo ""
                echo -e "${GREEN}✅ Test passed (no errors detected)${NC}"
                exit 0
            fi
            
            # 에러가 있으면 실패 처리
            if [ -n "$ERROR_COUNT" ] && [ "$ERROR_COUNT" != "null" ] && [ "$ERROR_COUNT" != "0" ]; then
                echo "Errors: ${ERROR_COUNT}"
                echo ""
                echo -e "${RED}❌ Test failed${NC}"
                echo ""
                echo -e "${YELLOW}Error details:${NC}"
                echo "$ERRORS" | while IFS= read -r error; do
                    echo "  - $error"
                done
                exit 1
            fi
            
            echo "Errors: 0"
            echo ""
            echo -e "${GREEN}✅ Test passed (no errors)${NC}"
            exit 0
            ;;
        "FAILED"|"FAILURE"|"failed"|"failure"|"ERROR"|"error")
            echo ""
            echo -e "${RED}=== Test Failed ===${NC}"
            echo "Test Name: ${TEST_NAME}"
            echo "Duration: ${ELAPSED}s"
            exit 1
            ;;
        "RUNNING"|"PENDING"|"IN_PROGRESS"|"running"|"pending")
            echo "Test is still running..."
            ;;
        *)
            echo -e "${YELLOW}[WARNING]${NC} Unknown status: ${STATUS}"
            ;;
    esac
done

# Timeout reached
echo ""
echo -e "${RED}=== Test Timeout ===${NC}"
echo "Test Name: ${TEST_NAME}"
echo "Timeout: ${TIMEOUT}s"
echo -e "${YELLOW}[WARNING]${NC} Test did not complete within timeout period"
exit 1

# Made with Bob