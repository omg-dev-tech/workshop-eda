# Instana Synthetic Monitoring - Part 2: Release Marker & 알림 설정

이 문서는 [INSTANA_SYNTHETIC_MONITORING_GUIDE.md](../INSTANA_SYNTHETIC_MONITORING_GUIDE.md)의 연속입니다.

## 5. Release Marker 구현 가이드

### 5.1 Release Marker란?

Release Marker는 Instana에서 배포 이벤트를 추적하고 시각화하는 기능입니다.

**주요 기능**:
- 배포 시점을 타임라인에 표시
- 배포 전후 성능/에러 변화 분석
- 배포와 장애의 상관관계 파악
- 롤백 지점 식별

**활용 사례**:
1. 배포 후 에러율 증가 → 배포가 원인임을 즉시 파악
2. 응답 시간 증가 → 특정 배포와 연관
3. 여러 배포 비교 → 어떤 변경이 성능에 영향을 주었는지 분석

---

### 5.2 Release Marker 정보 구조

```json
{
  "name": "v1.2.3-abc123",
  "start": 1234567890000,
  "applications": [
    {
      "name": "TXC Demo",
      "service": "order-service"
    }
  ],
  "scope": {
    "tag": {
      "key": "environment",
      "value": "production"
    }
  },
  "metadata": {
    "gitCommit": "abc123def456",
    "gitBranch": "main",
    "gitAuthor": "developer@example.com",
    "gitMessage": "Add new feature",
    "ciPipeline": "https://gitlab.com/project/pipelines/123",
    "deployedBy": "GitLab CI",
    "releaseNotes": "https://gitlab.com/project/-/releases/v1.2.3"
  }
}
```

**필수 필드**:
- `name`: Release 이름 (버전 또는 커밋 SHA)
- `start`: Release 시작 시간 (Unix timestamp, milliseconds)
- `applications`: 영향받는 애플리케이션 목록

**선택 필드**:
- `scope`: Release 범위 지정 (태그 기반)
- `metadata`: 추가 정보 (Git, CI/CD 등)

---

### 5.3 GitLab CI에서 Release Marker 등록

#### 5.3.1 기본 구현

```yaml
# .gitlab-ci.yml

release-marker:
  stage: release-marker
  image: curlimages/curl:latest
  script:
    - |
      echo "Creating Release Marker..."
      
      # 현재 시간 (milliseconds)
      START_TS=$(date +%s%3N)
      
      # Git 정보 수집
      GIT_COMMIT="${CI_COMMIT_SHA}"
      GIT_SHORT="${CI_COMMIT_SHORT_SHA}"
      GIT_BRANCH="${CI_COMMIT_REF_NAME}"
      GIT_AUTHOR="${CI_COMMIT_AUTHOR}"
      GIT_MESSAGE=$(echo "${CI_COMMIT_MESSAGE}" | head -n 1 | sed 's/"/\\"/g')
      
      # Release Marker 생성
      RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
        "${INSTANA_BASE_URL}/api/releases" \
        -H "Authorization: apiToken ${INSTANA_API_TOKEN}" \
        -H "Content-Type: application/json" \
        -d '{
          "name": "'"${GIT_SHORT}"'",
          "start": '"${START_TS}"',
          "applications": [
            {
              "name": "TXC Demo",
              "service": "order-service"
            }
          ],
          "scope": {
            "tag": {
              "key": "environment",
              "value": "production"
            }
          },
          "metadata": {
            "gitCommit": "'"${GIT_COMMIT}"'",
            "gitBranch": "'"${GIT_BRANCH}"'",
            "gitAuthor": "'"${GIT_AUTHOR}"'",
            "gitMessage": "'"${GIT_MESSAGE}"'",
            "ciPipeline": "'"${CI_PIPELINE_URL}"'",
            "deployedBy": "GitLab CI"
          }
        }')
      
      HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
      BODY=$(echo "$RESPONSE" | sed '$d')
      
      if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
        echo "✓ Release Marker created successfully"
        echo "$BODY" | jq '.'
      else
        echo "✗ Failed to create Release Marker: HTTP $HTTP_CODE"
        echo "$BODY"
        exit 1
      fi
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
  needs:
    - synthetic-test:api-happy-path
    - synthetic-test:browser-ui
  allow_failure: true  # Release Marker 실패해도 파이프라인은 성공
```

---

#### 5.3.2 고급 구현 (스크립트 분리)

**scripts/create-release-marker.sh**:

