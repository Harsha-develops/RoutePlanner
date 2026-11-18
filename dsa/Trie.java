import java.util.*;

/**
 * DATA STRUCTURE #3: Trie (Prefix Tree)
 * -------------------------------------
 * Har letter ek level hai. "ko" type karo to Koramangala jaise saare naam turant mil jaate hain.
 * Search time sirf prefix ki length par depend karta hai: O(L), total places par nahi.
 */
public class Trie {
    private static class TNode {
        final TreeMap<Character, TNode> kids = new TreeMap<>();  // sorted => alphabetical results
        int id = -1;                                              // >=0 means yahan ek naam khatam hota hai
    }

    private final TNode root = new TNode();

    public void insert(String word, int id) {
        TNode cur = root;
        for (char ch : word.toLowerCase().toCharArray()) {
            TNode next = cur.kids.get(ch);
            if (next == null) { next = new TNode(); cur.kids.put(ch, next); }
            cur = next;
        }
        cur.id = id;
    }

    /** Prefix se shuru hone waale saare place-ids. */
    public List<Integer> startsWith(String prefix) {
        TNode cur = root;
        for (char ch : prefix.toLowerCase().toCharArray()) {
            cur = cur.kids.get(ch);
            if (cur == null) return new ArrayList<>();
        }
        List<Integer> out = new ArrayList<>();
        collect(cur, out);
        return out;
    }

    private void collect(TNode n, List<Integer> out) {
        if (n.id >= 0) out.add(n.id);
        for (TNode k : n.kids.values()) collect(k, out);
    }
}
