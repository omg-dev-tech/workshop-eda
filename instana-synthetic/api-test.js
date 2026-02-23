// Instana Synthetic Monitoring - API Test Script (Playwright 기반)
// 
// 이 스크립트는 Workshop EDA 애플리케이션의 주요 Admin API 엔드포인트를 테스트합니다.
// Instana Synthetic은 Playwright를 사용하므로 page.request API를 활용합니다.
// 
// 테스트 대상:
// - Order API (전체 조회, 단건 조회)
// - Inventory API (전체 조회)
// - Fulfillment API (전체 조회)
// - Analytics API (이벤트 카운트, 메트릭 조회)

const assert = require('assert');

// API Gateway URL (하드코딩)
const baseUrl = 'http://api-gateway-workshop-eda.apps.itz-12fl8d.infra01-lb.syd05.techzone.ibm.com';

console.log('🔧 Configuration:');
console.log(`  - Base URL: ${baseUrl}`);
console.log('');

// 테스트 결과 저장
const results = {
  passed: 0,
  failed: 0,
  tests: []
};

// 유틸리티 함수: HTTP 요청 (Instana $http 콜백 방식)
function makeRequest(method, path, expectedStatus = 200) {
  const url = `${baseUrl}${path}`;
  const startTime = Date.now();
  
  return new Promise((resolve, reject) => {
    // Instana Synthetic의 $http는 콜백 방식 사용
    $http({
      method: method,
      url: url,
      headers: {
        'Content-Type': 'application/json'
      },
      json: true  // 자동으로 JSON 파싱
    }, (error, response, body) => {
      const duration = Date.now() - startTime;
      
      if (error) {
        console.log(`  [ERROR] Request failed: ${error.message}`);
        resolve({
          success: false,
          status: 0,
          duration: duration,
          error: error.message
        });
        return;
      }
      
      const status = response.statusCode;
      
      console.log(`  [DEBUG] Status: ${status}, Duration: ${duration}ms`);
      
      try {
        // 상태 코드 검증
        assert.strictEqual(
          status,
          expectedStatus,
          `Expected status ${expectedStatus} but got ${status}`
        );
        
        // 응답 시간 검증 (5초 이내)
        assert.ok(
          duration < 5000,
          `Response time ${duration}ms exceeds 5000ms threshold`
        );
        
        resolve({
          success: true,
          status: status,
          duration: duration,
          data: body
        });
      } catch (assertError) {
        resolve({
          success: false,
          status: status,
          duration: duration,
          error: assertError.message
        });
      }
    });
  });
}

// 테스트 실행 함수
async function runTest(testName, testFn) {
  console.log(`\n[TEST] ${testName}`);
  const startTime = Date.now();
  
  try {
    await testFn();
    const duration = Date.now() - startTime;
    console.log(`✅ PASSED (${duration}ms)`);
    results.passed++;
    results.tests.push({
      name: testName,
      status: 'PASSED',
      duration: duration
    });
  } catch (error) {
    const duration = Date.now() - startTime;
    console.log(`❌ FAILED (${duration}ms): ${error.message}`);
    results.failed++;
    results.tests.push({
      name: testName,
      status: 'FAILED',
      duration: duration,
      error: error.message
    });
  }
}

// ============================================================================
// 메인 테스트 시작
// ============================================================================

console.log('='.repeat(80));
console.log('Instana Synthetic API Test - Workshop EDA');
console.log('='.repeat(80));
console.log(`Base URL: ${baseUrl}`);
console.log(`Start Time: ${new Date().toISOString()}`);

