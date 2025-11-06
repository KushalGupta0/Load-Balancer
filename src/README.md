# Load Balancer System with GUI Dashboard

A comprehensive Java-based load balancer implementation with real-time monitoring dashboard. Built for educational purposes to demonstrate distributed systems concepts.

## Features

- ✅ **Round-Robin Load Balancing**: Evenly distributes traffic across multiple backend servers
- ✅ **Real-Time GUI Dashboard**: Monitor system metrics, backend health, and request distribution
- ✅ **Automatic Failover**: Gracefully handles backend failures and reroutes traffic
- ✅ **Configurable**: All settings in `config.properties` - no code changes needed
- ✅ **Continuous Client Simulation**: Test with realistic traffic patterns
- ✅ **Crash Recovery**: Kill and reboot backends via GUI to simulate failures
- ✅ **Thread-Safe**: Uses concurrent data structures for multi-threaded operation
- ✅ **Comprehensive Logging**: All events logged to file and displayed in GUI

## Architecture

```
┌─────────────┐
│   Clients   │ (Multiple concurrent)
└──────┬──────┘
       │ HTTP Requests
       ↓
┌─────────────────┐
│ Load Balancer   │ (Port 8080)
│ (Round-Robin)   │
└────────┬────────┘
         │ Distributes to:
    ┌────┴────┬────────┬────────┐
    ↓         ↓        ↓        ↓
┌────────┐┌────────┐┌────────┐┌────────┐
│Backend ││Backend ││Backend ││Backend │
│  8081  ││  8082  ││  8083  ││  8084  │
└────────┘└────────┘└────────┘└────────┘
```

## Quick Start

### 1. Compilation

```bash
javac *.java
```

### 2. Start Components (in separate terminals)

**Terminal 1 - Start Backend Servers:**
```bash
java BackendManager
```

**Terminal 2 - Start Load Balancer:**
```bash
java LoadBalancer
```

**Terminal 3 - Start Dashboard (GUI):**
```bash
java DashboardApp
```

**Terminal 4 - Start Client Simulator:**
```bash
# Single request mode (10 clients)
java ClientSimulator 10 single

# Continuous mode (5 clients running indefinitely)
java ClientSimulator 5 continuous
```

### 3. Using the Dashboard

The GUI provides:
- **System Metrics**: Total requests, uptime, error rate, active connections
- **Backend Table**: Status (UP/DOWN), active connections, total requests, avg latency
- **Request Distribution Chart**: Visual bar graph showing load distribution
- **Live Logs**: Real-time log viewer (last 50 entries)
- **Control Buttons**:
  - `Health Check`: Verify backend availability
  - `Start Clients`: Launch client simulator from GUI
  - `Stop All Clients`: Terminate running clients
  - `Kill Backend`: Simulate backend crash
  - `Reboot Backend`: Restart a down backend
  - `Reload Config`: Re-read config.properties
  - `System Info`: View memory, threads, configuration

## Configuration

Edit `config.properties` to customize behavior:

```properties
# Ports
loadBalancerPort=8080
backendPorts=8081,8082,8083,8084

# Client behavior
defaultClientCount=10
clientDelayMin=1000        # Min delay between requests (ms)
clientDelayMax=5000        # Max delay between requests (ms)
connectionHoldMin=1000     # Min connection hold time (ms)
connectionHoldMax=30000    # Max connection hold time (ms)

# Dashboard
dashboardRefreshInterval=2000   # GUI update interval (ms)
logDisplayLines=50              # Logs shown in GUI

# Logging
logFile=app.log
enableConsoleOutput=true
```

**Note**: Port changes require restart. Other settings can be reloaded via GUI.

## Testing Scenarios

### Scenario 1: Normal Load Balancing
1. Start all components
2. Launch 10 clients: `java ClientSimulator 10 single`
3. Observe in dashboard:
   - Requests evenly distributed across backends (check chart)
   - All backends showing "UP" status (green)
   - Total requests increasing