```bash
#!/bin/bash
set -e

# 환경 변수 확인
: "${INSTANA_BASE_URL:?Environment variable INSTANA_BASE_URL is required}"
: "${INSTANA_API_TOKEN:?Environment variable INSTANA_API_TOKEN is required}"

# 기본값 설정
RELEASE_NAME="${1:-${CI_COMMIT_SHORT_SHA:-unknown}}"
ENVIRONMENT="${2:-production}"
APPLICATION_NAME="${3:-TXC Demo}"

# 현재 시간 (milliseconds)
START_TS=$(date +%s%3N)

# Git 정보 수집
GIT_COMMIT="${CI_COMMIT_SHA:-$(git rev-parse HEAD)}"
GIT_BRANCH="${CI_COMMIT_REF_NAME:-$(git rev-parse --abbrev-ref HEAD)}"
GIT_AUTHOR="${CI_COMMIT_AUTHOR:-$(git log -1 --pretty=format:'%an <%ae>')}"
GIT_MESSAGE=$(echo "${CI_COMMIT_MESSAGE:-$(git log -1 --pretty=%B)}" | head -n 1 | sed 's/"/\\"/g')

echo "Creating Release Marker:"
echo "  Name: ${RELEASE_NAME}"
echo "  Environment: ${ENVIRONMENT}"
echo "  Application: ${APPLICATION_NAME}"
echo "  Git Commit: ${GIT_COMMIT}"
echo "  Git Branch: ${GIT_BRANCH}"

# Release Marker 생성
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
  "${INSTANA_BASE_URL}/api/releases" \
  -H "Authorization: apiToken ${INSTANA_API_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "'"${RELEASE_NAME}"'",
    "start": '"${START_TS}"',
    "applications": [
      {
        "name": "'"${APPLICATION_NAME}"'"
      }
    ],
    "scope": {
      "tag": {
        "key": "environment",
        "value": "'"${ENVIRONMENT}"'"
      }
    },
    "metadata": {
      "gitCommit": "'"${GIT_COMMIT}"'",
      "gitBranch": "'"${GIT_BRANCH}"'",
      "gitAuthor": "'"${GIT_AUTHOR}"'",
      "gitMessage": "'"${GIT_MESSAGE}"'",
      "ciPipeline": "'"${CI_PIPELINE_URL:-N/A}"'",
      "deployedBy": "GitLab CI"
    }
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
  echo "✓ Release Marker created successfully"
  echo "$BODY" | jq '.' || echo "$BODY"
  exit 0
else
  echo "✗ Failed to create Release Marker: HTTP $HTTP_CODE"
  echo "$BODY"
  exit 1
fi
```

**GitLab CI에서 사용**:

```yaml
release-marker:
  stage: release-marker
  image: curlimages/curl:latest
  before_script:
    - apk add --no-cache bash jq git
  script:
    - chmod +x scripts/create-release-marker.sh
    - ./scripts/create-release-marker.sh "${CI_COMMIT_SHORT_SHA}" "production" "TXC Demo"
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
  needs:
    - synthetic-test:api-happy-path
  allow_failure: true
```

---

### 5.4 Release Marker 조회 및 관리

#### 5.4.1 Release Marker 목록 조회

```bash
# 최근 Release Marker 조회
curl -X GET \
  "${INSTANA_BASE_URL}/api/releases?windowSize=86400000" \
  -H "Authorization: apiToken ${INSTANA_API_TOKEN}" \
  | jq '.'

# 특정 애플리케이션의 Release Marker 조회
curl -X GET \
  "${INSTANA_BASE_URL}/api/releases?applicationName=TXC%20Demo" \
  -H "Authorization: apiToken ${INSTANA_API_TOKEN}" \
  | jq '.'
```

#### 5.4.2 특정 Release Marker 조회

```bash
# Release ID로 조회
RELEASE_ID="abc123"
curl -X GET \
  "${INSTANA_BASE_URL}/api/releases/${RELEASE_ID}" \
  -H "Authorization: apiToken ${INSTANA_API_TOKEN}" \
  | jq '.'
```

#### 5.4.3 Release Marker 삭제

```bash
# Release Marker 삭제 (롤백 시)
RELEASE_ID="abc123"
curl -X DELETE \
  "${INSTANA_BASE_URL}/api/releases/${RELEASE_ID}" \
  -H "Authorization: apiToken ${INSTANA_API_TOKEN}"
```

---

### 5.5 Release Marker 활용 시나리오

#### 시나리오 1: 배포 후 성능 분석

1. Release Marker 등록
2. Instana UI에서 타임라인 확인
3. Release Marker 전후 메트릭 비교:
   - 응답 시간 변화
   - 에러율 변화
   - 처리량 변화
4. 문제 발견 시 롤백 결정

#### 시나리오 2: 여러 배포 비교

1. 여러 Release Marker 등록
2. 각 배포의 영향 분석
3. 가장 성능이 좋았던 버전 식별
4. 성능 저하를 일으킨 변경 사항 파악

#### 시나리오 3: 자동 롤백

