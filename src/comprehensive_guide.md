# Complete Educational Guide: Understanding Load Balancers Through Code

## 📚 Table of Contents

1. [Introduction for Absolute Beginners](#introduction)
2. [What is a Load Balancer?](#what-is-load-balancer)
3. [Project Architecture Overview](#architecture)
4. [Understanding Each File in Detail](#file-explanations)
5. [How Data Flows Through the System](#data-flow)
6. [Use Case 1: Testing the Load Balancer](#use-case-1)
7. [Use Case 2: Without Load Balancer (Direct Connection)](#use-case-2)
8. [Use Case 3: Server Crash Simulation](#use-case-3)
9. [Reading and Understanding app.log](#reading-logs)
10. [Key Java Concepts Explained](#java-concepts)
11. [Troubleshooting Guide](#troubleshooting)
12. [Learning Exercises](#exercises)

---

<a name="introduction"></a>
## 1. Introduction for Absolute Beginners

### Who Is This Guide For?

This guide is written for students and beginners who may have little to no experience with Java, networking, or distributed systems. We'll explain every concept from the ground up, using analogies and simple language.

### What Will You Learn?

By the end of this guide, you'll understand:
- How computers communicate over networks
- What a load balancer does and why it's important
- How to build multithreaded applications
- How to read server logs to debug problems
- Real-world scenarios of system failures and recovery

### Prerequisites

- Basic understanding of programming (variables, loops, functions)
- Ability to use a command line/terminal
- Java installed on your computer (JDK 8 or higher)

---

<a name="what-is-load-balancer"></a>
## 2. What is a Load Balancer?

### The Restaurant Analogy

Imagine a popular restaurant with multiple chefs in the kitchen:

**Without Load Balancer:**
- All customers line up at one chef
- That chef gets overwhelmed
- Other chefs stand idle
- Service is slow and inefficient
- If that chef gets sick, the restaurant closes!

**With Load Balancer:**
- A host (load balancer) greets customers at the door
- The host directs each customer to a different chef
- All chefs work equally
- Service is faster
- If one chef is sick, others keep working

### In Computer Terms

A **load balancer** is a traffic director for computer servers:

- **Client**: Your computer/phone making a request (like opening a website)
- **Load Balancer**: A special server that receives all requests first
- **Backend Servers**: Multiple servers that actually do the work
- **Round-Robin**: Taking turns (Server1 → Server2 → Server3 → Server4 → Server1...)

### Why Do We Need Load Balancers?

1. **Performance**: Distributes work so no single server is overwhelmed
2. **Reliability**: If one server crashes, others keep working
3. **Scalability**: Easy to add more servers as traffic grows
4. **Maintenance**: Can take servers offline for updates without downtime

### Real-World Examples

- **Google Search**: Your search goes through load balancers to thousands of servers
- **Netflix**: Video streaming is distributed across many servers
- **Amazon**: Shopping requests are balanced across global data centers
- **Facebook**: Posts and messages are handled by load-balanced servers

---

<a name="architecture"></a>
## 3. Project Architecture Overview

### The Big Picture

Our project simulates a complete load-balancing system with three main components:

```
┌─────────────────────────────────────────────────────────┐
│                     Internet/Network                     │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  Multiple Clients (Simulators)    │
        │  Client-1, Client-2, ... Client-N │
        └───────────────────────────────────┘
                            │
                            │ All connect to port 8080
                            ▼
        ┌───────────────────────────────────┐
        │      LOAD BALANCER (Port 8080)    │
        │  - Receives all client requests   │
        │  - Chooses backend using round-robin │
        │  - Forwards request & response    │
        └───────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
   ┌─────────┐        ┌─────────┐        ┌─────────┐
   │Backend-1│        │Backend-2│        │Backend-3│ ...
   │Port 8081│        │Port 8082│        │Port 8083│
   └─────────┘        └─────────┘        └─────────┘
```

### Components Breakdown

#### 1. **Logger.java**
- **What it does**: Writes messages to a file (app.log)
- **Why it exists**: All components need to log events, but only one should write at a time
- **Analogy**: Like a secretary taking meeting notes - only one person writes to avoid chaos

#### 2. **BackendManager.java**
- **What it does**: Starts 4 separate server programs
- **Why it exists**: Simulates multiple backend servers (like multiple chefs)
- **Ports**: 8081, 8082, 8083, 8084

#### 3. **LoadBalancer.java**
- **What it does**: Receives client requests and distributes them to backends
- **Why it exists**: This is the core - the traffic director
- **Port**: 8080

#### 4. **ClientSimulator.java**
- **What it does**: Creates fake clients to test the system
- **Why it exists**: We need to simulate real users making requests
- **Configurable**: Can create 10, 50, 100+ clients

---

<a name="file-explanations"></a>
## 4. Understanding Each File in Detail

---

### 4.1 Logger.java - The Record Keeper

#### What Problem Does It Solve?

Imagine 100 people trying to write in the same notebook simultaneously - the pages would be a mess! In programming, when multiple threads (mini-programs) try to write to the same file, the data gets corrupted.

**Solution**: The Logger class ensures only ONE thread writes at a time.

#### Line-by-Line Explanation

```java
public class Logger {
```
**Explanation**: This declares a class (a blueprint) named Logger. Think of a class as a template for creating objects.

```java
private static Logger instance;
```
**Explanation**: 
- `private`: Only this class can access it
- `static`: Shared by all parts of the program (not per-object)
- `Logger instance`: Stores the single Logger object
- **Why?**: This is the "Singleton Pattern" - ensuring only ONE logger exists

```java
private static final String LOG_FILE = "app.log";
```
**Explanation**:
- `final`: This value never changes (it's a constant)
- `LOG_FILE`: The name of our log file
- All events get written to "app.log"

```java
private PrintWriter writer;
```
**Explanation**: 
- `PrintWriter`: A Java class for writing text to files
- `writer`: Our tool for actually writing log messages

```java
private SimpleDateFormat dateFormat;
```
**Explanation**:
- Formats dates/times in a readable way
- Example: "2025-10-30 14:25:33.123"

```java
private Logger() {
    try {
        writer = new PrintWriter(new FileWriter(LOG_FILE, true), true);
```
**Explanation**:
- `private Logger()`: Constructor - runs when Logger is created
- `FileWriter(LOG_FILE, true)`: Opens app.log; `true` means APPEND (don't erase old logs)
- `PrintWriter(..., true)`: The second `true` means auto-flush (write immediately)

**Why private?**: We don't want anyone to create new Loggers directly. They must use `getInstance()`.

```java
public static synchronized Logger getInstance() {
    if (instance == null) {
        instance = new Logger();
    }
    return instance;
}
```
**Explanation**:
- `synchronized`: Only one thread can run this at a time (thread-safe)
- Checks if logger exists; if not, creates it
- Returns the single Logger instance
- **Analogy**: Like having one printer in an office - everyone shares it

```java
public synchronized void log(String level, String component, String message) {
    String timestamp = dateFormat.format(new Date());
    String logEntry = String.format("[%s] [%s] [%s] %s", 
                                   timestamp, level, component, message);
    writer.println(logEntry);
    writer.flush();
    System.out.println(logEntry);
}
```
**Explanation**:
- `synchronized`: Only one thread logs at a time
- `level`: INFO, ERROR, WARN (severity)
- `component`: Which part is logging (LoadBalancer, Backend, etc.)
- `message`: The actual log message
- `String.format`: Builds the log line with brackets
- `writer.println()`: Writes to file
- `writer.flush()`: Forces immediate write to disk
- `System.out.println()`: Also prints to console (screen)

**Example Output**: 
```
[2025-10-30 14:25:33.123] [INFO] [LoadBalancer] Started successfully
```

#### Key Concepts in Logger.java

1. **Singleton Pattern**: Only one logger exists
2. **Thread Safety**: `synchronized` prevents chaos
3. **File I/O**: Reading/writing files
4. **Timestamps**: Every log has a time marker

---

### 4.2 BackendManager.java - The Server Farm

#### What Problem Does It Solve?

In production, you'd have multiple physical servers. We simulate this by running 4 servers on different ports on the same machine.

#### Architecture

```
BackendManager (Main Program)
    │
    ├─> Spawns Thread 1: Backend on port 8081
    ├─> Spawns Thread 2: Backend on port 8082
    ├─> Spawns Thread 3: Backend on port 8083
    └─> Spawns Thread 4: Backend on port 8084

Each backend thread:
    1. Creates ServerSocket (listens for connections)
    2. Accepts connections in a loop
    3. For each connection, spawns a handler thread
```

#### Line-by-Line Explanation

```java
private static final int[] BACKEND_PORTS = {8081, 8082, 8083, 8084};
```
**Explanation**:
- Array of port numbers
- Each backend needs its own port
- **Port**: Like a door number on a building (computer has 65,535 doors/ports)

```java
public static void main(String[] args) {
    for (int port : BACKEND_PORTS) {
        BackendServer server = new BackendServer(port);
        Thread thread = new Thread(server);
        thread.setName("Backend-" + port);
        thread.start();
    }
}
```
**Explanation**:
- `main`: Entry point - program starts here
- `for` loop: Iterate through each port
- `new BackendServer(port)`: Create a server object for this port
- `new Thread(server)`: Wrap it in a thread (allows concurrent execution)
- `thread.setName()`: Name it "Backend-8081", etc. for logging
- `thread.start()`: Actually start running the server

**Analogy**: Like opening 4 different restaurant locations, each with its own address (port).

```java
static class BackendServer implements Runnable {
```
**Explanation**:
- `static class`: A class inside another class
- `implements Runnable`: Means this can run in a thread
- Must provide a `run()` method

```java
public void run() {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
```
**Explanation**:
- `run()`: This method executes when thread starts
- `ServerSocket`: Creates a listening socket
- `new ServerSocket(port)`: Binds to the port (claims that port number)
- `try (...)`: Auto-closes socket when done

**What's happening**: The server says "I'm listening on port 8081 for incoming connections"

```java
while (true) {
    Socket clientSocket = serverSocket.accept();
    Thread handler = new Thread(new ConnectionHandler(clientSocket, port));
    handler.start();
}
```
**Explanation**:
- `while (true)`: Infinite loop - keep accepting connections forever
- `serverSocket.accept()`: BLOCKS (waits) until a client connects
- When client connects, returns a `Socket` object
- Create a new thread to handle this connection
- `handler.start()`: Handle it concurrently

**Why threads?**: So the server can handle multiple clients simultaneously. While handling Client 1, it can still accept Client 2.

#### The ConnectionHandler Class

```java
static class ConnectionHandler implements Runnable {
    private Socket socket;
    private int port;
```
**Explanation**:
- Each connection gets its own handler
- `socket`: The connection to the client
- `port`: Which backend this is (for logging)

```java
public void run() {
    try (
        BufferedReader in = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
    ) {
```
**Explanation**:
- `socket.getInputStream()`: Reading data FROM client
- `socket.getOutputStream()`: Writing data TO client
- `BufferedReader`: Efficient text reading
- `PrintWriter`: Easy text writing

**Analogy**: Like setting up a phone call - one ear (input) and one mouth (output).

```java
StringBuilder request = new StringBuilder();
String line;
while ((line = in.readLine()) != null) {
    request.append(line);
    if (line.isEmpty()) {
        break;
    }
}
```
**Explanation**:
- Read lines from client until empty line
- HTTP requests end with a blank line
- Store the request in `request` variable

```java
String response = String.format(
    "HTTP/1.1 200 OK\r\n" +
    "Content-Type: text/plain\r\n" +
    "Connection: close\r\n" +
    "\r\n" +
    "Response from Backend on port %d\n" +
    "Timestamp: %s\n",
    port, new Date()
);
out.print(response);
```
**Explanation**:
- Build an HTTP response
- `200 OK`: Success status code
- Headers describe the response
- `\r\n\r\n`: Blank line separates headers from body
- Body contains port number and timestamp
- **Why?**: So we can see which backend handled the request!

#### Key Concepts in BackendManager.java

1. **Multithreading**: 4 servers run simultaneously
2. **ServerSocket**: Listens for incoming connections
3. **Socket**: Represents a connection
4. **HTTP Protocol**: Text-based request/response format

---

### 4.3 LoadBalancer.java - The Traffic Director

#### What Problem Does It Solve?

Clients need a single address to connect to. The load balancer:
1. Accepts client connections
2. Chooses a backend
3. Forwards the request
4. Forwards the response back

#### The Round-Robin Algorithm

```
Request 1 → Backend 8081
Request 2 → Backend 8082
Request 3 → Backend 8083
Request 4 → Backend 8084
Request 5 → Backend 8081  (wraps around)
Request 6 → Backend 8082
...
```

#### Line-by-Line Explanation

```java
private static final String[] BACKEND_HOSTS = {
    "localhost:8081",
    "localhost:8082",
    "localhost:8083",
    "localhost:8084"
};
```
**Explanation**:
- List of backend addresses
- `localhost`: The same machine (for testing)
- In production, these would be actual server IPs

```java
private static AtomicInteger currentBackendIndex = new AtomicInteger(0);
```
**Explanation**:
- `AtomicInteger`: Thread-safe counter
- Tracks which backend to use next
- Starts at 0 (first backend)

**Why Atomic?**: Multiple threads will increment this. Regular `int` would cause race conditions (two threads reading the same value before incrementing).

```java
private static void performHealthCheck() {
    for (String backend : BACKEND_HOSTS) {
        String[] parts = backend.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        
        try (Socket testSocket = new Socket(host, port)) {
            logger.log("INFO", "LoadBalancer", 
                      "✓ Backend " + backend + " is healthy");
        } catch (Exception e) {
            logger.log("WARN", "LoadBalancer", 
                      "✗ Backend " + backend + " is unreachable");
        }
    }
}
```
**Explanation**:
- Runs at startup
- Tries to connect to each backend
- Logs success (✓) or failure (✗)
- **Why?**: Better to know immediately if backends are down

```java
private static String getNextBackend() {
    for (int i = 0; i < BACKEND_HOSTS.length; i++) {
        int index = currentBackendIndex.getAndIncrement() % BACKEND_HOSTS.length;
        String backend = BACKEND_HOSTS[index];
```
**Explanation**:
- `getAndIncrement()`: Atomically increases counter and returns old value
- `% BACKEND_HOSTS.length`: Modulo operator - wraps around (0,1,2,3,0,1,2,3...)
- **Example**: 
  - Call 1: index=0, counter becomes 1
  - Call 2: index=1, counter becomes 2
  - Call 5: index=4, 4%4=0, wraps to first backend

```java
try (Socket testSocket = new Socket(parts[0], Integer.parseInt(parts[1]))) {
    return backend;
} catch (Exception e) {
    logger.log("WARN", "LoadBalancer", 
              "Backend " + backend + " is down, trying next...");
}
```
**Explanation**:
- Try to connect to chosen backend
- If succeeds, return it
- If fails, loop tries the next one
- **Failover**: Automatically skip dead backends

#### The Proxy Logic (Bidirectional Forwarding)

```java
Socket clientSocket = /* incoming client connection */;
Socket backendSocket = new Socket(backendHost, backendPort);
```
**Explanation**:
- Now we have TWO sockets
- `clientSocket`: Connection to client
- `backendSocket`: Connection to backend
- Need to forward data both ways

```java
Thread clientToBackend = new Thread(
    new Forwarder(clientSocket.getInputStream(), 
                 backendSocket.getOutputStream(), 
                 "Client→Backend")
);

Thread backendToClient = new Thread(
    new Forwarder(backendSocket.getInputStream(), 
                 clientSocket.getOutputStream(), 
                 "Backend→Client")
);

clientToBackend.start();
backendToClient.start();
```
**Explanation**:
- Create TWO forwarding threads
- Thread 1: Copies client's request to backend
- Thread 2: Copies backend's response to client
- **Why two threads?**: So they can work simultaneously (full-duplex)

**Analogy**: Like two people on walkie-talkies - both can talk and listen at the same time.

#### The Forwarder Class

```java
static class Forwarder implements Runnable {
    private InputStream input;
    private OutputStream output;
    
    public void run() {
        try {
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                output.flush();
            }
        } catch (Exception e) {
            // Connection closed
        }
    }
}
```
**Explanation**:
- `buffer`: Temporary storage (4KB)
- `input.read(buffer)`: Read up to 4KB from input
- Returns number of bytes read, or -1 if stream closed
- `output.write(buffer, 0, bytesRead)`: Write those bytes to output
- `output.flush()`: Send immediately

**Analogy**: Like a relay runner passing a baton - receive data, immediately pass it on.

#### Key Concepts in LoadBalancer.java

1. **Round-Robin**: Fair distribution algorithm
2. **AtomicInteger**: Thread-safe counter
3. **Proxy Pattern**: Forwarding between two connections
4. **Bidirectional Communication**: Simultaneous two-way data flow
5. **Health Checks**: Proactive monitoring

---

### 4.4 ClientSimulator.java - The Load Generator

#### What Problem Does It Solve?

We need to test the load balancer with multiple simultaneous clients. This simulates real-world traffic.

#### Line-by-Line Explanation

```java
public static void main(String[] args) {
    int numClients = 10;
    
    if (args.length > 0) {
        numClients = Integer.parseInt(args[0]);
    }
```
**Explanation**:
- Default: 10 clients
- `args`: Command-line arguments
- `java ClientSimulator 50` → `args[0]` = "50"
- Converts string "50" to integer 50

```java
for (int i = 1; i <= numClients; i++) {
    ClientThread client = new ClientThread(i);
    Thread thread = new Thread(client);
    thread.setName("Client-" + i);
    thread.start();
}
```
**Explanation**:
- Loop creates N client threads
- Each gets a unique ID (1, 2, 3, ...)
- Each runs independently
- All start "at once" (concurrently)

#### The ClientThread Class

```java
static class ClientThread implements Runnable {
    private int clientId;
    private Random random;
```
**Explanation**:
- Each client has an ID
- `Random`: For generating random delays

```java
public void run() {
    int delay = random.nextInt(2000);
    Thread.sleep(delay);
```
**Explanation**:
- Generate random delay: 0-2000 milliseconds (0-2 seconds)
- `Thread.sleep()`: Pause this thread
- **Why?**: Simulates clients connecting at different times (not all at once)

```java
Socket socket = new Socket(LOAD_BALANCER_HOST, LOAD_BALANCER_PORT);
```
**Explanation**:
- Connect to load balancer (NOT directly to backends!)
- `LOAD_BALANCER_HOST`: "localhost"
- `LOAD_BALANCER_PORT`: 8080

```java
String request = String.format(
    "GET / HTTP/1.1\r\n" +
    "Host: %s\r\n" +
    "User-Agent: %s\r\n" +
    "Connection: close\r\n" +
    "\r\n",
    LOAD_BALANCER_HOST, clientName
);
out.print(request);
```
**Explanation**:
- Build an HTTP GET request
- `GET /`: Request the root page
- `Host:`: Required HTTP header
- `User-Agent:`: Identifies the client (we use "Client-1", etc.)
- `Connection: close`: Close after response
- `\r\n\r\n`: Blank line ends request

```java
StringBuilder response = new StringBuilder();
String line;
while ((line = in.readLine()) != null) {
    response.append(line).append("\n");
}
logger.log("INFO", clientName, 
          "Received response:\n" + response.toString());
```
**Explanation**:
- Read all lines of response
- Store in `response`
- Log the complete response
- **Now we can see which backend handled it!**

#### Key Concepts in ClientSimulator.java

1. **Simulated Load**: Multiple concurrent clients
2. **Random Delays**: Realistic timing
3. **HTTP Protocol**: Standard web request format
4. **Connecting to Load Balancer**: Tests the full system

---

<a name="data-flow"></a>
## 5. How Data Flows Through the System

### Complete Request/Response Cycle

Let's trace a single request from Client-1:

```
Step 1: Client-1 sends request
┌──────────┐
│ Client-1 │  "GET / HTTP/1.1..."
└────┬─────┘
     │ Connects to localhost:8080
     ▼
     
Step 2: Load Balancer receives request
┌─────────────────┐
│  Load Balancer  │
│  (Port 8080)    │
│                 │
│ currentIndex=0  │  Selects Backend-8081
│ Increments to 1 │
└────┬────────────┘
     │ Connects to localhost:8081
     ▼
     
Step 3: Backend receives request
┌─────────────────┐
│  Backend-8081   │  "Received request: GET / ..."
│  (Port 8081)    │  Processes and generates response
└────┬────────────┘
     │ Response: "Response from Backend on port 8081..."
     ▼
     
Step 4: Load Balancer forwards response
┌─────────────────┐
│  Load Balancer  │  Forwards response bytes
│  (Forwarder)    │
└────┬────────────┘
     │
     ▼
     
Step 5: Client receives response
┌──────────┐
│ Client-1 │  "Response from Backend on port 8081
│          │   Timestamp: Thu Oct 30 14:25:33..."
└──────────┘
```

### Data Flow Diagrams

#### Scenario: 4 Clients Connecting

```
Time 0: All clients start

Client-1 ──┐
Client-2 ──┼─→ Load Balancer ──┬─→ Backend-8081 (Client-1)
Client-3 ──┤   (Round-Robin)   ├─→ Backend-8082 (Client-2)
Client-4 ──┘                    ├─→ Backend-8083 (Client-3)
                                └─→ Backend-8084 (Client-4)

Result: Perfect distribution!
```

#### Scenario: Backend Failure

```
Client-1 → Load Balancer → tries 8081 → FAILS
                         → tries 8082 → SUCCESS! ✓

Client-2 → Load Balancer → tries 8082 → SUCCESS! ✓

Client-3 → Load Balancer → tries 8083 → SUCCESS! ✓

Client-4 → Load Balancer → tries 8084 → SUCCESS! ✓

Client-5 → Load Balancer → tries 8081 → FAILS
                         → tries 8082 → SUCCESS! ✓
```

---

<a name="use-case-1"></a>
## 6. Use Case 1: Testing the Load Balancer

### Objective
Verify that the load balancer correctly distributes requests across all backends using round-robin.

### Step-by-Step Instructions

#### Step 1: Clean Slate
```bash
# Delete old logs
rm app.log

# Make sure no old processes are running
# On Mac/Linux:
killall java

# On Windows:
taskkill /F /IM java.exe
```

#### Step 2: Compile Everything
```bash
javac *.java
```

**Expected Output**: No errors. You'll see `.class` files created.

#### Step 3: Start Backends
Open Terminal 1:
```bash
java BackendManager
```

**What to Look For:**
```
[2025-10-30 14:25:30.123] [INFO] [BackendManager] Starting Backend Manager...
[2025-10-30 14:25:30.234] [INFO] [BackendManager] Started backend server on port 8081
[2025-10-30 14:25:30.245] [INFO] [BackendManager] Started backend server on port 8082
[2025-10-30 14:25:30.256] [INFO] [BackendManager] Started backend server on port 8083
[2025-10-30 14:25:30.267] [INFO] [BackendManager] Started backend server on port 8084
[2025-10-30 14:25:30.278] [INFO] [Backend-8081] Backend server listening on port 8081
[2025-10-30 14:25:30.289] [INFO] [Backend-8082] Backend server listening on port 8082
[2025-10-30 14:25:30.301] [INFO] [Backend-8083] Backend server listening on port 8083
[2025-10-30 14:25:30.312] [INFO] [Backend-8084] Backend server listening on port 8084
```

✅ **Success Indicators:**
- All 4 backends started
- No error messages
- "listening on port" appears for each backend

❌ **Common Errors:**
- "Address already in use" → Port is occupied (see troubleshooting)
- "Permission denied" → Try ports above 1024

#### Step 4: Start Load Balancer
Open Terminal 2:
```bash
java LoadBalancer
```

**What to Look For:**
```
[2025-10-30 14:26:00.100] [INFO] [LoadBalancer] Starting Load Balancer on port 8080
[2025-10-30 14:26:00.200] [INFO] [LoadBalancer] Performing health check on backends...
[2025-10-30 14:26:00.300] [INFO] [LoadBalancer] ✓ Backend localhost:8081 is healthy
[2025-10-30 14:26:00.310] [INFO] [LoadBalancer] ✓ Backend localhost:8082 is healthy
[2025-10-30 14:26:00.320] [INFO] [LoadBalancer] ✓ Backend localhost:8083 is healthy
[2025-10-30 14:26:00.330] [INFO] [LoadBalancer] ✓ Backend localhost:8084 is healthy
[2025-10-30 14:26:00.400] [INFO] [LoadBalancer] Load Balancer is ready and listening
```

✅ **Success Indicators:**
- All backends show ✓ (healthy)
- "ready and listening" message appears

❌ **Common Errors:**
- "✗ Backend ... is unreachable" → Backend not running
- "Address already in use" → Another program using port 8080

#### Step 5: Send Test Requests
Open Terminal 3:
```bash
java ClientSimulator 8
```

**What to Look For:**
```
[timestamp] [INFO] [ClientSimulator] Starting 8 client threads...
[timestamp] [INFO] [Client-1] Connecting to load balancer...
[timestamp] [INFO] [Client-2] Waiting 1250ms before sending request
[timestamp] [INFO] [LoadBalancer] Accepted connection from /127.0.0.1
[timestamp] [INFO] [LoadBalancer] Forwarding request to backend localhost:8081
[timestamp] [INFO] [Backend-8081] Connection received from /127.0.0.1
[timestamp] [INFO] [Backend-8081] Received request: GET / HTTP/1.1
[timestamp] [INFO] [Backend-8081] Response sent successfully
[timestamp] [INFO] [Client-1] Received response:
HTTP/1.1 200 OK
Content-Type: text/plain
Connection: close

Response from Backend on port 8081
Timestamp: Thu Oct 30 14:27:00 IST 2025
```

#### Step 6: Analyze Distribution
```bash
# Count requests per backend
grep "Forwarding request to backend" app.log | sort | uniq -c
```

**Expected Output:**
```
   2 Forwarding request to backend localhost:8081
   2 Forwarding request to backend localhost:8082
   2 Forwarding request to backend localhost:8083
   2 Forwarding request to backend localhost:8084
```

✅ **What This Means:**
- 8 requests were distributed evenly: 2 to each backend
- Round-robin is working correctly!

#### Step 7: Test with More Clients

```bash
java ClientSimulator 20
```

**Analyze Again:**
```bash
grep "Forwarding request to backend" app.log | tail -20 | sort | uniq -c
```

**Expected:** 5 requests to each backend (20 ÷ 4 = 5)

#### Step 8: Verify Round-Robin Order

```bash
grep "Forwarding request to backend" app.log | tail -20
```

**Expected Pattern:**
```
[timestamp] Forwarding request to backend localhost:8081
[timestamp] Forwarding request to backend localhost:8082
[timestamp] Forwarding request to backend localhost:8083
[timestamp] Forwarding request to backend localhost:8084
[timestamp] Forwarding request to backend localhost:8081  ← Wrapped back!
[timestamp] Forwarding request to backend localhost:8082
...
```

### Summary of Use Case 1

**What We Proved:**
1. ✅ Load balancer receives all client connections
2. ✅ Requests are distributed evenly across backends
3. ✅ Round-robin cycling works (8081→8082→8083→8084→8081...)
4. ✅ Multiple clients can connect simultaneously
5. ✅ Each backend handles its share of traffic

---

<a name="use-case-2"></a>
## 7. Use Case 2: Without Load Balancer (Direct Connection)

### Objective
Understand the problems that occur when clients connect directly to backends without load balancing.

### The Problem

Without a load balancer:
- **Poor Distribution**: All clients might connect to the same backend
- **Overload**: One server gets hammered while others sit idle
- **Client Complexity**: Clients need to know all backend addresses
- **No Failover**: If a backend crashes, clients must manually retry

### Experiment: Direct Connection Test

#### Step 1: Modify ClientSimulator (Create a Test Version)

Create a new file `DirectClient.java`:

```java
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class DirectClient {
    private static final Logger logger = Logger.getInstance();
    
    public static void main(String[] args) {
        // All clients connect to the SAME backend
        String backend = "localhost";
        int port = 8081;  // Everyone goes to 8081!
        
        logger.log("INFO", "DirectClient", 
                  "WARNING: All clients connecting directly to " + port);
        
        for (int i = 1; i <= 10; i++) {
            final int clientId = i;
            new Thread(() -> {
                try {
                    Socket socket = new Socket(backend, port);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                    );
                    
                    String request = "GET / HTTP/1.1\r\nHost: localhost\r\n\r\n";
                    out.print(request);
                    out.flush();
                    
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                    
                    logger.log("INFO", "Client-" + clientId, 
                              "Connected to backend " + port);
                    
                    socket.close();
                } catch (Exception e) {
                    logger.log("ERROR", "Client-" + clientId, 
                              "Failed: " + e.getMessage());
                }
            }).start();
        }
    }
}
```

#### Step 2: Compile and Run

```bash
javac DirectClient.java
java DirectClient
```

#### Step 3: Analyze the Results

```bash
grep "Connected to backend" app.log | tail -10
```

**Output:**
```
[timestamp] [INFO] [Client-1] Connected to backend 8081
[timestamp] [INFO] [Client-2] Connected to backend 8081
[timestamp] [INFO] [Client-3] Connected to backend 8081
[timestamp] [INFO] [Client-4] Connected to backend 8081
[timestamp] [INFO] [Client-5] Connected to backend 8081
[timestamp] [INFO] [Client-6] Connected to backend 8081
[timestamp] [INFO] [Client-7] Connected to backend 8081
[timestamp] [INFO] [Client-8] Connected to backend 8081
[timestamp] [INFO] [Client-9] Connected to backend 8081
[timestamp] [INFO] [Client-10] Connected to backend 8081
```

**Check Backend Load:**
```bash
grep "Backend-808[1-4]" app.log | grep "Connection received" | cut -d']' -f3 | sort | uniq -c
```

**Output:**
```
  10 [Backend-8081] Connection received
   0 [Backend-8082] Connection received
   0 [Backend-8083] Connection received
   0 [Backend-8084] Connection received
```

### The Problem Visualized

```
Without Load Balancer:

Client-1  ──┐
Client-2  ──┤
Client-3  ──┤
Client-4  ──┼──→ Backend-8081 (OVERLOADED! 100% traffic)
Client-5  ──┤
Client-6  ──┤
Client-7  ──┤
Client-8  ──┘

              Backend-8082 (IDLE - 0% traffic)
              Backend-8083 (IDLE - 0% traffic)
              Backend-8084 (IDLE - 0% traffic)


With Load Balancer:

Client-1  ──┐         ┌──→ Backend-8081 (25% traffic)
Client-2  ──┤         ├──→ Backend-8082 (25% traffic)
Client-3  ──┼──→ LB ──┼──→ Backend-8083 (25% traffic)
Client-4  ──┤         └──→ Backend-8084 (25% traffic)
Client-5  ──┘
```

### Real-World Analogy

**Without Load Balancer:**
Imagine a mall with 4 entrances, but everyone knows only about the main entrance. Result:
- Main entrance is crowded
- Long wait times
- Other 3 entrances are empty
- Poor customer experience

**With Load Balancer:**
Security guard at parking lot directs each car to a different entrance:
- All entrances used evenly
- No crowding
- Fast entry for everyone
- Better experience

### Summary of Use Case 2

**Problems Without Load Balancer:**
1. ❌ Uneven distribution (one server overloaded)
2. ❌ Wasted resources (idle servers)
3. ❌ Slower response times (queuing at busy server)
4. ❌ Poor scalability (can't easily add servers)
5. ❌ Client complexity (clients need server list logic)

**Benefits With Load Balancer:**
1. ✅ Even distribution
2. ✅ All servers utilized
3. ✅ Faster responses
4. ✅ Easy to scale (just add backends)
5. ✅ Simple clients (one address to connect to)

---

<a name="use-case-3"></a>
## 8. Use Case 3: Server Crash Simulation

### Objective
Demonstrate how the load balancer handles backend failures gracefully.

### Scenario Overview

We'll simulate three failure scenarios:
1. **Single Backend Crash**: One backend dies, others continue
2. **Multiple Backend Failures**: Two backends die
3. **All Backends Down**: Complete system failure

---

### Scenario 1: Single Backend Crash

#### Step 1: Start Everything Normally

```bash
# Terminal 1
java BackendManager

# Terminal 2
java LoadBalancer

# Wait for health checks to pass
```

#### Step 2: Kill One Backend

**On Mac/Linux:**
```bash
# Find the process ID for port 8081
lsof -ti:8081

# Example output: 12345

# Kill it
kill -9 12345
```

**On Windows:**
```bash
netstat -ano | findstr :8081
# Note the PID in the last column

taskkill /F /PID <PID>
```

**Alternative Method (if above doesn't work):**
In Terminal 1 where BackendManager is running, you'll see it's running all backends. You can't kill just one without modifying code, so for this test, we'll simulate by starting only 3 backends.

#### Modified Test: Start Only 3 Backends

Create `ThreeBackends.java`:
```java
import java.net.ServerSocket;
import java.net.Socket;

public class ThreeBackends {
    private static final int[] PORTS = {8081, 8082, 8083}; // Skip 8084!
    private static final Logger logger = Logger.getInstance();
    
    public static void main(String[] args) {
        logger.log("INFO", "ThreeBackends", 
                  "Starting only 3 backends (simulating 8084 crash)");
        
        for (int port : PORTS) {
            new Thread(() -> {
                try (ServerSocket ss = new ServerSocket(port)) {
                    logger.log("INFO", "Backend-" + port, "Listening");
                    while (true) {
                        Socket client = ss.accept();
                        // Handle connection (simplified)
                        new Thread(() -> {
                            try {
                                client.getOutputStream().write(
                                    ("HTTP/1.1 200 OK\r\n\r\nBackend " + port).getBytes()
                                );
                                client.close();
                            } catch (Exception e) {}
                        }).start();
                    }
                } catch (Exception e) {
                    logger.log("ERROR", "Backend-" + port, e.getMessage());
                }
            }).start();
        }
    }
}
```

#### Step 3: Run the Test

```bash
# Compile
javac ThreeBackends.java

# Terminal 1: Start 3 backends (8084 is "crashed")
java ThreeBackends

# Terminal 2: Start load balancer
java LoadBalancer
```

**Load Balancer Output:**
```
[timestamp] [INFO] [LoadBalancer] Performing health check on backends...
[timestamp] [INFO] [LoadBalancer] ✓ Backend localhost:8081 is healthy
[timestamp] [INFO] [LoadBalancer] ✓ Backend localhost:8082 is healthy
[timestamp] [INFO] [LoadBalancer] ✓ Backend localhost:8083 is healthy
[timestamp] [WARN] [LoadBalancer] ✗ Backend localhost:8084 is unreachable: Connection refused
[timestamp] [INFO] [LoadBalancer] Load Balancer is ready and listening
```

✅ **Notice:** Load balancer detected the dead backend but still started!

#### Step 4: Send Requests

```bash
# Terminal 3
java ClientSimulator 12
```

#### Step 5: Analyze Behavior

```bash
grep "Forwarding request to backend" app.log | tail -12
```

**Expected Output:**
```
[timestamp] Forwarding request to backend localhost:8081  ← Success
[timestamp] Forwarding request to backend localhost:8082  ← Success
[timestamp] Forwarding request to backend localhost:8083  ← Success
[timestamp] Backend localhost:8084 is down, trying next...  ← Skip!
[timestamp] Forwarding request to backend localhost:8081  ← Success (next available)
[timestamp] Forwarding request to backend localhost:8082  ← Success
[timestamp] Forwarding request to backend localhost:8083  ← Success
[timestamp] Backend localhost:8084 is down, trying next...  ← Skip!
[timestamp] Forwarding request to backend localhost:8081  ← Success
...
```

**Count Distribution:**
```bash
grep "Response sent successfully" app.log | tail -12 | grep -o "8081\|8082\|8083\|8084" | sort | uniq -c
```

**Output:**
```
   4 8081  ← Got extra traffic (33.3% → 50% due to 8084 being down)
   4 8082  ← Got extra traffic
   4 8083  ← Got extra traffic
   0 8084  ← Dead, received nothing
```

### What Happened?

1. **Automatic Detection**: Load balancer tried 8084, got connection error
2. **Failover**: Automatically tried next backend (8081)
3. **Redistribution**: 3 backends now share 100% of traffic (33.3% each)
4. **Zero Downtime**: Clients didn't notice! All requests succeeded
5. **Logging**: All failures logged for admin awareness

---

### Scenario 2: Multiple Backend Failures

#### Test: Only 2 Backends Running

```bash
# Start only backends 8081 and 8082
# Modify ThreeBackends.java to:
private static final int[] PORTS = {8081, 8082};

# Compile and run
javac ThreeBackends.java
java ThreeBackends

# In another terminal
java LoadBalancer

# Send 10 requests
java ClientSimulator 10
```

**Expected Distribution:**
```
   5 8081  ← 50% of traffic
   5 8082  ← 50% of traffic
   0 8083  ← Down
   0 8084  ← Down
```

**Key Insight:** System still works! Load balancer adapts to available backends.

---

### Scenario 3: All Backends Down (Complete Failure)

#### Test: No Backends Running

```bash
# Terminal 1: DO NOT start BackendManager

# Terminal 2: Start load balancer anyway
java LoadBalancer
```

**Output:**
```
[timestamp] [INFO] [LoadBalancer] Starting Load Balancer on port 8080
[timestamp] [INFO] [LoadBalancer] Performing health check on backends...
[timestamp] [WARN] [LoadBalancer] ✗ Backend localhost:8081 is unreachable
[timestamp] [WARN] [LoadBalancer] ✗ Backend localhost:8082 is unreachable
[timestamp] [WARN] [LoadBalancer] ✗ Backend localhost:8083 is unreachable
[timestamp] [WARN] [LoadBalancer] ✗ Backend localhost:8084 is unreachable
[timestamp] [INFO] [LoadBalancer] Load Balancer is ready and listening
```

#### Send a Request:

```bash
java ClientSimulator 1
```

**What Happens:**
```
[timestamp] [INFO] [Client-1] Connecting to load balancer...
[timestamp] [INFO] [LoadBalancer] Accepted connection from /127.0.0.1
[timestamp] [WARN] [LoadBalancer] Backend localhost:8081 is down, trying next...
[timestamp] [WARN] [LoadBalancer] Backend localhost:8082 is down, trying next...
[timestamp] [WARN] [LoadBalancer] Backend localhost:8083 is down, trying next...
[timestamp] [WARN] [LoadBalancer] Backend localhost:8084 is down, trying next...
[timestamp] [ERROR] [LoadBalancer] No backends available for request
[timestamp] [ERROR] [Client-1] Error during request: Connection reset
```

**Result:** Client gets an error (as expected - no servers available!)

---

### Real-World Comparison

#### Amazon AWS Elastic Load Balancer

When backends fail:
1. **Health Checks**: Every 30 seconds, load balancer checks backends
2. **Automatic Removal**: Failed backends removed from rotation
3. **Alerts**: Administrators notified
4. **Auto-Scaling**: New backends automatically added

Our simple load balancer does #1 and #2!

#### Netflix's Approach

Netflix expects failures:
- **Chaos Monkey**: Randomly kills servers in production
- **Circuit Breaker**: Stops sending to failing backends
- **Fallback**: Returns cached data when backends down

### Summary of Use Case 3

**Load Balancer Behavior During Failures:**

| Scenario | Backends Up | Traffic Distribution | Client Experience |
|----------|-------------|---------------------|-------------------|
| Normal | 4/4 (100%) | 25% each | Perfect |
| 1 Backend Down | 3/4 (75%) | 33% each | Seamless (no errors) |
| 2 Backends Down | 2/4 (50%) | 50% each | Seamless |
| 3 Backends Down | 1/4 (25%) | 100% to one | Slow but working |
| All Down | 0/4 (0%) | N/A | Errors |

**Key Learnings:**
1. ✅ Load balancer provides **fault tolerance**
2. ✅ Automatic **failover** to healthy backends
3. ✅ **Graceful degradation** (system slows before failing)
4. ✅ **Transparent to clients** (they don't know about failures)
5. ⚠️ **Not magic** - if all backends die, system fails

---

<a name="reading-logs"></a>
## 9. Reading and Understanding app.log

### Log Format Breakdown

Every log line follows this format:
```
[TIMESTAMP] [LEVEL] [COMPONENT] MESSAGE
```

**Example:**
```
[2025-10-30 14:25:33.123] [INFO] [LoadBalancer] Forwarding request to backend localhost:8081
```

**Breaking it down:**
- `[2025-10-30 14:25:33.123]`: When it happened (down to milliseconds)
- `[INFO]`: Severity level (INFO, WARN, ERROR)
- `[LoadBalancer]`: Which component logged this
- `Forwarding request...`: The actual message

---

### Log Levels Explained

#### INFO (Information)
Normal operations. Everything is working as expected.

**Examples:**
```
[INFO] [Backend-8081] Backend server listening on port 8081
[INFO] [Client-1] Received response: Response from Backend on port 8082
[INFO] [LoadBalancer] Forwarding request to backend localhost:8083
```

**When to look at INFO logs:**
- Tracing request flow
- Verifying distribution
- Understanding timing

---

#### WARN (Warning)
Something abnormal, but not critical. System still functioning.

**Examples:**
```
[WARN] [LoadBalancer] ✗ Backend localhost:8084 is unreachable: Connection refused
[WARN] [LoadBalancer] Backend localhost:8081 is down, trying next...
```

**When to look at WARN logs:**
- Backend failures
- Degraded performance
- Potential problems

---

#### ERROR (Error)
Something failed. Action may be needed.

**Examples:**
```
[ERROR] [LoadBalancer] No backends available for request
[ERROR] [Client-3] Error during request: Connection reset
[ERROR] [Backend-8082] Error handling connection: Socket closed
```

**When to look at ERROR logs:**
- Client failures
- System crashes
- Debugging problems

---

### Common Log Patterns

#### Pattern 1: Successful Request Flow

```
[14:25:33.100] [INFO] [Client-5] Connecting to load balancer at localhost:8080
[14:25:33.105] [INFO] [Client-5] Connected successfully
[14:25:33.110] [INFO] [LoadBalancer] Accepted connection from /127.0.0.1
[14:25:33.115] [INFO] [LoadBalancer] Forwarding request to backend localhost:8082
[14:25:33.120] [INFO] [LoadBalancer] Connected to backend localhost:8082
[14:25:33.125] [INFO] [Backend-8082] Connection received from /127.0.0.1
[14:25:33.130] [INFO] [Backend-8082] Received request: GET / HTTP/1.1
[14:25:33.135] [INFO] [Backend-8082] Response sent successfully
[14:25:33.140] [INFO] [LoadBalancer] Forwarding completed for Client→Backend-8082
[14:25:33.145] [INFO] [LoadBalancer] Forwarding completed for Backend-8082→Client
[14:25:33.150] [INFO] [LoadBalancer] Request/response cycle completed for backend localhost:8082
[14:25:33.155] [INFO] [Client-5] Received response:
HTTP/1.1 200 OK
Content-Type: text/plain
Connection: close

Response from Backend on port 8082
Timestamp: Thu Oct 30 14:25:33 IST 2025
Request: GET / HTTP/1.1
[14:25:33.160] [INFO] [Client-5] Request completed successfully
```

**Reading This:**
1. Client-5 connects to load balancer (lines 1-2)
2. Load balancer accepts and selects backend 8082 (lines 3-4)
3. Load balancer connects to backend (line 5)
4. Backend receives and processes request (lines 6-8)
5. Bidirectional forwarding completes (lines 9-10)
6. Load balancer finishes proxying (line 11)
7. Client receives response (lines 12-13)

**Timeline**: Total ~60ms for complete request/response cycle

---

#### Pattern 2: Backend Failure and Recovery

```
[14:26:10.100] [INFO] [Client-7] Connecting to load balancer...
[14:26:10.105] [INFO] [LoadBalancer] Accepted connection from /127.0.0.1
[14:26:10.110] [WARN] [LoadBalancer] Backend localhost:8084 is down, trying next...
[14:26:10.115] [INFO] [LoadBalancer] Forwarding request to backend localhost:8081
[14:26:10.120] [INFO] [Backend-8081] Connection received from /127.0.0.1
[14:26:10.125] [INFO] [Backend-8081] Response sent successfully
[14:26:10.130] [INFO] [Client-7] Request completed successfully
```

**Reading This:**
1. Client connects normally
2. Load balancer tries 8084 first (round-robin)
3. 8084 is down! **[WARN]** logged
4. Automatically tries next: 8081
5. 8081 works, request succeeds
6. Client never knew there was a problem!

---

#### Pattern 3: Concurrent Requests

```
[14:27:00.100] [INFO] [Client-1] Connecting to load balancer...
[14:27:00.150] [INFO] [Client-2] Connecting to load balancer...
[14:27:00.105] [INFO] [LoadBalancer] Accepted connection from /127.0.0.1
[14:27:00.110] [INFO] [LoadBalancer] Forwarding request to backend localhost:8081
[14:27:00.155] [INFO] [LoadBalancer] Accepted connection from /127.0.0.1
[14:27:00.160] [INFO] [LoadBalancer] Forwarding request to backend localhost:8082
[14:27:00.115] [INFO] [Backend-8081] Connection received from /127.0.0.1
[14:27:00.165] [INFO] [Backend-8082] Connection received from /127.0.0.1
```

**Reading This:**
- Notice timestamps: 100, 105, 110, 115 vs 150, 155, 160, 165
- Two requests happening simultaneously!
- Client-1 → Backend-8081
- Client-2 → Backend-8082
- **This is multithreading in action!**

---

### Useful Log Analysis Commands

#### 1. Count Requests Per Backend
```bash
grep "Forwarding request to backend" app.log | cut -d':' -f4 | sort | uniq -c
```

**Output:**
```
  12 8081
  12 8082
  13 8083
  13 8084
```
**Meaning:** Nearly even distribution (round-robin working!)

---

#### 2. Find All Errors
```bash
grep "\[ERROR\]" app.log
```

**Example Output:**
```
[14:28:00.100] [ERROR] [LoadBalancer] No backends available for request
[14:28:05.200] [ERROR] [Client-10] Error during request: Connection reset
```

---

#### 3. Track a Specific Client
```bash
grep "Client-5" app.log
```

**Output:**
```
[14:25:30.100] [INFO] [ClientSimulator] Started Client-5
[14:25:31.250] [INFO] [Client-5] Waiting 1150ms before sending request
[14:25:32.400] [INFO] [Client-5] Connecting to load balancer...
[14:25:32.405] [INFO] [Client-5] Connected successfully
[14:25:32.410] [INFO] [Client-5] Sending request to load balancer
[14:25:32.415] [INFO] [Client-5] Waiting for response...
[14:25:32.450] [INFO] [Client-5] Received response: ...
[14:25:32.455] [INFO] [Client-5] Request completed successfully
```

---

#### 4. See Backend Activity
```bash
grep "Backend-8081" app.log | head -20
```

---

#### 5. Calculate Average Response Time

```bash
# Extract timestamps for "Connecting" and "Request completed" for each client
grep "Client-1" app.log | grep -E "Connecting|completed"
```

**Manual calculation:**
- Connected: 14:25:32.400
- Completed: 14:25:32.455
- **Response time: 55 milliseconds**

---

#### 6. Health Check Results
```bash
grep "is healthy\|is unreachable" app.log
```

**Output:**
```
[14:25:30.100] [INFO] [LoadBalancer] ✓ Backend localhost:8081 is healthy
[14:25:30.110] [INFO] [LoadBalancer] ✓ Backend localhost:8082 is healthy
[14:25:30.120] [INFO] [LoadBalancer] ✓ Backend localhost:8083 is healthy
[14:25:30.130] [WARN] [LoadBalancer] ✗ Backend localhost:8084 is unreachable
```

---

#### 7. Find Backend Failures During Runtime
```bash
grep "is down, trying next" app.log
```

---

#### 8. Timeline of Events
```bash
cat app.log | tail -50
```
Shows the last 50 log entries in chronological order.

---

### Debugging Scenarios Using Logs

#### Scenario: "My client isn't getting responses!"

**Step 1: Check if client connected**
```bash
grep "Client-3" app.log | grep "Connected successfully"
```

**If YES** → Client connected to load balancer ✅  
**If NO** → Connection problem ❌

**Step 2: Check if load balancer forwarded**
```bash
grep "Client-3" app.log -A 3 | grep "Forwarding request"
```

**If YES** → Load balancer is working ✅  
**If NO** → Load balancer issue ❌

**Step 3: Check if backend responded**
```bash
# Find which backend was used
grep "Forwarding request" app.log | tail -1

# Check that backend's logs
grep "Backend-8082" app.log | grep "Response sent"
```

**If YES** → Backend is working ✅  
**If NO** → Backend crashed ❌

---

#### Scenario: "Requests are going to only one backend!"

**Check distribution:**
```bash
grep "Forwarding request to backend" app.log | awk '{print $7}' | sort | uniq -c
```

**If uneven:**
- Check if some backends are down
- Check for errors in round-robin logic
- Verify all backends started successfully

---

#### Scenario: "System is slow!"

**Check for backend failures:**
```bash
grep "\[WARN\]" app.log | grep "is down"
```

**Many warnings?** → Backends keep failing, causing retries

**Check concurrent load:**
```bash
grep "Accepted connection" app.log | wc -l
```

**High number?** → Too many clients, backends overloaded

---

### Log File Maintenance

#### Log Rotation
```bash
# Backup old log
mv app.log app.log.old

# Start fresh
# Next run will create new app.log
```

#### View Real-Time Logs
```bash
# On Mac/Linux
tail -f app.log

# On Windows (PowerShell)
Get-Content app.log -Wait -Tail 20
```

#### Search Within Time Range
```bash
# Find all logs between 14:25 and 14:26
grep "2025-10-30 14:25" app.log
```

---

<a name="java-concepts"></a>
## 10. Key Java Concepts Explained

### For Students New to Java

---

### 10.1 Classes and Objects

**Analogy:** Class = Blueprint, Object = House built from blueprint

```java
public class Logger {  // ← This is the blueprint
    private String logFile;
    
    public Logger(String file) {  // ← Constructor: how to build it
        this.logFile = file;
    }
}

// Creating objects:
Logger logger1 = new Logger("app.log");      // ← House #1
Logger logger2 = new Logger("error.log");    // ← House #2
```

**In Our Project:**
- `Logger` class defines how logging works
- We create ONE logger object (singleton)
- All components use that same object

---

### 10.2 Static vs Instance

```java
public class Example {
    static int sharedCounter = 0;     // ← ONE copy for entire class
    int instanceCounter = 0;          // ← Each object has its own copy
    
    public void increment() {
        sharedCounter++;     // Everyone sees this change
        instanceCounter++;   // Only this object's counter increases
    }
}

Example obj1 = new Example();
Example obj2 = new Example();

obj1.increment();
// obj1.instanceCounter = 1
// obj2.instanceCounter = 0
// Example.sharedCounter = 1  (shared!)

obj2.increment();
// obj1.instanceCounter = 1
// obj2.instanceCounter = 1
// Example.sharedCounter = 2  (both contributed!)
```

**In Our Project:**
- `currentBackendIndex` is static → All threads share the same counter
- This is how round-robin works!

---

### 10.3 Threads and Multithreading

**Analogy:** Threads = Workers in a factory

**Single-Threaded (One Worker):**
```
Worker does Task 1 → Task 2 → Task 3 → Task 4
Total time: 40 seconds (10 seconds each)
```

**Multi-Threaded (Four Workers):**
```
Worker 1 does Task 1 ──┐
Worker 2 does Task 2 ──┼─→ All happen simultaneously!
Worker 3 does Task 3 ──┤
Worker 4 does Task 4 ──┘
Total time: 10 seconds
```

**Creating Threads in Java:**

```java
// Method 1: Implement Runnable
class MyTask implements Runnable {
    public void run() {
        System.out.println("Task running in thread: " + 
                         Thread.currentThread().getName());
    }
}

// Create and start thread
Thread thread = new Thread(new MyTask());
thread.setName("Worker-1");
thread.start();  // ← This starts parallel execution
```

**In Our Project:**
- Each client runs in its own thread → All can connect simultaneously
- Each backend runs in its own thread → All can accept connections simultaneously
- Each connection handler runs in its own thread → One backend handles many clients

---

### 10.4 Thread Safety and Synchronization

**The Problem:**
```java
class Counter {
    int count = 0;
    
    void increment() {
        count++;  // ← NOT ATOMIC! Actually 3 operations:
                  // 1. Read count
                  // 2. Add 1
                  // 3. Write back
    }
}

// Two threads running simultaneously:
Thread A: Read count (0) → Add 1 → Write (1)
Thread B: Read count (0) → Add 1 → Write (1)
// Result: count = 1 (should be 2!)
```

**Solution 1: Synchronized**
```java
class SafeCounter {
    int count = 0;
    
    synchronized void increment() {  // ← Only one thread at a time
        count++;
    }
}
```

**Solution 2: AtomicInteger**
```java
import java.util.concurrent.atomic.AtomicInteger;

class SafeCounter {
    AtomicInteger count = new AtomicInteger(0);
    
    void increment() {
        count.incrementAndGet();  // ← Atomic operation
    }
}
```

**In Our Project:**
- `Logger.log()` is synchronized → Only one thread writes to file at a time
- `currentBackendIndex` is AtomicInteger → Thread-safe counter
- Without these, logs would be corrupted and round-robin would fail!

---

### 10.5 Sockets and Networking

**Socket:** A connection between two computers (or two programs)

**Analogy:** Like a telephone call
- ServerSocket = Phone number people can call
- Socket.accept() = Answering the phone
- InputStream = Listening (ear)
- OutputStream = Speaking (mouth)

**Creating a Server:**
```java
ServerSocket serverSocket = new ServerSocket(8080);  // ← Listen on port 8080
System.out.println("Waiting for clients...");

Socket clientSocket = serverSocket.accept();  // ← BLOCKS until client connects
System.out.println("Client connected!");

// Now we can communicate
OutputStream out = clientSocket.getOutputStream();
out.write("Hello, client!".getBytes());
```

**Creating a Client:**
```java
Socket socket = new Socket("localhost", 8080);  // ← Connect to server
System.out.println("Connected to server!");

// Read server's message
InputStream in = socket.getInputStream();
byte[] buffer = new byte[1024];
int bytesRead = in.read(buffer);
String message = new String(buffer, 0, bytesRead);
System.out.println("Received: " + message);  // "Hello, client!"
```

**In Our Project:**
- Backends create ServerSockets (phone numbers)
- Load balancer creates a ServerSocket (its own phone number)
- Clients connect using Sockets (making calls)
- Load balancer connects to backends using Sockets (forwarding calls)

---

### 10.6 Input/Output Streams

**Streams:** Flow of data (like water in a pipe)

**InputStream:** Data flowing IN (reading)
**OutputStream:** Data flowing OUT (writing)

```java
// Low-level: Reading bytes
InputStream in = socket.getInputStream();
byte[] buffer = new byte[1024];
int bytesRead = in.read(buffer);  // Read up to 1024 bytes

// High-level: Reading text lines
BufferedReader reader = new BufferedReader(
    new InputStreamReader(in)
);
String line = reader.readLine();  // Read one line of text
```

**In Our Project:**
- We use both approaches
- BufferedReader for reading HTTP requests (text)
- Raw streams for forwarding (byte-by-byte copy)

---

### 10.7 Exception Handling

**Try-Catch:** Handling errors gracefully

```java
try {
    // Code that might fail
    Socket socket = new Socket("localhost", 8080);
    System.out.println("Connected!");
} catch (IOException e) {
    // Handle the error
    System.out.println("Connection failed: " + e.getMessage());
} finally {
    // Always runs (cleanup)
    System.out.println("Attempt finished");
}
```

**Try-with-Resources:** Automatic cleanup

```java
try (
    Socket socket = new Socket("localhost", 8080);  // ← Automatically closed
    BufferedReader in = new BufferedReader(...)     // ← Automatically closed
) {
    // Use socket and reader
} 
// Socket and reader automatically closed here, even if exception occurs!
```

**In Our Project:**
- We use try-catch to handle connection failures
- We use try-with-resources for automatic socket cleanup
- This prevents resource leaks (sockets left open)

---

### 10.8 The Singleton Pattern

**Problem:** We need exactly ONE logger, shared by everyone.

**Bad Solution:**
```java
// Everyone creates their own logger
Logger logger1 = new Logger();  // ← Opens app.log
Logger logger2 = new Logger();  // ← Opens app.log again!
// Result: File corruption!
```

**Good Solution (Singleton):**
```java
public class Logger {
    private static Logger instance;  // ← The one and only instance
    
    private Logger() {  // ← Private constructor (can't use "new")
        // Initialize
    }
    
    public static Logger getInstance() {
        if (instance == null) {
            instance = new Logger();  // ← Created only once
        }
        return instance;  // ← Always returns the same object
    }
}

// Usage:
Logger logger1 = Logger.getInstance();  // ← Creates it
Logger logger2 = Logger.getInstance();  // ← Returns same one
// logger1 == logger2  (true!)
```

---

### 10.9 String Formatting

**Building Strings:**

```java
// Concatenation (old way)
String message = "Hello, " + name + "! You are " + age + " years old.";

// String.format (better)
String message = String.format("Hello, %s! You are %d years old.", name, age);

// Common format specifiers:
// %s = String
// %d = Integer
// %f = Float
// %t = Date/Time
```

**In Our Project:**
```java
String logEntry = String.format("[%s] [%s] [%s] %s", 
                               timestamp, level, component, message);
// Output: [2025-10-30 14:25:33] [INFO] [LoadBalancer] Started
```

---

### 10.10 Arrays and Loops

**Arrays:** Fixed-size list

```java
int[] ports = {8081, 8082, 8083, 8084};

// Access by index
int firstPort = ports[0];  // 8081
int lastPort = ports[3];   // 8084

// Loop through
for (int i = 0; i < ports.length; i++) {
    System.out.println("Port " + i + ": " + ports[i]);
}

// Enhanced for loop
for (int port : ports) {
    System.out.println("Port: " + port);
}
```

**In Our Project:**
```java
private static final int[] BACKEND_PORTS = {8081, 8082, 8083, 8084};

for (int port : BACKEND_PORTS) {
    // Start a backend on this port
    BackendServer server = new BackendServer(port);
    new Thread(server).start();
}
```

---

<a name="troubleshooting"></a>
## 11. Troubleshooting Guide

### Problem 1: "Address already in use"

**Error Message:**
```
java.net.BindException: Address already in use
```

**Cause:** Another program is using that port.

**Solution:**

**On Mac/Linux:**
```bash
# Find process using port 8080
lsof -ti:8080

# Kill it
kill -9 $(lsof -ti:8080)

# Or kill all Java processes
killall java
```

**On Windows:**
```bash
# Find process
netstat -ano | findstr :8080

# Kill it (replace 1234 with actual PID)
taskkill /F /PID 1234
```

---

### Problem 2: Compilation Errors

**Error: "cannot find symbol"**
```
Logger.java:10: error: cannot find symbol
    PrintWriter writer;
    ^
```

**Cause:** Missing import statement.

**Solution:** Add at top of file:
```java
import java.io.PrintWriter;
```

---

**Error: "class X is public, should be in file X.java"**

**Cause:** Class name doesn't match filename.

**Solution:** 
- File `Logger.java` must contain `public class Logger`
- Rename file or class to match

---

### Problem 3: No Logs Appearing

**Check 1: Logger initialized?**
```bash
ls -l app.log
```

If file doesn't exist, logger didn't initialize.

**Check 2: Permissions?**
```bash
# On Mac/Linux
chmod 666 app.log

# On Windows
# Right-click → Properties → Security → Edit permissions
```

**Check 3: Logger.getInstance() called?**

Every class should have:
```java
private static final Logger logger = Logger.getInstance();
```

---

### Problem 4: Clients Time Out

**Symptoms:** Client hangs, no response.

**Possible Causes:**

1. **Backend not running**
   ```bash
   # Check if backends are running
   lsof -i :8081 -i :8082 -i :8083 -i :8084
   ```

2. **Load balancer not running**
   ```bash
   lsof -i :8080
   ```

3. **Firewall blocking**
   - Temporarily disable firewall
   - Or add exception for Java

4. **Wrong host/port**
   - Client should connect to `localhost:8080`
   - NOT directly to backend ports

---

### Problem 5: Uneven Distribution

**Symptom:** All requests go to one backend.

**Check:**
```bash
grep "Forwarding request to backend" app.log | cut -d':' -f4 | sort | uniq -c
```

**If uneven:**

1. **Check round-robin counter:**
   ```java
   // In LoadBalancer.java, add debug logging:
   logger.log("INFO", "LoadBalancer", 
              "Current index: " + currentBackendIndex.get());
   ```

2. **Check if backends keep failing:**
   ```bash
   grep "is down, trying next" app.log
   ```
   If many failures, some backends are crashing.

3. **Verify AtomicInteger:**
   ```java
   // Should be:
   private static AtomicInteger currentBackendIndex = new AtomicInteger(0);
   // NOT:
   private static int currentBackendIndex = 0;  // ← NOT thread-safe!
   ```

---

### Problem 6: "Connection reset by peer"

**Error Message:**
```
java.net.SocketException: Connection reset by peer
```

**Cause:** Remote side closed connection abruptly.

**Common Scenarios:**
1. Backend crashed while handling request
2. Load balancer closed connection before sending response
3. Network issue

**Solution:**
- Check backend logs for errors
- Ensure proper socket closing (use try-with-resources)
- Add more error logging

---

### Problem 7: OutOfMemoryError

**Error Message:**
```
java.lang.OutOfMemoryError: unable to create new native thread
```

**Cause:** Too many threads created.

**Solution:**
1. **Limit concurrent clients:**
   ```bash
   java ClientSimulator 50  # ← Don't use 1000+
   ```

2. **Increase heap size:**
   ```bash
   java -Xmx1024m LoadBalancer  # ← 1GB heap
   ```

3. **Use thread pools (advanced):**
   ```java
   ExecutorService executor = Executors.newFixedThreadPool(100);
   ```

---

### Problem 8: Logs Are Jumbled/Corrupted

**Symptom:** Log lines mixed together:
```
[timestamp] [INFO] [Clie[timestamp] [INFO] [Loadnt-1] Conne
```

**Cause:** Missing `synchronized` on logger.

**Solution:** Ensure Logger.log() is synchronized:
```java
public synchronized void log(String level, String component, String message) {
    // ...
}
```

---

<a name="exercises"></a>
## 12. Learning Exercises

### Exercise 1: Add Request Counter

**Goal:** Track total requests handled.

**Task:**
1. Add a static AtomicInteger to LoadBalancer:
   ```java
   private static AtomicInteger requestCount = new AtomicInteger(0);
   ```

2. Increment in ClientHandler:
   ```java
   int reqNum = requestCount.incrementAndGet();
   logger.log("INFO", "LoadBalancer", "Request #" + reqNum);
   ```

3. Run 50 clients and verify count reaches 50.

**Learning:** Understanding atomic operations, static variables.

---

### Exercise 2: Add Response Time Logging

**Goal:** Measure how long each request takes.

**Task:**
1. In ClientThread, record start time:
   ```java
   long startTime = System.currentTimeMillis();
   ```

2. After receiving response, calculate duration:
   ```java
   long duration = System.currentTimeMillis() - startTime;
   logger.log("INFO", clientName, "Response time: " + duration + "ms");
   ```

3. Run clients and check log:
   ```bash
   grep "Response time" app.log
   ```

**Learning:** Timing operations, performance measurement.

---

### Exercise 3: Implement Least Connections

**Goal:** Replace round-robin with least-connections algorithm.

**Task:**
1. Track connections per backend:
   ```java
   private static ConcurrentHashMap<String, AtomicInteger> activeConnections 
       = new ConcurrentHashMap<>();
   
   // Initialize
   for (String backend : BACKEND_HOSTS) {
       activeConnections.put(backend, new AtomicInteger(0));
   }
   ```

2. Modify getNextBackend():
   ```java
   String leastBusy = null;
   int minConnections = Integer.MAX_VALUE;
   
   for (String backend : BACKEND_HOSTS) {
       int count = activeConnections.get(backend).get();
       if (count < minConnections) {
           minConnections = count;
           leastBusy = backend;
       }
   }
   return leastBusy;
   ```

3. Increment/decrement counts:
   ```java
   // Before connecting
   activeConnections.get(backend).incrementAndGet();
   
   // After done (in finally block)
   activeConnections.get(backend).decrementAndGet();
   ```

**Learning:** Algorithms, concurrent data structures.

---

### Exercise 4: Add HTTP Status Codes

**Goal:** Return proper error codes when backends are down.

**Task:**
1. When no backends available, send 503 response:
   ```java
   if (backend == null) {
       String errorResponse = 
           "HTTP/1.1 503 Service Unavailable\r\n" +
           "Content-Type: text/plain\r\n" +
           "\r\n" +
           "All backends are down. Please try again later.\n";
       clientSocket.getOutputStream().write(errorResponse.getBytes());
       clientSocket.close();
       return;
   }
   ```

2. Test by stopping all backends.

**Learning:** HTTP protocol, error handling.

---

### Exercise 5: Configuration File

**Goal:** Load backend list from a config file.

**Task:**
1. Create `backends.txt`:
   ```
   localhost:8081
   localhost:8082
   localhost:8083
   localhost:8084
   ```

2. Read in LoadBalancer:
   ```java
   List<String> backends = new ArrayList<>();
   BufferedReader reader = new BufferedReader(new FileReader("backends.txt"));
   String line;
   while ((line = reader.readLine()) != null) {
       backends.add(line.trim());
   }
   BACKEND_HOSTS = backends.toArray(new String[0]);
   ```

3. Now you can add/remove backends without recompiling!

**Learning:** File I/O, configuration management.

---

### Exercise 6: Web Dashboard

**Goal:** Create a simple web page showing stats.

**Task:**
1. Add a stats endpoint in LoadBalancer:
   ```java
   if (request.startsWith("GET /stats")) {
       String html = "<html><body>" +
           "<h1>Load Balancer Stats</h1>" +
           "<p>Total requests: " + requestCount.get() + "</p>" +
           "<p>Backends: " + BACKEND_HOSTS.length + "</p>" +
           "</body></html>";
       
       out.print("HTTP/1.1 200 OK\r\n");
       out.print("Content-Type: text/html\r\n\r\n");
       out.print(html);
       return;
   }
   ```

2. Visit `http://localhost:8080/stats` in browser!

**Learning:** HTTP serving, HTML generation.

---

### Exercise 7: Weighted Round-Robin

**Goal:** Give some backends more traffic.

**Task:**
1. Define weights:
   ```java
   private static final int[] WEIGHTS = {3, 2, 2, 1};
   // Backend 8081 gets 3x more than 8084
   ```

2. Expand backend list:
   ```java
   List<String> weighted = new ArrayList<>();
   for (int i = 0; i < BACKEND_HOSTS.length; i++) {
       for (int j = 0; j < WEIGHTS[i]; j++) {
           weighted.add(BACKEND_HOSTS[i]);
       }
   }
   // Result: [8081, 8081, 8081, 8082, 8082, 8083, 8083, 8084]
   ```

3. Use this list for round-robin.

**Learning:** Weighted algorithms, data structures.

---

### Exercise 8: Circuit Breaker

**Goal:** Stop sending to consistently failing backends.

**Task:**
1. Track failure count:
   ```java
   ConcurrentHashMap<String, AtomicInteger> failures = new ConcurrentHashMap<>();
   ```

2. Increment on failure:
   ```java
   catch (IOException e) {
       failures.get(backend).incrementAndGet();
       if (failures.get(backend).get() > 5) {
           logger.log("ERROR", "LoadBalancer", 
                     "Circuit breaker opened for " + backend);
           // Skip this backend for next 60 seconds
       }
   }
   ```

3. Reset on success:
   ```java
   failures.get(backend).set(0);
   ```

**Learning:** Resilience patterns, fault tolerance.

---

## 13. Conclusion

### What You've Learned

By working through this project and guide, you now understand:

1. **Networking Fundamentals**
   - How clients and servers communicate
   - Sockets and ports
   - Request/response patterns

2. **Load Balancing**
   - Why it's necessary
   - Round-robin algorithm
   - Fault tolerance and failover

3. **Multithreading**
   - Concurrent execution
   - Thread safety
   - Synchronization

4. **System Design**
   - Component architecture
   - Data flow
   - Logging and observability

5. **Java Programming**
   - Classes and objects
   - Streams and I/O
   - Exception handling
   - Design patterns

### Real-World Applications

These concepts are used in:
- **Web Services**: Nginx, HAProxy, AWS ELB
- **Databases**: MySQL replication, MongoDB sharding
- **Microservices**: Kubernetes, Service Mesh
- **CDNs**: Cloudflare, Akamai
- **Cloud Platforms**: AWS, Google Cloud, Azure

### Next Steps

1. **Implement the exercises** to reinforce learning
2. **Experiment** with different scenarios
3. **Read logs** to understand behavior
4. **Modify code** to add features
5. **Study production load balancers** (Nginx, HAProxy)

### Resources for Further Learning

- **Java Networking**: Oracle Java Tutorials
- **Concurrency**: "Java Concurrency in Practice" book
- **System Design**: "Designing Data-Intensive Applications" book
- **Load Balancing**: Nginx documentation
- **Distributed Systems**: MIT 6.824 course

---

## Appendix: Quick Reference

### Common Commands

```bash
# Compile
javac *.java

# Run backends
java BackendManager

# Run load balancer
java LoadBalancer

# Run 10 clients
java ClientSimulator 10

# View logs
cat app.log
tail -f app.log

# Analyze distribution
grep "Forwarding request" app.log | cut -d':' -f4 | sort | uniq -c

# Find errors
grep ERROR app.log

# Kill all Java processes
killall java  # Mac/Linux
taskkill /F /IM java.exe  # Windows
```

### Port Reference

| Component | Port | Purpose |
|-----------|------|---------|
| Backend 1 | 8081 | First backend server |
| Backend 2 | 8082 | Second backend server |
| Backend 3 | 8083 | Third backend server |
| Backend 4 | 8084 | Fourth backend server |
| Load Balancer | 8080 | Entry point for all clients |

### Log Level Guide

| Level | When to Use | Example |
|-------|------------|---------|
| INFO | Normal operations | "Request forwarded to backend" |
| WARN | Abnormal but handled | "Backend is down, trying next" |
| ERROR | Failures requiring attention | "No backends available" |

---

**End of Educational Guide**

🎉 **Congratulations!** You now have a deep understanding of load balancers and distributed systems!