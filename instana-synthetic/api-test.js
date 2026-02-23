// Instana Synthetic Monitoring - API Test Script
// 
// 이 스크립트는 Workshop EDA 애플리케이션의 주요 Admin API 엔드포인트를 테스트합니다.
// 
// 테스트 대상:
// - Order API (전체 조회, 단건 조회)
// - Inventory API (전체 조회, 단건 조회)
// - Fulfillment API (전체 조회, 단건 조회)
// - Analytics API (이벤트 카운트, 메트릭 조회)

const assert = require('assert');

// Instana 환경 변수에서 API Gateway URL 가져오기
const baseUrl = $env.API_GATEWAY_URL;

// 테스트 결과 저장
const results = {
  passed: 0,
  failed: 0,
  tests: []
};

// 유틸리티 함수: HTTP 요청
async function makeRequest(method, path, expectedStatus = 200) {
  const url = `${baseUrl}${path}`;
  const startTime = Date.now();
  
  try {
    const response = await $http({
      method: method,
      url: url,
      headers: {
        'Content-Type': 'application/json'
      },
      validateStatus: function (status) {
        return status >= 200 && status < 600; // 모든 상태 코드 허용
      }
    });
    
    const duration = Date.now() - startTime;
    
    // 상태 코드 검증
    assert.strictEqual(
      response.status, 
      expectedStatus, 
      `Expected status ${expectedStatus} but got ${response.status}`
    );
    
    // 응답 시간 검증 (5초 이내)
    assert.ok(
      duration < 5000, 
      `Response time ${duration}ms exceeds 5000ms threshold`
    );
    
    return {
      success: true,
      status: response.status,
      duration: duration,
      data: response.data
    };
  } catch (error) {
    const duration = Date.now() - startTime;
    return {
      success: false,
      status: error.response ? error.response.status : 0,
      duration: duration,
      error: error.message
    };
  }
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
  let inventorySkus = [];
  let fulfillmentIds = [];
  
  // ============================================================================
  // 1. Order API 테스트
  // ============================================================================
  
  await runTest('Order API - 전체 조회', async () => {
    const result = await makeRequest('GET', '/api/admin/orders');
    
    assert.ok(result.success, 'Request failed');
    assert.ok(Array.isArray(result.data), 'Response should be an array');
    
    console.log(`  - Found ${result.data.length} orders`);
    console.log(`  - Response time: ${result.duration}ms`);
    
    // 첫 번째 주문 ID 저장 (단건 조회용)
    if (result.data.length > 0) {
      orderIds = result.data.slice(0, 3).map(order => order.id);
      console.log(`  - Sample order IDs: ${orderIds.join(', ')}`);
    }
  });
  
  await runTest('Order API - 단건 조회', async () => {
    if (orderIds.length === 0) {
      console.log('  - SKIPPED: No orders available');
      return;
    }
    
    const orderId = orderIds[0];
    const result = await makeRequest('GET', `/api/admin/orders/${orderId}`);
    
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
    
    // 첫 번째 SKU 저장 (단건 조회용)
    if (result.data.length > 0) {
      inventorySkus = result.data.slice(0, 3).map(item => item.sku);
      console.log(`  - Sample SKUs: ${inventorySkus.join(', ')}`);
    }
  });
  
  await runTest('Inventory API - 단건 조회', async () => {
    if (inventorySkus.length === 0) {
      console.log('  - SKIPPED: No inventory items available');
      return;
    }
    
    const sku = inventorySkus[0];
    const result = await makeRequest('GET', `/api/admin/inventory/${sku}`);
    
    assert.ok(result.success, 'Request failed');
    assert.ok(result.data, 'Response data should exist');
    assert.strictEqual(result.data.sku, sku, 'SKU mismatch');
    
    console.log(`  - SKU: ${result.data.sku}`);
    console.log(`  - Product Name: ${result.data.productName}`);
    console.log(`  - Stock: ${result.data.stock}`);
    console.log(`  - Response time: ${result.duration}ms`);
  });
  
  // ============================================================================
  // 3. Fulfillment API 테스트
  // ============================================================================
  
  await runTest('Fulfillment API - 전체 조회', async () => {
    const result = await makeRequest('GET', '/api/admin/fulfillments');
    
    assert.ok(result.success, 'Request failed');
    assert.ok(Array.isArray(result.data), 'Response should be an array');
    
    console.log(`  - Found ${result.data.length} fulfillments`);
    console.log(`  - Response time: ${result.duration}ms`);
    
    // 첫 번째 fulfillment ID 저장 (단건 조회용)
    if (result.data.length > 0) {
      fulfillmentIds = result.data.slice(0, 3).map(f => f.id);
      console.log(`  - Sample fulfillment IDs: ${fulfillmentIds.join(', ')}`);
    }
  });
  
  await runTest('Fulfillment API - 단건 조회', async () => {
    if (fulfillmentIds.length === 0) {
      console.log('  - SKIPPED: No fulfillments available');
      return;
    }
    
    const fulfillmentId = fulfillmentIds[0];
    const result = await makeRequest('GET', `/api/admin/fulfillments/${fulfillmentId}`);
    
    assert.ok(result.success, 'Request failed');
    assert.ok(result.data, 'Response data should exist');
    assert.strictEqual(result.data.id, fulfillmentId, 'Fulfillment ID mismatch');
    
    console.log(`  - Fulfillment ID: ${result.data.id}`);
    console.log(`  - Order ID: ${result.data.orderId}`);
    console.log(`  - Status: ${result.data.status}`);
    console.log(`  - Response time: ${result.duration}ms`);
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
