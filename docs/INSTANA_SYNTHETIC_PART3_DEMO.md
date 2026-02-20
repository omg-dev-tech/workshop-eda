# Instana Synthetic Monitoring - Part 3: 데모 시나리오 & 트러블슈팅

이 문서는 [INSTANA_SYNTHETIC_MONITORING_GUIDE.md](../INSTANA_SYNTHETIC_MONITORING_GUIDE.md)의 연속입니다.

## 7. 데모 시나리오 스크립트

### 7.1 시나리오 1: 정기 점검 데모

**목표**: Synthetic Monitoring의 일상적인 모니터링 기능 시연

#### 준비 사항

```bash
# 1. 애플리케이션이 정상 동작 중인지 확인
kubectl get pods -n workshop-eda

# 2. Instana UI 접속
# - Synthetic Monitoring 메뉴로 이동
# - 설정된 테스트 목록 확인

# 3. 테스트 실행 확인
# - 마지막 실행 시간
# - 성공/실패 상태
# - 응답 시간
```

#### 데모 스크립트

**Step 1: 대시보드 소개 (2분)**

```
"Instana Synthetic Monitoring 대시보드입니다.
현재 5개의 테스트가 실행 중입니다:

1. API Happy Path - 매 5분마다 실행
2. API Out of Stock - 매 15분마다 실행
3. API Payment Failure - 매 15분마다 실행
4. Browser UI Test - 매 10분마다 실행
5. SSL Certificate - 매 6시간마다 실행

모든 테스트가 정상적으로 통과하고 있습니다."
```

**Step 2: 테스트 상세 보기 (3분)**

```
"API Happy Path 테스트를 자세히 살펴보겠습니다.

[테스트 클릭]

- 테스트 구성: 4개의 단계로 구성
  1. Health Check
  2. 주문 생성
  3. 주문 조회
  4. 완료 상태 확인

- 실행 결과:
  - 성공률: 100%
  - 평균 응답 시간: 245ms
  - P95 응답 시간: 387ms
  - P99 응답 시간: 512ms

- 지역별 결과:
  - Seoul: 245ms
  - Tokyo: 312ms
  
[응답 시간 그래프 표시]

지난 24시간 동안 응답 시간이 안정적으로 유지되고 있습니다."
```

**Step 3: 실패 시나리오 시뮬레이션 (5분)**

```bash
# 의도적으로 서비스 중단
kubectl scale deployment order-service --replicas=0 -n workshop-eda

# 1-2분 대기 (다음 테스트 실행 대기)

# Instana UI에서 확인
"테스트가 실패했습니다!
[실패 알림 표시]

실패 상세:
- 에러: Connection refused
- 실패 단계: Health Check
- 실패 시간: 2024-01-15 14:23:45

알림이 Slack으로 전송되었습니다.
[Slack 알림 확인]"

# 서비스 복구
kubectl scale deployment order-service --replicas=1 -n workshop-eda

# 복구 확인
"서비스가 복구되었고, 다음 테스트가 성공했습니다.
자동으로 복구 알림이 전송되었습니다."
```

---

### 7.2 시나리오 2: SSL 인증서 모니터링 데모

**목표**: SSL 인증서 만료 추적 및 알림 기능 시연

#### 데모 스크립트

**Step 1: SSL 테스트 결과 확인 (2분)**

```
"SSL Certificate 테스트 결과를 확인하겠습니다.

[SSL 테스트 클릭]

현재 인증서 정보:
- 발급자: Let's Encrypt
- 유효 기간: 2024-01-01 ~ 2024-04-01
- 남은 기간: 75일
- 상태: 정상

인증서 체인:
✓ Root CA: DST Root CA X3
✓ Intermediate: Let's Encrypt Authority X3
✓ End Entity: *.your-domain.com

TLS 설정:
✓ TLS 1.2 이상
✓ 강력한 암호화 스위트
✓ Forward Secrecy 활성화"
```

**Step 2: 만료 알림 설정 (3분)**

```
"알림 규칙을 설정했습니다:

- 30일 전: Warning (Email)
- 14일 전: Critical (Email + Slack)
- 7일 전: Emergency (Email + Slack + PagerDuty)

[알림 규칙 화면 표시]

실제 운영 환경에서는 인증서가 만료되기 전에
자동으로 알림을 받아 갱신할 수 있습니다."
```

**Step 3: 인증서 갱신 시뮬레이션 (선택사항)**

