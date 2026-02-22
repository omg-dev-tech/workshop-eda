// API Gateway URL (config.js에서 동적으로 생성됨)
const API_BASE_URL = window.__CONFIG__ ? window.__CONFIG__.BASE_URL : 'http://localhost:8080';

// 현재 로그인한 사용자 정보
let currentUser = null;

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', () => {
    checkSession();
});

// 세션 체크
function checkSession() {
    const user = sessionStorage.getItem('user');
    if (user) {
        currentUser = JSON.parse(user);
        showScreen(currentUser.role);
    } else {
        showScreen('login');
    }
}

// 로그인
function login() {
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    // 간단한 인증 (실제로는 백엔드에서 처리해야 함)
    if (username === 'client' && password === 'client') {
        currentUser = { username: 'client', role: 'client' };
        sessionStorage.setItem('user', JSON.stringify(currentUser));
        showScreen('client');
        loadClientOrders();
    } else if (username === 'admin' && password === 'admin') {
        currentUser = { username: 'admin', role: 'admin' };
        sessionStorage.setItem('user', JSON.stringify(currentUser));
        showScreen('admin');
        loadAllOrders();
    } else {
        alert('아이디 또는 비밀번호가 올바르지 않습니다.');
    }
}

// 로그아웃
function logout() {
    currentUser = null;
    sessionStorage.removeItem('user');
    showScreen('login');
}

// 화면 전환
function showScreen(screen) {
    document.getElementById('loginScreen').classList.add('hidden');
    document.getElementById('clientScreen').classList.add('hidden');
    document.getElementById('adminScreen').classList.add('hidden');

    if (screen === 'login') {
        document.getElementById('loginScreen').classList.remove('hidden');
    } else if (screen === 'client') {
        document.getElementById('clientScreen').classList.remove('hidden');
        document.getElementById('clientUsername').textContent = `고객: ${currentUser.username}`;
    } else if (screen === 'admin') {
        document.getElementById('adminScreen').classList.remove('hidden');
        document.getElementById('adminUsername').textContent = `관리자: ${currentUser.username}`;
    }
}

// 탭 전환
function showTab(tabName) {
    // 모든 탭 버튼 비활성화
    document.querySelectorAll('.tab').forEach(tab => tab.classList.remove('active'));
    // 모든 탭 컨텐츠 숨기기
    document.querySelectorAll('.tab-content').forEach(content => content.classList.add('hidden'));

    // 선택된 탭 활성화
    event.target.classList.add('active');
    document.getElementById(tabName + 'Tab').classList.remove('hidden');

    // 데이터 로드
    if (tabName === 'orders') loadAllOrders();
    else if (tabName === 'inventory') loadInventory();
    else if (tabName === 'fulfillment') loadFulfillments();
    else if (tabName === 'analytics') loadAnalytics();
}

// 주문 생성 (고객)
async function createOrder(event) {
    event.preventDefault();
    
    const customerId = document.getElementById('customerId').value;
    const amount = parseInt(document.getElementById('amount').value);
    const sku = document.getElementById('sku').value;
    const qty = parseInt(document.getElementById('qty').value);

    const orderData = {
        customerId: customerId,
        amount: amount,
        currency: 'KRW',
        items: [{ sku: sku, qty: qty }]
    };

    try {
        const response = await fetch(`${API_BASE_URL}/api/orders`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(orderData)
        });

        if (response.ok) {
            const result = await response.json();
            alert(`주문이 생성되었습니다!\n주문 ID: ${result.orderId}`);
            document.getElementById('orderForm').reset();
            document.getElementById('customerId').value = 'customer-001';
            document.getElementById('amount').value = '50000';
            document.getElementById('sku').value = 'LAPTOP-001';
            document.getElementById('qty').value = '1';
            loadClientOrders();
        } else {
            alert('주문 생성에 실패했습니다.');
        }
    } catch (error) {
        console.error('Error:', error);
        alert('주문 생성 중 오류가 발생했습니다.');
    }
}

// 고객 주문 목록 조회
async function loadClientOrders() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/orders`);
        const orders = await response.json();
        
        // 최근 10개만 표시
        const recentOrders = orders.slice(0, 10);
        displayOrders(recentOrders, 'clientOrders');
    } catch (error) {
        console.error('Error:', error);
        document.getElementById('clientOrders').innerHTML = '<p class="error">주문 목록을 불러올 수 없습니다.</p>';
    }
}

// 전체 주문 목록 조회 (관리자)
async function loadAllOrders() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/orders`);
        const orders = await response.json();
        displayOrders(orders, 'allOrders');
    } catch (error) {
        console.error('Error:', error);
        document.getElementById('allOrders').innerHTML = '<p class="error">주문 목록을 불러올 수 없습니다.</p>';
    }
}

