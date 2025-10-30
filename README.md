# Simple Load Balancer Project

A beginner-friendly Java implementation of a load balancer that demonstrates distributed systems concepts including multithreading, proxy patterns, and round-robin load distribution.

## 🎯 Project Overview

This project simulates a real-world load balancing system with:
- **Load Balancer** (port 8080): Acts as a proxy, distributing client requests across backend servers
- **4 Backend Servers** (ports 8081-8084): Handle actual requests and return responses
- **Client Simulator**: Spawns multiple concurrent clients to test the load balancer
- **Thread-Safe Logging**: All components log to a shared `app.log` file

## 📁 Project Structure

```
load-balancer-project/
├── Logger.java             # Thread-safe logging utility
├── BackendManager.java     # Manages 4 backend servers
├── LoadBalancer.java       # Main load balancer (proxy server)
├── ClientSimulator.java    # Generates client load for testing
├── README.md              # This file
└── app.log                # Generated log file (after running)
```

## 🔧 Prerequisites

- Java Development Kit (JDK) 8 or higher
- Terminal/Command Prompt (3 separate terminals recommended)
- Text editor for viewing logs

## 🚀 How to Run

### Step 1: Compile All Files

Open a terminal in the project directory and compile all Java files:

```bash
javac *.java
```

This should complete without errors. You should see `.class` files generated for each `.java` file.

### Step 2: Start Backend Servers

In **Terminal 1**, start all 4 backend servers:

```bash
java BackendManager
```

**Expected Output:**
```
[timestamp] [INFO] [BackendManager] Starting Backend Manager...
[timestamp] [INFO] [BackendManager] Started backend server on port 8081
[timestamp] [INFO] [BackendManager] Started backend server on port 8082
[timestamp] [INFO] [BackendManager] Started backend server on port 8083
[timestamp] [INFO] [BackendManager] Started backend server on port 8084
[timestamp] [INFO] [BackendManager] All 4 backend servers are running...
```

Leave this terminal running.

### Step 3: Start Load Balancer

In **Terminal 2**, start the load balancer:

```bash
java LoadBalancer
```

**Expected Output:**
```
[timestamp] [INFO] [LoadBalancer] Starting Load Balancer on port 8080
[timestamp] [INFO] [LoadBalancer] Performing health check on backends...
[timestamp] [INFO] [LoadBalancer] ✓ Backend localhost:8081 is healthy
[timestamp] [INFO] [LoadBalancer] ✓ Backend localhost:8082 is healthy
[timestamp] [INFO] [LoadBalancer] ✓ Backend localhost:8083 is healthy
[timestamp] [INFO] [LoadBalancer] ✓ Backend localhost:8084 is healthy
[timestamp] [INFO] [LoadBalancer] Load Balancer is ready and listening...
```

Leave this terminal running.

### Step 4: Run Client Simulator

In **Terminal 3**, start the client simulator with 10 clients (default):

```bash
java ClientSimulator 10
```

Or specify a custom number of clients:

```bash
java ClientSimulator 20
```

**Expected Output:**
```
[timestamp] [INFO] [ClientSimulator] Starting 10 client threads...
[timestamp] [INFO] [ClientSimulator] Started Client-1
[timestamp] [INFO] [ClientSimulator] Started Client-2
...
[timestamp] [INFO] [Client-1] Connecting to load balancer...
[timestamp] [INFO] [Client-1] Received response:
HTTP/1.1 200 OK
Response from Backend on port 8082
Timestamp: [timestamp]
```

### Step 5: View Logs

View the complete log file:

```bash
cat app.log
```

Or on Windows:

```cmd
type app.log
```

Or open `app.log` in any text editor.

## 📊 Testing & Verification

### Test 1: Verify Round-Robin Distribution

Run 10 clients and check the log file:

```bash
java ClientSimulator 10
grep "Forwarding request to backend" app.log | tail -10
```

You should see requests distributed evenly across backends 8081, 8082, 8083, 8084 in a rotating pattern.

### Test 2: Load Testing

Increase the number of clients to test concurrent handling:

```bash
java ClientSimulator 50
```

All clients should receive responses, and the log should show concurrent processing across all backends.

### Test 3: Backend Failure Handling

1. Stop the BackendManager (Ctrl+C in Terminal 1)
2. Restart it with `java BackendManager`
3. Run clients: `java ClientSimulator 5`

