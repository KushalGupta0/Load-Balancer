import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

/**
 * BackendManager spawns 4 independent backend servers on ports 8081-8084.
 * 
 * WHY: In a real load balancer setup, you'd have multiple backend servers.
 * This simulates that by running 4 servers in separate threads.
 * 
 * HOW: Creates 4 BackendServer threads, each listening on its own port.
 * Each server handles requests by echoing back a response with its port number.
 */
public class BackendManager {
    private static final int[] BACKEND_PORTS = {8081, 8082, 8083, 8084};
    private static final Logger logger = Logger.getInstance();
    
    public static void main(String[] args) {
        logger.log("INFO", "BackendManager", "Starting Backend Manager...");
        
        // Start all backend servers
        for (int port : BACKEND_PORTS) {
            BackendServer server = new BackendServer(port);
            Thread thread = new Thread(server);
            thread.setName("Backend-" + port);
            thread.start();
            logger.log("INFO", "BackendManager", 
                      "Started backend server on port " + port);
        }
        
        logger.log("INFO", "BackendManager", 
                  "All 4 backend servers are running and ready to accept connections");
    }
    
    /**
     * BackendServer - Runnable that creates a server socket and handles client requests.
     * 
     * WHY: Each backend needs to run independently and concurrently.
     * 
     * HOW: Creates a ServerSocket, accepts connections in a loop, and spawns
     * a new thread for each connection to handle it concurrently.
     */
    static class BackendServer implements Runnable {
        private int port;
        
        public BackendServer(int port) {
            this.port = port;
        }
        
        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                logger.log("INFO", "Backend-" + port, 
                          "Backend server listening on port " + port);
                
                // Continuously accept connections
                while (true) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        // Handle each connection in a separate thread
                        Thread handler = new Thread(new ConnectionHandler(clientSocket, port));
                        handler.start();
                    } catch (Exception e) {
                        logger.log("ERROR", "Backend-" + port, 
                                  "Error accepting connection: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.log("ERROR", "Backend-" + port, 
                          "Failed to start backend server: " + e.getMessage());
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
            try (
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                logger.log("INFO", "Backend-" + port, 
                          "Connection received from " + socket.getInetAddress());
                
                // Read the request (could be multiple lines for HTTP)
                StringBuilder request = new StringBuilder();
                String line;
                boolean firstLine = true;
                
                while ((line = in.readLine()) != null) {
                    if (firstLine) {
                        request.append(line);
                        firstLine = false;
                    }
                    
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
                
                logger.log("INFO", "Backend-" + port, 
                          "Response sent successfully");
                
            } catch (Exception e) {
                logger.log("ERROR", "Backend-" + port, 
                          "Error handling connection: " + e.getMessage());
            } finally {
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