```bash
# cert-manager를 사용한 자동 갱신 (OCP 환경)
kubectl describe certificate api-gateway-cert -n workshop-eda

# 수동 갱신 트리거
kubectl delete secret api-gateway-tls -n workshop-eda

# cert-manager가 자동으로 재발급
# Synthetic Test가 새 인증서 검증
```

---

### 7.3 시나리오 3: CI/CD 통합 데모

**목표**: GitLab CI/CD 파이프라인과 Synthetic Monitoring 통합 시연

#### 준비 사항

```bash
# 1. 테스트용 브랜치 생성
git checkout -b demo/synthetic-test

# 2. 간단한 변경 사항 추가
echo "# Demo Change" >> README.md
git add README.md
git commit -m "Demo: Test CI/CD integration"

# 3. GitLab에 푸시
git push origin demo/synthetic-test
```

#### 데모 스크립트

**Step 1: 파이프라인 시작 (1분)**

```
"코드 변경을 커밋하고 GitLab에 푸시했습니다.

[GitLab CI 화면 표시]

파이프라인이 자동으로 시작되었습니다:

Stages:
1. ✓ Build (완료)
2. ✓ Test (완료)
3. → Deploy (진행 중)
4. ⏳ Synthetic Test (대기 중)
5. ⏳ Release Marker (대기 중)"
```

**Step 2: 배포 완료 및 Synthetic Test 실행 (3분)**

```
"배포가 완료되었습니다.
이제 Synthetic Test가 자동으로 실행됩니다.

[파이프라인 로그 표시]

Running Synthetic Test: Order Flow - Happy Path
Test Result ID: abc123def456

Polling for test result...
Attempt 1/60: Status = RUNNING
Attempt 2/60: Status = RUNNING
Attempt 3/60: Status = SUCCESS

✓ Synthetic test passed!

모든 테스트가 통과했으므로 다음 단계로 진행합니다."
```

**Step 3: Release Marker 등록 (2분)**

```
"Release Marker를 등록합니다.

[파이프라인 로그 표시]

Creating Release Marker...
  Name: abc123
  Environment: production
  Application: TXC Demo
  Git Commit: abc123def456789
  Git Branch: demo/synthetic-test

✓ Release Marker created successfully

[Instana UI로 전환]

Release Marker가 타임라인에 표시되었습니다.
배포 전후의 메트릭을 비교할 수 있습니다."
```

**Step 4: 배포 영향 분석 (3분)**

```
"배포 전후 메트릭을 비교해보겠습니다.

[Instana Release 분석 화면]

Before Deployment:
- 평균 응답 시간: 245ms
- P95 응답 시간: 387ms
- 에러율: 0.1%
- 처리량: 150 req/min

After Deployment:
- 평균 응답 시간: 248ms (+3ms)
- P95 응답 시간: 392ms (+5ms)
- 에러율: 0.1% (변화 없음)
- 처리량: 152 req/min (+2)

결론: 이번 배포는 성능에 미미한 영향만 주었으며,
모든 지표가 정상 범위 내에 있습니다."
```

---

### 7.4 시나리오 4: 실패 시나리오 및 자동 롤백

**목표**: 배포 실패 감지 및 자동 롤백 시연

#### 준비 사항

```bash
# 의도적으로 버그가 있는 코드 커밋
git checkout -b demo/failing-deployment

# 예: 잘못된 환경 변수 설정
# helm/workshop-eda/values.yaml 수정
```

#### 데모 스크립트

**Step 1: 문제가 있는 배포 (2분)**

```
"의도적으로 버그가 있는 코드를 배포하겠습니다.

[GitLab CI 화면]

Stages:
1. ✓ Build (완료)
2. ✓ Test (완료)
3. ✓ Deploy (완료)
4. → Synthetic Test (진행 중)

배포는 성공했지만, Synthetic Test가 실행 중입니다."
```

**Step 2: Synthetic Test 실패 감지 (3분)**

```
"Synthetic Test가 실패했습니다!

[파이프라인 로그]

Running Synthetic Test: Order Flow - Happy Path
Test Result ID: xyz789abc123

Polling for test result...
Attempt 1/60: Status = RUNNING
Attempt 2/60: Status = RUNNING
Attempt 3/60: Status = FAILED

✗ Synthetic test failed!

Failure Details:
- Step: Create Order
- Error: 500 Internal Server Error
- Message: Database connection failed

파이프라인이 자동으로 중단되었습니다."
```

**Step 3: 알림 및 롤백 (3분)**

