import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BackendManager spawns independent backend servers based on config.properties.
 * 
 * WHY: In a real load balancer setup, you'd have multiple backend servers.
 * This simulates that by running multiple servers in separate threads.
 * 
 * HOW: Reads backend ports from config, creates BackendServer threads.
 * Each server handles requests and reports metrics to MetricsCollector.
 * 
 * UPDATED: Configurable ports, metrics reporting, controllable lifecycle for GUI.
 */
public class BackendManager {
    private static ConcurrentHashMap<Integer, BackendServer> servers = new ConcurrentHashMap<>();
    private static final Logger logger = Logger.getInstance();
    private static final MetricsCollector metrics = MetricsCollector.getInstance();
    
    public static void main(String[] args) {
        logger.log("INFO", "BackendManager", "Starting Backend Manager...");
        
        ConfigLoader config = ConfigLoader.getInstance();
        int[] backendPorts = config.getBackendPorts();
        
        // Start all backend servers
        for (int port : backendPorts) {
            startBackend(port);
        }
        
        logger.log("INFO", "BackendManager", 
                  "All " + backendPorts.length + " backend servers are running");
    }
    
    /**
     * Start a backend server on specified port (called by main or GUI).
     */
    public static synchronized void startBackend(int port) {
        if (servers.containsKey(port) && servers.get(port).isRunning()) {
            logger.log("WARN", "BackendManager", 
                      "Backend on port " + port + " is already running");
            return;
        }
        
        BackendServer server = new BackendServer(port);
        Thread thread = new Thread(server);
        thread.setName("Backend-" + port);
        thread.start();
        servers.put(port, server);
        
        metrics.setBackendStatus(port, true);
        logger.log("INFO", "BackendManager", 
                  "Started backend server on port " + port);
    }
    
    /**
     * Stop a backend server (for GUI crash simulation).
     */
    public static synchronized void stopBackend(int port) {
        BackendServer server = servers.get(port);
        if (server != null) {
            server.shutdown();
            servers.remove(port);
            metrics.setBackendStatus(port, false);
            logger.log("INFO", "BackendManager", 
                      "Stopped backend server on port " + port);
        } else {
            logger.log("WARN", "BackendManager", 
                      "No backend found on port " + port);
        }
    }
    
    /**
     * Check if a backend is running.
     */
    public static boolean isBackendRunning(int port) {
        BackendServer server = servers.get(port);
        return server != null && server.isRunning();
    }
    
    /**
     * BackendServer - Runnable that creates a server socket and handles client requests.
     * 
     * WHY: Each backend needs to run independently and concurrently.
     * 
     * HOW: Creates a ServerSocket, accepts connections in a loop, and spawns
     * a new thread for each connection to handle it concurrently.
     * 
     * UPDATED: Graceful shutdown support, metrics reporting.
     */
    static class BackendServer implements Runnable {
        private int port;
        private volatile boolean running = false;
        private ServerSocket serverSocket;
        
        public BackendServer(int port) {
            this.port = port;
        }
        
        public boolean isRunning() {
            return running;
        }
        
        public void shutdown() {
            running = false;
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (Exception e) {
                logger.log("ERROR", "Backend-" + port, 
                          "Error during shutdown: " + e.getMessage());
            }
        }
        
        @Override
        public void run() {
            running = true;
            try {
                serverSocket = new ServerSocket(port);
                logger.log("INFO", "Backend-" + port, 
                          "Backend server listening on port " + port);
                metrics.setBackendStatus(port, true);
                
                // Continuously accept connections
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        // Note: Connection metrics will be incremented in ConnectionHandler
                        // after determining if it's a health check or real request

                        // Handle each connection in a separate thread
                        Thread handler = new Thread(
                            new ConnectionHandler(clientSocket, port));
                        handler.start();
                    } catch (Exception e) {
                        if (running) {
                            logger.log("ERROR", "Backend-" + port, 
                                      "Error accepting connection: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                logger.log("ERROR", "Backend-" + port, 
                          "Failed to start backend server: " + e.getMessage());
            } finally {
                running = false;
                metrics.setBackendStatus(port, false);
                logger.log("INFO", "Backend-" + port, "Backend server stopped");
            }
        }
    }
    
    /**
     * ConnectionHandler - Handles an individual client connection to a backend.
     * 
     * WHY: We need to handle multiple clients simultaneously without blocking.
     * 
     * HOW: Reads the request from the client (forwarded by load balancer),
     * and sends back a response identifying which backend handled it.
     * 
     * UPDATED: Added latency tracking for metrics.
     */
    static class ConnectionHandler implements Runnable {
        private Socket socket;
        private int port;
        
        public ConnectionHandler(Socket socket, int port) {
            this.socket = socket;
            this.port = port;
        }
        
        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            boolean metricsIncremented = false;

            try (
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                logger.log("INFO", "Backend-" + port, 
                          "Connection received from " + socket.getInetAddress());
                
                // Read the first line to check for health check marker
                String firstLine = in.readLine();

                // Check if this is a health check
                if (firstLine != null && firstLine.equals("HEALTH_CHECK")) {
                    logger.log("INFO", "Backend-" + port,
                              "Health check received, skipping metrics");
                    // Close immediately without response or metrics
                    return;
                }

                // This is a real request, increment connection count IMMEDIATELY
                metrics.incrementBackendConnections(port);
                metricsIncremented = true;

                logger.log("INFO", "Backend-" + port,
                          "Real request detected, connection count incremented to " +
                          metrics.getBackendMetrics().get(port).getActiveConnections());

                // Read the request (could be multiple lines for HTTP)
                StringBuilder request = new StringBuilder();
                if (firstLine != null) {
                    request.append(firstLine);
                }

                String line;
                while ((line = in.readLine()) != null) {
                    // HTTP requests end with an empty line
                    if (line.isEmpty()) {
                        break;
                    }
                }
                
                logger.log("INFO", "Backend-" + port, 
                          "Received request: " + request.toString());
                
                // Generate response
                String response = String.format(
                    "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    "Response from Backend on port %d\n" +
                    "Timestamp: %s\n" +
                    "Request: %s\n",
                    port, new Date(), request.toString()
                );
                
                out.print(response);
                out.flush();
                
                long latency = System.currentTimeMillis() - startTime;
                metrics.recordBackendRequest(port, latency);
                
                logger.log("INFO", "Backend-" + port, 
                          "Response sent successfully (latency: " + latency + "ms, totalRequests now=" +
                          metrics.getBackendMetrics().get(port).getTotalRequests() + ")");

            } catch (Exception e) {
                logger.log("ERROR", "Backend-" + port, 
                          "Error handling connection: " + e.getMessage());
            } finally {
                // Only decrement if we actually incremented (not a health check)
                if (metricsIncremented) {
                    metrics.decrementBackendConnections(port);
                    logger.log("INFO", "Backend-" + port,
                              "Connection closed, active connections now: " +
                              metrics.getBackendMetrics().get(port).getActiveConnections());
                }
                try {
                    socket.close();
                } catch (Exception e) {
                    logger.log("ERROR", "Backend-" + port, 
                              "Error closing socket: " + e.getMessage());
                }
            }
        }
    }
}