```yaml
# GitLab CI - 자동 롤백 예제
rollback:
  stage: rollback
  image: curlimages/curl:latest
  script:
    - |
      echo "Checking deployment health..."
      
      # Synthetic Test 결과 확인
      # (실제로는 이전 단계의 결과를 확인)
      
      # 실패 시 롤백
      echo "Deployment failed, rolling back..."
      
      # 이전 버전으로 롤백
      kubectl rollout undo deployment/order-service -n ${K8S_NAMESPACE}
      
      # Release Marker 삭제
      curl -X DELETE \
        "${INSTANA_BASE_URL}/api/releases/${CI_COMMIT_SHORT_SHA}" \
        -H "Authorization: apiToken ${INSTANA_API_TOKEN}"
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
      when: on_failure
  needs:
    - synthetic-test:api-happy-path
```

---

## 6. 알림 및 대시보드 설정

### 6.1 알림 채널 설정

#### 6.1.1 Slack 통합

**Instana UI에서 설정**:

1. Settings → Team Settings → Alert Channels
2. Add Alert Channel → Slack
3. Webhook URL 입력: `https://hooks.slack.com/services/YOUR/WEBHOOK/URL`
4. Channel 이름 입력: `#instana-alerts`
5. Test Connection 클릭

**Slack Webhook 생성**:

1. Slack Workspace → Apps → Incoming Webhooks
2. Add to Slack
3. Channel 선택
4. Webhook URL 복사

**알림 메시지 커스터마이징**:

```json
{
  "channel": "#instana-alerts",
  "username": "Instana Synthetic",
  "icon_emoji": ":warning:",
  "attachments": [
    {
      "color": "danger",
      "title": "Synthetic Test Failed",
      "fields": [
        {
          "title": "Test Name",
          "value": "Order Flow - Happy Path",
          "short": true
        },
        {
          "title": "Location",
          "value": "Seoul",
          "short": true
        },
        {
          "title": "Failure Reason",
          "value": "API returned 500 Internal Server Error",
          "short": false
        }
      ],
      "footer": "Instana Synthetic Monitoring",
      "ts": 1234567890
    }
  ]
}
```

---

#### 6.1.2 Email 알림

**Instana UI에서 설정**:

1. Settings → Team Settings → Alert Channels
2. Add Alert Channel → Email
3. Email 주소 입력: `ops-team@example.com`
4. Subject Template 설정:
   ```
   [Instana] Synthetic Test Failed: {{testName}}
   ```
5. Body Template 설정:
   ```
   Test: {{testName}}
   Location: {{location}}
   Status: {{status}}
   Failure Reason: {{failureReason}}
   
   View Details: {{testUrl}}
   ```

---

#### 6.1.3 PagerDuty 통합 (선택사항)

**Instana UI에서 설정**:

1. Settings → Team Settings → Alert Channels
2. Add Alert Channel → PagerDuty
3. Integration Key 입력
4. Severity 매핑:
   - Critical → P1
   - Warning → P2
   - Info → P3

---

### 6.2 알림 규칙 설정

#### 6.2.1 Synthetic Test 실패 알림

**규칙 생성**:

1. Settings → Alerts → Create Alert
2. Alert Type: Synthetic Test
3. Condition:
   ```
   Test Status = Failed
   AND
   Consecutive Failures >= 2
   ```
4. Alert Channels: Slack, Email
5. Severity: Critical

**고급 조건**:

```json
{
  "condition": {
    "and": [
      {
        "field": "test.status",
        "operator": "equals",
        "value": "FAILED"
      },
      {
        "field": "test.consecutiveFailures",
        "operator": "greaterThanOrEqual",
        "value": 2
      },
      {
        "field": "test.location",
        "operator": "in",
        "value": ["seoul", "tokyo"]
      }
    ]
  },
  "actions": [
    {
      "type": "slack",
      "channel": "#instana-alerts"
    },
    {
      "type": "email",
      "recipients": ["ops-team@example.com"]
    }
  ]
}
```

---

#### 6.2.2 SSL 인증서 만료 알림

**규칙 생성**:

1. Settings → Alerts → Create Alert
2. Alert Type: SSL Certificate
3. Condition:
   ```
   Days Until Expiry <= 30
   ```
4. Alert Channels: Email
5. Severity: Warning

**다단계 알림**:

```yaml
# 30일 전: Warning
- condition: daysUntilExpiry <= 30
  severity: warning
  channels: [email]

# 14일 전: Critical
- condition: daysUntilExpiry <= 14
  severity: critical
  channels: [slack, email]

# 7일 전: Emergency
- condition: daysUntilExpiry <= 7
  severity: emergency
  channels: [slack, email, pagerduty]
```

---

#### 6.2.3 SLO 위반 알림

**SLO 정의**:

