import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClientSimulator - Spawns multiple client threads to test the load balancer.
 * 
 * WHY: To verify the load balancer works correctly, we need to simulate
 * multiple concurrent clients sending requests.
 * 
 * HOW: Creates N client threads (configurable), each running in a continuous
 * cycle: connect → send request → hold connection → disconnect → wait → repeat.
 * 
 * UPDATED: Continuous cycling mode, configurable delays, metrics reporting,
 * controllable from GUI.
 */
public class ClientSimulator {
    private static String loadBalancerHost;
    private static int loadBalancerPort;
    private static final Logger logger = Logger.getInstance();
    private static final MetricsCollector metrics = MetricsCollector.getInstance();
    private static ConcurrentHashMap<String, ClientThread> activeClients = new ConcurrentHashMap<>();
    private static volatile boolean runContinuously = true;
    
    // Static initializer block to load configuration when class is loaded
    static {
        ConfigLoader config = ConfigLoader.getInstance();
        loadBalancerHost = config.getProperty("loadBalancerHost", "localhost");
        loadBalancerPort = config.getIntProperty("loadBalancerPort", 8080);

        logger.log("INFO", "ClientSimulator",
                  "Configuration loaded: connecting to " + loadBalancerHost + ":" + loadBalancerPort);
    }

    public static void main(String[] args) {
        // Configuration is already loaded by static initializer block
        ConfigLoader config = ConfigLoader.getInstance();

        // Default to config value if no argument provided
        int numClients = config.getIntProperty("defaultClientCount", 10);
        
        if (args.length > 0) {
            try {
                numClients = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.log("ERROR", "ClientSimulator", 
                          "Invalid number of clients. Using default: " + numClients);
            }
        }
        
        // Check for continuous mode flag
        if (args.length > 1 && args[1].equalsIgnoreCase("continuous")) {
            runContinuously = true;
            logger.log("INFO", "ClientSimulator", "Running in CONTINUOUS mode");
        } else if (args.length > 1 && args[1].equalsIgnoreCase("single")) {
            runContinuously = false;
            logger.log("INFO", "ClientSimulator", "Running in SINGLE-REQUEST mode");
        }
        
        startClients(numClients);
    }
    
    /**
     * Start specified number of client threads (called by main or GUI).
     */
    public static void startClients(int numClients) {
        logger.log("INFO", "ClientSimulator", 
                  "Starting " + numClients + " client threads...");
        
        // Spawn client threads
        for (int i = 1; i <= numClients; i++) {
            String clientId = "Client-" + i;
            ClientThread client = new ClientThread(clientId, runContinuously);
            Thread thread = new Thread(client);
            thread.setName(clientId);
            thread.start();
            activeClients.put(clientId, client);
            
            logger.log("INFO", "ClientSimulator", "Started " + clientId);
        }
        
        logger.log("INFO", "ClientSimulator", 
                  "All " + numClients + " clients have been started");
    }
    
    /**
     * Stop all client threads (for GUI control).
     */
    public static void stopAllClients() {
        logger.log("INFO", "ClientSimulator", "Stopping all client threads...");
        for (ClientThread client : activeClients.values()) {
            client.shutdown();
        }
        activeClients.clear();
        logger.log("INFO", "ClientSimulator", "All clients stopped");
    }
    
    /**
     * Get count of active clients.
     */
    public static int getActiveClientCount() {
        return activeClients.size();
    }
    
    /**
     * ClientThread - Individual client that connects to load balancer.
     * 
     * WHY: Each client needs to run independently and concurrently to simulate real load.
     * 
     * HOW: 
     * 1. Wait a random delay to stagger requests
     * 2. Connect to load balancer
     * 3. Send HTTP GET request
     * 4. Hold connection for random time (simulating data transfer)
     * 5. Read response and disconnect
     * 6. Wait before next cycle
     * 7. Repeat if in continuous mode
     * 
     * UPDATED: Continuous cycling, configurable delays, metrics reporting.
     */
    static class ClientThread implements Runnable {
        private String clientId;
        private Random random;
        private boolean continuous;
        private volatile boolean running = true;
        
