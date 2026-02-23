# Instana Synthetic Monitoring Scripts

이 디렉토리에는 Instana Synthetic Monitoring에 등록할 Selenium IDE 스크립트가 포함되어 있습니다.

## 파일 설명

- `client-scenario.side`: 고객 시나리오 (로그인 → 주문 조회)
- `admin-scenario.side`: 관리자 시나리오 (로그인 → 재고/배송/분석 관리)
- `api-test.js`: API 모듈 테스트 (Admin API 엔드포인트 검증)

## Instana에 등록하는 방법

### 1. Client Scenario 등록

1. Instana 콘솔 접속
2. Synthetic Monitoring 메뉴로 이동
3. "Add script" 클릭
4. "Upload file" 선택
5. `client-scenario.side` 파일 업로드
6. 테스트 설정:
   - Name: "Workshop EDA - Client Scenario"
   - Frequency: 5분
   - Locations: 원하는 위치 선택
7. 저장

### 2. Admin Scenario 등록

1. "Add script" 클릭
2. "Upload file" 선택
3. `admin-scenario.side` 파일 업로드
4. 테스트 설정:
   - Name: "Workshop EDA - Admin Scenario"
   - Frequency: 10분
   - Locations: 원하는 위치 선택
5. 저장

### 3. API Module Test 등록

1. "Add script" 클릭
2. "JavaScript" 선택
3. `api-test.js` 파일 내용 복사하여 붙여넣기
4. 테스트 설정:
   - Name: "Workshop EDA - API Module Tests"
   - Frequency: 5분
   - Locations: 원하는 위치 선택
   - Environment Variables:
     - `API_GATEWAY_URL`: API Gateway URL (예: `http://api-gateway-workshop-eda.apps.xxx.com`)
5. 저장

## 테스트 시나리오

### Client Scenario (11 steps)
1. 웹 UI 접속
2. 페이지 로드 대기 (3초)
3. 고객 아이디 입력 (client)
4. 비밀번호 입력 (client)
5. 로그인 버튼 클릭
6. 로그인 완료 대기 (3초)
7. 고객 화면 표시 확인
8. 주문 목록 컨테이너 확인
9. 새로고침 버튼 클릭
10. 새로고침 완료 대기 (2초)
11. 주문 목록 로드 확인

### Admin Scenario (21 steps)
1. 웹 UI 접속
2. 페이지 로드 대기 (3초)
3. 관리자 아이디 입력 (admin)
4. 비밀번호 입력 (admin)
5. 로그인 버튼 클릭
6. 로그인 완료 대기 (3초)
7. 관리자 화면 표시 확인
8. 재고 관리 탭 클릭
9. 재고 탭 로드 대기 (2초)
10. 재고 탭 표시 확인
11. 재고 목록 컨테이너 확인
12. 배송 관리 탭 클릭
13. 배송 탭 로드 대기 (2초)
14. 배송 탭 표시 확인
15. 배송 목록 컨테이너 확인
16. 분석 탭 클릭
17. 분석 탭 로드 대기 (2초)
18. 분석 탭 표시 확인
19. 총 이벤트 수 통계 확인
20. 분석 데이터 로드 버튼 클릭
21. 분석 데이터 로드 대기 (2초)

### API Module Tests (8 tests)
1. Order API - 전체 조회 (`GET /api/admin/orders`)
   - 응답 상태 코드 200 검증
   - 배열 형식 응답 검증
   - 응답 시간 5초 이내 검증
2. Order API - 단건 조회 (`GET /api/admin/orders/{orderId}`)
   - 첫 번째 주문 ID로 조회
   - 주문 상세 정보 검증
3. Inventory API - 전체 조회 (`GET /api/admin/inventory`)
   - 재고 목록 조회
   - 배열 형식 응답 검증
4. Inventory API - 단건 조회 (`GET /api/admin/inventory/{sku}`)
   - 첫 번째 SKU로 조회
   - 재고 상세 정보 검증
5. Fulfillment API - 전체 조회 (`GET /api/admin/fulfillments`)
   - 배송 목록 조회
   - 배열 형식 응답 검증
6. Fulfillment API - 단건 조회 (`GET /api/admin/fulfillments/{id}`)
   - 첫 번째 배송 ID로 조회
   - 배송 상세 정보 검증
7. Analytics API - 이벤트 카운트 (`GET /api/admin/analytics/events/count`)
   - 총 이벤트 수 조회
   - 숫자 형식 검증
8. Analytics API - 주문 요약 (`GET /api/admin/analytics/summary?date={today}`)
   - 오늘 날짜 기준 주문 요약 조회
   - 총 주문 수, 총 금액 검증

