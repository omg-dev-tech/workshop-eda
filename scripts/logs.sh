#!/bin/bash

SERVICE_NAME=${1:-}
FOLLOW=${2:-false}

if [ -z "$SERVICE_NAME" ]; then
    echo "📝 EDA Workshop Logs"
    echo "==================="
    echo ""
    echo "Usage: $0 <service-name> [follow]"
    echo ""
    echo "Available services:"
    echo "  🌐 api-gateway"
    echo "  📋 order-service"
    echo "  📦 inventory-service"
    echo "  🚚 fulfillment-service"
    echo "  📊 analytics-service"
    echo "  📧 notification-service"
    echo "  💳 payment-simulator"
    echo "  📨 kafka"
    echo "  🗄️  order-db"
    echo "  🗄️  inventory-db"
    echo "  🗄️  fulfillment-db"
    echo "  🗄️  analytics-db"
    echo "  🗄️  notification-db"
    echo ""
    echo "Examples:"
    echo "  $0 order-service        # Show recent logs"
    echo "  $0 order-service follow # Follow logs in real-time"
    echo "  $0 all                  # Show all services logs"
    exit 1
fi

if [ "$SERVICE_NAME" == "all" ]; then
    if [ "$FOLLOW" == "follow" ]; then
        docker-compose logs -f
    else
        docker-compose logs --tail=100
    fi
elif [ "$FOLLOW" == "follow" ]; then
    docker-compose logs -f "$SERVICE_NAME"
else
    docker-compose logs --tail=100 "$SERVICE_NAME"
fi
