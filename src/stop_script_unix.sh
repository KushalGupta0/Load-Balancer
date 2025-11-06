#!/bin/bash

# stop_all.sh - Stop all components of the load balancer system

echo "=========================================="
echo "Stopping Load Balancer System"
echo "=========================================="
echo ""

# Kill by process name
echo "Stopping all components..."

pkill -f "BackendManager" && echo "✓ Backend Manager stopped"
pkill -f "LoadBalancer" && echo "✓ Load Balancer stopped"
pkill -f "DashboardApp" && echo "✓ Dashboard stopped"
pkill -f "ClientSimulator" && echo "✓ Client Simulator stopped"

# Also kill any backend server threads
pkill -f "Backend-" 2>/dev/null

# Clean up PID files
rm -f .backend.pid .loadbalancer.pid .dashboard.pid 2>/dev/null

sleep 1

echo ""
echo "=========================================="
echo "All processes stopped"
echo "=========================================="
