# IBM Bob - Instana Synthetic 테스트 자동 생성 데모 시나리오

**데모 시간**: 8-10분  
**목적**: IBM Bob 채팅 인터페이스를 통해 Instana Synthetic 모니터링 테스트를 자동으로 생성하는 과정 시연  
**대상**: 개발팀, DevOps 엔지니어, 관리자

---

## 🎬 데모 개요

**핵심**: IBM Bob 제품(채팅 인터페이스)과 대화하며 테스트를 생성하는 과정을 보여줍니다.

**화면 구성**:
- **메인 화면**: IBM Bob 채팅 인터페이스 (현재 이 화면)
- **보조 화면**: 생성된 파일 확인, Instana 콘솔, 웹 애플리케이션

**Bob이 생성한 결과물**:
- `client-scenario.side` (11 steps) - 고객 주문 시나리오
- `admin-scenario.side` (21 steps) - 관리자 대시보드 시나리오  
- `api-test.js` (283 lines) - API 엔드포인트 테스트

---

## 📋 시나리오 스크립트

### [00:00 - 00:30] 인트로 (30초)

**화면**: IBM Bob 채팅 인터페이스 전체 화면

**자막**:
```
IBM Bob - AI 기반 개발 어시스턴트
Instana Synthetic 모니터링 테스트 자동 생성 데모
```

**액션**:
- Bob 채팅 인터페이스 보여주기
- 깔끔한 대화 히스토리 (이전 대화 정리된 상태)
- 프로젝트 컨텍스트 표시 (좌측 또는 상단)

**설명 자막**:
```
"IBM Bob과 대화만으로 
Instana Synthetic 테스트를 자동 생성합니다"
```

---

### [00:30 - 02:00] Part 1: Bob에게 작업 요청 (1분 30초)

**화면**: Bob 채팅 인터페이스

**액션 1**: 채팅창에 요청 입력 (타이핑 효과)

```
사용자 입력:

LG U+ 주문 시스템에 대한 Instana Synthetic 모니터링 테스트를 만들어주세요.

시스템 정보:
- URL: http://web-workshop-eda.apps.itz-12fl8d.infra01-lb.syd05.techzone.ibm.com/
- 주문 시스템: 마이크로서비스 아키텍처
- 서비스: order, inventory, fulfillment, payment, analytics

필요한 테스트:
1. 고객 주문 시나리오 (Selenium IDE 형식)
2. 관리자 대시보드 시나리오 (Selenium IDE 형식)
3. API 엔드포인트 테스트 (JavaScript)

각 테스트는 실제 프로덕션에서 사용 가능한 수준으로 작성해주세요.
```

**액션 2**: Bob의 응답 표시

```
Bob 응답:

네, LG U+ 주문 시스템에 대한 Instana Synthetic 모니터링 테스트를 생성하겠습니다.

먼저 시스템을 분석하고 다음 3가지 테스트를 만들겠습니다:

1. 고객 주문 시나리오 (client-scenario.side)
   - 홈페이지 접속
   - 상품 선택
   - 주문 생성
   - 주문 완료 확인

2. 관리자 대시보드 시나리오 (admin-scenario.side)
   - 관리자 로그인
   - 주문 목록 조회
   - 재고 관리
   - 배송 관리
   - 분석 데이터 확인

3. API 테스트 (api-test.js)
   - 모든 마이크로서비스 헬스 체크
   - 주문 생성/조회 API
   - 재고 확인 API
   - 결제 처리 API
   - 배송 상태 API

잠시만 기다려주세요...
```

**자막**:
```
"Bob이 시스템을 분석하고 
테스트 시나리오를 설계합니다"
```

---

### [02:00 - 04:00] Part 2: Bob의 작업 과정 (2분)

**화면**: Bob 채팅 인터페이스 (작업 진행 상황 표시)

**액션**: Bob의 작업 진행 메시지 표시

