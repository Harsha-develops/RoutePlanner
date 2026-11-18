import java.util.*;

/**
 * DATA STRUCTURE #4: KD-Tree (2-D space partitioning)
 * ---------------------------------------------------
 * Har level par space ko x ya y axis se aadha-aadha baanta jaata hai.
 * "Sabse paas ka hospital?" jaise sawaal O(log n) average mein solve hote hain,
 * kyunki door wali branches ko skip kar diya jaata hai (pruning).
 */
public class KDTree {
    private static class K { Graph.Node p; K left, right; int axis; }

    private final K root;
    private Graph.Node best;
    private double bestDist;
    public int visited;   // kitne nodes check kiye (presentation ke liye)

    public KDTree(List<Graph.Node> points) { root = build(new ArrayList<>(points), 0); }

    private K build(List<Graph.Node> pts, int axis) {
        if (pts.isEmpty()) return null;
        pts.sort((a, b) -> Double.compare(axis == 0 ? a.x : a.y, axis == 0 ? b.x : b.y));
        int mid = pts.size() / 2;
        K k = new K();
        k.p = pts.get(mid); k.axis = axis;
        k.left = build(new ArrayList<>(pts.subList(0, mid)), 1 - axis);
        k.right = build(new ArrayList<>(pts.subList(mid + 1, pts.size())), 1 - axis);
        return k;
    }

    /** q ke sabse paas ka node (q khud ko chhod ke). */
    public Graph.Node nearest(Graph.Node q) {
        best = null; bestDist = Double.POSITIVE_INFINITY; visited = 0;
        search(root, q);
        return best;
    }

    private void search(K t, Graph.Node q) {
        if (t == null) return;
        visited++;
        double d = Math.hypot(t.p.x - q.x, t.p.y - q.y);
        if (t.p.id != q.id && d < bestDist) { bestDist = d; best = t.p; }
        double diff = (t.axis == 0 ? q.x - t.p.x : q.y - t.p.y);
        K near = diff < 0 ? t.left : t.right, far = diff < 0 ? t.right : t.left;
        search(near, q);
        if (Math.abs(diff) < bestDist) search(far, q);   // doosri side tabhi dekho jab wahan behtar mil sakta ho
    }
}