```
"실패 알림이 전송되었습니다.

[Slack 알림 표시]

🚨 Deployment Failed
Version: xyz789
Environment: production
Failure: Synthetic Test Failed
Details: API returned 500 error

[GitLab CI - 롤백 단계]

Rollback triggered...
Rolling back to previous version: abc123

kubectl rollout undo deployment/order-service -n workshop-eda

Rollback completed successfully.

[Synthetic Test 재실행]

Running post-rollback verification...
✓ All tests passed

시스템이 정상 상태로 복구되었습니다."
```

---

## 8. 트러블슈팅 가이드

### 8.1 Synthetic Test 관련 문제

#### 문제 1: 테스트가 실행되지 않음

**증상**:
- 테스트가 스케줄대로 실행되지 않음
- "Last Run" 시간이 업데이트되지 않음

**원인 및 해결**:

```bash
# 1. Synthetic Test 설정 확인
curl -X GET \
  "${INSTANA_BASE_URL}/api/synthetic-monitoring/tests/${TEST_ID}" \
  -H "Authorization: apiToken ${INSTANA_API_TOKEN}" \
  | jq '.enabled'

# 2. 테스트가 비활성화된 경우 활성화
curl -X PATCH \
  "${INSTANA_BASE_URL}/api/synthetic-monitoring/tests/${TEST_ID}" \
  -H "Authorization: apiToken ${INSTANA_API_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"enabled": true}'

# 3. 스케줄 확인
curl -X GET \
  "${INSTANA_BASE_URL}/api/synthetic-monitoring/tests/${TEST_ID}" \
  -H "Authorization: apiToken ${INSTANA_API_TOKEN}" \
  | jq '.schedule'

# 4. 수동 실행으로 테스트
curl -X POST \
  "${INSTANA_BASE_URL}/api/synthetic-monitoring/tests/${TEST_ID}/execute" \
  -H "Authorization: apiToken ${INSTANA_API_TOKEN}"
```

---

#### 문제 2: 테스트가 항상 실패함

**증상**:
- 모든 테스트 실행이 실패
- 애플리케이션은 정상 동작 중

**원인 및 해결**:

```bash
# 1. 네트워크 연결 확인
curl -v "${OCP_API_URL}/actuator/health"

# 2. DNS 해석 확인
nslookup your-domain.com

# 3. 방화벽/보안 그룹 확인
# - Instana Synthetic Test 위치의 IP 허용
# - OCP Route/Ingress 설정 확인

# 4. 테스트 타임아웃 증가
# Instana UI에서 테스트 설정 → Timeout 값 증가

# 5. 테스트 스크립트 검증
# - URL이 올바른지 확인
# - 헤더가 올바른지 확인
# - 요청 본문이 올바른지 확인
```

---

#### 문제 3: 간헐적 실패

**증상**:
- 테스트가 가끔 실패함
- 재시도하면 성공함

**원인 및 해결**:

```bash
# 1. 응답 시간 확인
# - 타임아웃 설정이 너무 짧을 수 있음
# - 애플리케이션 성능 문제

# 2. 재시도 설정 추가
# Instana UI에서:
# - Retry Count: 2-3
# - Retry Interval: 2-5초

# 3. 애플리케이션 로그 확인
kubectl logs -f deployment/order-service -n workshop-eda

# 4. 리소스 부족 확인
kubectl top pods -n workshop-eda

# 5. HPA 설정 확인
kubectl get hpa -n workshop-eda
```

---

### 8.2 GitLab CI/CD 통합 문제

#### 문제 1: Synthetic Test API 호출 실패

**증상**:
```
curl: (7) Failed to connect to instana.example.com port 443: Connection refused
```

**원인 및 해결**:

```bash
# 1. Instana URL 확인
echo $INSTANA_BASE_URL
# 올바른 형식: https://instana.example.com (trailing slash 없음)

# 2. API Token 확인
echo $INSTANA_API_TOKEN | wc -c
# 길이가 0이면 환경 변수가 설정되지 않음

# 3. GitLab CI 변수 설정 확인
# GitLab UI → Settings → CI/CD → Variables
# - INSTANA_BASE_URL
# - INSTANA_API_TOKEN (Protected, Masked)

# 4. 네트워크 연결 테스트
curl -v https://instana.example.com/api/health
```

---

#### 문제 2: Test Result ID를 가져올 수 없음

**증상**:
```
Test Result ID: null
```

**원인 및 해결**:

