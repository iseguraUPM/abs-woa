/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;


import es.upm.woa.group1.map.CellTranslation;
import es.upm.woa.group1.map.GameMap;
import es.upm.woa.group1.map.MapCell;
import es.upm.woa.group1.map.PathfinderGameMap;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DirectedPseudograph;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.HashSet;

/**
 *
 * @author ISU
 */
class GraphGameMap implements PathfinderGameMap {
    
    private Graph<MapCell, CellTranslation> mapGraph;
    // Non-serializable
    transient private DijkstraShortestPath<MapCell, CellTranslation> dijkstraShortestPath;
    
    private int maxWidth;
    private int minWidth;
    private int maxHeight;
    private int minHeight;
    
    private GraphGameMap() {
    }
    
    @Override
    public int getWidth() {
       return maxWidth - minWidth;
    }
    
    @Override
    public int getHeight() {
        return maxHeight - minHeight;
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
        newInstance.maxHeight = 0;
        newInstance.maxWidth = 0;
        newInstance.minHeight = 0;
        newInstance.minWidth = 0;
        
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
    
    @Override
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
    
    @Override
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
    public boolean addCell(MapCell mapCell) {if (mapCell.getXCoord() < 1 || mapCell.getYCoord() < 1) {
            throw new IndexOutOfBoundsException("Coordinates " + mapCell
                    + " exceed map dimensions");
        }
        
        if (mapGraph.addVertex(mapCell)) {
            if (mapGraph.vertexSet().size() == 1) {
                minHeight = mapCell.getXCoord() - 1;
                minWidth = mapCell.getYCoord() - 1;
            }
            else {
                minHeight = mapCell.getXCoord() < minHeight
                    ? mapCell.getXCoord() - 1 : minHeight;
                minWidth = mapCell.getYCoord() < minWidth
                    ? mapCell.getYCoord() - 1: minWidth;
            }
            
            maxHeight = mapCell.getXCoord() > maxHeight
                    ? mapCell.getXCoord() : maxHeight;
            maxWidth = mapCell.getYCoord()> maxWidth
                    ? mapCell.getYCoord(): maxWidth;
            return true;
        }
        else {
            return false;
        }
    }
    
    @Override
    public boolean connectPath(MapCell from, MapCell to, CellTranslation translation) {
        if (!mapGraph.containsVertex(from) || !mapGraph.containsVertex(to)) {
            throw new NoSuchElementException("Cannot connect unknown map cells");
        }
        
        if (mapGraph.getEdge(from, to) != null) {
            return false;
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
    
    @Override
    public MapCell getMapCellOnDirection(MapCell source, CellTranslation direction) {
        Set<CellTranslation> connections = mapGraph.outgoingEdgesOf(source);
        CellTranslation actualDirection = connections.stream()
                .filter(t -> direction.getTranslationCode()
                        == t.getTranslationCode()).findAny().orElse(null);
        if (actualDirection == null) {
            return null;
        }
        else {
            return mapGraph.getEdgeTarget(actualDirection);
        }
    }
    
    @Override
    public void copyMapData(PathfinderGameMap otherGameMap) {
        if (otherGameMap instanceof GraphGameMap) {
            GraphGameMap otherGraphGameMap = (GraphGameMap) otherGameMap;
            mapGraph = otherGraphGameMap.mapGraph;
        }
        else {
            throw new UnsupportedOperationException("Cannot copy from another"
                    + " PathfinderGameMap");
        }
    }
    
    private void updateDijskstraPath() {
        dijkstraShortestPath = new DijkstraShortestPath<>(mapGraph);
    }
    
    @Override
    public Set<MapCell> getNeighbours(MapCell mapCell) {
        if (!mapGraph.containsVertex(mapCell)) {
            return new HashSet<>();
        }
        
        Set<CellTranslation> connections = mapGraph.outgoingEdgesOf(mapCell);
        
        return connections.parallelStream().map(c -> mapGraph.getEdgeTarget(c))
                .collect(Collectors.toSet());
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        
        mapGraph.vertexSet().stream().forEach((MapCell mc) -> {
            long connections = mapGraph.outgoingEdgesOf(mc).stream().count();
            sb.append(mc).append(" has ").append(connections).append(" connections\n");
            sb.append("----------------\n");
        });
        
        return sb.toString();
        
    }
    
}