## 주의사항

### 필수 수정 사항
- **URL 변경**: 파일 내의 URL을 실제 OCP Route URL로 변경 필요
  - 현재: `http://web-workshop-eda.apps.itz-12fl8d.infra01-lb.syd05.techzone.ibm.com`
  - 변경: 실제 배포된 웹 서비스 URL

### 권장 설정
- Client 시나리오: 5분 주기 (가벼운 테스트)
- Admin 시나리오: 10분 주기 (더 많은 단계 포함)
- API Module Tests: 5분 주기 (빠른 API 검증)
- 실패 시 알림 설정 (Slack, Email 등)
- Timeout: 300초 (5분)

### API 테스트 환경 변수
API Module Tests를 실행하려면 다음 환경 변수가 필요합니다:
- `API_GATEWAY_URL`: API Gateway의 전체 URL (예: `http://api-gateway-workshop-eda.apps.xxx.com`)

### GitHub Secrets 설정
CI/CD 파이프라인에서 Instana 테스트를 실행하려면 다음 Secrets를 설정해야 합니다:
- `INSTANA_API_TOKEN`: Instana API 토큰
- `INSTANA_BASE_URL`: Instana 베이스 URL (예: `https://your-tenant.instana.io`)
- `INSTANA_CLIENT_TEST_ID`: Client Scenario 테스트 ID
- `INSTANA_ADMIN_TEST_ID`: Admin Scenario 테스트 ID
- `INSTANA_API_TEST_ID`: API Module Tests 테스트 ID
- `INSTANA_LOCATION_ID`: 테스트 실행 위치 ID

### 실제 HTML 구조 기반 Selector
스크립트는 실제 web/index.html 파일의 구조를 기반으로 작성되었습니다:
- 로그인 버튼: `xpath=//button[text()='로그인']`
- 고객 화면: `id=clientScreen`
- 주문 목록: `id=clientOrders`
- 새로고침 버튼: `css=.btn-refresh`
- 관리자 화면: `id=adminScreen`
- 탭 버튼: `xpath=//button[contains(@onclick, "showTab('...')")]`
- 재고 탭: `id=inventoryTab`
- 배송 탭: `id=fulfillmentTab`
- 분석 탭: `id=analyticsTab`

## 알림 설정 예시

- 연속 2회 실패 시 알림
- 응답 시간 10초 초과 시 경고
- 특정 요소 미발견 시 알림

## 트러블슈팅

### "element not interactable" 오류
- 페이지 로딩 시간이 부족할 수 있음 → pause 시간 증가 (3000ms → 5000ms)
- JavaScript가 완전히 로드되지 않았을 수 있음 → 추가 대기 시간 필요

### "element not found" 오류
- URL이 올바른지 확인
- 웹 애플리케이션이 정상적으로 실행 중인지 확인
- API Gateway가 정상적으로 응답하는지 확인

### 로그인 실패
- 기본 계정 정보:
  - 고객: client / client
  - 관리자: admin / admin
- 실제 환경에 맞게 수정 필요

## 파일 형식

- 파일은 compact JSON 형식으로 저장됨 (공백 제거)
- Selenium IDE에서 export한 표준 .side 형식과 호환
- Instana Synthetic Monitoring에서 직접 업로드 가능

## 성능 모니터링

Instana에서 다음 메트릭을 모니터링할 수 있습니다:

### UI 시나리오 (Client/Admin)
- 전체 시나리오 실행 시간
- 각 단계별 실행 시간
- 페이지 로드 시간
- 성공/실패율
- 가용성 (Availability)

### API Module Tests
- 각 API 엔드포인트별 응답 시간
- HTTP 상태 코드 분포
- API 가용성
- 에러율 및 실패 패턴
- 엔드포인트별 성능 추이

## API 테스트 특징

### 장점
- **빠른 실행**: UI 테스트보다 빠르게 실행 (평균 10-30초)
- **정확한 검증**: HTTP 상태 코드, 응답 데이터 구조 직접 검증
- **독립적 실행**: UI 렌더링 없이 백엔드 API만 테스트
- **상세한 로깅**: 각 API 호출의 상세 정보 출력

### 사용 사례
- 배포 후 즉시 API 가용성 확인
- 백엔드 서비스 헬스 체크
- API 성능 모니터링
- 데이터 무결성 검증

## CI/CD 통합

GitHub Actions 워크플로우에서 자동으로 실행됩니다:
1. 배포 완료 후 헬스 체크
2. Client Scenario 테스트 실행
3. Admin Scenario 테스트 실행
4. **API Module Tests 실행** (신규)
5. 모든 테스트 통과 시 배포 완료
6. 실패 시 자동 롤백