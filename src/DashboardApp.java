import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

/**
 * DashboardApp - Real-time monitoring GUI for the load balancer system.
 * 
 * WHY: Visual monitoring makes it easier to understand system behavior,
 * diagnose issues, and demonstrate load balancing concepts.
 * 
 * HOW: Swing JFrame with multiple panels showing:
 * - Overall metrics (requests, uptime, error rate)
 * - Backend server table (status, connections, latency)
 * - Request distribution bar chart
 * - Live log viewer
 * - Control buttons (start/stop backends, simulate clients, health check)
 * 
 * Uses Timer for periodic updates (every 2s) and SwingWorker for async operations.
 */
public class DashboardApp extends JFrame {
    private static final Logger logger = Logger.getInstance();
    private static final MetricsCollector metrics = MetricsCollector.getInstance();
    private static final ConfigLoader config = ConfigLoader.getInstance();
    
    // UI Components
    private JLabel totalRequestsLabel;
    private JLabel uptimeLabel;
    private JLabel errorRateLabel;
    private JLabel currentConnectionsLabel;
    
    private JTable backendTable;
    private DefaultTableModel backendTableModel;
    private ChartPanel chartPanel;
    private JTextArea logTextArea;
    
    private Timer updateTimer;
    
    public DashboardApp() {
        super("Load Balancer Dashboard - Real-Time Monitoring");
        initializeUI();
        startPeriodicUpdates();
        
        logger.log("INFO", "Dashboard", "Dashboard GUI initialized");
    }
    
    /**
     * Initialize the UI components and layout.
     */
    private void initializeUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        // Main container with border layout
        Container mainContainer = getContentPane();
        mainContainer.setLayout(new BorderLayout(10, 10));
        
        // Add padding around main container
        ((JPanel) mainContainer).setBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top panel: Overall metrics
        mainContainer.add(createMetricsPanel(), BorderLayout.NORTH);
        
