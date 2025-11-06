import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;

/**
 * MetricsCollector - Thread-safe centralized metrics for monitoring.
 * 
 * WHY: The GUI dashboard needs real-time statistics from all components.
 * Centralized collection avoids tight coupling between components.
 * 
 * HOW: Uses concurrent data structures and atomic types for thread-safety.
 * All components update metrics here, GUI reads them periodically.
 */
public class MetricsCollector {
    private static MetricsCollector instance;
    
    // Load balancer metrics
    private AtomicLong totalRequests = new AtomicLong(0);
    private AtomicInteger currentConnections = new AtomicInteger(0);
    private AtomicInteger failedRequests = new AtomicInteger(0);
    private long startTime = System.currentTimeMillis();
    
    // Backend metrics: port -> BackendMetrics
    private ConcurrentHashMap<Integer, BackendMetrics> backendMetrics;
    
    // Client metrics: clientId -> ClientMetrics
    private ConcurrentHashMap<String, ClientMetrics> clientMetrics;
    
    private MetricsCollector() {
        backendMetrics = new ConcurrentHashMap<>();
        clientMetrics = new ConcurrentHashMap<>();
        
        // Initialize backend metrics from config
        ConfigLoader config = ConfigLoader.getInstance();
        for (int port : config.getBackendPorts()) {
            backendMetrics.put(port, new BackendMetrics(port));
        }
    }
    
    public static synchronized MetricsCollector getInstance() {
        if (instance == null) {
            instance = new MetricsCollector();
        }
        return instance;
    }
    
    // Request tracking
    public void incrementTotalRequests() {
        totalRequests.incrementAndGet();
    }
    
    public void incrementCurrentConnections() {
        currentConnections.incrementAndGet();
    }
    
    public void decrementCurrentConnections() {
        currentConnections.decrementAndGet();
    }
    
    public void incrementFailedRequests() {
        failedRequests.incrementAndGet();
    }
    
    // Backend operations
    public void recordBackendRequest(int port, long latencyMs) {
        BackendMetrics metrics = backendMetrics.get(port);
        if (metrics != null) {
            metrics.recordRequest(latencyMs);
        }
    }
    
    public void setBackendStatus(int port, boolean isUp) {
        BackendMetrics metrics = backendMetrics.get(port);
        if (metrics != null) {
            metrics.setStatus(isUp);
        }
    }
    
    public void incrementBackendConnections(int port) {
        BackendMetrics metrics = backendMetrics.get(port);
        if (metrics != null) {
            metrics.incrementConnections();
        }
    }
    
    public void decrementBackendConnections(int port) {
        BackendMetrics metrics = backendMetrics.get(port);
        if (metrics != null) {
            metrics.decrementConnections();
        }
    }
    
    // Client operations
    public void updateClientStatus(String clientId, String status) {
        clientMetrics.computeIfAbsent(clientId, k -> new ClientMetrics(clientId))
                    .setStatus(status);
    }
    
    // Getters for GUI
    public long getTotalRequests() { return totalRequests.get(); }
    public int getCurrentConnections() { return currentConnections.get(); }
    public int getFailedRequests() { return failedRequests.get(); }
    public long getUptimeSeconds() { 
        return (System.currentTimeMillis() - startTime) / 1000; 
    }
    public double getErrorRate() {
        long total = totalRequests.get();
        return total > 0 ? (failedRequests.get() * 100.0 / total) : 0.0;
    }
    
    public Map<Integer, BackendMetrics> getBackendMetrics() {
        return new ConcurrentHashMap<>(backendMetrics);
    }
    
    public Map<String, ClientMetrics> getClientMetrics() {
        return new ConcurrentHashMap<>(clientMetrics);
    }
    
    /**
     * BackendMetrics - Per-backend statistics.
     */
    public static class BackendMetrics {
        private int port;
        private volatile boolean isUp = false;
        private AtomicInteger activeConnections = new AtomicInteger(0);
        private AtomicLong totalRequests = new AtomicLong(0);
        private AtomicLong totalLatency = new AtomicLong(0);
        
        public BackendMetrics(int port) {
            this.port = port;
        }
        
        public void recordRequest(long latencyMs) {
            totalRequests.incrementAndGet();
            totalLatency.addAndGet(latencyMs);
        }
        
        public void setStatus(boolean isUp) {
            this.isUp = isUp;
        }
        
        public void incrementConnections() {
            activeConnections.incrementAndGet();
        }
        
        public void decrementConnections() {
            activeConnections.decrementAndGet();
        }
        
        public int getPort() { return port; }
        public boolean isUp() { return isUp; }
        public int getActiveConnections() { return activeConnections.get(); }
        public long getTotalRequests() { return totalRequests.get(); }
        public double getAverageLatency() {
            long requests = totalRequests.get();
            return requests > 0 ? (totalLatency.get() / (double)requests) : 0.0;
        }
    }
    
    /**
     * ClientMetrics - Per-client statistics.
     */
    public static class ClientMetrics {
        private String clientId;
        private volatile String status = "idle";
        private volatile long lastRequestTime = 0;
        
        public ClientMetrics(String clientId) {
            this.clientId = clientId;
        }
        
        public void setStatus(String status) {
            this.status = status;
            this.lastRequestTime = System.currentTimeMillis();
        }
        
        public String getClientId() { return clientId; }
        public String getStatus() { return status; }
        public long getLastRequestTime() { return lastRequestTime; }
    }
}
