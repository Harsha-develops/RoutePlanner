import java.awt.Desktop;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

/** Starts the browser-based route planner. */
public class Main {
    public static void main(String[] args) throws Exception {
        RoutePlannerServer server = new RoutePlannerServer();
        server.start();
        URI address = URI.create("http://127.0.0.1:" + server.getPort());
        if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(address);
        System.out.println("Route Planner is running at " + address);
        System.out.println("Keep this window open while using the planner. Press Ctrl+C to stop it.");
        new CountDownLatch(1).await();
    }
}