The load balancer should automatically skip any temporarily unavailable backends and route to healthy ones.

### Test 4: Verify Thread Names

Check that client threads are properly named:

```bash
grep "Client-[0-9]" app.log | head -20
```

You should see logs from Client-1, Client-2, etc.

## 📈 Expected Log Output

A successful run will show:

1. **Backend Initialization**: All 4 backends start and listen
2. **Load Balancer Health Check**: All backends verified as healthy
3. **Client Connections**: Each client connects to load balancer
4. **Request Forwarding**: Load balancer forwards to backends in round-robin
5. **Response Delivery**: Backends respond, load balancer forwards to clients
6. **Completion**: All clients receive responses

Sample log sequence:
```
[timestamp] [INFO] [BackendManager] Started backend server on port 8081
[timestamp] [INFO] [LoadBalancer] Load Balancer is ready and listening
[timestamp] [INFO] [Client-1] Connecting to load balancer...
[timestamp] [INFO] [LoadBalancer] Forwarding request to backend localhost:8081
[timestamp] [INFO] [Backend-8081] Connection received from /127.0.0.1
[timestamp] [INFO] [Backend-8081] Response sent successfully
[timestamp] [INFO] [Client-1] Received response: Response from Backend on port 8081
```

## 🧪 Troubleshooting

### Port Already in Use

**Error:** `Address already in use`

**Solution:** Kill the process using the port:
```bash
# On Linux/Mac
lsof -ti:8080 | xargs kill -9
lsof -ti:8081 | xargs kill -9

# On Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Connection Refused

**Error:** `Connection refused`

**Solution:** Ensure backends and load balancer are running in this order:
1. Start BackendManager first
2. Start LoadBalancer second (it checks backends)
3. Start ClientSimulator last

### No Logs Appearing

**Solution:** 
- Check that `app.log` exists in the project directory
- Ensure you have write permissions
- Logs are appended, so check the end of the file

## 🎓 Learning Objectives

This project demonstrates:

1. **Socket Programming**: Creating server sockets, accepting connections, bidirectional communication
2. **Multithreading**: Handling concurrent clients with thread pools
3. **Thread Safety**: Using `synchronized` and `AtomicInteger` for shared state
4. **Proxy Pattern**: Forwarding requests between clients and backends
5. **Load Balancing**: Round-robin distribution algorithm
6. **Error Handling**: Graceful degradation when backends fail
7. **Logging**: Thread-safe file I/O for debugging distributed systems

## 🚀 Potential Improvements

Here are some optional extensions to enhance the project:

### 1. Least Connections Strategy
Instead of round-robin, track active connections per backend and route to the one with fewest connections:

```java
// Add to LoadBalancer:
private static ConcurrentHashMap<String, AtomicInteger> activeConnections;
// Update getNextBackend() to return backend with minimum connections
```

### 2. Weighted Round-Robin
Assign weights to backends (e.g., more powerful servers get more requests):

```java
private static final int[] BACKEND_WEIGHTS = {3, 2, 2, 1}; // 3x more to 8081
```

### 3. Health Check Daemon
Add a background thread that continuously monitors backend health:

```java
Thread healthChecker = new Thread(() -> {
    while (true) {
        performHealthCheck();
        Thread.sleep(30000); // Check every 30 seconds
    }
});
```

### 4. Request Metrics
Track and report statistics:
- Total requests per backend
- Average response time
- Error rates
- Current load distribution

### 5. Sticky Sessions
Route requests from the same client to the same backend:

```java
// Use client IP hash to consistently select backend
int backendIndex = clientIP.hashCode() % BACKEND_HOSTS.length;
```

## 📝 Code Comments Guide

The code includes extensive comments explaining:
- **WHY**: The reasoning behind design decisions
- **HOW**: The technical implementation details
- **WHAT**: What each component does

This makes the code educational and easy to understand for beginners.

## 🤝 Contributing

Feel free to extend this project with additional features! Some ideas:
- Add HTTPS support
- Implement more load balancing algorithms
- Add a web dashboard for monitoring
- Support configuration files (e.g., backend list, ports)
- Add request caching

## 📄 License

This is an educational project. Feel free to use, modify, and share!

---

**Happy Load Balancing! 🎉**

For questions or issues, review the logs in `app.log` for detailed debugging information.