```bash
# 1. API 응답 확인
RESPONSE=$(curl -s -X POST \
  "${INSTANA_BASE_URL}/api/synthetic-monitoring/tests/${TEST_ID}/execute" \
  -H "Authorization: apiToken ${INSTANA_API_TOKEN}" \
  -H "Content-Type: application/json")

echo "$RESPONSE" | jq '.'

# 2. 에러 메시지 확인
echo "$RESPONSE" | jq '.error'

# 3. Test ID 확인
# Instana UI에서 Test ID 복사
# 또는 API로 조회:
curl -X GET \
  "${INSTANA_BASE_URL}/api/synthetic-monitoring/tests" \
  -H "Authorization: apiToken ${INSTANA_API_TOKEN}" \
  | jq '.[] | {id, name}'

# 4. jq 설치 확인
which jq || apk add --no-cache jq
```

---

#### 문제 3: 파이프라인이 타임아웃됨

**증상**:
- Synthetic Test 결과를 기다리다가 파이프라인 타임아웃

**원인 및 해결**:

```yaml
# .gitlab-ci.yml 수정

synthetic-test:api-happy-path:
  timeout: 10m  # 타임아웃 증가
  script:
    - |
      # 폴링 횟수 조정
      MAX_ATTEMPTS=120  # 10분 (5초 간격)
      ATTEMPT=0
      
      while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
        STATUS=$(curl -s \
          "${INSTANA_BASE_URL}/api/synthetic-monitoring/results/${RESULT_ID}" \
          -H "Authorization: apiToken ${INSTANA_API_TOKEN}" \
          | jq -r '.status')
        
        if [ "${STATUS}" = "SUCCESS" ] || [ "${STATUS}" = "FAILED" ]; then
          break
        fi
        
        sleep 5
        ATTEMPT=$((ATTEMPT+1))
      done
```

---

### 8.3 Release Marker 문제

#### 문제 1: Release Marker가 생성되지 않음

**증상**:
- API 호출은 성공하지만 Instana UI에 표시되지 않음

**원인 및 해결**:

```bash
# 1. 응답 확인
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
  "${INSTANA_BASE_URL}/api/releases" \
  -H "Authorization: apiToken ${INSTANA_API_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-release",
    "start": '"$(date +%s%3N)"',
    "applications": [{"name": "TXC Demo"}]
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "HTTP Code: $HTTP_CODE"
echo "Body: $BODY"

# 2. 타임스탬프 형식 확인
# milliseconds 단위여야 함
date +%s%3N

# 3. Application 이름 확인
# Instana에 등록된 정확한 이름 사용

# 4. Release Marker 조회
curl -X GET \
  "${INSTANA_BASE_URL}/api/releases?windowSize=86400000" \
  -H "Authorization: apiToken ${INSTANA_API_TOKEN}" \
  | jq '.'
```

---

#### 문제 2: Release Marker가 타임라인에 표시되지 않음

**원인 및 해결**:

```bash
# 1. 시간 범위 확인
# Instana UI에서 시간 범위를 Release 시간을 포함하도록 조정

# 2. Application 필터 확인
# 올바른 Application이 선택되었는지 확인

# 3. Scope 태그 확인
# Release Marker의 scope.tag가 현재 필터와 일치하는지 확인

# 4. 브라우저 캐시 클리어
# Ctrl+Shift+R (하드 리프레시)
```

---

### 8.4 SSL 인증서 테스트 문제

#### 문제 1: 인증서 검증 실패

**증상**:
```
SSL certificate problem: unable to get local issuer certificate
```

**원인 및 해결**:

```bash
# 1. 인증서 체인 확인
openssl s_client -connect your-domain.com:443 -showcerts

# 2. Intermediate 인증서 누락
# cert-manager 설정 확인:
kubectl describe certificate api-gateway-cert -n workshop-eda

# 3. Let's Encrypt Intermediate 인증서 추가
# Certificate 리소스에 명시적으로 추가

# 4. 수동 검증
curl -v https://your-domain.com/actuator/health
```

---

#### 문제 2: 자체 서명 인증서 사용 시

**원인 및 해결**:

```javascript
// Synthetic Test 스크립트에서 자체 서명 인증서 허용
const https = require('https');

const agent = new https.Agent({
  rejectUnauthorized: false  // 개발 환경에서만 사용
});

// 또는 CA 인증서 제공
const agent = new https.Agent({
  ca: fs.readFileSync('ca-cert.pem')
});
```

