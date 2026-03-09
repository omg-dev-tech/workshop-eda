# Instana Synthetic Monitoring 데모 스크립트

## 개요
이 문서는 IBM Bob AI를 활용한 Instana Synthetic Monitoring 테스트 자동 생성 데모 영상 촬영을 위한 스크립트입니다.

---

## 데모 시나리오 (7단계)

### 1단계: Bob 화면 소개
**화면:** Bob AI 인터페이스 (초기 화면)

**내레이션:**
> "안녕하세요. 오늘은 IBM Bob AI를 활용하여 Instana Synthetic Monitoring 테스트를 자동으로 생성하는 과정을 보여드리겠습니다. Bob은 개발자의 자연어 요청을 이해하고, 복잡한 작업을 자동으로 수행할 수 있는 AI 어시스턴트입니다."

**보여줄 내용:**
- Bob AI 인터페이스
- 깨끗한 작업 공간

---

### 2단계: 프롬프트로 테스트 생성 요청
**화면:** Bob에 프롬프트 입력

**내레이션:**
> "이제 Bob에게 Instana Synthetic 테스트를 생성해달라고 요청하겠습니다. 3가지 테스트가 필요합니다: API 테스트 1개와 브라우저 시나리오 테스트 2개입니다."

**프롬프트 예시:**
```
Instana Synthetic Monitoring을 위한 테스트를 생성해주세요:
1. API 테스트 (api-test.js) - 주문 API 엔드포인트 테스트
2. 관리자 시나리오 (admin-scenario.side) - 관리자 대시보드 테스트
3. 고객 시나리오 (client-scenario.side) - 주문 생성 플로우 테스트
```

**보여줄 내용:**
- 프롬프트 입력 과정
- Bob이 요청을 이해하는 모습

---

### 3단계: Orchestrator의 계획 수립 및 위임
**화면:** Bob의 내부 작동 과정 (Orchestrator 모드)

**내레이션:**
> "Bob의 Orchestrator가 요청을 분석하고 작업 계획을 수립합니다. 파일 생성이 필요하므로 Code 모드에게 작업을 위임합니다."

**보여줄 내용:**
- Orchestrator의 계획 수립 메시지
- "Code 모드로 전환합니다" 메시지
- 작업 분석 및 TODO 리스트 생성

---

### 4단계: Code 모드의 파일 생성
**화면:** Code 모드가 파일을 생성하는 과정

**내레이션:**
> "Code 모드가 3개의 테스트 파일을 생성합니다. API 테스트를 위한 JavaScript 파일 1개와 Selenium IDE 형식의 브라우저 테스트 파일 2개가 생성됩니다."

**보여줄 내용:**
- `instana-synthetic/api-test.js` 생성
- `instana-synthetic/admin-scenario.side` 생성
- `instana-synthetic/client-scenario.side` 생성
- 각 파일의 내용 미리보기

**생성된 파일 하이라이트:**
- API 테스트: 주문 생성, 조회, 목록 API 검증
- 관리자 시나리오: 로그인, 대시보드 접근, 주문 관리
- 고객 시나리오: 상품 선택, 주문 생성, 결제 프로세스

---

### 5단계: Instana Synthetic Monitoring 화면 전환
**화면:** Instana 웹 콘솔

**내레이션:**
> "이제 생성된 테스트 파일을 Instana Synthetic Monitoring에 등록하겠습니다. Instana 콘솔에서 'Create Synthetic Test' 버튼을 클릭합니다."

**보여줄 내용:**
- Instana 대시보드
- Synthetic Monitoring 메뉴
- "Create Synthetic Test" 버튼 클릭

---

### 6단계: Configuration 진행 (실제 생성하지 않음)

#### 6-1. API Test 설정 (api-test.js)
**화면:** API Test 생성 화면

**내레이션:**
> "첫 번째로 API 테스트를 설정합니다. 생성된 api-test.js 파일을 업로드하고, 테스트를 실행할 로케이션을 선택합니다."

**보여줄 내용:**
- Test Type: API Simple 또는 API Script 선택
- 파일 업로드: `api-test.js`
- Location 선택: Seoul, Tokyo 등
- Scheduling 설정: 5분 간격
- Dashboard 연결: Order Service Dashboard
- Alert 설정 (선택사항)

**설정 항목:**
```
- Name: Order API Health Check
- Description: 주문 서비스 API 엔드포인트 모니터링
- Locations: Seoul (Primary), Tokyo (Secondary)
- Schedule: Every 5 minutes
- Retries: 2
- Timeout: 30 seconds
```

---

#### 6-2. Browser Test 설정 (admin-scenario.side)
**화면:** Browser Test 생성 화면

**내레이션:**
> "두 번째로 관리자 시나리오 브라우저 테스트를 설정합니다. Selenium IDE 파일을 업로드하고 실행 환경을 구성합니다."

