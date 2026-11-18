# Smart City Route Planner

An interactive city route planner that visualizes graph algorithms in a browser-based interface. The application uses Java's built-in HTTP server and the project's existing data structures and algorithms; no external libraries are required.

## Run the application

1. Install Java JDK 11 or newer. Check the installation with `javac -version`; download it from https://adoptium.net if needed.
2. Extract the project folder.
3. Start the application:
   - **Windows:** Open PowerShell in the project folder and run `./run.ps1`.
   - **Mac / Linux:** Run `./run.sh` in a terminal.

The Windows command and `run.sh` compile the project and open the planner in your default browser. The frontend source is in `frontend/`; the server provides the API required for live DSA features. Keep the terminal open while using the page, and press Ctrl+C there to stop the server.

## Use the planner

| Feature | How to use it |
|---|---|
| Choose a start and destination | Click two map locations in order, or select them through the search box. |
| Search locations | Type a place name; Trie autocomplete suggests matches. |
| Find a route | Select Dijkstra, A*, or Bidirectional Dijkstra, then choose **Find best route**. |
| Change road conditions | Click a road to cycle through clear, heavy traffic, and blocked. |
| Plan a multi-stop trip | Shift-click or right-click 1–8 stops, then choose **Multi-stop trip**. |
| Find a nearby facility | Select a start, then choose the nearest hospital, petrol station, or ATM. |
| Compare algorithms | Choose a start and destination, then select **Compare algorithms**. |
| Show the minimum road network | Select **Show minimum tree**. |
| Generate another city | Select **New city**. |
| View the DSA reference | Select **DSA guide & complexity**. |

## Source layout

| File | Purpose |
|---|---|
| `frontend/index.html` | Browser interface, styling, map drawing, controls, and animations. |
| `backend/RoutePlannerServer.java` | Local HTTP adapter connecting the browser interface to the Java graph. |
| `backend/Main.java` | Starts the local server and opens the browser. |
| `dsa/MinHeap.java` | Binary min-heap used by shortest-path search. |
| `dsa/DSU.java` | Disjoint set union for cycle checks and connectivity. |
| `dsa/Trie.java` | Place-name autocomplete. |
| `dsa/KDTree.java` | Nearest hospital, petrol station, and ATM lookup. |
| `dsa/Graph.java` | Weighted city graph and adjacency list. |
| `dsa/Algorithms.java` | Dijkstra, A*, Bidirectional Dijkstra, TSP bitmask DP, and Kruskal MST. |

## Complexity reference

- Dijkstra / A*: O((V + E) log V)
- Bidirectional Dijkstra: same worst-case complexity, with a smaller search area in many cases
- TSP bitmask dynamic programming: O(2^k × k²)
- KD-tree nearest lookup: O(log n) average
- Trie search: O(L)
- Kruskal MST: O(E log E)
- DSU: nearly O(1) per operation
