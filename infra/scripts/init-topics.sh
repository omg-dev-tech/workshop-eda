#!/bin/bash
set -e

BROKER="${KAFKA_BROKER:-kafka:9092}"
VERSION="${EVENT_VERSION:-v1}"

echo "🚀 Initializing Kafka topics..."
echo "📍 Broker: $BROKER"
echo "📍 Version: $VERSION"

# ================================================================
# Order Service 발행 토픽
# ================================================================
order_topics=(
  "order.${VERSION}.created"
  "order.${VERSION}.payment_validated"
  "order.${VERSION}.payment_rejected"
  "order.${VERSION}.inventory_check_requested"
  "order.${VERSION}.confirmed"
  "order.${VERSION}.cancelled"
  "order.${VERSION}.completed"
)

# ================================================================
# Inventory Service 발행 토픽
# ================================================================
inventory_topics=(
  "inventory.${VERSION}.reserved"
  "inventory.${VERSION}.released"
  "inventory.${VERSION}.reservation_failed"
  "inventory.${VERSION}.stock_updated"
)

# ================================================================
# Fulfillment Service 발행 토픽
# ================================================================
fulfillment_topics=(
  "fulfillment.${VERSION}.scheduled"
  "fulfillment.${VERSION}.picked"
  "fulfillment.${VERSION}.packed" 
  "fulfillment.${VERSION}.shipped"
  "fulfillment.${VERSION}.delivered"
  "fulfillment.${VERSION}.failed"
)

# ================================================================
# Analytics Service 발행 토픽 (분석 결과)
# ================================================================
analytics_topics=(
  "analytics.${VERSION}.sales_report_generated"
  "analytics.${VERSION}.inventory_report_generated"
  "analytics.${VERSION}.customer_report_generated"
)

# ================================================================
# Notification Service 발행 토픽 (알림 상태)
# ================================================================
notification_topics=(
  "notification.${VERSION}.email_sent"
  "notification.${VERSION}.sms_sent"
  "notification.${VERSION}.push_sent"
  "notification.${VERSION}.notification_failed"
)

# ================================================================
# Dead Letter Queues
# ================================================================
dlq_topics=(
  "dlq.order.${VERSION}"
  "dlq.inventory.${VERSION}"
  "dlq.fulfillment.${VERSION}"
  "dlq.analytics.${VERSION}"
  "dlq.notification.${VERSION}"
)

# ================================================================
# 모든 토픽 생성
# ================================================================
all_topics=(
  "${order_topics[@]}"
  "${inventory_topics[@]}"
  "${fulfillment_topics[@]}"
  "${analytics_topics[@]}"
  "${notification_topics[@]}"
  "${dlq_topics[@]}"
)

echo "📝 Creating ${#all_topics[@]} topics..."

for topic in "${all_topics[@]}"; do
  echo "  ✅ Creating: $topic"
  
  kafka-topics.sh --bootstrap-server "$BROKER" \
    --create --if-not-exists \
    --topic "$topic" \
    --partitions 3 \
    --replication-factor 1 \
    --config retention.ms=604800000 \
    --config segment.ms=86400000 \
    --config cleanup.policy=delete > /dev/null 2>&1
    
  if [ $? -eq 0 ]; then
    echo "     ✅ Success"
  else
    echo "     ❌ Failed"
    exit 1
  fi
done

echo ""
echo "🎉 Topic creation completed!"
echo "📊 Total topics created: ${#all_topics[@]}"
echo ""
echo "📋 Topic Summary:"
echo "  📦 Order topics: ${#order_topics[@]}"
echo "  📋 Inventory topics: ${#inventory_topics[@]}"
echo "  🚚 Fulfillment topics: ${#fulfillment_topics[@]}"
echo "  📊 Analytics topics: ${#analytics_topics[@]}"
echo "  📧 Notification topics: ${#notification_topics[@]}"
echo "  💀 DLQ topics: ${#dlq_topics[@]}"
echo ""

# 토픽 목록 확인
echo "🔍 Verifying topics..."
kafka-topics.sh --bootstrap-server "$BROKER" --list | grep -E "\\.${VERSION}\\." | sort

echo "✨ All topics ready!"
