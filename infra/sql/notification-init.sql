-- =================================================================
-- Notification Service Database Schema
-- =================================================================

-- 알림 템플릿 테이블
CREATE TABLE IF NOT EXISTS notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_name VARCHAR(100) NOT NULL UNIQUE,
    template_type VARCHAR(20) NOT NULL, -- EMAIL, SMS, PUSH
    subject VARCHAR(255),
    content TEXT NOT NULL,
    variables JSONB, -- 템플릿에서 사용할 변수들
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_templates_name_type ON notification_templates(template_name, template_type);

-- 알림 발송 이력 테이블
CREATE TABLE IF NOT EXISTS notification_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient VARCHAR(255) NOT NULL,
    notification_type VARCHAR(20) NOT NULL, -- EMAIL, SMS, PUSH
    template_name VARCHAR(100) NOT NULL,
    subject VARCHAR(255),
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SENT, FAILED
    error_message TEXT,
    order_id VARCHAR(255),
    customer_id VARCHAR(255),
    sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_history_recipient ON notification_history(recipient);
CREATE INDEX IF NOT EXISTS idx_notification_history_status ON notification_history(status);
CREATE INDEX IF NOT EXISTS idx_notification_history_order_id ON notification_history(order_id);
CREATE INDEX IF NOT EXISTS idx_notification_history_created_at ON notification_history(created_at);

-- 고객 알림 설정 테이블
CREATE TABLE IF NOT EXISTS customer_notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id VARCHAR(255) NOT NULL UNIQUE,
    email_enabled BOOLEAN DEFAULT true,
    sms_enabled BOOLEAN DEFAULT false,
    push_enabled BOOLEAN DEFAULT true,
    email_address VARCHAR(255),
    phone_number VARCHAR(20),
    push_token VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_customer_prefs_customer_id ON customer_notification_preferences(customer_id);

-- 이벤트 처리 상태 추적
CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 알림 템플릿 기본 데이터
INSERT INTO notification_templates (template_name, template_type, subject, content, variables) VALUES
('order_created', 'EMAIL', '주문이 접수되었습니다', 
 '안녕하세요 {{customerName}}님,<br><br>주문번호 {{orderId}}가 성공적으로 접수되었습니다.<br>총 결제금액: {{totalAmount}}원<br><br>감사합니다.',
 '{"customerName": "string", "orderId": "string", "totalAmount": "number"}'),

('payment_validated', 'EMAIL', '결제가 완료되었습니다',
 '안녕하세요 {{customerName}}님,<br><br>주문번호 {{orderId}}의 결제가 완료되었습니다.<br>결제금액: {{paymentAmount}}원<br>결제수단: {{paymentMethod}}<br><br>상품 준비 중이니 조금만 기다려 주세요.',
 '{"customerName": "string", "orderId": "string", "paymentAmount": "number", "paymentMethod": "string"}'),

('order_confirmed', 'EMAIL', '주문이 확정되었습니다',
 '안녕하세요 {{customerName}}님,<br><br>주문번호 {{orderId}}가 확정되었습니다.<br>예상 배송일: {{estimatedDelivery}}<br><br>배송 준비가 완료되면 다시 알려드리겠습니다.',
 '{"customerName": "string", "orderId": "string", "estimatedDelivery": "string"}'),

('fulfillment_shipped', 'EMAIL', '상품이 발송되었습니다',
 '안녕하세요 {{customerName}}님,<br><br>주문번호 {{orderId}}의 상품이 발송되었습니다.<br>택배사: {{carrier}}<br>송장번호: {{trackingNumber}}<br><br>배송 조회를 통해 실시간으로 확인하실 수 있습니다.',
 '{"customerName": "string", "orderId": "string", "carrier": "string", "trackingNumber": "string"}'),

('fulfillment_delivered', 'EMAIL', '배송이 완료되었습니다',
 '안녕하세요 {{customerName}}님,<br><br>주문번호 {{orderId}}의 배송이 완료되었습니다.<br><br>상품은 만족하셨나요? 리뷰를 남겨주시면 다른 고객들에게 많은 도움이 됩니다.',
 '{"customerName": "string", "orderId": "string"}'),

-- SMS 템플릿
('payment_validated', 'SMS', '', 
 '[EDA-Workshop] {{customerName}}님, 주문번호 {{orderId}} 결제완료({{paymentAmount}}원). 상품준비중입니다.',
 '{"customerName": "string", "orderId": "string", "paymentAmount": "number"}'),

('fulfillment_shipped', 'SMS', '',
 '[EDA-Workshop] {{customerName}}님, 주문번호 {{orderId}} 발송완료. 송장번호: {{trackingNumber}}',
 '{"customerName": "string", "orderId": "string", "trackingNumber": "string"}')

ON CONFLICT (template_name, template_type) DO NOTHING;

-- 기본 고객 알림 설정 (테스트용)
INSERT INTO customer_notification_preferences (customer_id, email_address, phone_number) VALUES
('CUST-001', 'customer1@example.com', '010-1234-5678'),
('CUST-002', 'customer2@example.com', '010-2345-6789'),
('CUST-003', 'customer3@example.com', '010-3456-7890')
ON CONFLICT (customer_id) DO NOTHING;
