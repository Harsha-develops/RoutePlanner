import java.util.*;

/**
 * Saare algorithms ek jagah. Har method ke upar likha hai ki wo kya karta hai aur uski complexity.
 */
public class Algorithms {
    static final double INF = Double.POSITIVE_INFINITY;

    /** Search ka result: path, total cost, aur kin nodes ko explore kiya (animation ke liye). */
    public static class Result {
        public final String name;
        public List<Integer> path;                       // null => route nahi mila
        public double cost = INF;
        public List<Integer> explored = new ArrayList<>();
        public long nanos;
        Result(String name) { this.name = name; }
        public boolean found() { return path != null; }
    }

    private static double heuristic(Graph g, int a, int b) {   // seedhi line ki doori (kabhi overestimate nahi karti)
        Graph.Node p = g.nodes.get(a), q = g.nodes.get(b);
        return Math.hypot(p.x - q.x, p.y - q.y);
    }

    private static List<Integer> trace(int[] prev, int s, int t) {
        LinkedList<Integer> p = new LinkedList<>();
        for (int v = t; v != -1; v = prev[v]) { p.addFirst(v); if (v == s) break; }
        return p;
    }

    /**
     * DIJKSTRA (useAstar=false) ya A* (useAstar=true).
     * Min-heap se hamesha sabse sasta node pehle nikalte hain. A* mein priority = cost + heuristic,
     * isse search goal ki taraf jhukti hai aur kam nodes explore hote hain.  Time: O((V+E) log V)
     */
    public static Result search(Graph g, int s, int t, boolean useAstar) {
        long t0 = System.nanoTime();
        int n = g.nodes.size();
        double[] dist = new double[n]; Arrays.fill(dist, INF);
        int[] prev = new int[n]; Arrays.fill(prev, -1);
        boolean[] done = new boolean[n];
        MinHeap pq = new MinHeap();
        Result r = new Result(useAstar ? "A*" : "Dijkstra");
        dist[s] = 0; pq.push(useAstar ? heuristic(g, s, t) : 0, s);
        while (!pq.isEmpty()) {
            int u = pq.pop();
            if (done[u]) continue;            // purani (stale) entry skip
            done[u] = true; r.explored.add(u);
            if (u == t) break;
            for (Graph.Edge e : g.adj.get(u)) {
                double w = e.cost();
                if (w == INF) continue;       // blocked road
                int v = e.other(u);
                if (dist[u] + w < dist[v]) {  // relaxation
                    dist[v] = dist[u] + w; prev[v] = u;
                    pq.push(dist[v] + (useAstar ? heuristic(g, v, t) : 0), v);
                }
            }
        }
        r.cost = dist[t];
        r.path = dist[t] < INF ? trace(prev, s, t) : null;
        r.nanos = System.nanoTime() - t0;
        return r;
    }

    /**
     * BIDIRECTIONAL DIJKSTRA: start se aage aur end se peeche, dono taraf se ek saath search.
     * Jab dono milte hain aur koi behtar meeting point nahi bacha, ruk jaate hain. Search area ~aadha.
     */
    public static Result bidirectional(Graph g, int s, int t) {
        long t0 = System.nanoTime();
        int n = g.nodes.size();
        double[][] d = new double[2][n]; int[][] prev = new int[2][n]; boolean[][] done = new boolean[2][n];
        MinHeap[] pq = {new MinHeap(), new MinHeap()};
        for (int k = 0; k < 2; k++) { Arrays.fill(d[k], INF); Arrays.fill(prev[k], -1); }
        d[0][s] = 0; d[1][t] = 0; pq[0].push(0, s); pq[1].push(0, t);
        double best = INF; int meet = -1;
        Result r = new Result("Bidirectional");
        while (!pq[0].isEmpty() && !pq[1].isEmpty() && pq[0].peekPriority() + pq[1].peekPriority() < best) {
            int k = pq[0].size() <= pq[1].size() ? 0 : 1;   // chhoti queue wali taraf expand karo
            int u = pq[k].pop();
            if (done[k][u]) continue;
            done[k][u] = true; r.explored.add(u);
            for (Graph.Edge e : g.adj.get(u)) {
                double w = e.cost();
                if (w == INF) continue;
                int v = e.other(u);
                if (d[k][u] + w < d[k][v]) { d[k][v] = d[k][u] + w; prev[k][v] = u; pq[k].push(d[k][v], v); }
                if (d[0][v] + d[1][v] < best) { best = d[0][v] + d[1][v]; meet = v; }
            }
        }
        r.cost = best;
        if (meet >= 0) {
            List<Integer> front = trace(prev[0], s, meet);
            List<Integer> back = trace(prev[1], t, meet);   // t ... meet
            Collections.reverse(back);                      // meet ... t
            front.addAll(back.subList(1, back.size()));
            r.path = front;
        }
        r.nanos = System.nanoTime() - t0;
        return r;
    }