---

### 8.5 성능 문제

#### 문제 1: 테스트 응답 시간이 느림

**원인 및 해결**:

```bash
# 1. 애플리케이션 성능 확인
kubectl top pods -n workshop-eda

# 2. 리소스 제한 확인
kubectl describe pod order-service-xxx -n workshop-eda | grep -A 5 "Limits"

# 3. HPA 설정
kubectl get hpa -n workshop-eda

# 4. 데이터베이스 성능 확인
# - 연결 풀 크기
# - 쿼리 성능
# - 인덱스 확인

# 5. 네트워크 레이턴시 확인
# 다른 지역에서 테스트 실행
```

---

#### 문제 2: 동시 테스트 실행 시 실패

**원인 및 해결**:

```bash
# 1. 동시 실행 제한 설정
# Instana UI에서 테스트 스케줄 조정
# - 테스트 간 간격 두기
# - 동일 시간대 실행 피하기

# 2. 애플리케이션 확장
kubectl scale deployment order-service --replicas=3 -n workshop-eda

# 3. Rate Limiting 확인
# API Gateway에서 rate limit 설정 확인

# 4. 데이터베이스 연결 풀 증가
# application.properties:
# spring.datasource.hikari.maximum-pool-size=20
```

---

### 8.6 일반적인 문제 해결 체크리스트

#### 테스트 실패 시

- [ ] 애플리케이션이 실행 중인가?
- [ ] 네트워크 연결이 가능한가?
- [ ] DNS가 올바르게 해석되는가?
- [ ] 방화벽/보안 그룹이 허용하는가?
- [ ] API 엔드포인트가 올바른가?
- [ ] 인증/인가가 필요한가?
- [ ] 타임아웃 설정이 적절한가?
- [ ] 테스트 데이터가 유효한가?

#### CI/CD 통합 실패 시

- [ ] 환경 변수가 설정되었는가?
- [ ] API Token이 유효한가?
- [ ] Test ID가 올바른가?
- [ ] jq가 설치되었는가?
- [ ] 네트워크 연결이 가능한가?
- [ ] 파이프라인 타임아웃이 충분한가?

#### Release Marker 문제 시

- [ ] 타임스탬프 형식이 올바른가? (milliseconds)
- [ ] Application 이름이 정확한가?
- [ ] API Token 권한이 충분한가?
- [ ] 시간 범위가 올바른가?
- [ ] 필터 설정이 올바른가?

---

## 9. 구현 작업 목록 및 일정

### 9.1 작업 분류

#### Phase 1: 기본 설정 (Week 1)

**우선순위: 필수**

- [ ] **Task 1.1**: Instana Synthetic Test 생성
  - API 테스트 3개 (Happy Path, Out of Stock, Payment Failure)
  - 예상 시간: 4시간
  - 담당자: DevOps Engineer

- [ ] **Task 1.2**: 브라우저 테스트 생성
  - UI 테스트 2개 (Order Creation, Error Handling)
  - 예상 시간: 6시간
  - 담당자: QA Engineer

- [ ] **Task 1.3**: SSL 인증서 테스트 설정
  - 인증서 모니터링 테스트
  - 예상 시간: 2시간
  - 담당자: Security Engineer

- [ ] **Task 1.4**: 테스트 스케줄 설정
  - 각 테스트의 실행 주기 설정
  - 예상 시간: 1시간
  - 담당자: DevOps Engineer

**총 예상 시간**: 13시간 (약 2일)

---

#### Phase 2: CI/CD 통합 (Week 2)

**우선순위: 필수**

- [ ] **Task 2.1**: GitLab CI 파이프라인 수정
  - Synthetic Test 단계 추가
  - 예상 시간: 4시간
  - 담당자: DevOps Engineer

- [ ] **Task 2.2**: Smoke Test 구현
  - 배포 직후 빠른 검증
  - 예상 시간: 3시간
  - 담당자: DevOps Engineer

- [ ] **Task 2.3**: Release Marker 자동 등록
  - 스크립트 작성 및 통합
  - 예상 시간: 3시간
  - 담당자: DevOps Engineer

- [ ] **Task 2.4**: 환경 변수 설정
  - GitLab CI 변수 설정
  - 예상 시간: 1시간
  - 담당자: DevOps Engineer

- [ ] **Task 2.5**: 파이프라인 테스트
  - 전체 플로우 검증
  - 예상 시간: 3시간
  - 담당자: DevOps Engineer