**보여줄 내용:**
- Test Type: Browser Script 선택
- 파일 업로드: `admin-scenario.side`
- Browser 선택: Chrome
- Location 선택: Seoul
- Scheduling 설정: 10분 간격
- Dashboard 연결: Admin Dashboard
- Screenshot 옵션 활성화

**설정 항목:**
```
- Name: Admin Dashboard Scenario
- Description: 관리자 대시보드 접근 및 주문 관리 테스트
- Browser: Chrome (Latest)
- Locations: Seoul
- Schedule: Every 10 minutes
- Retries: 1
- Timeout: 60 seconds
- Take screenshots on failure: Yes
```

---

#### 6-3. Browser Test 설정 (client-scenario.side)
**화면:** Browser Test 생성 화면

**내레이션:**
> "마지막으로 고객 주문 시나리오 브라우저 테스트를 설정합니다. 실제 사용자의 주문 프로세스를 시뮬레이션합니다."

**보여줄 내용:**
- Test Type: Browser Script 선택
- 파일 업로드: `client-scenario.side`
- Browser 선택: Chrome
- Location 선택: Seoul, Tokyo
- Scheduling 설정: 15분 간격
- Dashboard 연결: Customer Experience Dashboard
- Performance metrics 수집 활성화

**설정 항목:**
```
- Name: Customer Order Flow
- Description: 고객 주문 생성 전체 플로우 테스트
- Browser: Chrome (Latest)
- Locations: Seoul (Primary), Tokyo (Secondary)
- Schedule: Every 15 minutes
- Retries: 2
- Timeout: 90 seconds
- Collect performance metrics: Yes
- Take screenshots on failure: Yes
```

---

### 7단계: 테스트 결과 확인
**화면:** Instana Synthetic Test Results

**내레이션:**
> "설정이 완료되면 Instana가 자동으로 테스트를 실행하고 결과를 수집합니다. 여기서 API 응답 시간, 성공률, 그리고 브라우저 테스트의 각 단계별 성능을 확인할 수 있습니다."

**보여줄 내용:**
- Test Results 대시보드
- 선택한 테스트 (예: API Test) 결과 상세 화면
- 성공/실패 상태
- Response Time 그래프
- Location별 성능 비교
- 최근 실행 이력
- Alert 발생 여부

**결과 화면 하이라이트:**
```
✓ API Test: 200 OK (Response Time: 145ms)
✓ Admin Scenario: All steps passed (Total Time: 8.2s)
✓ Customer Flow: Order created successfully (Total Time: 12.5s)

Performance Metrics:
- Availability: 99.8%
- Average Response Time: 156ms
- Success Rate: 98.5%
```

---

## 마무리

**내레이션:**
> "이렇게 IBM Bob AI를 활용하면 복잡한 Synthetic Monitoring 테스트를 자연어 명령만으로 빠르게 생성할 수 있습니다. 개발자는 테스트 작성에 시간을 쓰는 대신, 비즈니스 로직 개발에 집중할 수 있습니다. 감사합니다."

---

## 촬영 팁

### 화면 전환
- Bob AI → Instana Console 전환 시 부드러운 트랜지션 사용
- 각 단계는 3-5초 정도 충분히 보여주기
- 중요한 설정 항목은 하이라이트 또는 줌인

### 타이밍
- 전체 데모: 약 5-7분
- 각 단계별 시간 배분:
  - 1단계: 30초
  - 2단계: 45초
  - 3단계: 1분
  - 4단계: 1분 30초
  - 5단계: 30초
  - 6단계: 2분 30초 (각 테스트당 50초)
  - 7단계: 1분

### 주의사항
- 실제 생성은 하지 않고 설정 화면만 보여주기
- 민감한 정보(API 키, 비밀번호 등)는 모자이크 처리
- 화면 해상도: 1920x1080 권장
- 마우스 커서 움직임을 천천히, 명확하게

---

## 준비물 체크리스트

- [ ] Bob AI 환경 준비 및 테스트
- [ ] Instana 콘솔 접근 권한 확인
- [ ] 생성된 테스트 파일 3개 확인
- [ ] 데모용 대시보드 준비
- [ ] 화면 녹화 소프트웨어 설정
- [ ] 마이크 테스트
- [ ] 스크립트 리허설

---

## 추가 참고 자료

- [INSTANA_SYNTHETIC_MONITORING_GUIDE.md](../INSTANA_SYNTHETIC_MONITORING_GUIDE.md)
- [INSTANA_SYNTHETIC_DEMO_CONTEXT.md](./INSTANA_SYNTHETIC_DEMO_CONTEXT.md)
- [DEMO_RECORDING_GUIDE.md](./DEMO_RECORDING_GUIDE.md)