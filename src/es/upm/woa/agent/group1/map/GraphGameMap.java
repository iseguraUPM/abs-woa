/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.map;

import static es.upm.woa.agent.group1.map.CellTranslation.POS_OPERATORS;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DirectedPseudograph;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 *
 * @author ISU
 */
public class GraphGameMap implements GameMap {
    
    private final int width;
    private final int height;
    
    private Graph<MapCell, CellTranslation> mapGraph;
    private DijkstraShortestPath<MapCell, CellTranslation> dijkstraShortestPath;
    
    private GraphGameMap(int width, int height) {
        this.width = width;
        this.height = height;
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
     * The maximum possible coordinate is [height,width]. The cells are wrapped
     * around in a way that the [1,1] cell is always adjacent to the [height,width]
     * cell and other 5 cells.
     * @param width of the map
     * @param height of the map
     * @return 
     */
    public static GraphGameMap getInstance(int width, int height) {
        if (width < 1 || height < 1) {
            return null;
        }
        
        GraphGameMap newInstance = new GraphGameMap(width, height);
        newInstance.mapGraph = new DirectedPseudograph<>(CellTranslation.class);
        
        return newInstance;
    }
    
    
    @Override
    public MapCell getCellAt(int x, int y) throws NoSuchElementException {
        if (x < 1 || y < 1 || x > height || y > width) {
            throw new NoSuchElementException("Coordinates (" + x + "," + y
                    + ") exceed map dimensions");
        }
        
        MapCell targetCell = mapGraph.vertexSet().stream()
                .filter(c -> c.getXCoord() == x && c.getYCoord() == y)
                .findFirst().orElse(null);
        
        if (targetCell == null) {
            throw new NoSuchElementException("Cell " + x + "," + y + " is not located"
                    + " in the map");
        }
        
        return targetCell;
    }

    @Override
    public Iterable<MapCell> getKnownCellsIterable() {
        return mapGraph.vertexSet();
    }
    
    /**
     * Compute the shortest path to the target cell.
     * @param source
     * @param target
     * @return the list of cells that make the path (including source)
     * or an empty list if there is not a viable path.
     */
    public List<MapCell> findShortestPath(MapCell source, MapCell target) {
        if (dijkstraShortestPath == null) {
            return new ArrayList<>();
        }
        
        GraphPath<MapCell, CellTranslation> shortestPath
                = dijkstraShortestPath.getPath(source, target);
        if (shortestPath == null) {
            return new ArrayList<>();
        }
        
        return shortestPath.getVertexList();
    }
    
    /**
     * Compute the shortest path to the nearest candidate that passes the
     * predicate filter.
     * @param source 
     * @param filterPredicate the predicate to filter candidate cells
     * @return the list of cells that make the path (including source)
     * or an empty list if there is not a viable path.
     */
    public List<MapCell> findShortestPathTo(MapCell source
            , Predicate<MapCell> filterPredicate) {
        List<MapCell> availableCells = mapGraph.vertexSet().stream()
                .filter(filterPredicate).collect(Collectors.toList());
        
        if (availableCells.isEmpty()) {
            return new ArrayList<>();
        }
        
        ShortestPathAlgorithm.SingleSourcePaths<MapCell, CellTranslation> paths
                = dijkstraShortestPath.getPaths(source);
        
        GraphPath<MapCell, CellTranslation> bestPath = null;
        for (MapCell candidate : availableCells) {
            GraphPath<MapCell, CellTranslation> candidatePath = paths.getPath(candidate);
            if (bestPath == null || bestPath.getLength()
                    > candidatePath.getLength()) {
                bestPath = candidatePath;
            } 
        }
        
        if (bestPath == null) {
            return new ArrayList<>();
        }
        else {
            return bestPath.getVertexList();
        }
    }
    
    @Override
    public boolean addCell(MapCell mapCell) {
        if (mapCell.getXCoord() < 1 || mapCell.getYCoord() < 1
                || mapCell.getXCoord() > height || mapCell.getYCoord() > width) {
            throw new IndexOutOfBoundsException("Coordinates ("
                    + mapCell.getXCoord() + "," + mapCell.getYCoord()
                    + ") exceed map dimensions");
        }
        
        if (mapGraph.containsVertex(mapCell)) {
            return false;
        }
        else {
            mapGraph.addVertex(mapCell);
            connectToNeighbours(mapCell);
            updateDijskstraPath();
            return true;
        }
    }
    
    private void updateDijskstraPath() {
        dijkstraShortestPath = new DijkstraShortestPath<>(mapGraph);
    }
    
    private void connectToNeighbours(MapCell cell) {
        for (int[] translationVector : POS_OPERATORS) {
            int[] neighbourPosition = generateNeighbourPosition(cell, translationVector);
            
            try {
                MapCell neighbour = getCellAt(neighbourPosition[0]
                    , neighbourPosition[1]);
                
                CellTranslation translation = new CellTranslation(translationVector);
                mapGraph.addEdge(cell, neighbour, translation);
                mapGraph.addEdge(neighbour, cell, translation.generateInverse());
            }
            catch (NoSuchElementException ex) {}
        }
    }
    
    private int [] generateNeighbourPosition(MapCell cell, int [] translationVector) {       
        int[] neighbourPosition = GameMapCoordinate.applyTranslation(width
                , height, cell.getXCoord(), cell.getYCoord(), translationVector);
        
        if (neighbourPosition == null) {
            // NOTE: should not reach
            throw new NoSuchElementException();
        }
        
        return neighbourPosition;
    }
    
    
    
}
