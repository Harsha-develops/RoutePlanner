/**
 * DATA STRUCTURE #2: Union-Find (Disjoint Set Union)
 * ---------------------------------------------------
 * Bataata hai ki do nodes ek hi group (component) mein hain ya nahi.
 * Path compression + union by rank ki wajah se lagbhag O(1) per operation.
 * Use: Kruskal MST mein cycle check, aur "city disconnected ho gayi?" check.
 */
public class DSU {
    private final int[] parent, rank;

    public DSU(int n) {
        parent = new int[n]; rank = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
    }

    public int find(int x) {
        while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; } // path compression
        return x;
    }

    /** true agar do alag groups ko jodna pada, false agar pehle se ek hi group mein the. */
    public boolean union(int a, int b) {
        int ra = find(a), rb = find(b);
        if (ra == rb) return false;
        if (rank[ra] < rank[rb]) { int t = ra; ra = rb; rb = t; }
        parent[rb] = ra;
        if (rank[ra] == rank[rb]) rank[ra]++;
        return true;
    }
}