        public ClientThread(String clientId, boolean continuous) {
            this.clientId = clientId;
            this.continuous = continuous;
            this.random = new Random();
        }
        
        public void shutdown() {
            running = false;
        }
        
        @Override
        public void run() {
            ConfigLoader config = ConfigLoader.getInstance();
            int delayMin = config.getIntProperty("clientDelayMin", 1000);
            int delayMax = config.getIntProperty("clientDelayMax", 5000);
            int holdMin = config.getIntProperty("connectionHoldMin", 1000);
            int holdMax = config.getIntProperty("connectionHoldMax", 30000);
            
            int cycleCount = 0;
            
            do {
                cycleCount++;
                try {
                    // Random delay before connecting (stagger requests)
                    int delay = delayMin + random.nextInt(delayMax - delayMin);
                    logger.log("INFO", clientId, 
                              "Cycle " + cycleCount + ": Waiting " + delay + 
                              "ms before sending request");
                    metrics.updateClientStatus(clientId, "waiting");
                    Thread.sleep(delay);
                    
                    if (!running) break;
                    
                    // Connect to load balancer
                    logger.log("INFO", clientId, 
                              "Cycle " + cycleCount + ": Connecting to load balancer at " + 
                              loadBalancerHost + ":" + loadBalancerPort);
                    metrics.updateClientStatus(clientId, "connecting");
                    
                    Socket socket = new Socket(loadBalancerHost, loadBalancerPort);
                    
                    logger.log("INFO", clientId, 
                              "Cycle " + cycleCount + ": Connected successfully");
                    metrics.updateClientStatus(clientId, "connected");
                    
                    // Send HTTP GET request
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                    );
                    
                    String request = String.format(
                        "GET / HTTP/1.1\r\n" +
                        "Host: %s\r\n" +
                        "User-Agent: %s\r\n" +
                        "Connection: close\r\n" +
                        "\r\n",
                        loadBalancerHost, clientId
                    );
                    
                    logger.log("INFO", clientId, 
                              "Cycle " + cycleCount + ": Sending request");
                    metrics.updateClientStatus(clientId, "sending");
                    out.print(request);
                    out.flush();
                    
                    // Read response immediately (don't wait)
                    logger.log("INFO", clientId,
                              "Cycle " + cycleCount + ": Waiting for response...");
                    metrics.updateClientStatus(clientId, "receiving");
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    // Read all response data
                    while ((line = in.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                    
                    logger.log("INFO", clientId, 
                              "Cycle " + cycleCount + ": Received response:\n" + 
                              response.toString().trim());
                    
                    if (!running) {
                        socket.close();
                        break;
                    }

                    // Simulate processing time AFTER receiving response
                    int holdTime = holdMin + random.nextInt(holdMax - holdMin);
                    logger.log("INFO", clientId,
                              "Cycle " + cycleCount + ": Processing response for " +
                              holdTime + "ms (simulating client-side processing)");
                    metrics.updateClientStatus(clientId, "processing");
                    Thread.sleep(holdTime);

                    // Close connection
                    in.close();
                    out.close();
                    socket.close();
                    
                    logger.log("INFO", clientId, 
                              "Cycle " + cycleCount + ": Request completed successfully");
                    metrics.updateClientStatus(clientId, "completed");
                    
                } catch (Exception e) {
                    logger.log("ERROR", clientId, 
                              "Cycle " + cycleCount + ": Error during request: " + 
                              e.getMessage());
                    metrics.updateClientStatus(clientId, "error");
                }
                
                // Wait before next cycle if continuous
                if (continuous && running) {
                    try {
                        int waitTime = delayMin + random.nextInt(delayMax - delayMin);
                        logger.log("INFO", clientId, 
                                  "Waiting " + waitTime + "ms before next cycle...");
                        metrics.updateClientStatus(clientId, "idle");
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                
            } while (continuous && running);
            
            logger.log("INFO", clientId, 
                      "Client terminated after " + cycleCount + " cycles");
            metrics.updateClientStatus(clientId, "terminated");
            activeClients.remove(clientId);
        }
    }
}