**총 예상 시간**: 14시간 (약 2일)

---

#### Phase 3: 알림 및 대시보드 (Week 3)

**우선순위: 권장**

- [ ] **Task 3.1**: Slack 통합
  - Webhook 설정 및 테스트
  - 예상 시간: 2시간
  - 담당자: DevOps Engineer

- [ ] **Task 3.2**: Email 알림 설정
  - 알림 템플릿 작성
  - 예상 시간: 2시간
  - 담당자: DevOps Engineer

- [ ] **Task 3.3**: 알림 규칙 설정
  - 테스트 실패, SSL 만료, SLO 위반
  - 예상 시간: 3시간
  - 담당자: DevOps Engineer

- [ ] **Task 3.4**: 대시보드 구성
  - Synthetic Test 결과 대시보드
  - SLO 추적 대시보드
  - Release 영향 분석 대시보드
  - 예상 시간: 6시간
  - 담당자: DevOps Engineer

**총 예상 시간**: 13시간 (약 2일)

---

#### Phase 4: 데모 준비 (Week 4)

**우선순위: 필수**

- [ ] **Task 4.1**: 데모 시나리오 작성
  - 4개 시나리오 스크립트
  - 예상 시간: 4시간
  - 담당자: Solution Architect

- [ ] **Task 4.2**: 데모 환경 준비
  - 테스트 데이터 준비
  - 예상 시간: 2시간
  - 담당자: DevOps Engineer

- [ ] **Task 4.3**: 데모 리허설
  - 전체 시나리오 실행
  - 예상 시간: 4시간
  - 담당자: 전체 팀

- [ ] **Task 4.4**: 문서화
  - 사용자 가이드 작성
  - 예상 시간: 4시간
  - 담당자: Technical Writer

**총 예상 시간**: 14시간 (약 2일)

---

### 9.2 전체 일정

```
Week 1: 기본 설정
├─ Day 1-2: Synthetic Test 생성
└─ Day 3: 테스트 검증

Week 2: CI/CD 통합
├─ Day 1-2: 파이프라인 수정
└─ Day 3: 통합 테스트

Week 3: 알림 및 대시보드
├─ Day 1: 알림 설정
└─ Day 2: 대시보드 구성

Week 4: 데모 준비
├─ Day 1: 시나리오 작성
└─ Day 2: 리허설 및 문서화
```

**총 예상 기간**: 4주 (20일)
**총 예상 공수**: 54시간 (약 7 man-days)

---

### 9.3 리스크 및 대응 방안

#### 리스크 1: Instana API 변경

**확률**: 낮음  
**영향**: 높음  
**대응**: API 버전 고정, 정기적인 업데이트 확인

#### 리스크 2: 네트워크 연결 문제

**확률**: 중간  
**영향**: 높음  
**대응**: 재시도 로직 구현, 타임아웃 조정

#### 리스크 3: 테스트 데이터 부족

**확률**: 중간  
**영향**: 중간  
**대응**: 테스트 데이터 자동 생성 스크립트

#### 리스크 4: 성능 저하

**확률**: 낮음  
**영향**: 중간  
**대응**: HPA 설정, 리소스 모니터링

---

### 9.4 성공 기준

#### 기술적 성공 기준

- [ ] 모든 Synthetic Test가 정상 실행됨
- [ ] CI/CD 파이프라인 통합 완료
- [ ] Release Marker 자동 등록 동작
- [ ] 알림이 정상적으로 전송됨
- [ ] 대시보드가 정확한 데이터 표시

#### 비즈니스 성공 기준

- [ ] 장애 감지 시간 < 5분
- [ ] 배포 검증 자동화 100%
- [ ] 인증서 만료 사전 알림
- [ ] 데모 성공적으로 완료

---

## 다음 단계

1. Phase 1 작업 시작
2. 주간 진행 상황 리뷰
3. 문제 발생 시 트러블슈팅 가이드 참조
4. 데모 1주일 전 리허설

---

## 참고 문서

- [Instana Synthetic Monitoring Guide (Main)](../INSTANA_SYNTHETIC_MONITORING_GUIDE.md)
- [Part 2: Release Marker & 알림 설정](INSTANA_SYNTHETIC_PART2_RELEASE_MARKER.md)
- [Instana API Documentation](https://www.ibm.com/docs/en/instana-observability/current?topic=apis-web-rest-api)
- [GitLab CI/CD Documentation](https://docs.gitlab.com/ee/ci/)