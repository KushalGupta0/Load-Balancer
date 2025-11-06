#!/bin/bash

# start_all.sh - Start all components of the load balancer system

echo "=========================================="
echo "Load Balancer System Startup"
echo "=========================================="
echo ""

# Check if compiled
if [ ! -f "BackendManager.class" ]; then
    echo "Compiling Java files..."
    javac *.java
    if [ $? -ne 0 ]; then
        echo "❌ Compilation failed!"
        exit 1
    fi
    echo "✓ Compilation successful"
    echo ""
fi

# Start backends
echo "Starting Backend Servers..."
java BackendManager > /dev/null 2>&1 &
BACKEND_PID=$!
echo "✓ Backend Manager started (PID: $BACKEND_PID)"
sleep 3

# Start load balancer
echo "Starting Load Balancer..."
java LoadBalancer > /dev/null 2>&1 &
LB_PID=$!
echo "✓ Load Balancer started (PID: $LB_PID)"
sleep 2

# Start dashboard
echo "Starting Dashboard GUI..."
java DashboardApp > /dev/null 2>&1 &
DASH_PID=$!
echo "✓ Dashboard started (PID: $DASH_PID)"

echo ""
echo "=========================================="
echo "System Started Successfully!"
echo "=========================================="
echo ""
echo "Components:"
echo "  - Backend Servers: localhost:8081-8084"
echo "  - Load Balancer: localhost:8080"
echo "  - Dashboard: GUI window should be visible"
echo ""
echo "To test:"
echo "  java ClientSimulator 10 single"
echo "  java ClientSimulator 5 continuous"
echo ""
echo "To stop all:"
echo "  ./stop_all.sh"
echo ""
echo "Logs: app.log"
echo "=========================================="

# Save PIDs for cleanup
echo "$BACKEND_PID" > .backend.pid
echo "$LB_PID" > .loadbalancer.pid
echo "$DASH_PID" > .dashboard.pid
