import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

/**
 * ClientSimulator - Spawns multiple client threads to test the load balancer.
 * 
 * WHY: To verify the load balancer works correctly, we need to simulate
 * multiple concurrent clients sending requests.
 * 
 * HOW: Creates N client threads (configurable via command line), each connecting
 * to the load balancer (port 8080), sending a request, and reading the response.
 */
public class ClientSimulator {
    private static final String LOAD_BALANCER_HOST = "localhost";
    private static final int LOAD_BALANCER_PORT = 8080;
    private static final Logger logger = Logger.getInstance();
    
    public static void main(String[] args) {
        // Default to 10 clients if no argument provided
        int numClients = 10;
        
        if (args.length > 0) {
            try {
                numClients = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.log("ERROR", "ClientSimulator", 
                          "Invalid number of clients. Using default: 10");
            }
        }
        
        logger.log("INFO", "ClientSimulator", 
                  "Starting " + numClients + " client threads...");
        
        // Spawn client threads
        for (int i = 1; i <= numClients; i++) {
            ClientThread client = new ClientThread(i);
            Thread thread = new Thread(client);
            thread.setName("Client-" + i);
            thread.start();
            
            logger.log("INFO", "ClientSimulator", 
                      "Started Client-" + i);
        }
        
        logger.log("INFO", "ClientSimulator", 
                  "All " + numClients + " clients have been started");
    }
    
    /**
     * ClientThread - Individual client that connects to load balancer.
     * 
     * WHY: Each client needs to run independently and concurrently to simulate real load.
     * 
     * HOW: 
     * 1. Wait a random delay (0-2s) to stagger requests
     * 2. Connect to load balancer
     * 3. Send HTTP GET request with client name
     * 4. Read and log the response
     */
    static class ClientThread implements Runnable {
        private int clientId;
        private Random random;
        
        public ClientThread(int clientId) {
            this.clientId = clientId;
            this.random = new Random();
        }
        
        @Override
        public void run() {
            String clientName = "Client-" + clientId;
            
            try {
                // Random delay to simulate staggered requests (0-2 seconds)
                int delay = random.nextInt(2000);
                logger.log("INFO", clientName, 
                          "Waiting " + delay + "ms before sending request");
                Thread.sleep(delay);
                
                // Connect to load balancer
                logger.log("INFO", clientName, 
                          "Connecting to load balancer at " + LOAD_BALANCER_HOST + 
                          ":" + LOAD_BALANCER_PORT);
                
                Socket socket = new Socket(LOAD_BALANCER_HOST, LOAD_BALANCER_PORT);
                
                logger.log("INFO", clientName, "Connected successfully");
                
                // Send HTTP GET request
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
                );
                
                // Build HTTP request with client identification
                String request = String.format(
                    "GET / HTTP/1.1\r\n" +
                    "Host: %s\r\n" +
                    "User-Agent: %s\r\n" +
                    "Connection: close\r\n" +
                    "\r\n",
                    LOAD_BALANCER_HOST, clientName
                );
                
                logger.log("INFO", clientName, "Sending request to load balancer");
                out.print(request);
                out.flush();
                
                // Read response
                logger.log("INFO", clientName, "Waiting for response...");
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = in.readLine()) != null) {
                    response.append(line).append("\n");
                }
                
                logger.log("INFO", clientName, 
                          "Received response:\n" + response.toString().trim());
                
                // Close connection
                in.close();
                out.close();
                socket.close();
                
                logger.log("INFO", clientName, "Request completed successfully");
                
            } catch (Exception e) {
                logger.log("ERROR", clientName, 
                          "Error during request: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}