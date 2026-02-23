#!/bin/bash

################################################################################
# OpenShift Rollback Script
# 
# This script performs automatic rollback of Helm releases on OpenShift
# 
# Usage:
#   ./scripts/rollback-ocp.sh --project <name> --release <name> --revision <num>
#
################################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
PROJECT_NAME=""
RELEASE_NAME=""
REVISION=""
TIMEOUT="5m"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --project)
            PROJECT_NAME="$2"
            shift 2
            ;;
        --release)
            RELEASE_NAME="$2"
            shift 2
            ;;
        --revision)
            REVISION="$2"
            shift 2
            ;;
        --timeout)
            TIMEOUT="$2"
            shift 2
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Validate required parameters
if [ -z "$PROJECT_NAME" ] || [ -z "$RELEASE_NAME" ]; then
    echo -e "${RED}Error: Missing required parameters${NC}"
    echo "Usage: $0 --project <name> --release <name> [--revision <num>]"
    exit 1
fi

echo -e "${BLUE}=== OpenShift Rollback ===${NC}"
echo "Project: ${PROJECT_NAME}"
echo "Release: ${RELEASE_NAME}"
echo "Target Revision: ${REVISION:-previous}"
echo ""

################################################################################
# Function: Log messages
################################################################################
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

################################################################################
# Function: Check if Helm release exists
################################################################################
check_release_exists() {
    log_info "Checking if Helm release exists..."
    
    if ! helm list -n "${PROJECT_NAME}" | grep -q "${RELEASE_NAME}"; then
        log_error "Helm release '${RELEASE_NAME}' not found in project '${PROJECT_NAME}'"
        exit 1
    fi
    
    log_success "Helm release found"
}

################################################################################
# Function: Get current revision
################################################################################
get_current_revision() {
    log_info "Getting current revision..."
    
    CURRENT_REVISION=$(helm list -n "${PROJECT_NAME}" | grep "${RELEASE_NAME}" | awk '{print $3}')
    
    if [ -z "$CURRENT_REVISION" ]; then
        log_error "Could not determine current revision"
        exit 1
    fi
    
    log_info "Current revision: ${CURRENT_REVISION}"
    echo "$CURRENT_REVISION"
}

################################################################################
# Function: Perform rollback
################################################################################
perform_rollback() {
    log_info "Starting rollback..."
    
    if [ -n "$REVISION" ] && [ "$REVISION" != "0" ]; then
        # Rollback to specific revision
        log_info "Rolling back to revision ${REVISION}..."
        
        if helm rollback "${RELEASE_NAME}" "${REVISION}" -n "${PROJECT_NAME}" --wait --timeout="${TIMEOUT}"; then
            log_success "Rollback to revision ${REVISION} completed"
        else
            log_error "Rollback to revision ${REVISION} failed"
            exit 1
        fi
    else
        # Rollback to previous revision
        log_info "Rolling back to previous revision..."
        
        if helm rollback "${RELEASE_NAME}" -n "${PROJECT_NAME}" --wait --timeout="${TIMEOUT}"; then
            log_success "Rollback to previous revision completed"
        else
            log_error "Rollback to previous revision failed"
            exit 1
        fi
    fi
}

################################################################################
# Function: Verify rollback
################################################################################
verify_rollback() {
    log_info "Verifying rollback..."
    
    # Wait a bit for pods to stabilize
    sleep 10
    
    # Check pod status
    log_info "Checking pod status..."
    
    NOT_RUNNING=$(oc get pods -n "${PROJECT_NAME}" --field-selector=status.phase!=Running --no-headers 2>/dev/null | wc -l)
    
    if [ "$NOT_RUNNING" -gt 0 ]; then
        log_warning "Some pods are not running after rollback:"
        oc get pods -n "${PROJECT_NAME}" --field-selector=status.phase!=Running
        
        # Wait a bit more
        log_info "Waiting 30s for pods to stabilize..."
        sleep 30
        
        NOT_RUNNING=$(oc get pods -n "${PROJECT_NAME}" --field-selector=status.phase!=Running --no-headers 2>/dev/null | wc -l)
        
        if [ "$NOT_RUNNING" -gt 0 ]; then
            log_error "Rollback verification failed - pods not running"
            return 1
        fi
    fi
    
    log_success "All pods are running"
    
    # Check deployment status
    log_info "Checking deployment status..."
    
    DEPLOYMENTS=(
        "order-service"
        "inventory-service"
        "fulfillment-service"
        "payment-adapter-ext"
        "analytics-service"
        "api-gateway"
        "web"
    )
    
    for deployment in "${DEPLOYMENTS[@]}"; do
        if oc get deployment "${deployment}" -n "${PROJECT_NAME}" &> /dev/null; then
            log_info "Checking ${deployment}..."
            
            if ! oc rollout status deployment/"${deployment}" -n "${PROJECT_NAME}" --timeout=2m; then
                log_warning "${deployment} rollout status check failed (non-critical)"
            fi
        fi
    done
    
    log_success "Rollback verification completed"
}

################################################################################
# Function: Print rollback summary
################################################################################
print_summary() {
    echo ""
    echo "========================================"
    log_success "Rollback Completed"
    echo "========================================"
    echo ""
    log_info "Project: ${PROJECT_NAME}"
    log_info "Release: ${RELEASE_NAME}"
    
    NEW_REVISION=$(get_current_revision)
    log_info "Current Revision: ${NEW_REVISION}"
    
    echo ""
    log_info "Pod Status:"
    oc get pods -n "${PROJECT_NAME}"
    
    echo ""
    log_info "Helm Release History:"
    helm history "${RELEASE_NAME}" -n "${PROJECT_NAME}" --max 5
    
    echo ""
}

################################################################################
# Main Execution
################################################################################

main() {
    log_info "Starting rollback process..."
    echo ""
    
    # 1. Check if release exists
    check_release_exists
    echo ""
    
    # 2. Get current revision
    CURRENT_REV=$(get_current_revision)
    echo ""
    
    # 3. Perform rollback
    perform_rollback
    echo ""
    
    # 4. Verify rollback
    if verify_rollback; then
        echo ""
        # 5. Print summary
        print_summary
        exit 0
    else
        log_error "Rollback verification failed"
        echo ""
        log_info "Current pod status:"
        oc get pods -n "${PROJECT_NAME}"
        exit 1
    fi
}

# Run main function
main

# Made with Bob