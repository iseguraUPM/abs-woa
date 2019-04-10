/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.map;


import static es.upm.woa.agent.group1.map.CellTraslation.POS_OPERATORS;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultUndirectedGraph;

/**
 *
 * @author ISU
 */
public class WorldMap implements GameMap {
    
    private final int width;
    private final int height;
    
    private final Graph<MapCell, CellTraslation> mapGraph;
    private final Map<Integer, MapCell> mapCells;
    
    private WorldMap(int width, int height) {
        this.width = width;
        this.height = height;
        mapCells = new TreeMap<>();
        mapGraph = new DefaultUndirectedGraph<>(CellTraslation.class);
    }
    
    @Override
    public int getWidth() {
       return width;
    }
    
    @Override
    public int getHeight() {
        return height;
    }
    
    /**
     * Generates a map structure with the coordinates of a matrix. The origin is
     * at 1,1. The first coordinate raises DOWN of the origin. The second coordinate
     * raises RIGHT of the origin. Both coordinates are always ODD or both EVEN.
     * @param width of the map
     * @param height of the map
     * @return 
     */
    public static WorldMap getInstance(int width, int height) {
        if (width < 1 || height < 1) {
            return null;
        }
        
        WorldMap newInstance = new WorldMap(width, height);
        
        return newInstance;
    }
    
    private static Graph<MapCell, CellTraslation> buildMap(int width
            , int height) {
        Graph<MapCell, CellTraslation> mapGraph = new DefaultDirectedGraph<>(CellTraslation.class);
        
        MapCell currentCell;
        for (int i = 1; i < height; i += 2) {
            for (int j = 1; j < width; j += 2) {
                currentCell = new EmptyMapCell(1, 1);
                createNeighbours(width, height, currentCell, mapGraph);
            }
        }
        
        return mapGraph;
    }
    
    private static void createNeighbours(int width, int height, MapCell cell
            , Graph<MapCell, CellTraslation> mapGraph) {
        for (int[] translationVector : POS_OPERATORS) {
            int[] neighbourPosition = generateNeighbourPosition(width, height
                    , cell, translationVector);
            
            MapCell neighbour = new EmptyMapCell(neighbourPosition[0]
                    , neighbourPosition[1]);
            CellTraslation translation = new CellTraslation(translationVector);
            if (!mapGraph.containsVertex(neighbour)) {
                mapGraph.addVertex(neighbour);
            }
            if (!mapGraph.containsEdge(cell, neighbour)) {
                mapGraph.addEdge(cell, neighbour, translation);
            }
        }
    }
    
    private static int [] generateNeighbourPosition(int width, int height
            , MapCell cell, int [] translationVector) {
        int x = cell.getXCoord() + translationVector[0];
        int y = cell.getXCoord() + translationVector[1];
        
        return correctPosition(width, height, x, y);
    }
    
    private static int [] correctPosition(int width, int height, int x, int y) {
        int[] pos = new int[] {x, y};
        
        if (x >= 1 && y >= 1
                && x <= height && y <= width) {
            return pos;
        }
        
        // Case: position outside square map by the lower right corner
        if (x > height && y > width) {
            pos[0] = 1;
            pos[1] = 1;
            return pos;
        }
        
        if (x < 1 && y % 2 == 0) {
            pos[0] = closestLowerEven(height);
        }
        else if (x < 1) {
            pos[0] = closestLowerOdd(height);
        }
        
        if (x > height && y % 2 == 0) {
            pos[0] = 2;
        }
        if (x > height) {
            pos[0] = 1;
        }
        
        if (y < 1 && x % 2 == 0) {
            pos[1] = closestLowerEven(width);
        }
        else if (y < 1) {
            pos[1] = closestLowerOdd(width);
        }
        
        if (y > width && x % 2 == 0) {
            pos[1] = 2;
        }
        if (x > width) {
            pos[1] = 1;
        }
        
        return pos;
    }
    
    private static int closestLowerEven(int number) {
        if (number % 2 == 0) {
            return number;
        }
        else {
            return number - 1;
        }
    }
    
    private static int closestLowerOdd(int number) {
        if (number % 2 != 0) {
            return number;
        }
        else {
            return number - 1;
        }
    }

    
    @Override
    public boolean addCell(MapCell mapCell) {
        int index = toAbsolutePosition(mapCell.getXCoord(), mapCell.getYCoord());
        
        if (mapCells.containsKey(index))
            return false;
        
        mapCells.put(index, mapCell);
        return true;
    }
    
    
    @Override
    public MapCell getCellAt(int x, int y) throws NoSuchElementException {
        if (x < 1 || y < 1 || x > height || y > width) {
            throw new IndexOutOfBoundsException("Coordinates (" + x + "," + y
                    + ") exceed map dimensions");
        }
        
        int index = toAbsolutePosition(x, y);
        
        MapCell targetCell = mapCells.get(index);
        if (targetCell == null) {
            targetCell = new EmptyMapCell(x, y);
        }
        
        return targetCell;
    }
    
    private int toAbsolutePosition(int x, int y) {
        return (x - 1) * height + y;
    }

    @Override
    public Iterable<MapCell> getKnownCellsIterable() {
        return mapCells.values();
    }
   
}