```
Bob 작업 진행:

✓ 시스템 아키텍처 분석 완료
✓ 웹 애플리케이션 구조 파악
✓ API 엔드포인트 목록 확인
✓ 데이터 플로우 분석

📝 고객 주문 시나리오 생성 중...
   - 11개 테스트 스텝 생성
   - 각 단계별 검증 로직 추가
   - 대기 시간 최적화
✓ client-scenario.side 생성 완료

📝 관리자 대시보드 시나리오 생성 중...
   - 21개 테스트 스텝 생성
   - 복잡한 워크플로우 구현
   - 데이터 검증 로직 추가
✓ admin-scenario.side 생성 완료

📝 API 테스트 스크립트 생성 중...
   - 7개 마이크로서비스 테스트
   - 283줄 완전한 테스트 코드
   - 에러 핸들링 및 리포팅 포함
✓ api-test.js 생성 완료

모든 테스트 파일이 생성되었습니다!
```

**자막**:
```
"Bob이 3개의 완전한 테스트를 
자동으로 생성합니다"
```

---

### [04:00 - 05:30] Part 3: 생성된 파일 확인 (1분 30초)

**화면**: Bob 채팅 + 파일 탐색기 (화면 분할 또는 전환)

**액션 1**: Bob의 완료 메시지

```
Bob 응답:

✅ 테스트 생성 완료!

생성된 파일:
📁 instana-synthetic/
  ├── client-scenario.side (11 steps)
  ├── admin-scenario.side (21 steps)
  ├── api-test.js (283 lines)
  └── README.md

각 파일의 주요 내용:

1️⃣ client-scenario.side
   - 고객 주문 전체 플로우
   - 11개 단계로 구성
   - 각 단계마다 검증 포함

2️⃣ admin-scenario.side
   - 관리자 대시보드 전체 기능
   - 21개 단계로 구성
   - 주문/재고/배송/분석 모두 포함

3️⃣ api-test.js
   - 모든 API 엔드포인트 테스트
   - 283줄의 완전한 코드
   - "Made with Bob" 서명 포함

모든 파일은 Instana에 바로 업로드하여 사용할 수 있습니다.
```

**액션 2**: 파일 탐색기에서 생성된 파일 보여주기

```
instana-synthetic/
├── client-scenario.side      ← 클릭하여 내용 미리보기
├── admin-scenario.side        ← 클릭하여 내용 미리보기
├── api-test.js                ← 클릭하여 내용 미리보기
└── README.md
```

**액션 3**: 각 파일 간단히 열어서 내용 확인 (5-10초씩)

- `client-scenario.side`: JSON 구조, 11개 commands 확인
- `admin-scenario.side`: JSON 구조, 21개 commands 확인
- `api-test.js`: 코드 스크롤, "Made with Bob" 주석 확인

**자막**:
```
"프로덕션 레벨의 완전한 테스트 코드
즉시 사용 가능"
```

---

### [05:30 - 07:00] Part 4: Instana에서 테스트 실행 (1분 30초)

**화면**: Instana 웹 콘솔
- URL: `https://eum-lguplus.instana.io/#/home`

**액션 1**: Instana Synthetic Monitoring 섹션으로 이동

```
Navigation:
Instana 홈 → Synthetic Monitoring → Tests
```

**액션 2**: 업로드된 테스트 목록 보여주기

```
Synthetic Tests:

✓ LG U+ Order System - Client Scenario
  - Status: Active
  - Last Run: 2 minutes ago
  - Success Rate: 100%
  - Avg Response Time: 1.2s

✓ LG U+ Order System - Admin Dashboard
  - Status: Active
  - Last Run: 1 minute ago
  - Success Rate: 100%
  - Avg Response Time: 2.5s

✓ LG U+ Order System - API Tests
  - Status: Active
  - Last Run: 30 seconds ago
  - Success Rate: 100%
  - Avg Response Time: 450ms
```

**액션 3**: 하나의 테스트 클릭하여 상세 보기

- 테스트 실행 결과 그래프
- 각 스텝별 실행 시간
- 성공/실패 히스토리
- 응답 시간 트렌드

**자막**:
```
"Instana에서 실시간 모니터링
성능 저하 즉시 감지"
```