    /** TSP result: poora path, total cost, aur stops ko kis order mein visit karna hai. */
    public static class Tsp { public List<Integer> path, order; public double cost; }

    /**
     * MULTI-STOP ROUTE = Travelling Salesman Problem, DP + BITMASK se.
     * dp[mask][i] = "mask wale stops visit kiye, aakhri stop i" ka minimum cost.  Time: O(2^k * k^2)
     */
    public static Tsp tsp(Graph g, int s, List<Integer> stops, int t) {
        int k = stops.size();
        List<Integer> pts = new ArrayList<>(); pts.add(s); pts.addAll(stops); pts.add(t);
        Result[][] M = new Result[k + 2][k + 2];                 // har pair ke beech shortest path (A*)
        for (int i = 0; i < k + 2; i++) for (int j = 0; j < k + 2; j++)
            if (i != j) M[i][j] = search(g, pts.get(i), pts.get(j), true);
        int full = 1 << k;
        double[][] dp = new double[full][k]; int[][] par = new int[full][k];
        for (double[] row : dp) Arrays.fill(row, INF);
        for (int[] row : par) Arrays.fill(row, -1);
        for (int i = 0; i < k; i++) dp[1 << i][i] = M[0][i + 1].cost;
        for (int mask = 1; mask < full; mask++)
            for (int i = 0; i < k; i++) {
                if ((mask >> i & 1) == 0 || dp[mask][i] == INF) continue;
                for (int j = 0; j < k; j++) {
                    if ((mask >> j & 1) != 0) continue;
                    double c = dp[mask][i] + M[i + 1][j + 1].cost;
                    if (c < dp[mask | 1 << j][j]) { dp[mask | 1 << j][j] = c; par[mask | 1 << j][j] = i; }
                }
            }
        double best = INF; int bi = -1;
        for (int i = 0; i < k; i++) { double c = dp[full - 1][i] + M[i + 1][k + 1].cost; if (c < best) { best = c; bi = i; } }
        if (bi < 0) return null;
        LinkedList<Integer> ord = new LinkedList<>();
        for (int mask = full - 1, i = bi; i >= 0; ) { ord.addFirst(i); int p = par[mask][i]; mask ^= 1 << i; i = p; }
        Tsp res = new Tsp(); res.cost = best; res.order = new ArrayList<>(); res.path = new ArrayList<>(); res.path.add(s);
        int cur = 0;
        for (int o : ord) { res.path.addAll(M[cur][o + 1].path.subList(1, M[cur][o + 1].path.size())); res.order.add(stops.get(o)); cur = o + 1; }
        res.path.addAll(M[cur][k + 1].path.subList(1, M[cur][k + 1].path.size()));
        return res;
    }

    public static class Mst { public List<Graph.Edge> edges = new ArrayList<>(); public double total; }

    /** KRUSKAL MST: sabse sasti roads pehle, DSU se cycle avoid. Poori city ko minimum cost mein jodta hai. O(E log E) */
    public static Mst mst(Graph g) {
        List<Graph.Edge> sorted = new ArrayList<>();
        for (Graph.Edge e : g.edges) if (!e.blocked()) sorted.add(e);
        sorted.sort(Comparator.comparingDouble(Graph.Edge::cost));
        DSU dsu = new DSU(g.nodes.size());
        Mst m = new Mst();
        for (Graph.Edge e : sorted) if (dsu.union(e.a, e.b)) { m.edges.add(e); m.total += e.cost(); }
        return m;
    }

    /** DSU se connected zones ginta hai (1 = poori city connected). */
    public static int components(Graph g) {
        DSU dsu = new DSU(g.nodes.size());
        for (Graph.Edge e : g.edges) if (!e.blocked()) dsu.union(e.a, e.b);
        Set<Integer> roots = new HashSet<>();
        for (Graph.Node n : g.nodes) roots.add(dsu.find(n.id));
        return roots.size();
    }
}
