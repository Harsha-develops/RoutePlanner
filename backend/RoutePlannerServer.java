import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/** Lightweight HTTP adapter. All graph and algorithm work stays in the existing DSA classes. */
public class RoutePlannerServer {
    private final HttpServer server;
    private Graph graph = new Graph(System.nanoTime());
    private boolean showMst;

    public RoutePlannerServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/state", this::state);
        server.createContext("/api/search", this::search);
        server.createContext("/api/action", this::action);
        server.createContext("/", this::page);
    }

    public void start() { server.start(); }
    public int getPort() { return server.getAddress().getPort(); }

    private void page(HttpExchange x) throws IOException {
        String requestPath = x.getRequestURI().getPath();
        String fileName, contentType;
        if ("/".equals(requestPath) || "/index.html".equals(requestPath)) {
            fileName = "index.html"; contentType = "text/html; charset=utf-8";
        } else if ("/app.js".equals(requestPath)) {
            fileName = "app.js"; contentType = "text/javascript; charset=utf-8";
        } else if ("/styles.css".equals(requestPath)) {
            fileName = "styles.css"; contentType = "text/css; charset=utf-8";
        } else {
            send(x, 404, "text/plain; charset=utf-8", "File not found."); return;
        }
        Path file = Paths.get("frontend", fileName);
        if (!Files.exists(file)) { send(x, 404, "text/plain; charset=utf-8", "Web interface file not found."); return; }
        byte[] body = Files.readAllBytes(file);
        x.getResponseHeaders().set("Content-Type", contentType);
        x.getResponseHeaders().set("Cache-Control", "no-store");
        x.sendResponseHeaders(200, body.length);
        try (OutputStream out = x.getResponseBody()) { out.write(body); }
    }

    private void state(HttpExchange x) throws IOException {
        if (!"GET".equals(x.getRequestMethod())) { send(x, 405, "application/json", "{}") ; return; }
        synchronized (this) { send(x, 200, "application/json; charset=utf-8", json(graphState())); }
    }

    private void search(HttpExchange x) throws IOException {
        Map<String, String> q = query(x);
        String prefix = q.getOrDefault("q", "");
        synchronized (this) {
            List<Integer> ids = prefix.isEmpty() ? Collections.emptyList() : graph.trie.startsWith(prefix);
            StringBuilder b = new StringBuilder("[\n");
            for (int i = 0; i < Math.min(6, ids.size()); i++) {
                if (i > 0) b.append(',');
                Graph.Node n = graph.nodes.get(ids.get(i));
                b.append("{\"id\":").append(n.id).append(",\"name\":").append(str(n.name)).append(",\"type\":").append(str(n.type.label)).append('}');
            }
            send(x, 200, "application/json; charset=utf-8", b.append(']').toString());
        }
    }

    private void action(HttpExchange x) throws IOException {
        if (!"POST".equals(x.getRequestMethod())) { send(x, 405, "application/json", "{}"); return; }
        Map<String, String> p = form(x);
        synchronized (this) {
            try { send(x, 200, "application/json; charset=utf-8", actionJson(p)); }
            catch (RuntimeException ex) { send(x, 400, "application/json; charset=utf-8", "{\"error\":" + str("Unable to complete that action.") + "}"); }
        }
    }

    private String actionJson(Map<String, String> p) {
        String act = p.getOrDefault("act", "");
        Map<String, Object> result = new LinkedHashMap<>();
        switch (act) {
            case "road": {
                Graph.Edge e = graph.edges.get(integer(p, "id")); e.cycleTraffic();
                int zones = Algorithms.components(graph);
                result.put("title", zones == 1 ? "Road updated" : "City divided into zones");
                result.put("message", zones == 1 ? "The city remains connected. Find a route again to see how the changes affect it." : "The city is now split into " + zones + " separate zones. Routes cannot cross between them.");
                result.put("zones", zones); break;
            }
            case "reset":
                for (Graph.Edge e : graph.edges) e.traffic = 1.0;
                result.put("title", "Roads reset"); result.put("message", "All roads are clear and traffic has been reset."); break;
            case "city":
                graph = new Graph(System.nanoTime()); showMst = false;
                result.put("title", "New city created"); result.put("message", "Choose a start and destination to plan a route."); break;
            case "mst":
                showMst = !showMst;
                Algorithms.Mst mst = Algorithms.mst(graph);
                result.put("title", showMst ? "Minimum spanning tree" : "Minimum spanning tree hidden");
                result.put("message", showMst ? mst.edges.size() + " roads connect the city at a total cost of " + km(mst.total) + ". Kruskal's algorithm uses DSU to avoid cycles." : "The minimum spanning tree is hidden.");
                result.put("mst", showMst); result.put("mstEdges", edgeIds(mst.edges)); result.put("cost", mst.total); break;
            case "route": {
                int s = integer(p, "start"), t = integer(p, "end");
                String a = p.getOrDefault("algorithm", "astar");
                Algorithms.Result r = run(s, t, a);
                result.putAll(routeResult(r, s, t)); break;
            }
            case "compare": {
                int s = integer(p, "start"), t = integer(p, "end");
                List<Object> rows = new ArrayList<>(); Algorithms.Result selected = null;
                String[] ids = {"dijkstra", "astar", "bidirectional"};
                for (String id : ids) {
                    Algorithms.Result r = null; long begin = System.nanoTime();
                    for (int i = 0; i < 200; i++) r = run(s, t, id);
                    Map<String, Object> row = new LinkedHashMap<>(); row.put("name", r.name); row.put("explored", r.explored.size()); row.put("distance", r.found() ? km(r.cost) : "—"); row.put("time", ((System.nanoTime() - begin) / 200 / 1000) + " μs"); rows.add(row);
                    if (id.equals("astar")) selected = r;
                }
                result.put("title", "Algorithm comparison"); result.put("message", "All three algorithms find the same optimal distance. The comparison shows how many nodes each one explores."); result.put("rows", rows);
                if (selected != null) { result.put("exploredNodes", selected.explored); result.put("path", selected.path == null ? Collections.emptyList() : selected.path); }
                break;
            }
            case "tsp": {
                int s = integer(p, "start"), t = integer(p, "end"); List<Integer> stops = ints(p.getOrDefault("stops", ""));
                if (stops.isEmpty() || stops.size() > 8) throw new IllegalArgumentException();
                Algorithms.Tsp r = Algorithms.tsp(graph, s, stops, t);
                result.put("title", r == null ? "Stops unreachable" : "Best visiting order");
                result.put("message", r == null ? "One or more stops cannot be reached because roads are blocked." : "Total distance: " + km(r.cost) + ". Bitmask dynamic programming checked " + (1 << stops.size()) + " subsets for " + stops.size() + " stops.");
                if (r != null) {
                    List<String> sequence = new ArrayList<>(); sequence.add(graph.nodes.get(s).name);
                    for (int id : r.order) sequence.add(graph.nodes.get(id).name);
                    sequence.add(graph.nodes.get(t).name); result.put("visitingOrder", sequence);
                }
                result.put("path", r == null ? Collections.emptyList() : r.path); result.put("order", r == null ? Collections.emptyList() : r.order); break;
            }
            case "nearest": {
                int s = integer(p, "start"); Graph.Type type = Graph.Type.valueOf(p.getOrDefault("type", "HOSPITAL"));
                List<Graph.Node> pool = new ArrayList<>(); for (Graph.Node n : graph.nodes) if (n.type == type) pool.add(n);
                KDTree kd = new KDTree(pool); Graph.Node best = kd.nearest(graph.nodes.get(s));
                Algorithms.Result r = Algorithms.search(graph, s, best.id, true);
                result.put("title", "Nearest " + best.type.label); result.put("message", best.name + (r.found() ? " is " + km(r.cost) + " away by road." : " was found, but the route is blocked.") + " The KD-tree checked " + kd.visited + " of " + pool.size() + " facilities.");
                result.put("end", best.id); result.put("path", r.path == null ? Collections.emptyList() : r.path); result.put("exploredNodes", r.explored); break;
            }
            default: throw new IllegalArgumentException();
        }
        result.put("state", graphState());
        return json(result);
    }

    private Algorithms.Result run(int s, int t, String a) {
        if (a.equals("dijkstra")) return Algorithms.search(graph, s, t, false);
        if (a.equals("bidirectional")) return Algorithms.bidirectional(graph, s, t);
        return Algorithms.search(graph, s, t, true);
    }

    private Map<String, Object> routeResult(Algorithms.Result r, int s, int t) {
        Map<String, Object> o = new LinkedHashMap<>(); o.put("title", r.found() ? r.name + " route" : "Route not found");
        o.put("message", r.found() ? graph.nodes.get(s).name + " to " + graph.nodes.get(t).name + ": " + km(r.cost) + ", " + r.explored.size() + " nodes explored, " + (r.path.size() - 1) + " road segments." : "Blocked roads have separated these locations. Reopen a road and try again.");
        o.put("path", r.path == null ? Collections.emptyList() : r.path); o.put("exploredNodes", r.explored); o.put("distance", r.found() ? km(r.cost) : "—"); return o;
    }

    private Map<String, Object> graphState() {
        Map<String, Object> data = new LinkedHashMap<>(); data.put("width", 900); data.put("height", 620);
        List<Object> nodes = new ArrayList<>();
        for (Graph.Node n : graph.nodes) { Map<String, Object> item = new LinkedHashMap<>(); item.put("id", n.id); item.put("x", n.x); item.put("y", n.y); item.put("name", n.name); item.put("type", n.type.name()); item.put("label", n.type.label); nodes.add(item); }
        List<Object> edges = new ArrayList<>();
        for (int i = 0; i < graph.edges.size(); i++) { Graph.Edge e = graph.edges.get(i); Map<String, Object> item = new LinkedHashMap<>(); item.put("id", i); item.put("a", e.a); item.put("b", e.b); item.put("length", e.length); item.put("traffic", e.blocked() ? "blocked" : e.traffic > 1 ? "heavy" : "clear"); edges.add(item); }
        List<Integer> mstIds = showMst ? edgeIds(Algorithms.mst(graph).edges) : Collections.emptyList();
        data.put("nodes", nodes); data.put("edges", edges); data.put("showMst", showMst); data.put("mstEdges", mstIds); return data;
    }

    private List<Integer> edgeIds(List<Graph.Edge> list) { List<Integer> ids = new ArrayList<>(); for (Graph.Edge target : list) ids.add(graph.edges.indexOf(target)); return ids; }
    // Graph edges are stable in the list; look up by identity when serializing an MST.
    private static int integer(Map<String, String> p, String key) { return Integer.parseInt(p.get(key)); }
    private static List<Integer> ints(String s) { List<Integer> r = new ArrayList<>(); if (!s.isEmpty()) for (String v : s.split(",")) r.add(Integer.parseInt(v)); return r; }
    private static String km(double d) { return String.format(Locale.US, "%.1f km", Graph.km(d)); }
    private static Map<String, String> query(HttpExchange x) { return decode(x.getRequestURI().getRawQuery()); }
    private static Map<String, String> form(HttpExchange x) throws IOException { return decode(new String(x.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)); }
    private static Map<String, String> decode(String input) {
        Map<String, String> m = new HashMap<>(); if (input == null) return m;
        for (String pair : input.split("&")) { String[] kv = pair.split("=", 2); try { m.put(URLDecoder.decode(kv[0], "UTF-8"), URLDecoder.decode(kv.length > 1 ? kv[1] : "", "UTF-8")); } catch (Exception ignored) { } }
        return m;
    }
    private static String str(String s) { return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\""; }
    private static String json(Object v) {
        if (v == null) return "null";
        if (v instanceof String) return str((String) v);
        if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);
        if (v instanceof Collection) { StringBuilder b = new StringBuilder("["); boolean first = true; for (Object o : (Collection<?>) v) { if (!first) b.append(','); first = false; b.append(json(o)); } return b.append(']').toString(); }
        if (v instanceof Map) { StringBuilder b = new StringBuilder("{"); boolean first = true; for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) { if (!first) b.append(','); first = false; b.append(str(String.valueOf(e.getKey()))).append(':').append(json(e.getValue())); } return b.append('}').toString(); }
        return str(String.valueOf(v));
    }
    private static void send(HttpExchange x, int code, String type, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8); x.getResponseHeaders().set("Content-Type", type); x.getResponseHeaders().set("Cache-Control", "no-store");
        x.sendResponseHeaders(code, data.length); try (OutputStream out = x.getResponseBody()) { out.write(data); }
    }
}