### Scenario 2: Backend Failure & Recovery
1. With clients running, click "Kill Backend" in GUI
2. Select a backend (e.g., 8082)
3. Observe:
   - Backend status turns RED
   - Traffic reroutes to remaining 3 backends
   - No client errors (automatic failover!)
4. Click "Reboot Backend", select 8082
5. Backend returns to service (GREEN)

### Scenario 3: Continuous Load Testing
1. Start continuous clients: `java ClientSimulator 5 continuous`
2. Watch "Active Connections" metric fluctuate
3. Observe client lifecycle in logs:
   ```
   waiting → connecting → sending → transferring → receiving → completed → idle
   ```
4. Kill a backend mid-cycle - clients continue without errors

### Scenario 4: System Limits
1. Start 50 continuous clients
2. Monitor "System Info" for memory/thread usage
3. Observe error rate (should stay at 0%)
4. Kill 2 backends - system still operational with remaining 2

## File Descriptions

| File | Purpose |
|------|---------|
| `config.properties` | Configuration file (ports, delays, settings) |
| `ConfigLoader.java` | Singleton for loading configuration |
| `Logger.java` | Thread-safe logging utility |
| `MetricsCollector.java` | Centralized metrics aggregation |
| `BackendManager.java` | Spawns and manages backend servers |
| `LoadBalancer.java` | Main load balancer with round-robin |
| `ClientSimulator.java` | Simulates multiple concurrent clients |
| `DashboardApp.java` | Swing GUI for real-time monitoring |

## Logs

All events are logged to `app.log` with format:
```
[2025-11-04 10:30:45.123] [INFO] [LoadBalancer] Accepted connection from /127.0.0.1
[2025-11-04 10:30:45.234] [INFO] [LoadBalancer] Forwarding request to backend localhost:8081
[2025-11-04 10:30:45.345] [INFO] [Backend-8081] Connection received from /127.0.0.1
```

## Troubleshooting

**Problem**: Dashboard shows all backends DOWN
- **Solution**: Start `BackendManager` first, then click "Health Check" in GUI

**Problem**: "Address already in use" error
- **Solution**: Kill process using the port:
  ```bash
  # Linux/Mac
  lsof -i :8080
  kill -9 <PID>
  
  # Windows
  netstat -ano | findstr :8080
  taskkill /PID <PID> /F
  ```

**Problem**: GUI freezes when clicking buttons
- **Solution**: Restart dashboard. Operations run in background threads (SwingWorker)

**Problem**: Uneven load distribution
- **Solution**: Run longer - distribution evens out over time. Initial variance is normal.

## System Requirements

- **Java**: JDK 8 or higher
- **OS**: Windows, Linux, macOS (any with JVM)
- **Memory**: 256MB minimum (512MB recommended)
- **Display**: GUI requires graphical environment

## Performance

Typical performance on modern hardware:
- **Requests/sec**: 500-1000
- **Max concurrent connections**: 100-200
- **Memory usage**: 50-150MB (all components)
- **CPU usage**: <5% idle, 20-40% under load

## Educational Value

This project demonstrates:
- **Network Programming**: Sockets, ServerSocket, TCP/IP
- **Multithreading**: Thread safety, concurrent collections, synchronization
- **Design Patterns**: Singleton, Observer
- **Load Balancing**: Round-robin, health checks, failover
- **GUI Development**: Swing, custom painting, event handling
- **Configuration Management**: Properties files, runtime reloading
- **Monitoring**: Metrics collection, real-time visualization

## Advanced Exercises

1. **Implement Weighted Round-Robin**: Some backends handle more traffic
2. **Add HTTPS Support**: SSL/TLS termination
3. **Create Docker Containers**: Containerize each component
4. **Deploy to Cloud**: AWS/GCP with multiple VMs
5. **Implement Session Persistence**: Sticky sessions for stateful apps
6. **Add Rate Limiting**: Limit requests per client IP
7. **Create REST API**: Programmatic control instead of GUI

## License

Educational use only. Feel free to modify and extend for learning purposes.

## Author

Created as a comprehensive educational demonstration of load balancing concepts.

---

**Need Help?** Check the logs in `app.log` or use the GUI's "System Info" button for diagnostics.