// 주문 목록 표시
function displayOrders(orders, containerId) {
    const container = document.getElementById(containerId);
    const isAdmin = containerId === 'allOrders';
    
    if (orders.length === 0) {
        container.innerHTML = '<p class="empty">주문이 없습니다.</p>';
        return;
    }

    const html = orders.map(order => {
        // 날짜 포맷팅 처리
        let createdAtText = '날짜 정보 없음';
        if (order.createdAt) {
            try {
                const date = new Date(order.createdAt);
                if (!isNaN(date.getTime())) {
                    createdAtText = date.toLocaleString('ko-KR', {
                        year: 'numeric',
                        month: '2-digit',
                        day: '2-digit',
                        hour: '2-digit',
                        minute: '2-digit',
                        second: '2-digit'
                    });
                }
            } catch (e) {
                console.error('Date parsing error:', e);
            }
        }
        
        // 재처리 버튼 (관리자 화면 + 재처리 가능한 상태)
        const retryableStatuses = ['PENDING', 'INVENTORY_REJECTED', 'PAYMENT_FAILED', 'INVENTORY_RESERVED'];
        const retryButton = isAdmin && retryableStatuses.includes(order.status)
            ? `<button onclick="retryOrder('${order.id}')" class="btn-small btn-warning">🔄 재처리</button>`
            : '';
        
        return `
            <div class="order-item">
                <div class="order-header">
                    <span class="order-id">주문 #${order.id.substring(0, 8)}</span>
                    <span class="status status-${order.status.toLowerCase()}">${getStatusText(order.status)}</span>
                </div>
                <div class="order-details">
                    <p>고객: ${order.customerId}</p>
                    <p>금액: ${order.amount.toLocaleString()} ${order.currency}</p>
                    <p>생성일: ${createdAtText}</p>
                    ${retryButton ? `<div class="order-actions">${retryButton}</div>` : ''}
                </div>
            </div>
        `;
    }).join('');

    container.innerHTML = html;
}

// 상태 텍스트 변환
function getStatusText(status) {
    const statusMap = {
        'PENDING': '대기중',
        'INVENTORY_RESERVED': '재고확보',
        'INVENTORY_REJECTED': '재고부족',
        'PAYMENT_FAILED': '결제실패',
        'COMPLETED': '완료',
        'FAILED': '실패'
    };
    return statusMap[status] || status;
}

// 주문 재처리
async function retryOrder(orderId) {
    if (!confirm('이 주문을 재처리하시겠습니까?\n재고가 충분한지 확인해주세요.')) return;

    try {
        const response = await fetch(`${API_BASE_URL}/api/orders/${orderId}/retry`, {
            method: 'POST'
        });

        if (response.ok) {
            alert('주문이 재처리되었습니다!\n재고 확인 후 결제가 진행됩니다.');
            loadAllOrders();
        } else {
            const error = await response.text();
            alert(`재처리 실패: ${error}`);
        }
    } catch (error) {
        console.error('Error:', error);
        alert('주문 재처리 중 오류가 발생했습니다.');
    }
}

// 재고 추가
async function addInventory(event) {
    event.preventDefault();
    
    const sku = document.getElementById('invSku').value;
    const productName = document.getElementById('invName').value;
    const qty = parseInt(document.getElementById('invQty').value);

    const inventoryData = {
        sku: sku,
        productName: productName,
        qty: qty
    };

    try {
        const response = await fetch(`${API_BASE_URL}/api/admin/inventory`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(inventoryData)
        });

        if (response.ok) {
            alert('재고가 추가되었습니다!');
            document.getElementById('inventoryForm').reset();
            loadInventory();
        } else {
            alert('재고 추가에 실패했습니다.');
        }
    } catch (error) {
        console.error('Error:', error);
        alert('재고 추가 중 오류가 발생했습니다.');
    }
}