---

### [07:00 - 08:00] Part 5: 실제 웹사이트 확인 (1분)

**화면**: 실제 주문 시스템 웹사이트
- URL: `http://web-workshop-eda.apps.itz-12fl8d.infra01-lb.syd05.techzone.ibm.com/`

**액션 1**: 고객 화면 시연 (30초)

```
1. 로그인 (client/client)
2. 주문 생성
3. 주문 목록 확인
```

**액션 2**: 관리자 화면 시연 (30초)

```
1. 로그인 (admin/admin)
2. 주문 관리 탭
3. 재고 관리 탭
4. 배송 관리 탭
5. 분석 탭
```

**자막**:
```
"Bob이 테스트한 실제 시스템
모든 기능이 정상 작동"
```

---

### [08:00 - 08:30] 마무리 (30초)

**화면**: IBM Bob 채팅 인터페이스로 돌아오기

**액션**: Bob의 최종 메시지

```
Bob 응답:

✨ 작업 완료 요약

생성된 테스트:
✓ 고객 주문 시나리오 (11 steps)
✓ 관리자 대시보드 시나리오 (21 steps)
✓ API 테스트 (283 lines)

소요 시간:
- 전통적 방법: 12-16시간
- IBM Bob: 30분

결과:
✓ 프로덕션 레벨 품질
✓ Instana에서 즉시 사용 가능
✓ 실시간 모니터링 활성화

추가 작업이 필요하시면 언제든 말씀해주세요! 😊
```

**자막**:
```
IBM Bob
AI가 개발을 더 스마트하게

개발 시간 95% 단축
일관된 품질 보장
즉시 사용 가능한 결과물
```

---

## 🎯 핵심 메시지

### 1. 대화형 인터페이스
- 자연어로 요청
- Bob이 이해하고 실행
- 실시간 진행 상황 확인

### 2. 자동화의 힘
- 16시간 → 30분
- 수동 작업 제거
- 즉시 사용 가능

### 3. 품질 보장
- 프로덕션 레벨 코드
- 완전한 테스트 커버리지
- 베스트 프랙티스 적용

### 4. 실용성
- Instana 즉시 연동
- 실제 시스템에서 작동
- 추가 수정 불필요

---

## 📝 데모 준비 체크리스트

### IBM Bob 준비
- [ ] Bob 채팅 인터페이스 접속
- [ ] 이전 대화 히스토리 정리
- [ ] 프로젝트 컨텍스트 로드
- [ ] 네트워크 연결 확인

### 환경 준비
- [ ] Instana 계정 로그인
- [ ] 주문 시스템 URL 접속 확인
- [ ] 생성된 파일 위치 확인
- [ ] 브라우저 탭 준비

### 녹화 준비
- [ ] QuickTime 설정 확인
- [ ] 화면 해상도: 1920x1080
- [ ] 알림 끄기
- [ ] 불필요한 앱 종료

---

## 🔗 참조 링크

- **Instana**: https://eum-lguplus.instana.io/#/home
- **주문 시스템**: http://web-workshop-eda.apps.itz-12fl8d.infra01-lb.syd05.techzone.ibm.com/
- **생성된 파일**: /Users/hansol/Workspace/TXC/0903_LG/order/instana-synthetic/

---

## 📌 녹화 팁

### 화면 구성
1. **메인**: IBM Bob 채팅 인터페이스 (80% 시간)
2. **보조**: 파일 확인, Instana, 웹사이트 (20% 시간)

### 전환 타이밍
- Bob 응답 완료 후 3초 대기
- 화면 전환 시 부드럽게 (Cmd + Tab)
- 중요한 부분은 마우스로 가리키기

### 강조 포인트
- Bob의 작업 진행 메시지
- 생성된 파일 개수와 라인 수
- "Made with Bob" 서명
- Instana 테스트 성공률 100%

---

**문서 버전**: 2.0  
**작성일**: 2026-03-03  
**작성자**: IBM Bob Demo Team  
**업데이트**: IBM Bob 채팅 인터페이스 중심으로 전면 개편