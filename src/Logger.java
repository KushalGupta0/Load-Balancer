import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe Logger utility for logging events across all components.
 * 
 * WHY: In a multithreaded application, multiple threads writing to the same file
 * simultaneously can corrupt data. We need synchronized access to ensure thread-safety.
 * 
 * HOW: Uses a singleton pattern with synchronized methods to ensure only one thread
 * writes to the log file at a time. All logs include timestamps for debugging.
 * 
 * UPDATED: Added in-memory log buffer for GUI dashboard access and configurable
 * console output via config.properties.
 */
public class Logger {
    private static Logger instance;
    private String logFile;
    private PrintWriter writer;
    private SimpleDateFormat dateFormat;
    private boolean enableConsoleOutput;
    
    // In-memory buffer for recent logs (for GUI display)
    private List<String> recentLogs;
    private static final int MAX_RECENT_LOGS = 200;
    
    /**
     * Private constructor for singleton pattern.
     * Opens the log file in append mode so logs persist across runs.
     */
    private Logger() {
        try {
            // Load configuration
            ConfigLoader config = ConfigLoader.getInstance();
            this.logFile = config.getProperty("logFile", "app.log");
            this.enableConsoleOutput = Boolean.parseBoolean(
                config.getProperty("enableConsoleOutput", "true"));
            
            // FileWriter with 'true' parameter opens in append mode
            writer = new PrintWriter(new FileWriter(logFile, true), true);
            dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            recentLogs = new ArrayList<>();
            
            log("INFO", "Logger", "Logger initialized - logging to " + logFile);
        } catch (IOException e) {
            System.err.println("Failed to initialize logger: " + e.getMessage());
        }
    }
    
    /**
     * Get the singleton Logger instance.
     * Synchronized to prevent race conditions during initialization.
     */
    public static synchronized Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }
    
    /**
     * Log a message with timestamp, level, component, and message.
     * Synchronized to ensure thread-safe file writing.
     * 
     * @param level The log level (INFO, ERROR, WARN, etc.)
     * @param component The component logging the message (LoadBalancer, Backend, Client)
     * @param message The log message
     */
    public synchronized void log(String level, String component, String message) {
        String timestamp = dateFormat.format(new Date());
        String logEntry = String.format("[%s] [%s] [%s] %s", 
                                       timestamp, level, component, message);
        writer.println(logEntry);
        writer.flush(); // Ensure immediate write to disk
        
        // Add to in-memory buffer for GUI
        recentLogs.add(logEntry);
        if (recentLogs.size() > MAX_RECENT_LOGS) {
            recentLogs.remove(0);
        }
        
        // Print to console if enabled
        if (enableConsoleOutput) {
            System.out.println(logEntry);
        }
    }
    
    /**
     * Get recent log entries for GUI display.
     * 
     * @param count Number of recent logs to retrieve
     * @return List of recent log entries
     */
    public synchronized List<String> getRecentLogs(int count) {
        int start = Math.max(0, recentLogs.size() - count);
        return new ArrayList<>(recentLogs.subList(start, recentLogs.size()));
    }
    
    /**
     * Close the logger when shutting down.
     */
    public synchronized void close() {
        if (writer != null) {
            writer.close();
        }
    }
}
