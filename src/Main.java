public class Main {
    public static void main(String[] args) {
        // Start backends in threads
        new Thread(() -> BackendManager.main(new String[]{})).start();

        // Wait for backends to start
        try { Thread.sleep(3000); } catch (Exception e) {}

        // Start load balancer in thread
        new Thread(() -> LoadBalancer.main(new String[]{})).start();

        // Wait for LB to start
        try { Thread.sleep(2000); } catch (Exception e) {}

        // Start dashboard on main thread (Swing needs this)
        DashboardApp.main(new String[]{});
    }
}