```yaml
slo:
  availability:
    target: 99.9%
    window: 30d
  
  responseTime:
    p95: 500ms
    p99: 1000ms
    window: 1h
  
  errorRate:
    target: 1%
    window: 1h
```

**알림 규칙**:

```json
{
  "name": "SLO Violation - Availability",
  "condition": {
    "field": "slo.availability.current",
    "operator": "lessThan",
    "value": 99.9
  },
  "severity": "critical",
  "channels": ["slack", "email"]
}
```

---

#### 6.2.4 배포 완료 알림

```yaml
# GitLab CI에서 Slack 알림
notify-deployment:
  stage: release-marker
  image: curlimages/curl:latest
  script:
    - |
      curl -X POST "${SLACK_WEBHOOK_URL}" \
        -H "Content-Type: application/json" \
        -d '{
          "text": "✅ Deployment Completed",
          "attachments": [
            {
              "color": "good",
              "fields": [
                {
                  "title": "Version",
                  "value": "'"${CI_COMMIT_SHORT_SHA}"'",
                  "short": true
                },
                {
                  "title": "Branch",
                  "value": "'"${CI_COMMIT_REF_NAME}"'",
                  "short": true
                },
                {
                  "title": "Author",
                  "value": "'"${CI_COMMIT_AUTHOR}"'",
                  "short": true
                },
                {
                  "title": "Pipeline",
                  "value": "<'"${CI_PIPELINE_URL}"'|View Pipeline>",
                  "short": true
                }
              ]
            }
          ]
        }'
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
  needs:
    - release-marker
```

---

### 6.3 대시보드 구성

#### 6.3.1 Synthetic Test 결과 대시보드

**위젯 구성**:

1. **Test Status Overview**
   - 모든 테스트의 현재 상태
   - 성공/실패 비율
   - 마지막 실행 시간

2. **Response Time Trends**
   - 시간별 응답 시간 그래프
   - P50, P95, P99 라인
   - 목표 응답 시간 기준선

3. **Success Rate**
   - 테스트별 성공률
   - 지역별 성공률
   - 시간대별 성공률

4. **Failure Analysis**
   - 실패 원인 분류
   - 실패 빈도
   - 영향받은 엔드포인트

5. **Geographic Distribution**
   - 지역별 테스트 결과
   - 지역별 응답 시간
   - 지도 시각화

---

#### 6.3.2 SLO 추적 대시보드

**위젯 구성**:

1. **SLO Compliance**
   - 현재 SLO 달성률
   - 목표 대비 현황
   - Error Budget 잔여량

2. **Availability Trend**
   - 30일 가용성 추이
   - 다운타임 기록
   - MTTR (Mean Time To Recovery)

3. **Performance Metrics**
   - P95/P99 응답 시간
   - 처리량 (Throughput)
   - 동시 사용자 수

4. **Error Rate**
   - 시간별 에러율
   - 에러 타입 분류
   - 영향받은 사용자 수

---

#### 6.3.3 Release 영향 분석 대시보드

**위젯 구성**:

1. **Release Timeline**
   - Release Marker 타임라인
   - 배포 빈도
   - 배포 성공/실패율

2. **Before/After Comparison**
   - 배포 전후 응답 시간 비교
   - 배포 전후 에러율 비교
   - 배포 전후 처리량 비교

3. **Release Impact Score**
   - 각 배포의 영향도 점수
   - 긍정적/부정적 영향 분류
   - 롤백 필요 여부

4. **Deployment Frequency**
   - 일별/주별 배포 횟수
   - 배포 시간대 분석
   - 배포 소요 시간

---

### 6.4 대시보드 예제 (JSON)

```json
{
  "dashboard": {
    "name": "Synthetic Monitoring Overview",
    "widgets": [
      {
        "type": "status",
        "title": "Test Status",
        "query": {
          "metric": "synthetic.test.status",
          "groupBy": ["test.name"]
        },
        "visualization": "table"
      },
      {
        "type": "timeseries",
        "title": "Response Time (P95)",
        "query": {
          "metric": "synthetic.test.responseTime",
          "aggregation": "p95",
          "groupBy": ["test.name"]
        },
        "visualization": "line",
        "threshold": {
          "warning": 500,
          "critical": 1000
        }
      },
      {
        "type": "gauge",
        "title": "Success Rate",
        "query": {
          "metric": "synthetic.test.successRate",
          "window": "1h"
        },
        "visualization": "gauge",
        "target": 99.9
      },
      {
        "type": "map",
        "title": "Geographic Distribution",
        "query": {
          "metric": "synthetic.test.status",
          "groupBy": ["location"]
        },
        "visualization": "map"
      }
    ]
  }
}
```

---

## 다음 문서

계속해서 다음 내용을 확인하세요:
- [Part 3: 데모 시나리오 & 트러블슈팅](INSTANA_SYNTHETIC_PART3_DEMO.md)