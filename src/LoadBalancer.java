import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LoadBalancer - Acts as a proxy server distributing requests across backends.
 * 
 * WHY: Load balancers distribute traffic across multiple servers to:
 * 1. Prevent any single server from being overwhelmed
 * 2. Provide redundancy (if one fails, others continue)
 * 3. Improve response times by spreading the load
 * 
 * HOW: Listens on port 8080, accepts client connections, selects a backend
 * using round-robin (cycling through 8081-8084), and forwards traffic bidirectionally.
 */
public class LoadBalancer {
    private static final int LOAD_BALANCER_PORT = 8080;
    private static final String[] BACKEND_HOSTS = {
        "localhost:8081",
        "localhost:8082", 
        "localhost:8083",
        "localhost:8084"
    };
    
    // AtomicInteger ensures thread-safe counter increments for round-robin
    private static AtomicInteger currentBackendIndex = new AtomicInteger(0);
    private static final Logger logger = Logger.getInstance();
    
    public static void main(String[] args) {
        logger.log("INFO", "LoadBalancer", "Starting Load Balancer on port " + LOAD_BALANCER_PORT);
        
        // Health check: Test connectivity to all backends
        performHealthCheck();
        
        try (ServerSocket serverSocket = new ServerSocket(LOAD_BALANCER_PORT)) {
            logger.log("INFO", "LoadBalancer", 
                      "Load Balancer is ready and listening for connections");
            
            // Main loop: Accept client connections
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logger.log("INFO", "LoadBalancer", 
                              "Accepted connection from " + clientSocket.getInetAddress());
                    
                    // Handle each client in a separate thread
                    Thread handler = new Thread(new ClientHandler(clientSocket));
                    handler.start();
                } catch (Exception e) {
                    logger.log("ERROR", "LoadBalancer", 
                              "Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.log("ERROR", "LoadBalancer", 
                      "Failed to start Load Balancer: " + e.getMessage());
        }
    }
    
    /**
     * Health check: Verify all backends are reachable on startup.
     * 
     * WHY: It's better to know immediately if backends are down rather than
     * discovering it when clients start connecting.
     */
    private static void performHealthCheck() {
        logger.log("INFO", "LoadBalancer", "Performing health check on backends...");
        
        for (String backend : BACKEND_HOSTS) {
            String[] parts = backend.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            
            try (Socket testSocket = new Socket(host, port)) {
                logger.log("INFO", "LoadBalancer", 
                          "✓ Backend " + backend + " is healthy");
            } catch (Exception e) {
                logger.log("WARN", "LoadBalancer", 
                          "✗ Backend " + backend + " is unreachable: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get the next backend using round-robin strategy.
     * 
     * WHY: Round-robin ensures even distribution of load across all backends.
     * 
     * HOW: Use AtomicInteger to safely increment and wrap around the index.
     * Try each backend in sequence until one connects (skip failed backends).
     */
    private static String getNextBackend() {
        // Try all backends in round-robin order
        for (int i = 0; i < BACKEND_HOSTS.length; i++) {
            int index = currentBackendIndex.getAndIncrement() % BACKEND_HOSTS.length;
            String backend = BACKEND_HOSTS[index];
            
            // Test if backend is reachable (quick check)
            String[] parts = backend.split(":");
            try (Socket testSocket = new Socket(parts[0], Integer.parseInt(parts[1]))) {
                return backend;
            } catch (Exception e) {
                logger.log("WARN", "LoadBalancer", 
                          "Backend " + backend + " is down, trying next...");
            }
        }
        
        // All backends failed
        logger.log("ERROR", "LoadBalancer", "All backends are unavailable!");
        return null;
    }
    
    /**
     * ClientHandler - Handles a single client connection and proxies to a backend.
     * 
     * WHY: Each client needs independent handling to support concurrency.
     * 
     * HOW: 
     * 1. Select a backend using round-robin
     * 2. Connect to the backend
     * 3. Create two forwarding threads: client→backend and backend→client
     * 4. This bidirectional forwarding allows full-duplex communication
     */
    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        
        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }
        
        @Override
        public void run() {
            String backend = getNextBackend();
            
            if (backend == null) {
                logger.log("ERROR", "LoadBalancer", "No backends available for request");
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    logger.log("ERROR", "LoadBalancer", "Error closing client socket");
                }
                return;
            }
            
            String[] parts = backend.split(":");
            String backendHost = parts[0];
            int backendPort = Integer.parseInt(parts[1]);
            
            logger.log("INFO", "LoadBalancer", 
                      "Forwarding request to backend " + backend);
            
            try {
                // Connect to the selected backend
                Socket backendSocket = new Socket(backendHost, backendPort);
                
                logger.log("INFO", "LoadBalancer", 
                          "Connected to backend " + backend);
                
                // Create two forwarding threads for bidirectional communication
                // Thread 1: Client → Backend (request forwarding)
                Thread clientToBackend = new Thread(
                    new Forwarder(clientSocket.getInputStream(), 
                                 backendSocket.getOutputStream(), 
                                 "Client→Backend-" + backendPort)
                );
                
                // Thread 2: Backend → Client (response forwarding)
                Thread backendToClient = new Thread(
                    new Forwarder(backendSocket.getInputStream(), 
                                 clientSocket.getOutputStream(), 
                                 "Backend-" + backendPort + "→Client")
                );
                
                clientToBackend.start();
                backendToClient.start();
                
                // Wait for both forwarding threads to complete
                clientToBackend.join();
                backendToClient.join();
                
                logger.log("INFO", "LoadBalancer", 
                          "Request/response cycle completed for backend " + backend);
                
                // Clean up
                backendSocket.close();
                clientSocket.close();
                
            } catch (Exception e) {
                logger.log("ERROR", "LoadBalancer", 
                          "Error proxying request to " + backend + ": " + e.getMessage());
                try {
                    clientSocket.close();
                } catch (IOException ex) {
                    logger.log("ERROR", "LoadBalancer", "Error closing sockets");
                }
            }
        }
    }
    
    /**
     * Forwarder - Forwards data from one stream to another.
     * 
     * WHY: To proxy traffic, we need to copy bytes from input to output streams.
     * We use separate threads for each direction to avoid blocking.
     * 
     * HOW: Continuously read from input and write to output until the stream closes.
     */
    static class Forwarder implements Runnable {
        private InputStream input;
        private OutputStream output;
        private String direction;
        
        public Forwarder(InputStream input, OutputStream output, String direction) {
            this.input = input;
            this.output = output;
            this.direction = direction;
        }
        
        @Override
        public void run() {
            try {
                byte[] buffer = new byte[4096];
                int bytesRead;
                
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                    output.flush();
                }
                
                logger.log("INFO", "LoadBalancer", 
                          "Forwarding completed for " + direction);
            } catch (Exception e) {
                // This is normal when connection closes
                logger.log("INFO", "LoadBalancer", 
                          "Forwarding ended for " + direction);
            }
        }
    }
}