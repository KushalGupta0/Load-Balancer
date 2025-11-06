import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * ConfigLoader - Singleton for loading and accessing configuration.
 * 
 * WHY: Centralized configuration allows easy customization without code changes.
 * Singleton pattern ensures all components use the same config instance.
 * 
 * HOW: Loads config.properties on first access, provides getters with defaults.
 */
public class ConfigLoader {
    private static ConfigLoader instance;
    private Properties properties;
    private static final String CONFIG_FILE = "config.properties";
    
    private ConfigLoader() {
        properties = new Properties();
        loadConfig();
    }
    
    public static synchronized ConfigLoader getInstance() {
        if (instance == null) {
            instance = new ConfigLoader();
        }
        return instance;
    }
    
    /**
     * Load configuration from file, use defaults if file missing.
     */
    private void loadConfig() {
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
            System.out.println("Configuration loaded from " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("Warning: Could not load " + CONFIG_FILE + 
                             ", using defaults: " + e.getMessage());
            setDefaults();
        }
    }
    
    /**
     * Reload configuration at runtime (for GUI refresh button).
     */
    public synchronized void reload() {
        properties.clear();
        loadConfig();
        System.out.println("Configuration reloaded");
    }
    
    /**
     * Set default values if config file is missing.
     */
    private void setDefaults() {
        properties.setProperty("loadBalancerPort", "8080");
        properties.setProperty("backendPorts", "8081,8082,8083,8084");
        properties.setProperty("defaultClientCount", "10");
        properties.setProperty("clientDelayMin", "1000");
        properties.setProperty("clientDelayMax", "5000");
        properties.setProperty("connectionHoldMin", "1000");
        properties.setProperty("connectionHoldMax", "30000");
        properties.setProperty("dashboardRefreshInterval", "2000");
        properties.setProperty("logDisplayLines", "50");
        properties.setProperty("healthCheckInterval", "5000");
        properties.setProperty("logFile", "app.log");
        properties.setProperty("enableConsoleOutput", "true");
    }
    
    /**
     * Get property with default fallback.
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Get integer property with default fallback.
     */
    public int getIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, 
                                   String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Get array of backend ports from comma-separated config.
     */
    public int[] getBackendPorts() {
        String portsString = getProperty("backendPorts", "8081,8082,8083,8084");
        String[] parts = portsString.split(",");
        int[] ports = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            ports[i] = Integer.parseInt(parts[i].trim());
        }
        return ports;
    }
}
