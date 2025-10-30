import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Thread-safe Logger utility for logging events across all components.
 * 
 * WHY: In a multithreaded application, multiple threads writing to the same file
 * simultaneously can corrupt data. We need synchronized access to ensure thread-safety.
 * 
 * HOW: Uses a singleton pattern with synchronized methods to ensure only one thread
 * writes to the log file at a time. All logs include timestamps for debugging.
 */
public class Logger {
    private static Logger instance;
    private static final String LOG_FILE = "app.log";
    private PrintWriter writer;
    private SimpleDateFormat dateFormat;
    
    /**
     * Private constructor for singleton pattern.
     * Opens the log file in append mode so logs persist across runs.
     */
    private Logger() {
        try {
            // FileWriter with 'true' parameter opens in append mode
            writer = new PrintWriter(new FileWriter(LOG_FILE, true), true);
            dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            log("INFO", "Logger", "Logger initialized - logging to " + LOG_FILE);
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
        
        // Also print to console for real-time monitoring
        System.out.println(logEntry);
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