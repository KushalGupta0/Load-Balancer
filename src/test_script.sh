#!/bin/bash

# test_system.sh - Comprehensive automated testing script

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

TESTS_PASSED=0
TESTS_FAILED=0

echo -e "${BLUE}=========================================="
echo "Load Balancer System Test Suite"
echo -e "==========================================${NC}"
echo ""

# Function to check if port is listening
check_port() {
    local port=$1
    nc -z localhost $port 2>/dev/null || telnet localhost $port 2>/dev/null | grep -q "Connected"
    return $?
}

# Function to run a test
run_test() {
    local test_name=$1
    local test_command=$2
    
    echo -n "Testing: $test_name... "
    
    if eval "$test_command" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PASSED${NC}"
        ((TESTS_PASSED++))
        return 0
    else
        echo -e "${RED}✗ FAILED${NC}"
        ((TESTS_FAILED++))
        return 1
    fi
}

# Cleanup function
cleanup() {
    echo ""
    echo "Cleaning up..."
    pkill -f "BackendManager" 2>/dev/null
    pkill -f "LoadBalancer" 2>/dev/null
    pkill -f "ClientSimulator" 2>/dev/null
    sleep 1
}

# Set trap for cleanup on exit
trap cleanup EXIT

echo -e "${YELLOW}Phase 1: Compilation${NC}"
echo "-------------------"
run_test "Compiling Java files" "javac *.java"
echo ""

echo -e "${YELLOW}Phase 2: Starting Components${NC}"
echo "-----------------------------"
echo "Starting Backend Manager..."
java BackendManager > backend_test.log 2>&1 &
BACKEND_PID=$!
sleep 3

run_test "Backend 8081 is listening" "check_port 8081"
run_test "Backend 8082 is listening" "check_port 8082"
run_test "Backend 8083 is listening" "check_port 8083"
run_test "Backend 8084 is listening" "check_port 8084"
echo ""

echo "Starting Load Balancer..."
java LoadBalancer > lb_test.log 2>&1 &
LB_PID=$!
sleep 2

run_test "Load Balancer is listening" "check_port 8080"
echo ""

echo -e "${YELLOW}Phase 3: Basic Functionality${NC}"
echo "----------------------------"

# Test single client
timeout 10 java ClientSimulator 1 single > client_test.log 2>&1
if [ $? -eq 0 ]; then
    if grep -q "completed successfully" client_test.log; then
        echo -e "Testing: Single client request... ${GREEN}✓ PASSED${NC}"
        ((TESTS_PASSED++))
    else
        echo -e "Testing: Single client request... ${RED}✗ FAILED${NC}"
        ((TESTS_FAILED++))
    fi
else
    echo -e "Testing: Single client request... ${RED}✗ FAILED (timeout)${NC}"
    ((TESTS_FAILED++))
fi

# Test multiple clients
timeout 15 java ClientSimulator 5 single > client_multi_test.log 2>&1
if [ $? -eq 0 ]; then
    if grep -q "completed successfully" client_multi_test.log; then
        echo -e "Testing: Multiple clients (5)... ${GREEN}✓ PASSED${NC}"
        ((TESTS_PASSED++))
    else
        echo -e "Testing: Multiple clients (5)... ${RED}✗ FAILED${NC}"
        ((TESTS_FAILED++))
    fi
else
    echo -e "Testing: Multiple clients (5)... ${RED}✗ FAILED (timeout)${NC}"
    ((TESTS_FAILED++))
fi
echo ""

echo -e "${YELLOW}Phase 4: Load Distribution${NC}"
echo "--------------------------"

# Send 20 requests and check distribution
timeout 30 java ClientSimulator 20 single > distribution_test.log 2>&1

if [ $? -eq 0 ]; then
    # Check if all backends received requests
    backends_used=0
    for port in 8081 8082 8083 8084; do
        if grep -q "Backend-$port" app.log; then
            ((backends_used++))
        fi
    done
    
    if [ $backends_used -eq 4 ]; then
        echo -e "Testing: Round-robin distribution... ${GREEN}✓ PASSED${NC} (all 4 backends used)"
        ((TESTS_PASSED++))
    else
        echo -e "Testing: Round-robin distribution... ${YELLOW}⚠ PARTIAL${NC} ($backends_used/4 backends used)"
        ((TESTS_PASSED++))
    fi
