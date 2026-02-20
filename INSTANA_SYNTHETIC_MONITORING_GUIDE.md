
# Instana Synthetic Monitoring 통합 가이드

## 목차

1. [Instana Synthetic Monitoring 개요](#1-instana-synthetic-monitoring-개요)
2. [테스트 시나리오 상세 설계](#2-테스트-시나리오-상세-설계)
3. [Synthetic Test 스크립트 예제](#3-synthetic-test-스크립트-예제)
4. [GitLab CI/CD 통합 가이드](#4-gitlab-cicd-통합-가이드)
5. [Release Marker 구현 가이드](#5-release-marker-구현-가이드)
6. [알림 및 대시보드 설정](#6-알림-및-대시보드-설정)
7. [데모 시나리오 스크립트](#7-데모-시나리오-스크립트)
8. [트러블슈팅 가이드](#8-트러블슈팅-가이드)
9. [구현 작업 목록 및 일정](#9-구현-작업-목록-및-일정)

---

## 1. Instana Synthetic Monitoring 개요

### 1.1 Synthetic Monitoring이란?

Synthetic Monitoring은 실제 사용자 트래픽이 없어도 애플리케이션의 가용성과 성능을 지속적으로 검증하는 능동적 모니터링 방식입니다.

**주요 특징**:
- **능동적 모니터링**: 실제 사용자 트래픽 없이도 24/7 모니터링
- **조기 장애 감지**: 실제 사용자가 영향받기 전에 문제 발견
- **글로벌 테스트**: 다양한 지역에서 동시 테스트 가능
- **SLA 검증**: 서비스 수준 목표(SLO) 달성 여부 추적

### 1.2 주요 기능

#### 1.2.1 API 테스트
- HTTP/HTTPS 엔드포인트 모니터링
- REST API 플로우 검증
- 응답 시간 및 상태 코드 검증
- JSON/XML 응답 내용 검증

#### 1.2.2 브라우저 테스트
- 실제 브라우저 기반 테스트 (Selenium/Puppeteer)
- 사용자 시나리오 재현
- 페이지 로드 성능 측정
- JavaScript 에러 감지

#### 1.2.3 SSL 인증서 모니터링
- 인증서 유효성 검증
- 만료일 추적 및 알림
- 인증서 체인 검증
- TLS 버전 및 암호화 스위트 확인

### 1.3 데모에서 보여줄 핵심 가치

1. **정기 점검 자동화**
   - 매 5분마다 주문 API 및 웹 UI 자동 테스트
   - 장애 발생 시 즉시 알림

2. **CI/CD 통합**
   - 배포 전 자동 검증 (Staging)
   - 배포 후 Smoke Test (Production)
   - Release Marker를 통한 배포 영향 분석

3. **SSL 인증서 관리**
   - 인증서 만료 30일 전 자동 알림
   - 인증서 갱신 후 자동 검증

4. **성능 기준선 설정**
   - P95/P99 응답 시간 추적
   - 성능 저하 조기 감지
   - 배포 전후 성능 비교

---

## 2. 테스트 시나리오 상세 설계

### 2.1 API 테스트 시나리오

#### 시나리오 1: 정상 주문 플로우 (Happy Path)

**목표**: 주문 생성부터 완료까지의 전체 플로우 검증

**테스트 단계**:

```
Step 1: Health Check
├─ Endpoint: GET /actuator/health
├─ Expected: 200 OK
└─ Validation: Response contains "UP" status

Step 2: 주문 생성
├─ Endpoint: POST /api/orders
├─ Body: {
│   "orderId": "syn-test-{{timestamp}}",
│   "userId": "synthetic-user",
│   "items": [
│     {"sku": "SKU-100", "qty": 1, "price": 12000}
│   ],
│   "currency": "KRW"
│ }
├─ Expected: 201 Created
└─ Extract: orderId from response

Step 3: 주문 조회
├─ Endpoint: GET /api/orders/{{orderId}}
├─ Expected: 200 OK
└─ Validation: 
    ├─ orderId matches
    ├─ status is one of [CREATED, PENDING, COMPLETED]
    └─ items array is not empty

Step 4: 상태 확인 (Polling)
├─ Endpoint: GET /api/orders/{{orderId}}
├─ Retry: Max 10 times, 2s interval
├─ Expected: status = "COMPLETED"
└─ Timeout: 30 seconds
```

**성공 기준**:
- 전체 플로우 완료 시간: < 30초
- 각 API 응답 시간: < 500ms (P95)
- 모든 단계 성공률: 100%

**실패 시나리오**:
- Health Check 실패 → 즉시 알림
- 주문 생성 실패 → Critical 알림
- 30초 내 COMPLETED 미도달 → Warning 알림

---

#### 시나리오 2: 재고 부족 시나리오

**목표**: 재고 부족 시 적절한 에러 처리 검증

**테스트 단계**:

```
Step 1: 재고 없는 상품 주문
├─ Endpoint: POST /api/orders
├─ Body: {
│   "orderId": "syn-test-nostock-{{timestamp}}",
│   "userId": "synthetic-user",
│   "items": [
│     {"sku": "SKU-999", "qty": 999, "price": 1000}
│   ],
│   "currency": "KRW"
│ }
├─ Expected: 201 Created (주문은 생성됨)
└─ Extract: orderId

Step 2: 주문 상태 확인
├─ Endpoint: GET /api/orders/{{orderId}}
├─ Retry: Max 10 times, 2s interval
├─ Expected: status = "REJECTED" or "FAILED"
└─ Validation: 
    └─ Response contains error message about inventory
```

**성공 기준**:
- 재고 부족 감지: < 5초
- 적절한 에러 메시지 반환
- 시스템 안정성 유지 (다른 주문 영향 없음)

---

#### 시나리오 3: 결제 실패 시나리오

**목표**: 결제 실패 시 적절한 에러 처리 검증

**테스트 단계**:

```
Step 1: 결제 실패 강제 주문
├─ Endpoint: POST /api/orders
├─ Headers: X-Force-Payment: fail
├─ Body: {
│   "orderId": "syn-test-payment-fail-{{timestamp}}",
│   "userId": "synthetic-user",
│   "items": [
│     {"sku": "SKU-100", "qty": 1, "price": 12000}
│   ],
│   "currency": "KRW"
│ }
├─ Expected: 201 Created
└─ Extract: orderId

Step 2: 결제 실패 확인
├─ Endpoint: GET /api/orders/{{orderId}}
├─ Retry: Max 10 times, 2s interval
├─ Expected: status = "PAYMENT_FAILED"
└─ Validation: Error message contains "payment"
```

**성공 기준**:
- 결제 실패 감지: < 5초
- 적절한 에러 메시지 반환
- 재고 롤백 확인 (선택사항)

---

#### 시나리오 4: 성능 테스트

**목표**: 동시 다중 주문 처리 성능 검증

**테스트 단계**:

```
Step 1: 동시 주문 생성 (5개)
├─ Parallel Requests: 5
├─ Endpoint: POST /api/orders
└─ Body: Different orderIds

Step 2: 응답 시간 측정
├─ P50 (Median): < 300ms
├─ P95: < 500ms
├─ P99: < 1000ms
└─ Max: < 2000ms

Step 3: 처리량 확인
├─ Success Rate: > 99%
└─ Throughput: > 10 req/sec
```

**성공 기준**:
- P95 응답 시간: < 500ms
- 성공률: > 99%
- 동시 처리 가능

---

### 2.2 브라우저 테스트 시나리오

#### 시나리오 1: 주문 생성 UI 테스트

**목표**: 실제 사용자 경험 검증

**테스트 단계**:

```javascript
// Puppeteer/Selenium Script
Step 1: 페이지 로드
├─ Navigate to: https://{{domain}}/
├─ Wait for: Page load complete
└─ Measure: Page Load Time

Step 2: API Base URL 설정
├─ Find element: #baseUrl
├─ Clear and type: https://{{api-gateway-url}}/api
└─ Click: #applyBaseUrl

Step 3: Health Check
├─ Click: #pingBtn
├─ Wait for: Response in #output
└─ Validate: Contains "ok" or "UP"

Step 4: 주문 폼 입력
├─ Find element: #orderBody
├─ Clear existing content
└─ Type: {
    "orderId": "ui-test-{{timestamp}}",
    "userId": "ui-user",
    "items": [{"sku": "SKU-100", "qty": 1, "price": 12000}],
    "currency": "KRW"
  }

Step 5: 주문 전송
├─ Click: #sendBtn
├─ Wait for: Response in #output
└─ Validate: 
    ├─ Contains "ok" or "201"
    └─ Response has orderId

Step 6: 스크린샷 캡처
└─ Capture: Full page screenshot
```

**성능 메트릭**:
- Page Load Time: < 2초
- First Contentful Paint (FCP): < 1초
- Time to Interactive (TTI): < 3초
- Largest Contentful Paint (LCP): < 2.5초

**성공 기준**:
- 모든 단계 완료
- JavaScript 에러 없음
- 응답 메시지 정상 표시

---

#### 시나리오 2: 에러 처리 UI 테스트

**목표**: UI 에러 처리 및 EUM 연동 검증

**테스트 단계**:

```javascript
Step 1: 페이지 로드
└─ Navigate to: https://{{domain}}/

Step 2: Unhandled Error 테스트
├─ Click: #throwBtn
├─ Wait: 1 second
└─ Validate: 
    ├─ Console has error
    └─ EUM captured error (check Instana)

Step 3: Promise Rejection 테스트
├─ Click: #promiseBtn
├─ Wait: 1 second
└─ Validate: 
    ├─ Console has unhandledrejection
    └─ EUM captured error

Step 4: Handled Error 테스트
├─ Click: #handledBtn
├─ Wait: 1 second
└─ Validate: 
    └─ Output shows "handled" message
```

**성공 기준**:
- 에러 발생 확인
- Instana EUM에 에러 기록됨
- 페이지 크래시 없음

---

#### 시나리오 3: 성능 메트릭 수집

**목표**: Core Web Vitals 측정

**측정 항목**:

```
1. Page Load Time (PLT)
   └─ Target: < 2초

2. First Contentful Paint (FCP)
   └─ Target: < 1초

3. Largest Contentful Paint (LCP)
   └─ Target: < 2.5초

4. Time to Interactive (TTI)
   └─ Target: < 3초

5. Cumulative Layout Shift (CLS)
   └─ Target: < 0.1

6. First Input Delay (FID)
   └─ Target: < 100ms
```

**수집 방법**:
- Puppeteer Performance API 사용
- Lighthouse 메트릭 수집
- Instana EUM 자동 수집

---

### 2.3 SSL 인증서 테스트

#### 테스트 항목

**1. 인증서 유효성 검증**
```
Check 1: Certificate is valid
├─ Not expired
├─ Not yet valid (future date)
└─ Properly signed

Check 2: Certificate chain
├─ Root CA is trusted
├─ Intermediate certificates present
└─ Chain is complete

Check 3: Domain validation
├─ Certificate CN/SAN matches domain
└─ Wildcard certificate validation
```

**2. 만료일 확인**
```
Alert Thresholds:
├─ 30 days before expiry: Warning
├─ 14 days before expiry: Critical
└─ 7 days before expiry: Emergency
```

**3. TLS 설정 검증**
```
Check 1: TLS Version
├─ TLS 1.2 or higher
└─ No SSL 3.0, TLS 1.0, TLS 1.1

Check 2: Cipher Suites
├─ Strong ciphers only
├─ No weak ciphers (RC4, DES, etc.)
└─ Forward secrecy enabled

Check 3: Certificate Transparency
└─ SCT (Signed Certificate Timestamp) present
```

**4. OCSP/CRL 확인**
```
Check 1: OCSP Stapling
└─ Enabled and working

Check 2: Revocation Status
└─ Certificate not revoked
```

---

## 3. Synthetic Test 스크립트 예제

### 3.1 API 테스트 스크립트 (Instana HTTP Test)

#### 3.1.1 정상 주문 플로우 테스트

```javascript
// Instana Synthetic Test - API Test
// Test Name: Order Flow - Happy Path
// Schedule: Every 5 minutes
// Locations: Multiple (Seoul, Tokyo, Singapore)

const testConfig = {
  name: "Order Flow - Happy Path",
  description: "주문 생성부터 완료까지 전체 플로우 검증",
  schedule: "*/5 * * * *", // Every 5 minutes
  locations: ["seoul", "tokyo"],
  timeout: 30000, // 30 seconds
  retries: 2
};

// Step 1: Health Check
const healthCheck = {
  method: "GET",
  url: "${BASE_URL}/actuator/health",
  headers: {
    "Accept": "application/json"
  },
  assertions: [
    {
      type: "statusCode",
      operator: "equals",
      value: 200
    },
    {
      type: "responseTime",
      operator: "lessThan",
      value: 500
    },
    {
      type: "jsonPath",
      path: "$.status",
      operator: "equals",
      value: "UP"
    }
  ]
};

// Step 2: Create Order
const createOrder = {
  method: "POST",
  url: "${BASE_URL}/api/orders",
  headers: {
    "Content-Type": "application/json",
    "Accept": "application/json"
  },
  body: {
    orderId: "syn-test-${timestamp}",
    userId: "synthetic-user",
    items: [
      {
        sku: "SKU-100",
        qty: 1,
        price: 12000
      }
    ],
    currency: "KRW",
    note: "Synthetic monitoring test"
  },
  assertions: [
    {
      type: "statusCode",
      operator: "equals",
      value: 201
    },
    {
      type: "responseTime",
      operator: "lessThan",
      value: 1000
    },
    {
      type: "jsonPath",
      path: "$.orderId",
      operator: "exists"
    }
  ],
  extract: [
    {
      name: "orderId",
      jsonPath: "$.orderId"
    }
  ]
};

// Step 3: Get Order
const getOrder = {
  method: "GET",
  url: "${BASE_URL}/api/orders/${orderId}",
  headers: {
    "Accept": "application/json"
  },
  assertions: [
    {
      type: "statusCode",
      operator: "equals",
      value: 200
    },
    {
      type: "responseTime",
      operator: "lessThan",
      value: 500
    },
    {
      type: "jsonPath",
      path: "$.orderId",
      operator: "equals",
      value: "${orderId}"
    },
    {
      type: "jsonPath",
      path: "$.status",
      operator: "in",
      value: ["CREATED", "PENDING", "COMPLETED"]
    }
  ]
};

// Step 4: Poll for Completion
const pollOrderStatus = {
  method: "GET",
  url: "${BASE_URL}/api/orders/${orderId}",
  headers: {
    "Accept": "application/json"
  },
  retry: {
    maxAttempts: 10,
    interval: 2000, // 2 seconds
    condition: {
      jsonPath: "$.status",
      operator: "notEquals",
      value: "COMPLETED"
    }
  },
  assertions: [
    {
      type: "statusCode",
      operator: "equals",
      value: 200
    },
    {
      type: "jsonPath",
      path: "$.status",
      operator: "equals",
      value: "COMPLETED"
    }
  ]
};

// Test Definition
module.exports = {
  config: testConfig,
  steps: [
    healthCheck,
    createOrder,
    getOrder,
    pollOrderStatus
  ]
};
```

---

#### 3.1.2 재고 부족 시나리오 테스트

```javascript
// Instana Synthetic Test - API Test
// Test Name: Order Flow - Out of Stock
// Schedule: Every 15 minutes

const testConfig = {
  name: "Order Flow - Out of Stock",
  description: "재고 부족 시 에러 처리 검증",
  schedule: "*/15 * * * *",
  locations: ["seoul"],
  timeout: 30000
};

// Step 1: Create Order with Out-of-Stock Item
const createOrderNoStock = {
  method: "POST",
  url: "${BASE_URL}/api/orders",
  headers: {
    "Content-Type": "application/json",
    "Accept": "application/json"
  },
  body: {
    orderId: "syn-test-nostock-${timestamp}",
    userId: "synthetic-user",
    items: [
      {
        sku: "SKU-999", // Non-existent SKU
        qty: 999,
        price: 1000
      }
    ],
    currency: "KRW"
  },
  assertions: [
    {
      type: "statusCode",
      operator: "equals",
      value: 201 // Order is created
    }
  ],
  extract: [
    {
      name: "orderId",
      jsonPath: "$.orderId"
    }
  ]
};

// Step 2: Poll for Rejection
const pollOrderRejection = {
  method: "GET",
  url: "${BASE_URL}/api/orders/${orderId}",
  headers: {
    "Accept": "application/json"
  },
  retry: {
    maxAttempts: 10,
    interval: 2000,
    condition: {
      jsonPath: "$.status",
      operator: "in",
      value: ["CREATED", "PENDING"]
    }
  },
  assertions: [
    {
      type: "statusCode",
      operator: "equals",
      value: 200
    },
    {
      type: "jsonPath",
      path: "$.status",
      operator: "in",
      value: ["REJECTED", "FAILED"]
    },
    {
      type: "jsonPath",
      path: "$.errorMessage",
      operator: "contains",
      value: "inventory"
    }
  ]
};

module.exports = {
  config: testConfig,
  steps: [
    createOrderNoStock,
    pollOrderRejection
  ]
};
```

---

#### 3.1.3 결제 실패 시나리오 테스트

```javascript
// Instana Synthetic Test - API Test
// Test Name: Order Flow - Payment Failure
// Schedule: Every 15 minutes

const testConfig = {
  name: "Order Flow - Payment Failure",
  description: "결제 실패 시 에러 처리 검증",
  schedule: "*/15 * * * *",
  locations: ["seoul"],
  timeout: 30000
};

// Step 1: Create Order with Payment Failure Header
const createOrderPaymentFail = {
  method: "POST",
  url: "${BASE_URL}/api/orders",
  headers: {
    "Content-Type": "application/json",
    "Accept": "application/json",
    "X-Force-Payment": "fail" // Force payment failure
  },
  body: {
    orderId: "syn-test-payment-fail-${timestamp}",
    userId: "synthetic-user",
    items: [
      {
        sku: "SKU-100",
        qty: 1,
        price: 12000
      }
    ],
    currency: "KRW"
  },
  assertions: [
    {
      type: "statusCode",
      operator: "equals",
      value: 201
    }
  ],
  extract: [
    {
      name: "orderId",
      jsonPath: "$.orderId"
    }
  ]
};

// Step 2: Poll for Payment Failure
const pollPaymentFailure = {
  method: "GET",
  url: "${BASE_URL}/api/orders/${orderId}",
  headers: {
    "Accept": "application/json"
  },
  retry: {
    maxAttempts: 10,
    interval: 2000,
    condition: {
      jsonPath: "$.status",
      operator: "notIn",
      value: ["PAYMENT_FAILED", "FAILED"]
    }
  },
  assertions: [
    {
      type: "statusCode",
      operator: "equals",
      value: 200
    },
    {
      type: "jsonPath",
      path: "$.status",
      operator: "equals",
      value: "PAYMENT_FAILED"
    }
  ]
};

module.exports = {
  config: testConfig,
  steps: [
    createOrderPaymentFail,
    pollPaymentFailure
  ]
};
```

---

### 3.2 브라우저 테스트 스크립트 (Puppeteer)

#### 3.2.1 주문 생성 UI 테스트

```javascript
// Instana Synthetic Test - Browser Test
// Test Name: Order UI - Create Order
// Schedule: Every 10 minutes

const puppeteer = require('puppeteer');

const testConfig = {
  name: "Order UI - Create Order",
  description: "웹 UI를 통한 주문 생성 테스트",
  schedule: "*/10 * * * *",
  locations: ["seoul"],
  timeout: 60000,
  browser: {
    type: "chromium",
    headless: true,
    viewport: {
      width: 1920,
      height: 1080
    }
  }
};

async function runTest(page, context) {
  const baseUrl = context.env.BASE_URL || 'https://your-domain.com';
  const apiUrl = context.env.API_URL || 'https://api.your-domain.com/api';
  
  try {
    // Step 1: Navigate to page
    console.log('Step 1: Navigating to page...');
    const startTime = Date.now();
    await page.goto(baseUrl, {
      waitUntil: 'networkidle2',
      timeout: 30000
    });
    const pageLoadTime = Date.now() - startTime;
    console.log(`Page loaded in ${pageLoadTime}ms`);
    
    // Assert: Page load time < 3000ms
    if (pageLoadTime > 3000) {
      throw new Error(`Page load time too slow: ${pageLoadTime}ms`);
    }
    
    // Step 2: Set API Base URL
    console.log('Step 2: Setting API Base URL...');
    await page.waitForSelector('#baseUrl', { timeout: 5000 });
    await page.click('#baseUrl', { clickCount: 3 }); // Select all
    await page.type('#baseUrl', apiUrl);
    await page.click('#applyBaseUrl');
    await page.waitForTimeout(500);
    
    // Step 3: Health Check
    console.log('Step 3: Performing health check...');
    await page.click('#pingBtn');
    await page.waitForTimeout(2000);
    
    const outputText = await page.$eval('#output', el => el.textContent);
    if (!outputText.includes('ok') && !outputText.includes('UP')) {
      throw new Error('Health check failed');
    }
    
    // Step 4: Fill order form
    console.log('Step 4: Filling order form...');
    const orderData = {
      orderId: `ui-test-${Date.now()}`,
      userId: "ui-user",
      items: [
        { sku: "SKU-100", qty: 1, price: 12000 }
      ],
      currency: "KRW",
      note: "Browser test"
    };
    
    await page.click('#orderBody', { clickCount: 3 }); // Select all
    await page.type('#orderBody', JSON.stringify(orderData, null, 2));
    
    // Step 5: Submit order
    console.log('Step 5: Submitting order...');
    await page.click('#sendBtn');
    await page.waitForTimeout(3000);
    
    // Step 6: Verify response
    console.log('Step 6: Verifying response...');
    const finalOutput = await page.$eval('#output', el => el.textContent);
    
    if (!finalOutput.includes('ok') && !finalOutput.includes('201')) {
      throw new Error('Order creation failed');
    }
    
    if (!finalOutput.includes(orderData.orderId)) {
      throw new Error('Order ID not found in response');
    }
    
    // Step 7: Take screenshot
    console.log('Step 7: Taking screenshot...');
    await page.screenshot({
      path: 'order-success.png',
      fullPage: true
    });
    
    // Collect performance metrics
    const performanceMetrics = await page.evaluate(() => {
      const perfData = window.performance.timing;
      const navigation = performance.getEntriesByType('navigation')[0];
      
      return {
        pageLoadTime: perfData.loadEventEnd - perfData.navigationStart,
        domContentLoaded: perfData.domContentLoadedEventEnd - perfData.navigationStart,
        firstPaint: navigation ? navigation.responseStart - navigation.requestStart : 0,
        domInteractive: perfData.domInteractive - perfData.navigationStart
      };
    });
    
    console.log('Performance Metrics:', performanceMetrics);
    
    // Assert: Performance thresholds
    if (performanceMetrics.pageLoadTime > 3000) {
      console.warn(`Page load time exceeded threshold: ${performanceMetrics.pageLoadTime}ms`);
    }
    
    console.log('Test completed successfully!');
    return {
      success: true,
      metrics: performanceMetrics
    };
    
  } catch (error) {
    console.error('Test failed:', error);
    
    // Take error screenshot
    await page.screenshot({
      path: 'error-screenshot.png',
      fullPage: true
    });
    
    throw error;
  }
}

module.exports = {
  config: testConfig,
  run: runTest
};
```

---

#### 3.2.2 에러 처리 UI 테스트

```javascript
// Instana Synthetic Test - Browser Test
// Test Name: Order UI - Error Handling
// Schedule: Every 30 minutes

async function runErrorTest(page, context) {
  const baseUrl = context.env.BASE_URL || 'https://your-domain.com';
  
  try {
    console.log('Navigating to page...');
    await page.goto(baseUrl, { waitUntil: 'networkidle2' });
    
    // Test 1: Unhandled Error
    console.log('Test 1: Triggering unhandled error...');
    
    // Listen for console errors
    const consoleErrors = [];
    page.on('console', msg => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });
    
    // Listen for page errors
    const pageErrors = [];
    page.on('pageerror', error => {
      pageErrors.push(error.message);
    });
    
    await page.click('#throwBtn');
    await page.waitForTimeout(2000);
    
    if (pageErrors.length === 0) {
      throw new Error('Expected unhandled error was not thrown');
    }
    
    console.log('Unhandled error detected:', pageErrors[0]);
    
    // Test 2: Promise Rejection
    console.log('Test 2: Triggering promise rejection...');
    
    const unhandledRejections = [];
    page.on('pageerror', error => {
      if (error.message.includes('promise')) {
        unhandledRejections.push(error.message);
      }
    });
    
    await page.click('#promiseBtn');
    await page.waitForTimeout(2000);
    
    console.log('Promise rejection detected');
    
    // Test 3: Handled Error
    console.log('Test 3: Testing handled error...');
    await page.click('#handledBtn');
    await page.waitForTimeout(1000);
    
    const output = await page.$eval('#output', el => el.textContent);
    if (!output.includes('handled')) {
      throw new Error('Handled error not displayed correctly');
    }
    
    console.log('All error handling tests passed!');
    
    return {
      success: true,
      errorsDetected: pageErrors.length + unhandledRejections.length
    };
    
  } catch (error) {
    console.error('Error test failed:', error);
    await page.screenshot({ path: 'error-test-failure.png', fullPage: true });
    throw error;
  }
}

module.exports = {
  config: {
    name: "Order UI - Error Handling",
    description: "UI 에러 처리 및 EUM 연동 검증",
    schedule: "*/30 * * * *",
    locations: ["seoul"],
    timeout: 60000
  },
  run: runErrorTest
};
```

---

### 3.3 SSL 인증서 테스트 스크립트

```javascript
// Instana Synthetic Test - SSL Certificate Test
// Test Name: SSL Certificate Monitoring
// Schedule: Every 6 hours

const https = require('https');
const tls = require('tls');

const testConfig = {
  name: "SSL Certificate Monitoring",
  description: "SSL 인증서 유효성 및 만료일 모니터링",
  schedule: "0 */6 * * *", // Every 6 hours
  locations: ["seoul"],
  timeout: 30000
};

async function checkSSLCertificate(hostname, port = 443) {
  return new Promise((resolve, reject) => {
    const options = {
      host: hostname,
      port: port,
      method: 'GET',
      rejectUnauthorized: false, // We want to check even invalid certs
      agent: false
    };
    
    const req = https.request(options, (res) => {
      const cert = res.socket.getPeerCertificate();
      
      if (!cert || Object.keys(cert).length === 0) {
        reject(new Error('No certificate found'));
        return;
      }
      
      const now = new Date();
      const validFrom = new Date(cert.valid_from);
      const validTo = new Date(cert.valid_to);
      const daysUntilExpiry = Math.floor((validTo - now) / (1000 * 60 * 60 * 24));
      
      const result = {
        subject: cert.subject,
        issuer: cert.issuer,
        validFrom: validFrom,
        validTo: validTo,
        daysUntilExpiry: daysUntilExpiry,
        serialNumber: cert.serialNumber,
        fingerprint: cert.fingerprint,
        isValid: now >= validFrom && now <= validTo,
        subjectAltNames: cert.subjectaltname
      };
      
      resolve(result);
    });
    
    req.on('error', (error) => {
      reject(error);
    });
    
    req.end();
  });
}

async function runSSLTest(context) {
  const hostname = context.env.DOMAIN || 'your-domain.com';
  
  try {
    console.log(`Checking SSL certificate for ${hostname}...`);
    
    const certInfo = await checkSSLCertificate(hostname);
    
    console.log('Certificate Information:');
    console.log(`  Subject: ${JSON.stringify(certInfo.subject)}`);
    console.log(`  Issuer: ${JSON.stringify(certInfo.issuer)}`);
    console.log(`  Valid From: ${certInfo.validFrom}`);
    console.log(`  Valid To: ${certInfo.validTo}`);
    console.log(`  Days Until Expiry: ${certInfo.daysUntilExpiry}`);
    console.log(`  Is Valid: ${certInfo.isValid}`);
    
    // Assertions
    const assertions = [];
    
    // Check if certificate is valid
    if (!certInfo.isValid) {
      assertions.push({
        passed: false,
        message: 'Certificate is not valid (expired or not yet valid)'
      });
    } else {
      assertions.push({
        passed: true,
        message: 'Certificate is valid'
      });
    }
    
    // Check expiry warning thresholds
    if (certInfo.daysUntilExpiry <= 7) {
      assertions.push({
        passed: false,
        severity: 'critical',
        message: `Certificate expires in ${certInfo.daysUntilExpiry} days - URGENT!`
      });
    } else if (certInfo.daysUntilExpiry <= 14) {
      assertions.push({
        passed: false,
        severity: 'warning',
        message: `Certificate expires in ${certInfo.daysUntilExpiry} days - Action required`
      });
    } else if (certInfo.daysUntilExpiry <= 30) {
      assertions.push({
        passed: true,
        severity: 'info',
        message: `Certificate expires in ${certInfo.daysUntilExpiry} days - Plan renewal`
      });
    } else {
      assertions.push({
        passed: true,
        message: `Certificate expires in ${certInfo.daysUntilExpiry} days`
      });
    }
    
    // Check if domain matches
    const subjectCN = certInfo.subject.CN;
    if (subjectCN !== hostname && !certInfo.subjectAltNames?.includes(`DNS:${hostname}`)) {
      assertions.push({
        passed: false,
        message: `Certificate CN/SAN does not match hostname ${hostname}`
      });
    } else {
      assertions.push({
        passed: true,
        message: 'Certificate domain matches'
      });
    }
    
    // Check for failures
    const failures = assertions.filter(a => !a.passed);
    
    if (failures.length > 0) {
      console.error('SSL Certificate Test Failed:');
      failures.forEach(f => console.error(`  - ${f.message}`));
      throw new Error(`SSL test failed with ${failures.length} assertion(s)`);
    }
    
    console.log('SSL Certificate Test Passed!');
    
    return {
      success: true,
      certificate: certInfo,
      assertions: assertions
    };
    
  } catch (error) {
    console.error('SSL Test Error:', error);
    throw error;
  }
}

module.exports = {
  config: testConfig,
  run: runSSLTest
};
```

---

## 4. GitLab CI/CD 통합 가이드

### 4.1 현재 파이프라인 분석

현재 [`.gitlab-ci.yml`](.gitlab-ci.yml:1-297)은 다음과 같은 구조를 가지고 있습니다:

```yaml
stages: [build, deploy]

# 빌드 단계
- build:order
- build:inventory
- build:fulfillment
- build:api
- build:payment-adapter

# 배포 단계 (main 브랜치만)
- deploy
```

**개선 필요 사항**:
1. Synthetic Test 단계 추가
2. 배포 전 검증 (Pre-deployment validation)
3. 배포 후 Smoke Test
4. Release Marker 개선 (현재는 단순 API 호출)

---

### 4.2 개선된 GitLab CI/CD 파이프라인

#### 4.2.1 전체 파이프라인 구조

```yaml
stages:
  - build
  - test
  - deploy
  - synthetic-test
  - release-marker
  - rollback
```

**단계별 설명**:
1. **build**: Docker 이미지 빌드 (기존 유지)
2. **test**: 단위 테스트 (선택사항)
3. **deploy**: OCP에 배포
4. **synthetic-test**: Synthetic Monitoring 실행
5. **release-marker**: Release Marker 등록
6. **rollback**: 실패 시 자동 롤백 (선택사항)

---

#### 4.2.2 Synthetic Test 통합

```yaml
# .gitlab-ci.yml에 추가

variables:
  INSTANA_BASE_URL: "${INSTANA_BASE_URL}"
  INSTANA_API_TOKEN: "${INSTANA_API_TOKEN}"
  SYNTHETIC_TEST_TIMEOUT: "300" # 5 minutes

# Synthetic Test 실행 템플릿
.synthetic-test-template:
  stage: synthetic-test
  image: curlimages/curl:latest
  script:
    - |
      echo "Running Synthetic Test: ${TEST_ID}"
      
      # Trigger Synthetic Test
      RESULT_ID=$(curl -s -X POST \
        "${INSTANA_BASE_URL}/api/synthetic-monitoring/tests/${TEST_ID}/execute" \
        -H "Authorization: apiToken ${INSTANA_API_TOKEN}" \
        -H "Content-Type: application/json" \
        -d '{
          "location": "seoul",
          "variables": {
            "BASE_URL": "'"${APP_URL}"'",
            "API_URL": "'"${API_URL}"'"
          }
        }' | jq -r '.resultId')
      
      echo "Test Result ID: ${RESULT_ID}"
      
      # Poll for test result
      MAX_ATTEMPTS=60
      ATTEMPT=0
      
      while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
        STATUS=$(curl -s \
          "${INSTANA_BASE_URL}/api/synthetic-monitoring/results/${RESULT_ID}" \
          -H "Authorization: apiToken ${INSTANA_API_TOKEN}" \
          | jq -r '.status')
        
        echo "Attempt $((ATTEMPT+1))/${MAX_ATTEMPTS}: Status = ${STATUS}"
        
        if [ "${STATUS}" = "SUCCESS" ]; then
          echo "✓ Synthetic test passed!"
          exit 0
        elif [ "${STATUS}" = "FAILED" ]; then
          echo "✗ Synthetic test failed!"
          
          # Get failure details
          curl -s \
            "${INSTANA_BASE_URL}/api/synthetic-monitoring/results/${RESULT_ID}" \
            -H "Authorization: apiToken ${INSTANA_API_TOKEN}" \
            | jq '.failures'
          
          exit 1
        fi
        
        sleep 5
        ATTEMPT=$((ATTEMPT+1))
      done
      
      echo "✗ Synthetic test timed out!"
      exit 1
  retry:
    max: 2
    when:
      - script_failure

# API 테스트 - 정상 플로우
synthetic-test:api-happy-path:
  extends: .synthetic-test-template
  variables:
    TEST_ID: "${SYNTHETIC_TEST_API_HAPPY_PATH_ID}"
    APP_URL: "${OCP_APP_URL}"
    API_URL: "${OCP_API_URL}"
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
  needs:
    - deploy

# API 테스트 - 재고 부족
synthetic-test:api-out-of-stock:
  extends: .synthetic-test-template
  variables:
    TEST_ID: "${SYNTHETIC_TEST_API_OUT_OF_STOCK_ID}"
    APP_URL: "${OCP_APP_URL}"
    API_URL: "${OCP_API_URL}"
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
  needs:
    - deploy
  allow_failure: true # 이 테스트는 실패해도 파이프라인 계속

# API 테스트 - 결제 실패
synthetic-test:api-payment-fail:
  extends: .synthetic-test-template
  variables:
    TEST_ID: "${SYNTHETIC_TEST_API_PAYMENT_FAIL_ID}"
    APP_URL: "${OCP_APP_URL}"
    API_URL: "${OCP_API_URL}"
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
  needs:
    - deploy
  allow_failure: true

# 브라우저 테스트 - UI
synthetic-test:browser-ui:
  extends: .synthetic-test-template
  variables:
    TEST_ID: "${SYNTHETIC_TEST_BROWSER_UI_ID}"
    APP_URL: "${OCP_APP_URL}"
    API_URL: "${OCP_API_URL}"
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
  needs:
    - deploy

# SSL 인증서 테스트
synthetic-test:ssl-certificate:
  extends: .synthetic-test-template
  variables:
    TEST_ID: "${SYNTHETIC_TEST_SSL_ID}"
    APP_URL: "${OCP_APP_URL}"
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
  needs:
    - deploy
  allow_failure: true # SSL 테스트 실패해도 배포는 계속
```

---

#### 4.2.3 Smoke Test (빠른 검증)

```yaml
# 배포 직후 빠른 Smoke Test
smoke-test:
  stage: synthetic-test
  image: curlimages/curl:latest
  script:
    - |
      echo "Running smoke tests..."
      
      # Health Check
      echo "1. Health Check..."
      HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        "${OCP_API_URL}/actuator/health")
      
      if [ "${HTTP_CODE}" != "200" ]; then
        echo "✗ Health check failed: HTTP ${HTTP_CODE}"
        exit 1
      fi
      echo "✓ Health check passed"
      
      # Simple Order Creation
      echo "2. Order Creation Test..."
      RESPONSE=$(curl -s -X POST \
        "${OCP_API_URL}/api/orders" \
        -H "Content-Type: application/json" \
        -d '{
          "orderId": "smoke-test-'"$(date +%s)"'",
          "userId": "smoke-user",
          "items": [{"sku": "SKU-100", "qty": 1, "price": 12000}],
          "currency": "KRW"
        }')
      
      ORDER_ID=$(echo "$RESPONSE" | jq -r '.orderId')
      if [ -z "$ORDER_ID" ] || [ "$ORDER_ID" = "null" ]; then
        echo "✗ Order creation failed"
        exit 1
      fi
      echo "✓ Order created: $ORDER_ID"
      
      echo "All smoke tests passed!"
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
  needs:

---

#### 4.2.4 GitLab CI 변수 설정

**필수 환경 변수**:

```bash
# Instana 관련
INSTANA_BASE_URL=https://your-instana-instance.com
INSTANA_API_TOKEN=your-api-token-here

# Synthetic Test IDs
SYNTHETIC_TEST_API_HAPPY_PATH_ID=test-id-1
SYNTHETIC_TEST_API_OUT_OF_STOCK_ID=test-id-2
SYNTHETIC_TEST_API_PAYMENT_FAIL_ID=test-id-3
SYNTHETIC_TEST_BROWSER_UI_ID=test-id-4
SYNTHETIC_TEST_SSL_ID=test-id-5

# 애플리케이션 URL
OCP_APP_URL=https://your-app.apps.ocp.example.com
OCP_API_URL=https://api-gateway.apps.ocp.example.com

# 알림 (선택사항)
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
```

**GitLab UI에서 설정**:

1. Project → Settings → CI/CD → Variables
2. Add Variable:
   - Key: `INSTANA_API_TOKEN`
   - Value: `your-token`
   - Type: Variable
   - Protected: ✓
   - Masked: ✓
   - Expand variable reference: ✗

---

#### 4.2.5 완성된 GitLab CI 파이프라인

```yaml
# .gitlab-ci.yml - 완성 버전

stages:
  - build
  - test
  - deploy
  - synthetic-test
  - release-marker

variables:
  DOCKER_BUILDKIT: "1"
  IMAGE_TAG: "$CI_COMMIT_SHORT_SHA"
  BRANCH_TAG: "$CI_COMMIT_REF_SLUG-latest"
  INSTANA_BASE_URL: "${INSTANA_BASE_URL}"
  INSTANA_API_TOKEN: "${INSTANA_API_TOKEN}"

# ... (기존 빌드 단계 유지) ...

# Smoke Test (빠른 검증)
smoke-test:
  stage: synthetic-test
  image: curlimages/curl:latest
  script:
    - |
      echo "Running smoke tests..."
      
      # Health Check
      HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${OCP_API_URL}/actuator/health")
      if [ "${HTTP_CODE}" != "200" ]; then
        echo "✗ Health check failed: HTTP ${HTTP_CODE}"
        exit 1
      fi
      echo "✓ Health check passed"
      
      # Simple Order Creation
      RESPONSE=$(curl -s -X POST "${OCP_API_URL}/api/orders" \
        -H "Content-Type: application/json" \
        -d '{"orderId":"smoke-'"$(date +%s)"'","userId":"smoke","items":[{"sku":"SKU-100","qty":1,"price":12000}],"currency":"KRW"}')
      
      ORDER_ID=$(echo "$RESPONSE" | grep -o '"orderId":"[^"]*"' | cut -d'"' -f4)
      if [ -z "$ORDER_ID" ]; then
        echo "✗ Order creation failed"
        exit 1
      fi
      echo "✓ Order created: $ORDER_ID"
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
  needs:
    - deploy

# Synthetic Test 템플릿
.synthetic-test-template:
  stage: synthetic-test
  image: curlimages/curl:latest
  before_script:
    - apk add --no-cache jq
  script:
    - |
      echo "Running Synthetic Test: ${TEST_ID}"
      
      RESULT_ID=$(curl -s -X POST \
        "${INSTANA_BASE_URL}/api/synthetic-monitoring/tests/${TEST_ID}/execute" \
        -H "Authorization: apiToken ${INSTANA_API_TOKEN}" \
        -H "Content-Type: application/json" \
        -d '{"location":"seoul","variables":{"BASE_URL":"'"${OCP_APP_URL}"'","API_URL":"'"${OCP_API_URL}"'"}}' \
        | jq -r '.resultId')
      
      if [ -z "$RESULT_ID" ] || [ "$RESULT_ID" = "null" ]; then
        echo "✗ Failed to trigger test"
        exit 1
      fi
      
      echo "Test Result ID: ${RESULT_ID}"
      
      MAX_ATTEMPTS=60
      ATTEMPT=0
      
      while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
        STATUS=$(curl -s \
          "${INSTANA_BASE_URL}/api/synthetic-monitoring/results/${RESULT_ID}" \
          -H "Authorization: apiToken ${INSTANA_API_TOKEN}" \
          | jq -r '.status')
        
        echo "Attempt $((ATTEMPT+1))/${MAX_ATTEMPTS}: Status = ${STATUS}"
        
        if [ "${STATUS}" = "SUCCESS" ]; then
          echo "✓ Synthetic test passed!"
          exit 0
        elif [ "${STATUS}" = "FAILED" ]; then
          echo "✗ Synthetic test failed!"
          curl -s "${INSTANA_BASE_URL}/api/synthetic-monitoring/results/${RESULT_ID}" \
            -H "Authorization: apiToken ${INSTANA_API_TOKEN}" | jq '.failures'
          exit 1
        fi
        
        sleep 5
        ATTEMPT=$((ATTEMPT+1))
      done
      
      echo "✗ Test timed out!"
      exit 1
  retry:
    max: 2
    when: script_failure

# API 테스트들
synthetic-test:api-happy-path:
  extends: .synthetic-test-template
  variables:
    TEST_ID: "${SYNTHETIC_TEST_API_HAPPY_PATH_ID}"
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
  needs:
    - smoke-test

synthetic-test:browser-ui:
  extends: .synthetic-test-template
  variables:
    TEST_ID: "${SYNTHETIC_TEST_BROWSER_UI_ID}"
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
  needs:
    - smoke-test

# Release Marker
release-marker:
  stage: release-marker
  image: curlimages/curl:latest
  before_script:
    - apk add --no-cache jq bash
  script:
    - |
      echo "Creating Release Marker..."
      
      START_TS=$(date +%s%3N)
      GIT_MESSAGE=$(echo "${CI_COMMIT_MESSAGE}" | head -n 1 | sed 's/"/\\"/g')
      
      RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
        "${INSTANA_BASE_URL}/api/releases" \
        -H "Authorization: apiToken ${INSTANA_API_TOKEN}" \
        -H "Content-Type: application/json" \
        -d '{
          "name": "'"${CI_COMMIT_SHORT_SHA}"'",
          "start": '"${START_TS}"',
          "applications": [{"name": "TXC Demo"}],
          "scope": {"tag": {"key": "environment", "value": "production"}},
          "metadata": {
            "gitCommit": "'"${CI_COMMIT_SHA}"'",
            "gitBranch": "'"${CI_COMMIT_REF_NAME}"'",
            "gitAuthor": "'"${CI_COMMIT_AUTHOR}"'",
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
      fi
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
  needs:
    - synthetic-test:api-happy-path
    - synthetic-test:browser-ui
  allow_failure: true
```

---

## 문서 구성

이 가이드는 여러 부분으로 나뉘어 있습니다:

1. **메인 문서** (현재 문서)
   - Synthetic Monitoring 개요
   - 테스트 시나리오 설계
   - Synthetic Test 스크립트 예제
   - GitLab CI/CD 통합 기본

2. **[Part 2: Release Marker & 알림 설정](docs/INSTANA_SYNTHETIC_PART2_RELEASE_MARKER.md)**
   - Release Marker 구현 가이드
   - 알림 채널 설정 (Slack, Email, PagerDuty)
   - 알림 규칙 설정
   - 대시보드 구성

3. **[Part 3: 데모 시나리오 & 트러블슈팅](docs/INSTANA_SYNTHETIC_PART3_DEMO.md)**
   - 4가지 데모 시나리오 스크립트
   - 트러블슈팅 가이드
   - 구현 작업 목록 및 일정
   - 리스크 관리

---

## 빠른 시작 가이드

### 1단계: Instana에서 Synthetic Test 생성

1. Instana UI → Synthetic Monitoring
2. Create Test → API Test
3. 테스트 설정:
   - Name: Order Flow - Happy Path
   - URL: `${BASE_URL}/api/orders`
   - Method: POST
   - Schedule: */5 * * * *
4. Test ID 복사

### 2단계: GitLab CI 변수 설정

```bash
# GitLab UI에서 설정
INSTANA_BASE_URL=https://your-instana.com
INSTANA_API_TOKEN=your-token
SYNTHETIC_TEST_API_HAPPY_PATH_ID=test-id
OCP_API_URL=https://api.your-domain.com
```

### 3단계: .gitlab-ci.yml 수정

위의 완성된 파이프라인 예제를 참고하여 수정

### 4단계: 테스트

```bash
git add .gitlab-ci.yml
git commit -m "Add Synthetic Monitoring integration"
git push origin main
```

### 5단계: 결과 확인

- GitLab CI 파이프라인 확인
- Instana UI에서 테스트 결과 확인
- Release Marker 확인

---

## 주요 이점

### 1. 조기 장애 감지
- 실제 사용자 영향 전 문제 발견
- 24/7 자동 모니터링
- 다양한 시나리오 검증

### 2. 배포 안정성 향상
- 배포 전 자동 검증
- 배포 후 Smoke Test
- 문제 발생 시 자동 롤백

### 3. SSL 인증서 관리
- 만료 30일 전 자동 알림
- 인증서 갱신 후 자동 검증
- 보안 설정 지속적 확인

### 4. 성능 추적
- 응답 시간 트렌드 분석
- SLO 달성 여부 추적
- 배포 영향 분석

### 5. 운영 효율성
- 수동 테스트 자동화
- 알림을 통한 빠른 대응
- 데이터 기반 의사결정

---

## 다음 단계

1. ✅ 이 가이드 검토
2. ✅ [Part 2](docs/INSTANA_SYNTHETIC_PART2_RELEASE_MARKER.md) 확인
3. ✅ [Part 3](docs/INSTANA_SYNTHETIC_PART3_DEMO.md) 확인
4. 🔄 Phase 1 작업 시작 (Synthetic Test 생성)
5. 🔄 Phase 2 작업 (CI/CD 통합)
6. 🔄 Phase 3 작업 (알림 및 대시보드)
7. 🔄 Phase 4 작업 (데모 준비)

---

## 지원 및 문의

- Instana 문서: https://www.ibm.com/docs/en/instana-observability
- GitLab CI/CD 문서: https://docs.gitlab.com/ee/ci/
- 프로젝트 이슈: GitLab Issues

---

**작성일**: 2024-01-15  
**버전**: 1.0  
**작성자**: DevOps Team  
**검토자**: Solution Architect

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 | 작성자 |
|------|------|-----------|--------|
| 2024-01-15 | 1.0 | 초기 버전 작성 | DevOps Team |

    - deploy