// 재고 목록 조회
async function loadInventory() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/admin/inventory`);
        const inventory = await response.json();
        displayInventory(inventory);
    } catch (error) {
        console.error('Error:', error);
        document.getElementById('inventoryList').innerHTML = '<p class="error">재고 목록을 불러올 수 없습니다.</p>';
    }
}

// 재고 목록 표시
function displayInventory(inventory) {
    const container = document.getElementById('inventoryList');
    
    if (inventory.length === 0) {
        container.innerHTML = '<p class="empty">재고가 없습니다.</p>';
        return;
    }

    const html = `
        <table>
            <thead>
                <tr>
                    <th>SKU</th>
                    <th>상품명</th>
                    <th>수량</th>
                    <th>작업</th>
                </tr>
            </thead>
            <tbody>
                ${inventory.map(item => `
                    <tr>
                        <td>${item.sku}</td>
                        <td>${item.productName}</td>
                        <td>${item.qty}</td>
                        <td>
                            <button onclick="updateInventory('${item.sku}', ${item.qty})" class="btn-small">수정</button>
                            <button onclick="deleteInventory('${item.sku}')" class="btn-small btn-danger">삭제</button>
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;

    container.innerHTML = html;
}

// 재고 수정
async function updateInventory(sku, currentQty) {
    const newQty = prompt(`새 수량을 입력하세요 (현재: ${currentQty}):`, currentQty);
    if (newQty === null) return;

    try {
        const response = await fetch(`${API_BASE_URL}/api/admin/inventory/${sku}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ qty: parseInt(newQty) })
        });

        if (response.ok) {
            alert('재고가 수정되었습니다!');
            loadInventory();
        } else {
            alert('재고 수정에 실패했습니다.');
        }
    } catch (error) {
        console.error('Error:', error);
        alert('재고 수정 중 오류가 발생했습니다.');
    }
}

// 재고 삭제
async function deleteInventory(sku) {
    if (!confirm(`정말 ${sku} 재고를 삭제하시겠습니까?`)) return;

    try {
        const response = await fetch(`${API_BASE_URL}/api/admin/inventory/${sku}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            alert('재고가 삭제되었습니다!');
            loadInventory();
        } else {
            alert('재고 삭제에 실패했습니다.');
        }
    } catch (error) {
        console.error('Error:', error);
        alert('재고 삭제 중 오류가 발생했습니다.');
    }
}

// 배송 목록 조회
async function loadFulfillments() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/admin/fulfillments`);
        const fulfillments = await response.json();
        displayFulfillments(fulfillments);
    } catch (error) {
        console.error('Error:', error);
        document.getElementById('fulfillmentList').innerHTML = '<p class="error">배송 목록을 불러올 수 없습니다.</p>';
    }
}

// 배송 목록 표시
function displayFulfillments(fulfillments) {
    const container = document.getElementById('fulfillmentList');
    
    if (fulfillments.length === 0) {
        container.innerHTML = '<p class="empty">배송 정보가 없습니다.</p>';
        return;
    }

    const html = `
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>주문 ID</th>
                    <th>배송 ID</th>
                    <th>상태</th>
                    <th>작업</th>
                </tr>
            </thead>
            <tbody>
                ${fulfillments.map(item => `
                    <tr>
                        <td>${item.id}</td>
                        <td>${item.orderId.substring(0, 8)}</td>
                        <td>${item.shippingId || '-'}</td>
                        <td><span class="status status-${item.status.toLowerCase()}">${item.status}</span></td>
                        <td>
                            ${item.status === 'SCHEDULED' ? 
                                `<button onclick="shipFulfillment(${item.id})" class="btn-small">배송처리</button>` : 
                                '-'}
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;

    container.innerHTML = html;
}

// 배송 처리
async function shipFulfillment(id) {
    if (!confirm('배송 처리하시겠습니까?')) return;

    try {
        const response = await fetch(`${API_BASE_URL}/api/admin/fulfillments/${id}/ship`, {
            method: 'PUT'
        });

        if (response.ok) {
            alert('배송 처리되었습니다!');
            loadFulfillments();
        } else {
            alert('배송 처리에 실패했습니다.');
        }
    } catch (error) {
        console.error('Error:', error);
        alert('배송 처리 중 오류가 발생했습니다.');
    }
}

// 분석 데이터 조회
async function loadAnalytics() {
    try {
        // 이벤트 수
        const eventsResponse = await fetch(`${API_BASE_URL}/api/admin/analytics/events/count`);
        const eventsData = await eventsResponse.json();
        document.getElementById('totalEvents').textContent = eventsData.count.toLocaleString();

        // 오늘 주문 통계 (로컬 시간 기준)
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        const today = `${year}-${month}-${day}`;
        const summaryResponse = await fetch(`${API_BASE_URL}/api/admin/analytics/summary?date=${today}`);
        const summaryData = await summaryResponse.json();
        document.getElementById('todayOrders').textContent = summaryData.totalOrders.toLocaleString();
        document.getElementById('todayRevenue').textContent = summaryData.totalAmount.toLocaleString() + ' KRW';
    } catch (error) {
        console.error('Error:', error);
        document.getElementById('totalEvents').textContent = '오류';
        document.getElementById('todayOrders').textContent = '오류';
        document.getElementById('todayRevenue').textContent = '오류';
    }
}

// Made with Bob