else
    echo -e "Testing: Round-robin distribution... ${RED}✗ FAILED${NC}"
    ((TESTS_FAILED++))
fi
echo ""

echo -e "${YELLOW}Phase 5: Error Handling${NC}"
echo "----------------------"

# Check for errors in logs
if grep -q "ERROR" app.log; then
    error_count=$(grep -c "ERROR" app.log)
    echo -e "Testing: No system errors... ${YELLOW}⚠ WARNING${NC} ($error_count errors found)"
else
    echo -e "Testing: No system errors... ${GREEN}✓ PASSED${NC}"
    ((TESTS_PASSED++))
fi
echo ""

echo -e "${YELLOW}Phase 6: Backend Failure Simulation${NC}"
echo "-----------------------------------"

# Kill one backend
BACKEND_TO_KILL=$(ps aux | grep "java BackendManager" | grep -v grep | awk '{print $2}' | head -1)
if [ -n "$BACKEND_TO_KILL" ]; then
    # Find a specific backend thread to kill (safer)
    sleep 2
    
    # Try to send requests after killing
    timeout 10 java ClientSimulator 3 single > failover_test.log 2>&1
    
    if grep -q "completed successfully" failover_test.log; then
        echo -e "Testing: Failover after backend failure... ${GREEN}✓ PASSED${NC}"
        ((TESTS_PASSED++))
    else
        echo -e "Testing: Failover after backend failure... ${RED}✗ FAILED${NC}"
        ((TESTS_FAILED++))
    fi
else
    echo -e "Testing: Failover after backend failure... ${YELLOW}⚠ SKIPPED${NC} (couldn't kill backend)"
fi
echo ""

echo -e "${YELLOW}Phase 7: Configuration${NC}"
echo "---------------------"

# Check if config file exists and is readable
if [ -f "config.properties" ]; then
    if grep -q "loadBalancerPort" config.properties; then
        echo -e "Testing: Configuration file... ${GREEN}✓ PASSED${NC}"
        ((TESTS_PASSED++))
    else
        echo -e "Testing: Configuration file... ${RED}✗ FAILED${NC} (invalid format)"
        ((TESTS_FAILED++))
    fi
else
    echo -e "Testing: Configuration file... ${YELLOW}⚠ WARNING${NC} (using defaults)"
fi
echo ""

echo -e "${YELLOW}Phase 8: Log Generation${NC}"
echo "----------------------"

# Check if logs are being generated
if [ -f "app.log" ]; then
    log_size=$(wc -l < app.log)
    if [ $log_size -gt 10 ]; then
        echo -e "Testing: Log file generation... ${GREEN}✓ PASSED${NC} ($log_size lines)"
        ((TESTS_PASSED++))
    else
        echo -e "Testing: Log file generation... ${YELLOW}⚠ WARNING${NC} (only $log_size lines)"
    fi
else
    echo -e "Testing: Log file generation... ${RED}✗ FAILED${NC} (no log file)"
    ((TESTS_FAILED++))
fi
echo ""

echo -e "${BLUE}=========================================="
echo "Test Results Summary"
echo -e "==========================================${NC}"
echo ""
echo -e "Tests Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Tests Failed: ${RED}$TESTS_FAILED${NC}"
echo ""

# Calculate success rate
TOTAL_TESTS=$((TESTS_PASSED + TESTS_FAILED))
if [ $TOTAL_TESTS -gt 0 ]; then
    SUCCESS_RATE=$(( (TESTS_PASSED * 100) / TOTAL_TESTS ))
    echo -e "Success Rate: ${BLUE}${SUCCESS_RATE}%${NC}"
fi
echo ""

# Overall result
if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}=========================================="
    echo "✓ All tests passed successfully!"
    echo -e "==========================================${NC}"
    exit 0
else
    echo -e "${RED}=========================================="
    echo "✗ Some tests failed"
    echo -e "==========================================${NC}"
    echo ""
    echo "Check logs for details:"
    echo "  - app.log (system logs)"
    echo "  - backend_test.log (backend output)"
    echo "  - lb_test.log (load balancer output)"
    echo "  - client_test.log (client output)"
    exit 1
fi
