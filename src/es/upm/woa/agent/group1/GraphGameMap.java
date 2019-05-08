/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;


import es.upm.woa.agent.group1.map.CellTranslation;
import es.upm.woa.agent.group1.map.GameMap;
import es.upm.woa.agent.group1.map.GameMapCoordinate;
import es.upm.woa.agent.group1.map.MapCell;

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
class GraphGameMap implements GameMap {
    
    private Graph<MapCell, CellTranslation> mapGraph;
    private DijkstraShortestPath<MapCell, CellTranslation> dijkstraShortestPath;
    
    private GraphGameMap() {
    }
    
    @Override
    public int getWidth() {
       return 0;
    }
    
    @Override
    public int getHeight() {
        return 0;
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
    public static GraphGameMap getInstance() {
        
        GraphGameMap newInstance = new GraphGameMap();
        newInstance.mapGraph = new DirectedPseudograph<>(CellTranslation.class);
        
        return newInstance;
    }
    
    
    @Override
    public MapCell getCellAt(int x, int y) throws NoSuchElementException {
        if (x < 1 || y < 1) {
            throw new NoSuchElementException("Coordinates (" + x + "," + y
                    + ") exceed map dimensions");
        }
        
        MapCell targetCell = mapGraph.vertexSet().stream()
                .filter(c -> c.getXCoord() == x && c.getYCoord() == y)
                .findFirst().orElse(null);
        
        if (targetCell == null) {
            throw new NoSuchElementException("Cell [" + x + "," + y + "] is not located"
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
     * @return the sequence of translations required that make the path
     * or an empty list if there is not a viable path.
     */
    public List<CellTranslation> findShortestPath(MapCell source, MapCell target) {
        if (dijkstraShortestPath == null) {
            updateDijskstraPath();
        }
        
        GraphPath<MapCell, CellTranslation> shortestPath
                = dijkstraShortestPath.getPath(source, target);
        if (shortestPath == null) {
            return new ArrayList<>();
        }
        
        return shortestPath.getEdgeList();
    }
    
    /**
     * Compute the shortest path to the nearest candidate that passes the
     * predicate filter.
     * @param source 
     * @param filterPredicate the predicate to filter candidate cells
     * @return the sequence of translations required that make the path
     * or an empty list if there is not a viable path.
     */
    public List<CellTranslation> findShortestPathTo(MapCell source
            , Predicate<MapCell> filterPredicate) {
        List<MapCell> availableCells = mapGraph.vertexSet().stream()
                .filter(filterPredicate).collect(Collectors.toList());
        
        if (availableCells.isEmpty()) {
            return new ArrayList<>();
        }
        
        if (dijkstraShortestPath == null) {
            updateDijskstraPath();
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
            return bestPath.getEdgeList();
        }
    }
    
    @Override
    public boolean addCell(MapCell mapCell) {
        if (mapCell.getXCoord() < 1 || mapCell.getYCoord() < 1) {
            throw new IndexOutOfBoundsException("Coordinates " + mapCell
                    + " exceed map dimensions");
        }
        
        if (mapGraph.containsVertex(mapCell)) {
            return false;
        }
        else {
            mapGraph.addVertex(mapCell);
            return true;
        }
    }
    
    public boolean connectPath(MapCell from, MapCell to, CellTranslation translation) {
        if (!mapGraph.containsVertex(from) || !mapGraph.containsVertex(to)) {
            throw new NoSuchElementException("Cannot connect unknown map cells");
        }
        
        boolean success = mapGraph.addEdge(from, to, translation);
        if (!success) {
            return false;
        }
        else {
            updateDijskstraPath();
            return true;
        }
    }
    
    private void updateDijskstraPath() {
        dijkstraShortestPath = new DijkstraShortestPath<>(mapGraph);
    } 
    
}