        // Center panel: Backend table and chart
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            createBackendPanel(), createChartPanel());
        centerSplit.setDividerLocation(600);
        mainContainer.add(centerSplit, BorderLayout.CENTER);
        
        // Bottom panel: Log viewer
        mainContainer.add(createLogPanel(), BorderLayout.SOUTH);
        
        // Right panel: Control buttons
        mainContainer.add(createControlPanel(), BorderLayout.EAST);
    }
    
    /**
     * Create metrics panel showing overall statistics.
     */
    private JPanel createMetricsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 0));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY, 2),
            "System Metrics",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14)));
        
        totalRequestsLabel = createMetricLabel("Total Requests: 0");
        uptimeLabel = createMetricLabel("Uptime: 0s");
        errorRateLabel = createMetricLabel("Error Rate: 0.0%");
        currentConnectionsLabel = createMetricLabel("Active Connections: 0");
        
        panel.add(createMetricBox("📊", totalRequestsLabel, new Color(59, 130, 246)));
        panel.add(createMetricBox("⏱️", uptimeLabel, new Color(16, 185, 129)));
        panel.add(createMetricBox("⚠️", errorRateLabel, new Color(239, 68, 68)));
        panel.add(createMetricBox("🔗", currentConnectionsLabel, new Color(168, 85, 247)));
        
        return panel;
    }
    
    /**
     * Create a metric box with icon, label, and colored border.
     */
    private JPanel createMetricBox(String icon, JLabel label, Color borderColor) {
        JPanel box = new JPanel(new BorderLayout(5, 5));
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 3),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        box.setBackground(Color.WHITE);
        
        JLabel iconLabel = new JLabel(icon, SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        
        label.setHorizontalAlignment(SwingConstants.CENTER);
        
        box.add(iconLabel, BorderLayout.NORTH);
        box.add(label, BorderLayout.CENTER);
        
        return box;
    }
    
    /**
     * Create metric label with consistent styling.
     */
    private JLabel createMetricLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }
    
    /**
     * Create backend servers table panel.
     */
    private JPanel createBackendPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Backend Servers"));
        
        // Table columns
        String[] columns = {"Port", "Status", "Active Conn.", "Total Requests", "Avg Latency (ms)"};
        backendTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        backendTable = new JTable(backendTableModel);
        backendTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
        backendTable.setRowHeight(30);
        
        // Custom renderer for status column (colored)
        backendTable.getColumnModel().getColumn(1).setCellRenderer(
            new StatusCellRenderer());
        
        // Center align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 2; i < columns.length; i++) {
            backendTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        JScrollPane scrollPane = new JScrollPane(backendTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create request distribution chart panel.
     */
    private JPanel createChartPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Request Distribution"));
        
        chartPanel = new ChartPanel();
        panel.add(chartPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create log viewer panel.
     */
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Live Logs"));
        panel.setPreferredSize(new Dimension(0, 200));
        
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        logTextArea.setBackground(Color.BLACK);
        logTextArea.setForeground(Color.GREEN);
        
        JScrollPane scrollPane = new JScrollPane(logTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Clear logs button
        JButton clearButton = new JButton("Clear Logs");
        clearButton.addActionListener(e -> logTextArea.setText(""));
        panel.add(clearButton, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Create control panel with action buttons.
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Controls"));
        panel.setPreferredSize(new Dimension(200, 0));
        
        // Health check button
        JButton healthCheckBtn = createControlButton("Health Check");
        healthCheckBtn.addActionListener(e -> performHealthCheck());
        
        // Start clients button
        JButton startClientsBtn = createControlButton("Start Clients");
        startClientsBtn.addActionListener(e -> startClientsDialog());
        
        // Stop clients button
        JButton stopClientsBtn = createControlButton("Stop All Clients");
        stopClientsBtn.addActionListener(e -> stopAllClients());
        
        // Backend control buttons
        JButton killBackendBtn = createControlButton("Kill Backend");
        killBackendBtn.setBackground(new Color(239, 68, 68));
        killBackendBtn.setForeground(Color.BLACK);
        killBackendBtn.addActionListener(e -> killBackendDialog());
        
        JButton rebootBackendBtn = createControlButton("Reboot Backend");
        rebootBackendBtn.addActionListener(e -> rebootBackendDialog());
        
        // Reload config button
        JButton reloadConfigBtn = createControlButton("Reload Config");
        reloadConfigBtn.addActionListener(e -> reloadConfiguration());
        
        // System info button
        JButton systemInfoBtn = createControlButton("System Info");
        systemInfoBtn.addActionListener(e -> showSystemInfo());
        
        // Add buttons with spacing
        panel.add(Box.createVerticalStrut(10));
        panel.add(healthCheckBtn);
        panel.add(Box.createVerticalStrut(10));
        panel.add(startClientsBtn);
        panel.add(Box.createVerticalStrut(10));
        panel.add(stopClientsBtn);
        panel.add(Box.createVerticalStrut(20));
        panel.add(new JSeparator());
        panel.add(Box.createVerticalStrut(20));
        panel.add(killBackendBtn);
        panel.add(Box.createVerticalStrut(10));
        panel.add(rebootBackendBtn);
        panel.add(Box.createVerticalStrut(20));
        panel.add(new JSeparator());
        panel.add(Box.createVerticalStrut(20));
        panel.add(reloadConfigBtn);
        panel.add(Box.createVerticalStrut(10));
        panel.add(systemInfoBtn);
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    /**
     * Create styled control button.
     */
    private JButton createControlButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setMaximumSize(new Dimension(180, 40));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFocusPainted(false);
        return button;
    }
    
    /**
     * Start periodic UI updates using Timer.
     */
    private void startPeriodicUpdates() {
        int refreshInterval = config.getIntProperty("dashboardRefreshInterval", 2000);
        
        updateTimer = new Timer(refreshInterval, e -> {
            updateMetrics();
            updateBackendTable();
            updateChart();
            updateLogs();
        });
        
        updateTimer.start();
        logger.log("INFO", "Dashboard", "Started periodic updates (interval: " + 
                  refreshInterval + "ms)");
    }
    
    /**
     * Update overall metrics display.
     */
    private void updateMetrics() {
        totalRequestsLabel.setText("Total Requests: " + metrics.getTotalRequests());
        uptimeLabel.setText("Uptime: " + formatUptime(metrics.getUptimeSeconds()));
        errorRateLabel.setText(String.format("Error Rate: %.2f%%", metrics.getErrorRate()));
        currentConnectionsLabel.setText("Active Connections: " + 
                                       metrics.getCurrentConnections());
    }
    
    /**
     * Format uptime in human-readable form.
     */
    private String formatUptime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
    
    /**
     * Update backend servers table.
     */
    private void updateBackendTable() {
        backendTableModel.setRowCount(0);
        
        Map<Integer, MetricsCollector.BackendMetrics> backendMetrics = 
            metrics.getBackendMetrics();
        
        // Sort by port for consistent display
        List<Integer> ports = new ArrayList<>(backendMetrics.keySet());
        Collections.sort(ports);
        
        for (int port : ports) {
            MetricsCollector.BackendMetrics bm = backendMetrics.get(port);
            Object[] row = {
                port,
                bm.isUp() ? "UP" : "DOWN",
                bm.getActiveConnections(),
                bm.getTotalRequests(),
                String.format("%.2f", bm.getAverageLatency())
            };
            backendTableModel.addRow(row);
        }
    }
    
    /**
     * Update request distribution chart.
     */
    private void updateChart() {
        Map<Integer, MetricsCollector.BackendMetrics> backendMetrics = 
            metrics.getBackendMetrics();
        chartPanel.updateData(backendMetrics);
    }
    
    /**
     * Update log viewer with recent logs.
     */
    private void updateLogs() {
        int logLines = config.getIntProperty("logDisplayLines", 50);
        List<String> recentLogs = logger.getRecentLogs(logLines);
        
        StringBuilder logText = new StringBuilder();
        for (String log : recentLogs) {
            logText.append(log).append("\n");
        }
        
        logTextArea.setText(logText.toString());
        
        // Auto-scroll to bottom
        logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
    }
    
    /**
     * Perform health check in background thread.
     */
    private void performHealthCheck() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                logger.log("INFO", "Dashboard", "Manual health check initiated");
                LoadBalancer.performHealthCheck();
                return null;
            }
            
            @Override
            protected void done() {
                JOptionPane.showMessageDialog(DashboardApp.this,
                    "Health check completed. Check logs for results.",
                    "Health Check",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        };
        worker.execute();
    }
    
    /**
     * Show dialog to start client simulator.
     */
    private void startClientsDialog() {
        String input = JOptionPane.showInputDialog(this,
            "Enter number of clients to start:",
            "Start Clients",
            JOptionPane.QUESTION_MESSAGE);
        
        if (input != null && !input.trim().isEmpty()) {
            try {
                int numClients = Integer.parseInt(input.trim());
                if (numClients > 0 && numClients <= 100) {
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() {
                            logger.log("INFO", "Dashboard", 
                                      "Starting " + numClients + " clients from GUI");
                            ClientSimulator.startClients(numClients);
                            return null;
                        }
                    };
                    worker.execute();
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Please enter a number between 1 and 100.",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                    "Please enter a valid number.",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Stop all client threads.
     */
    private void stopAllClients() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Stop all active client threads?",
            "Confirm Stop",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    logger.log("INFO", "Dashboard", "Stopping all clients from GUI");
                    ClientSimulator.stopAllClients();
                    return null;
                }
                
                @Override
                protected void done() {
                    JOptionPane.showMessageDialog(DashboardApp.this,
                        "All clients stopped.",
                        "Clients Stopped",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            };
            worker.execute();
        }
    }
    
    /**
     * Show dialog to kill a backend server.
     */
    private void killBackendDialog() {
        Map<Integer, MetricsCollector.BackendMetrics> backendMetrics = 
            metrics.getBackendMetrics();
        
        List<Integer> runningPorts = new ArrayList<>();
        for (Map.Entry<Integer, MetricsCollector.BackendMetrics> entry : 
             backendMetrics.entrySet()) {
            if (entry.getValue().isUp()) {
                runningPorts.add(entry.getKey());
            }
        }
        
        if (runningPorts.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No backends are currently running.",
                "No Backends",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        Integer[] ports = runningPorts.toArray(new Integer[0]);
        Integer selectedPort = (Integer) JOptionPane.showInputDialog(this,
            "Select backend to kill:",
            "Kill Backend",
            JOptionPane.WARNING_MESSAGE,
            null,
            ports,
            ports[0]);
        
        if (selectedPort != null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to kill backend on port " + selectedPort + "?",
                "Confirm Kill",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        logger.log("WARN", "Dashboard", 
                                  "Killing backend on port " + selectedPort + " via GUI");
                        BackendManager.stopBackend(selectedPort);
                        return null;
                    }
                    
                    @Override
                    protected void done() {
                        JOptionPane.showMessageDialog(DashboardApp.this,
                            "Backend on port " + selectedPort + " has been killed.\n" +
                            "Load balancer will route traffic to other backends.",
                            "Backend Killed",
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                };
                worker.execute();
            }
        }
    }
    
    /**
     * Show dialog to reboot a backend server.
     */
    private void rebootBackendDialog() {
        Map<Integer, MetricsCollector.BackendMetrics> backendMetrics = 
            metrics.getBackendMetrics();
        
        List<Integer> downPorts = new ArrayList<>();
        for (Map.Entry<Integer, MetricsCollector.BackendMetrics> entry : 
             backendMetrics.entrySet()) {
            if (!entry.getValue().isUp()) {
                downPorts.add(entry.getKey());
            }
        }
        
        if (downPorts.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "All backends are currently running.\n" +
                "Use 'Kill Backend' first to simulate a crash.",
                "All Backends Running",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        Integer[] ports = downPorts.toArray(new Integer[0]);
        Integer selectedPort = (Integer) JOptionPane.showInputDialog(this,
            "Select backend to reboot:",
            "Reboot Backend",
            JOptionPane.QUESTION_MESSAGE,
            null,
            ports,
            ports[0]);
        
        if (selectedPort != null) {
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    logger.log("INFO", "Dashboard", 
                              "Rebooting backend on port " + selectedPort + " via GUI");
                    BackendManager.startBackend(selectedPort);
                    return null;
                }
                
                @Override
                protected void done() {
                    JOptionPane.showMessageDialog(DashboardApp.this,
                        "Backend on port " + selectedPort + " has been rebooted.\n" +
                        "It will now receive traffic from the load balancer.",
                        "Backend Rebooted",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            };
            worker.execute();
        }
    }
    
    /**
     * Reload configuration from file.
     */
    private void reloadConfiguration() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Reload configuration from config.properties?\n" +
            "Note: Some settings require restart to take effect.",
            "Reload Configuration",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                config.reload();
                logger.log("INFO", "Dashboard", "Configuration reloaded via GUI");
                JOptionPane.showMessageDialog(this,
                    "Configuration reloaded successfully.\n" +
                    "Some changes may require restarting components.",
                    "Configuration Reloaded",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                logger.log("ERROR", "Dashboard", "Failed to reload config: " + e.getMessage());
                JOptionPane.showMessageDialog(this,
                    "Failed to reload configuration:\n" + e.getMessage(),
                    "Reload Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Show system information dialog.
     */
    private void showSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        
        int activeThreads = Thread.activeCount();
        
        String info = String.format(
            "System Information:\n\n" +
            "Java Version: %s\n" +
            "Operating System: %s\n" +
            "OS Architecture: %s\n\n" +
            "Memory Usage:\n" +
            "  Used: %d MB\n" +
            "  Free: %d MB\n" +
            "  Total: %d MB\n" +
            "  Max: %d MB\n\n" +
            "Active Threads: %d\n" +
            "Active Clients: %d\n\n" +
            "Configuration:\n" +
            "  Load Balancer Port: %s\n" +
            "  Backend Ports: %s",
            System.getProperty("java.version"),
            System.getProperty("os.name"),
            System.getProperty("os.arch"),
            usedMemory,
            freeMemory,
            totalMemory,
            maxMemory,
            activeThreads,
            ClientSimulator.getActiveClientCount(),
            config.getProperty("loadBalancerPort", "N/A"),
            config.getProperty("backendPorts", "N/A")
        );
        
        JTextArea textArea = new JTextArea(info);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        
        JOptionPane.showMessageDialog(this,
            scrollPane,
            "System Information",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Custom cell renderer for status column (colored).
     */
    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                      boolean isSelected, boolean hasFocus,
                                                      int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected,
                                                             hasFocus, row, column);
            
            if (value != null) {
                String status = value.toString();
                if (status.equals("UP")) {
                    c.setBackground(new Color(187, 247, 208)); // Light green
                    c.setForeground(new Color(21, 128, 61)); // Dark green
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if (status.equals("DOWN")) {
                    c.setBackground(new Color(254, 202, 202)); // Light red
                    c.setForeground(new Color(185, 28, 28)); // Dark red
                    setFont(getFont().deriveFont(Font.BOLD));
                }
                setHorizontalAlignment(SwingConstants.CENTER);
            }
            
            return c;
        }
    }
    
    /**
     * ChartPanel - Custom JPanel for drawing request distribution bar chart.
     */
    private static class ChartPanel extends JPanel {
        private Map<Integer, Long> data = new HashMap<>();
        
        public ChartPanel() {
            setPreferredSize(new Dimension(400, 300));
            setBackground(Color.WHITE);
        }
        
        public void updateData(Map<Integer, MetricsCollector.BackendMetrics> backendMetrics) {
            data.clear();
            for (Map.Entry<Integer, MetricsCollector.BackendMetrics> entry : 
                 backendMetrics.entrySet()) {
                data.put(entry.getKey(), entry.getValue().getTotalRequests());
            }
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            if (data.isEmpty()) {
                g.setFont(new Font("Arial", Font.PLAIN, 14));
                g.setColor(Color.GRAY);
                String msg = "No data available";
                FontMetrics fm = g.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(msg)) / 2;
                int y = getHeight() / 2;
                g.drawString(msg, x, y);
                return;
            }
            
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                                RenderingHints.VALUE_ANTIALIAS_ON);
            
            int padding = 40;
            int chartWidth = getWidth() - 2 * padding;
            int chartHeight = getHeight() - 2 * padding;
            
            // Find max value for scaling
            long maxValue = data.values().stream().max(Long::compareTo).orElse(1L);
            if (maxValue == 0) maxValue = 1;
            
            // Sort ports for consistent display
            List<Integer> ports = new ArrayList<>(data.keySet());
            Collections.sort(ports);
            
            int barCount = ports.size();
            int barWidth = (chartWidth - (barCount - 1) * 10) / barCount;
            
            // Draw bars
            Color[] colors = {
                new Color(59, 130, 246),   // Blue
                new Color(16, 185, 129),   // Green
                new Color(239, 68, 68),    // Red
                new Color(168, 85, 247)    // Purple
            };
            
            for (int i = 0; i < ports.size(); i++) {
                int port = ports.get(i);
                long value = data.get(port);
                
                int barHeight = (int) ((value * chartHeight) / maxValue);
                int x = padding + i * (barWidth + 10);
                int y = padding + chartHeight - barHeight;
                
                // Draw bar with gradient
                GradientPaint gradient = new GradientPaint(
                    x, y, colors[i % colors.length],
                    x, y + barHeight, colors[i % colors.length].darker());
                g2d.setPaint(gradient);
                g2d.fillRect(x, y, barWidth, barHeight);
                
                // Draw border
                g2d.setColor(colors[i % colors.length].darker());
                g2d.drawRect(x, y, barWidth, barHeight);
                
                // Draw value on top of bar
                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                String valueStr = String.valueOf(value);
                FontMetrics fm = g2d.getFontMetrics();
                int textX = x + (barWidth - fm.stringWidth(valueStr)) / 2;
                int textY = y - 5;
                g2d.drawString(valueStr, textX, textY);
                
                // Draw port label
                g2d.setFont(new Font("Arial", Font.PLAIN, 11));
                String label = String.valueOf(port);
                int labelX = x + (barWidth - fm.stringWidth(label)) / 2;
                int labelY = padding + chartHeight + 20;
                g2d.drawString(label, labelX, labelY);
            }
            
            // Draw title
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            String title = "Requests per Backend";
            FontMetrics fm = g2d.getFontMetrics();
            int titleX = (getWidth() - fm.stringWidth(title)) / 2;
            g2d.drawString(title, titleX, 20);
            
            // Draw axes
            g2d.setColor(Color.GRAY);
            g2d.drawLine(padding, padding, padding, padding + chartHeight); // Y-axis
            g2d.drawLine(padding, padding + chartHeight, 
                        padding + chartWidth, padding + chartHeight); // X-axis
        }
    }
    
    /**
     * Main method to launch the dashboard.
     */
    public static void main(String[] args) {
        // Use system look and feel for native appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fall back to default if system L&F not available
        }
        
        SwingUtilities.invokeLater(() -> {
            DashboardApp dashboard = new DashboardApp();
            dashboard.setVisible(true);
        });
    }
}