# Quick Reference Guide

## Commands Cheat Sheet

### Compilation
```bash
javac *.java
```

### Starting Components (Manual)

```bash
# Terminal 1
java BackendManager

# Terminal 2
java LoadBalancer

# Terminal 3
java DashboardApp

# Terminal 4 (optional)
java ClientSimulator 10 single
java ClientSimulator 5 continuous
```

### Using Startup Scripts

**Linux/Mac:**
```bash
chmod +x start_all.sh stop_all.sh
./start_all.sh
./stop_all.sh
```

**Windows:**
```batch
start_all.bat
stop_all.bat
```

## Port Reference

| Component | Port | Purpose |
|-----------|------|---------|
| Load Balancer | 8080 | Main entry point for clients |
| Backend 1 | 8081 | First backend server |
| Backend 2 | 8082 | Second backend server |
| Backend 3 | 8083 | Third backend server |
| Backend 4 | 8084 | Fourth backend server |

## Client Simulator Modes

```bash
# Single request mode (each client sends 1 request and exits)
java ClientSimulator <count> single

# Continuous mode (clients run indefinitely in cycles)
java ClientSimulator <count> continuous

# Examples
java ClientSimulator 10 single      # 10 clients, 1 request each
java ClientSimulator 5 continuous   # 5 clients, continuous cycling
```

## Dashboard GUI Buttons

| Button | Action |
|--------|--------|
| **Health Check** | Test connectivity to all backends |
| **Start Clients** | Launch client simulator from GUI |
| **Stop All Clients** | Terminate all running client threads |
| **Kill Backend** | Simulate backend crash (select port) |
| **Reboot Backend** | Restart a down backend (select port) |
| **Reload Config** | Re-read config.properties |
| **System Info** | View memory, threads, configuration |

## Configuration Quick Edit

Edit `config.properties`:

```properties
# Change load balancer port
loadBalancerPort=9000

# Add more backends
backendPorts=8081,8082,8083,8084,8085,8086

# Faster client requests
clientDelayMin=500
clientDelayMax=2000

# Longer connection holds (simulate large transfers)
connectionHoldMin=5000
connectionHoldMax=60000

# Faster dashboard updates
dashboardRefreshInterval=1000
```

**Apply changes:** Click "Reload Config" in GUI (some settings require restart)

## Testing Scenarios

### Test 1: Basic Load Balancing
```bash
# Start system
./start_all.sh

# Send 20 requests
java ClientSimulator 20 single

# Check dashboard: requests should be distributed ~5 to each backend
```

### Test 2: Backend Failure
```bash
# With continuous clients running
java ClientSimulator 5 continuous

# In GUI: Click "Kill Backend" → Select 8082
# Observe: Traffic reroutes to other 3 backends, no errors

# Restore: Click "Reboot Backend" → Select 8082
```

### Test 3: High Load
```bash
# Start 50 continuous clients
java ClientSimulator 50 continuous

# Monitor in GUI:
# - Active Connections should fluctuate
# - Error Rate should stay at 0%
# - CPU usage increases

# Stop: Click "Stop All Clients" in GUI
```

### Test 4: Multiple Failures
```bash
# Start clients
java ClientSimulator 10 continuous

# Kill multiple backends via GUI (e.g., 8082, 8083)
# System continues with 2 remaining backends

# Reboot all backends one by one
# Traffic distributes across all 4 again
```

## Log Analysis

View logs in real-time:
```bash
# Linux/Mac
tail -f app.log

# Windows
powershell Get-Content app.log -Wait -Tail 50
```

Search for specific events:
```bash
# Find all errors
grep ERROR app.log

# Find backend responses
grep "Backend-8081" app.log

# Find client connections
grep "Client-" app.log
```

## Troubleshooting Quick Fixes

### Port Already in Use
```bash
# Linux/Mac
lsof -i :8080
kill -9 <PID>

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Dashboard Shows All Backends DOWN
1. Start `BackendManager` first
2. Wait 3 seconds
3. Start `LoadBalancer`
4. Click "Health Check" in Dashboard

### GUI Freezes
- Restart dashboard: `java DashboardApp`
- Operations use background threads (shouldn't freeze)

### Uneven Distribution
- Normal for small request counts
- Run longer: `java ClientSimulator 50 single`
- Distribution evens out over time

### Memory Issues
```bash
# Increase heap size if needed
java -Xmx512m DashboardApp
java -Xmx256m BackendManager
```

## Metrics Interpretation

### System Metrics Panel
- **Total Requests**: Lifetime request count
- **Uptime**: How long load balancer has been running
- **Error Rate**: Percentage of failed requests (should be ~0%)
- **Active Connections**: Current simultaneous connections

### Backend Table
- **Status**: UP (green) = healthy, DOWN (red) = unreachable
- **Active Conn.**: Current connections to this backend
- **Total Requests**: Lifetime requests handled
- **Avg Latency**: Average response time in milliseconds

### Request Distribution Chart
- Bars should be roughly equal height
- Uneven bars indicate:
  - Recent backend restart (catching up)
  - Very few requests (not enough data)
  - One backend responding slower (more time per request)

## Performance Tuning

### For High Throughput
```properties
# Reduce client delays
clientDelayMin=100
clientDelayMax=500

# Shorter connection holds
connectionHoldMin=100
connectionHoldMax=1000
```

### For Stress Testing
```bash
# Many clients, continuous mode
java ClientSimulator 100 continuous

# Monitor System Info → Memory Usage
```

### For Realistic Web Traffic
```properties
# Variable delays (some fast, some slow)
clientDelayMin=500
clientDelayMax=10000

# Variable transfer sizes
connectionHoldMin=100
connectionHoldMax=30000
```

## Files You Can Safely Delete

- `app.log` - Regenerated automatically
- `*.class` - Recompile with `javac *.java`
- `.backend.pid`, `.loadbalancer.pid`, `.dashboard.pid` - Process tracking (Unix only)

## Files You Should NOT Delete

- `*.java` - Source code
- `config.properties` - Configuration (regenerates with defaults if missing)

## Quick Experiments

### Experiment 1: Different Load Balancing
Currently uses round-robin. To implement least-connections:
1. Track connections per backend in `MetricsCollector`
2. In `LoadBalancer.getNextBackend()`, select backend with fewest connections
3. Observe different distribution pattern

### Experiment 2: Add 5th Backend
```properties
# config.properties
backendPorts=8081,8082,8083,8084,8085
```
Restart all components. Dashboard shows 5 bars.

### Experiment 3: Simulate Network Delay
In `BackendManager.ConnectionHandler.run()`, add:
```java
Thread.sleep(100); // Simulate 100ms network delay
```
Observe increased latency in dashboard.

## Getting Help

1. Check `app.log` for detailed error messages
2. Click "System Info" in GUI for diagnostics
3. Verify all backends are UP in dashboard table
4. Ensure ports 8080-8084 are not in use
5. Confirm Java version: `java -version` (need 8+)

## Next Steps

- Read `README.md` for detailed explanations
- Try all testing scenarios
- Modify `config.properties` and observe changes
- Extend the code (add features, new algorithms)
- Deploy to cloud (AWS, GCP) for distributed testing

---

**Remember:** Always start backends before load balancer!
