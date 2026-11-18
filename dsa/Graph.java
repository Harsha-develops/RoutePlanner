import java.util.*;

/**
 * City ka map = WEIGHTED GRAPH (adjacency list).
 * Node = jagah (area / hospital / petrol pump / ATM), Edge = road jiska cost = length x traffic.
 */
public class Graph {
    public enum Type {
        AREA("Area"), HOSPITAL("Hospital"), PETROL("Petrol pump"), ATM("ATM");
        public final String label;
        Type(String label) { this.label = label; }
    }

    public static class Node {
        public final int id; public final double x, y; public final String name; public final Type type;
        Node(int id, double x, double y, String name, Type type) {
            this.id = id; this.x = x; this.y = y; this.name = name; this.type = type;
        }
    }

    public static class Edge {
        public final int a, b; public final double length;
        public double traffic = 1.0;   // 1 = normal, 2.5 = heavy traffic, INFINITY = road blocked
        Edge(int a, int b, double length) { this.a = a; this.b = b; this.length = length; }
        public double cost() { return length * traffic; }
        public boolean blocked() { return traffic == Double.POSITIVE_INFINITY; }
        public int other(int u) { return u == a ? b : a; }
        public void cycleTraffic() {
            traffic = traffic == 1.0 ? 2.5 : (traffic == 2.5 ? Double.POSITIVE_INFINITY : 1.0);
        }
    }

    public static final double W = 900, H = 620;
    public static double km(double len) { return len / 12.0; }

    private static final String[] NAMES = {
        "MG Road", "Indiranagar", "Koramangala", "Whitefield", "Hebbal", "Jayanagar", "Malleshwaram", "Yelahanka",
        "Marathahalli", "BTM Layout", "HSR Layout", "Electronic City", "Banashankari", "Rajajinagar", "Shivajinagar",
        "Majestic", "Lalbagh", "Cubbon Park", "Basavanagudi", "Domlur", "Ulsoor", "KR Puram", "Bellandur", "Sarjapur",
        "Hennur", "Yeshwanthpur", "Vijayanagar", "JP Nagar", "Frazer Town", "Richmond Town", "Peenya", "Bommanahalli",
        "Kengeri", "Nagawara", "Sadashivanagar", "Vasanth Nagar", "Kalyan Nagar", "Brookefield", "Varthur", "Mathikere"
    };

    public final List<Node> nodes = new ArrayList<>();
    public final List<Edge> edges = new ArrayList<>();
    public final List<List<Edge>> adj = new ArrayList<>();   // adjacency list
    public final Trie trie = new Trie();

    public Graph(long seed) {
        Random rnd = new Random(seed);
        // 1) random jagah par nodes rakho (ek doosre se thoda door)
        for (int tries = 0; nodes.size() < NAMES.length && tries < 30000; tries++) {
            double x = 45 + rnd.nextDouble() * (W - 90), y = 45 + rnd.nextDouble() * (H - 90);
            boolean ok = true;
            for (Node n : nodes) if (Math.hypot(n.x - x, n.y - y) < 66) { ok = false; break; }
            if (ok) { int i = nodes.size(); nodes.add(new Node(i, x, y, NAMES[i], typeFor(i))); }
        }
        int n = nodes.size();
        // 2) saare possible roads ko length ke hisaab se sort karo
        List<double[]> pairs = new ArrayList<>();
        for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++)
            pairs.add(new double[]{i, j, Math.hypot(nodes.get(i).x - nodes.get(j).x, nodes.get(i).y - nodes.get(j).y)});
        pairs.sort((p, q) -> Double.compare(p[2], q[2]));
        // 3) Kruskal (DSU) se connected backbone, phir har node ke 2 nearest roads => loops / alternate routes
        Set<Long> seen = new HashSet<>();
        DSU dsu = new DSU(n);
        for (double[] p : pairs) if (dsu.union((int) p[0], (int) p[1])) addEdge(seen, p);
        for (int i = 0; i < n; i++) {
            int c = 0;
            for (double[] p : pairs) if ((int) p[0] == i || (int) p[1] == i) { addEdge(seen, p); if (++c == 2) break; }
        }
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (Edge e : edges) { adj.get(e.a).add(e); adj.get(e.b).add(e); }
        for (Node nd : nodes) trie.insert(nd.name, nd.id);
    }

    private void addEdge(Set<Long> seen, double[] p) {
        int a = (int) p[0], b = (int) p[1];
        if (seen.add((long) a * 1000 + b)) edges.add(new Edge(a, b, p[2]));
    }

    private static Type typeFor(int i) {
        return i % 7 == 2 ? Type.HOSPITAL : i % 7 == 4 ? Type.PETROL : i % 7 == 6 ? Type.ATM : Type.AREA;
    }
}
