import os
import time
import random
import requests
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Dict, List

# 환경변수
API_GATEWAY_URL = os.getenv('API_GATEWAY_URL', 'http://api-gateway:8080')
TPS = int(os.getenv('TPS', '10'))  # Transactions Per Second
DURATION = int(os.getenv('DURATION', '60'))  # seconds
WORKERS = int(os.getenv('WORKERS', '10'))
REPLENISH_INTERVAL = int(os.getenv('REPLENISH_INTERVAL', '30'))  # 재고 보충 주기 (초)
SHIP_INTERVAL = int(os.getenv('SHIP_INTERVAL', '20'))  # 배송 처리 주기 (초)
REPLENISH_QTY = int(os.getenv('REPLENISH_QTY', '100'))  # 재고 보충 수량

# 샘플 데이터
PRODUCTS = ['product-001', 'product-002', 'product-003', 'LAPTOP-001']
CUSTOMERS = ['customer-001', 'customer-002', 'customer-003']

# 통계
stats = {
    'orders_success': 0,
    'orders_failure': 0,
    'replenish_success': 0,
    'replenish_failure': 0,
    'ship_success': 0,
    'ship_failure': 0
}

def create_order() -> bool:
    """주문 생성"""
    payload = {
        'customerId': random.choice(CUSTOMERS),
        'productId': random.choice(PRODUCTS),
        'quantity': random.randint(1, 3)
    }
    
    try:
        response = requests.post(
            f'{API_GATEWAY_URL}/api/orders',
            json=payload,
            timeout=5
        )
        return response.status_code in [200, 201]
    except Exception as e:
        print(f"[Order Error] {e}")
        return False

def replenish_inventory() -> int:
    """재고 보충 - 모든 상품의 재고를 확인하고 부족하면 보충"""
    success_count = 0
    
    try:
        # 현재 재고 조회
        response = requests.get(
            f'{API_GATEWAY_URL}/api/admin/inventory',
            timeout=5
        )
        
        if response.status_code != 200:
            print(f"[Replenish] Failed to get inventory list")
            return 0
        
        inventories = response.json()
        
        for inv in inventories:
            sku = inv.get('sku')
            current_qty = inv.get('qty', 0)
            
            # 재고가 50개 미만이면 보충
            if current_qty < 50:
                new_qty = current_qty + REPLENISH_QTY
                update_response = requests.put(
                    f'{API_GATEWAY_URL}/api/admin/inventory/{sku}',
                    json={'qty': new_qty},
                    timeout=5
                )
                
                if update_response.status_code == 200:
                    print(f"[Replenish] {sku}: {current_qty} -> {new_qty}")
                    success_count += 1
                else:
                    print(f"[Replenish] Failed to update {sku}")
        
        return success_count
        
    except Exception as e:
        print(f"[Replenish Error] {e}")
        return 0

def ship_scheduled_fulfillments() -> int:
    """SCHEDULED 상태인 배송 처리"""
    success_count = 0
    
    try:
        # 모든 fulfillment 조회
        response = requests.get(
            f'{API_GATEWAY_URL}/api/admin/fulfillments',
            timeout=5
        )
        
        if response.status_code != 200:
            print(f"[Ship] Failed to get fulfillment list")
            return 0
        
        fulfillments = response.json()
        
        # SCHEDULED 상태인 것들만 배송 처리
        for fulfillment in fulfillments:
            if fulfillment.get('status') == 'SCHEDULED':
                fulfillment_id = fulfillment.get('id')
                
                ship_response = requests.put(
                    f'{API_GATEWAY_URL}/api/admin/fulfillments/{fulfillment_id}/ship',
                    timeout=5
                )
                
                if ship_response.status_code == 200:
                    print(f"[Ship] Fulfillment #{fulfillment_id} shipped (Order: {fulfillment.get('orderId')})")
                    success_count += 1
                else:
                    print(f"[Ship] Failed to ship fulfillment #{fulfillment_id}")
        
        return success_count
        
    except Exception as e:
        print(f"[Ship Error] {e}")
        return 0

