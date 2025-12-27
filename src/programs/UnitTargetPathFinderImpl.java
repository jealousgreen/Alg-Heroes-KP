package programs;

import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.Edge;
import com.battle.heroes.army.programs.UnitTargetPathFinder;

import java.util.*;

/**
 * Поиск кратчайшего пути на сетке 27x21 с препятствиями (юнитами).
 *
 * Используется A*:
 *  - g(n): длина пути от старта
 *  - h(n): эвристика Чебышёва (max(|dx|,|dy|)) для 8 направлений
 *
 * Сложность: O((W*H) log(W*H)) в худшем случае из-за priority queue.
 */
public class UnitTargetPathFinderImpl implements UnitTargetPathFinder {

    private static final int WIDTH = 27;
    private static final int HEIGHT = 21;

    private static final int[] DX = {-1,-1,-1, 0,0, 1,1,1};
    private static final int[] DY = {-1, 0, 1,-1,1,-1,0,1};

    @Override
    public List<Edge> getTargetPath(Unit attackUnit, Unit targetUnit, List<Unit> existingUnitList) {
        if (attackUnit == null || targetUnit == null) return Collections.emptyList();

        int sx = attackUnit.getxCoordinate();
        int sy = attackUnit.getyCoordinate();
        int tx = targetUnit.getxCoordinate();
        int ty = targetUnit.getyCoordinate();

        if (!inBounds(sx, sy) || !inBounds(tx, ty)) return Collections.emptyList();

        // препятствия: все живые юниты, кроме стартового и целевого
        boolean[][] blocked = new boolean[WIDTH][HEIGHT];
        if (existingUnitList != null) {
            for (Unit u : existingUnitList) {
                if (u == null || !u.isAlive()) continue;
                int x = u.getxCoordinate();
                int y = u.getyCoordinate();
                if (!inBounds(x, y)) continue;
                if ((x == sx && y == sy) || (x == tx && y == ty)) continue;
                blocked[x][y] = true;
            }
        }

        // A* структуры
        int[][] gScore = new int[WIDTH][HEIGHT];
        for (int i=0;i<WIDTH;i++) Arrays.fill(gScore[i], Integer.MAX_VALUE);

        Node[][] parent = new Node[WIDTH][HEIGHT];
        boolean[][] closed = new boolean[WIDTH][HEIGHT];

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));

        Node start = new Node(sx, sy, 0, heuristic(sx, sy, tx, ty));
        gScore[sx][sy] = 0;
        open.add(start);

        while (!open.isEmpty()) {
            Node cur = open.poll();
            if (closed[cur.x][cur.y]) continue;
            closed[cur.x][cur.y] = true;

            if (cur.x == tx && cur.y == ty) {
                return reconstructPath(parent, tx, ty);
            }

            for (int k=0;k<8;k++) {
                int nx = cur.x + DX[k];
                int ny = cur.y + DY[k];
                if (!inBounds(nx, ny)) continue;
                if (blocked[nx][ny]) continue;
                if (closed[nx][ny]) continue;

                int tentativeG = gScore[cur.x][cur.y] + 1; // стоимость шага = 1 (включая диагональ)
                if (tentativeG < gScore[nx][ny]) {
                    gScore[nx][ny] = tentativeG;
                    parent[nx][ny] = cur;
                    int f = tentativeG + heuristic(nx, ny, tx, ty);
                    open.add(new Node(nx, ny, tentativeG, f));
                }
            }
        }

        return Collections.emptyList();
    }

    private List<Edge> reconstructPath(Node[][] parent, int tx, int ty) {
        LinkedList<Edge> path = new LinkedList<>();
        int x = tx, y = ty;
        path.addFirst(new Edge(x, y));
        Node p = parent[x][y];
        while (p != null) {
            path.addFirst(new Edge(p.x, p.y));
            Node pp = parent[p.x][p.y];
            p = pp;
        }
        return path;
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }

    private int heuristic(int x, int y, int tx, int ty) {
        return Math.max(Math.abs(tx - x), Math.abs(ty - y));
    }

    private static class Node {
        final int x, y;
        final int g;
        final int f;

        Node(int x, int y, int g, int f) {
            this.x = x;
            this.y = y;
            this.g = g;
            this.f = f;
        }
    }
}
