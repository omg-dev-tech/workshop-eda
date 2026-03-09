#!/bin/bash

# Apply database migration to OpenShift PostgreSQL
# Usage: ./scripts/apply-db-migration.sh <migration-file>

set -e

MIGRATION_FILE=${1:-"infra/sql/analytics-migration-add-event-count-summary.sql"}
NAMESPACE=${NAMESPACE:-"workshop-eda"}

# Get PostgreSQL pod using correct label
echo "🔍 Searching for PostgreSQL pod in namespace: $NAMESPACE"
DB_POD=$(oc get pods -n $NAMESPACE -l deploymentconfig=analytics-db -o jsonpath='{.items[0].metadata.name}')

# Check if pod was found
if [ -z "$DB_POD" ]; then
    echo "❌ Error: PostgreSQL pod not found in namespace $NAMESPACE"
    echo "   Please check if the pod exists with: oc get pods -n $NAMESPACE -l deploymentconfig=analytics-db"
    exit 1
fi

# Check if pod is running
POD_STATUS=$(oc get pod $DB_POD -n $NAMESPACE -o jsonpath='{.status.phase}')
if [ "$POD_STATUS" != "Running" ]; then
    echo "❌ Error: Pod $DB_POD is not in Running state (current: $POD_STATUS)"
    echo "   Please check pod status with: oc describe pod $DB_POD -n $NAMESPACE"
    exit 1
fi

echo "✅ Found PostgreSQL pod: $DB_POD (Status: $POD_STATUS)"
echo "📄 Applying migration: $MIGRATION_FILE"

# Copy migration file to pod
oc cp $MIGRATION_FILE $NAMESPACE/$DB_POD:/tmp/migration.sql

# Execute migration
echo "⚙️  Executing migration..."
oc exec -n $NAMESPACE $DB_POD -- psql -U analytics -d analyticsdb -f /tmp/migration.sql

echo "✅ Migration completed successfully!"

# Verify table exists
echo "🔍 Verifying table creation..."
oc exec -n $NAMESPACE $DB_POD -- psql -U analytics -d analyticsdb -c "\d event_count_summary"

echo "📊 Current event counts:"
oc exec -n $NAMESPACE $DB_POD -- psql -U analytics -d analyticsdb -c "SELECT * FROM event_count_summary ORDER BY count DESC;"

# Made with Bob