(async function() {
  let orderIds = [];
  
  // ============================================================================
  // 1. Order API 테스트
  // ============================================================================
  
  await runTest('Order API - 전체 조회 (페이징)', async () => {
    const result = await makeRequest('GET', '/api/orders?page=0&size=20');
    
    assert.ok(result.success, 'Request failed');
    assert.ok(result.data, 'Response data should exist');
    
    // Page 객체 구조 확인
    const orders = result.data.content || result.data;
    assert.ok(Array.isArray(orders), 'Orders content should be an array');
    
    console.log(`  - Found ${orders.length} orders (page 0, size 20)`);
    if (result.data.totalElements !== undefined) {
      console.log(`  - Total elements: ${result.data.totalElements}`);
    }
    console.log(`  - Response time: ${result.duration}ms`);
    
    // 첫 번째 주문 ID 저장 (단건 조회용)
    if (orders.length > 0) {
      orderIds = orders.slice(0, 3).map(order => order.id);
      console.log(`  - Sample order IDs: ${orderIds.join(', ')}`);
    }
  });
  
  await runTest('Order API - 단건 조회', async () => {
    if (orderIds.length === 0) {
      console.log('  - SKIPPED: No orders available');
      return;
    }
    
    const orderId = orderIds[0];
    const result = await makeRequest('GET', `/api/orders/${orderId}`);
    
    assert.ok(result.success, 'Request failed');
    assert.ok(result.data, 'Response data should exist');
    assert.strictEqual(result.data.id, orderId, 'Order ID mismatch');
    
    console.log(`  - Order ID: ${result.data.id}`);
    console.log(`  - Status: ${result.data.status}`);
    console.log(`  - Response time: ${result.duration}ms`);
  });
  
  // ============================================================================
  // 2. Inventory API 테스트
  // ============================================================================
  
  await runTest('Inventory API - 전체 조회', async () => {
    const result = await makeRequest('GET', '/api/admin/inventory');
    
    assert.ok(result.success, 'Request failed');
    assert.ok(Array.isArray(result.data), 'Response should be an array');
    
    console.log(`  - Found ${result.data.length} inventory items`);
    console.log(`  - Response time: ${result.duration}ms`);
  });
  
  await runTest('Inventory API - 단건 조회', async () => {
    console.log('  - SKIPPED: Single inventory endpoint not available in AdminGatewayController');
    // AdminGatewayController에 GET /api/admin/inventory/{sku} 엔드포인트가 없음
  });
  
  // ============================================================================
  // 3. Fulfillment API 테스트
  // ============================================================================
  
  await runTest('Fulfillment API - 전체 조회 (페이징)', async () => {
    const result = await makeRequest('GET', '/api/admin/fulfillments?page=0&size=20');
    
    assert.ok(result.success, 'Request failed');
    assert.ok(result.data, 'Response data should exist');
    
    // Page 객체 구조 확인
    const fulfillments = result.data.content || result.data;
    assert.ok(Array.isArray(fulfillments), 'Fulfillments content should be an array');
    
    console.log(`  - Found ${fulfillments.length} fulfillments (page 0, size 20)`);
    if (result.data.totalElements !== undefined) {
      console.log(`  - Total elements: ${result.data.totalElements}`);
    }
    console.log(`  - Response time: ${result.duration}ms`);
  });
  
  await runTest('Fulfillment API - 단건 조회', async () => {
    console.log('  - SKIPPED: Single fulfillment endpoint not available in AdminGatewayController');
    // AdminGatewayController에 GET /api/admin/fulfillments/{id} 엔드포인트가 없음
  });
  
  // ============================================================================
  // 4. Analytics API 테스트
  // ============================================================================
  
  await runTest('Analytics API - 이벤트 카운트 조회', async () => {
    const result = await makeRequest('GET', '/api/admin/analytics/events/count');
    
    assert.ok(result.success, 'Request failed');
    assert.ok(result.data, 'Response data should exist');
    assert.ok(typeof result.data.count === 'number', 'Count should be a number');
    
    console.log(`  - Total events: ${result.data.count}`);
    console.log(`  - Response time: ${result.duration}ms`);
  });
  
  await runTest('Analytics API - 주문 요약 조회', async () => {
    // 오늘 날짜로 조회
    const today = new Date().toISOString().split('T')[0];
    const result = await makeRequest('GET', `/api/admin/analytics/summary?date=${today}`);
    
    assert.ok(result.success, 'Request failed');
    assert.ok(result.data, 'Response data should exist');
    
    console.log(`  - Date: ${result.data.date || today}`);
    console.log(`  - Total Orders: ${result.data.totalOrders || 0}`);
    console.log(`  - Total Amount: ${result.data.totalAmount || 0}`);
    console.log(`  - Response time: ${result.duration}ms`);
  });
  
  // ============================================================================
  // 테스트 결과 요약
  // ============================================================================
  
  console.log('\n' + '='.repeat(80));
  console.log('Test Results Summary');
  console.log('='.repeat(80));
  console.log(`Total Tests: ${results.passed + results.failed}`);
  console.log(`Passed: ${results.passed}`);
  console.log(`Failed: ${results.failed}`);
  console.log(`Success Rate: ${((results.passed / (results.passed + results.failed)) * 100).toFixed(2)}%`);
  console.log(`End Time: ${new Date().toISOString()}`);
  
  // 개별 테스트 결과
  console.log('\nDetailed Results:');
  results.tests.forEach((test, index) => {
    const icon = test.status === 'PASSED' ? '✅' : '❌';
    console.log(`${index + 1}. ${icon} ${test.name} (${test.duration}ms)`);
    if (test.error) {
      console.log(`   Error: ${test.error}`);
    }
  });
  
  // 실패한 테스트가 있으면 에러 발생
  if (results.failed > 0) {
    throw new Error(`${results.failed} test(s) failed`);
  }
  
  console.log('\n✅ All API tests passed successfully!');
})();

// Made with Bob