def background_tasks(start_time: float, end_time: float):
    """백그라운드 작업 (재고 보충 및 배송 처리)"""
    last_replenish = start_time
    last_ship = start_time
    
    while time.time() < end_time:
        current_time = time.time()
        
        # 재고 보충
        if current_time - last_replenish >= REPLENISH_INTERVAL:
            replenish_count = replenish_inventory()
            if replenish_count > 0:
                stats['replenish_success'] += replenish_count
            else:
                stats['replenish_failure'] += 1
            last_replenish = current_time
        
        # 배송 처리
        if current_time - last_ship >= SHIP_INTERVAL:
            ship_count = ship_scheduled_fulfillments()
            if ship_count > 0:
                stats['ship_success'] += ship_count
            else:
                stats['ship_failure'] += 1
            last_ship = current_time
        
        time.sleep(1)

def run_load_test():
    """부하 테스트 실행"""
    print("=" * 60)
    print("=== Load Generator Started ===")
    print(f"API Gateway URL: {API_GATEWAY_URL}")
    print(f"Target TPS: {TPS}")
    print(f"Duration: {DURATION} seconds")
    print(f"Workers: {WORKERS}")
    print(f"Replenish Interval: {REPLENISH_INTERVAL}s (Qty: {REPLENISH_QTY})")
    print(f"Ship Interval: {SHIP_INTERVAL}s")
    print(f"Start Time: {datetime.now().isoformat()}")
    print("=" * 60)
    
    start_time = time.time()
    end_time = start_time + DURATION
    
    # 백그라운드 작업 시작 (재고 보충 및 배송 처리)
    with ThreadPoolExecutor(max_workers=WORKERS + 1) as executor:
        # 백그라운드 작업 스레드
        bg_future = executor.submit(background_tasks, start_time, end_time)
        
        # 주문 생성 작업
        total_requests = 0
        last_report_time = start_time
        
        while time.time() < end_time:
            batch_start = time.time()
            
            # TPS만큼 요청 생성
            futures = [executor.submit(create_order) for _ in range(TPS)]
            
            # 결과 수집
            for future in as_completed(futures):
                total_requests += 1
                if future.result():
                    stats['orders_success'] += 1
                else:
                    stats['orders_failure'] += 1
            
            # 진행 상황 출력 (10초마다)
            current_time = time.time()
            if current_time - last_report_time >= 10:
                elapsed = current_time - start_time
                current_tps = total_requests / elapsed
                print(f"[{int(elapsed)}s] Orders: {total_requests} (Success: {stats['orders_success']}, Fail: {stats['orders_failure']}), TPS: {current_tps:.2f}")
                last_report_time = current_time
            
            # 1초 대기 (TPS 조절)
            batch_elapsed = time.time() - batch_start
            if batch_elapsed < 1.0:
                time.sleep(1.0 - batch_elapsed)
        
        # 백그라운드 작업 완료 대기
        bg_future.result()
    
    # 최종 통계 출력
    actual_duration = time.time() - start_time
    total_orders = stats['orders_success'] + stats['orders_failure']
    
    print("\n" + "=" * 60)
    print("=== Load Test Results ===")
    print(f"End Time: {datetime.now().isoformat()}")
    print(f"Duration: {actual_duration:.2f}s")
    print()
    print("--- Order Creation ---")
    print(f"Total Requests: {total_orders}")
    print(f"Success: {stats['orders_success']} ({stats['orders_success']/total_orders*100:.2f}%)")
    print(f"Failure: {stats['orders_failure']} ({stats['orders_failure']/total_orders*100:.2f}%)")
    print(f"Actual TPS: {total_orders/actual_duration:.2f}")
    print(f"Target TPS: {TPS}")
    print()
    print("--- Inventory Replenishment ---")
    print(f"Success: {stats['replenish_success']} items replenished")
    print(f"Failure: {stats['replenish_failure']} attempts failed")
    print()
    print("--- Fulfillment Shipping ---")
    print(f"Success: {stats['ship_success']} fulfillments shipped")
    print(f"Failure: {stats['ship_failure']} attempts failed")
    print("=" * 60)

if __name__ == '__main__':
    run_load_test()

# Made with Bob
