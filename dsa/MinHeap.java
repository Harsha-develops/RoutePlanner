import java.util.Arrays;

/**
 * DATA STRUCTURE #1: Binary Min-Heap (Priority Queue)
 * ---------------------------------------------------
 * Sabse chhota priority wala element hamesha top par hota hai.
 * push() aur pop() dono O(log n). Dijkstra / A* ka dil yahi hai.
 */
public class MinHeap {
    private double[] pri = new double[64];   // priority (cost)
    private int[] val = new int[64];         // value (node id)
    private int size = 0;

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public double peekPriority() { return pri[0]; }

    /** Naya element daalo, fir usse upar "sift-up" karo jab tak sahi jagah na mil jaye. */
    public void push(double p, int v) {
        if (size == pri.length) { pri = Arrays.copyOf(pri, size * 2); val = Arrays.copyOf(val, size * 2); }
        int i = size++;
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (pri[parent] <= p) break;
            pri[i] = pri[parent]; val[i] = val[parent];
            i = parent;
        }
        pri[i] = p; val[i] = v;
    }

    /** Sabse chhota element nikalo, aakhri element ko root par rakh ke "sift-down" karo. */
    public int pop() {
        int top = val[0];
        size--;
        if (size > 0) {
            double p = pri[size]; int v = val[size];
            int i = 0;
            while (true) {
                int c = 2 * i + 1;
                if (c >= size) break;
                if (c + 1 < size && pri[c + 1] < pri[c]) c++;
                if (pri[c] >= p) break;
                pri[i] = pri[c]; val[i] = val[c];
                i = c;
            }
            pri[i] = p; val[i] = v;
        }
        return top;
    }
}
