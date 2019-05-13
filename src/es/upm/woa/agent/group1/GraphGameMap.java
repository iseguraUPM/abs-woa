/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;


import es.upm.woa.agent.group1.map.CellTranslation;
import es.upm.woa.agent.group1.map.GameMap;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.ontology.Empty;

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
import java.util.Collection;
import java.util.HashSet;

/**
 *
 * @author ISU
 */
class GraphGameMap implements GameMap {
    
    private Graph<MapCell, CellTranslation> mapGraph;
    // Non-serializable
    transient private DijkstraShortestPath<MapCell, CellTranslation> dijkstraShortestPath;
    
    private int maxWidth;
    private int maxHeight;
    
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
        newInstance.maxHeight = 0;
        newInstance.maxWidth = 0;
        
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
        
        if (mapGraph.addVertex(mapCell)) {
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
    
    /**
     * Adds a connection between two existing cells
     * @param from
     * @param to
     * @param translation
     * @return if the connection was added and did not exist before
     * @throws NoSuchElementException if the map did not contain both cells
     */
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
    
    /**
     * Return the cell adjacent in the selected direction from a given source.
     * @param source map cell
     * @param direction where the connection should be
     * @return the target cell or null if does not exist
     */
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
    
    /**
     * Complete missing information in current graph with new possible cells
     * that are contained in the other map. Only connecting cells will be added
     * so that there is always a path between this graph and the new additions.
     * @param otherGraph 
     */
    public void mergeMapData(GraphGameMap otherGraph) {
        int mapGraphSize = mapGraph.vertexSet().size();
        
        if (mapGraph.vertexSet().isEmpty()) {
            mergeAll(otherGraph);
        }
        else {
            Collection<MapCell> unknownCells = new HashSet<>();
            mapGraph.vertexSet().forEach((myCell) -> {
                addMissingConnections(myCell, otherGraph, unknownCells);
            });

            if (!unknownCells.isEmpty()) {
                mergeMissingMapData(otherGraph, unknownCells);
            }
        }
        
        System.err.println("Merged map data " + mapGraphSize + " -> "
                + mapGraph.vertexSet().size());
        
        updateDijskstraPath();
    }
    
    private void mergeAll(GraphGameMap otherGraphMap) {
        mapGraph = otherGraphMap.mapGraph;
    }
    
    private void mergeMissingMapData(GraphGameMap otherGraph, Collection<MapCell> newAdditions) {
        newAdditions.stream().forEach(mc -> mapGraph.addVertex(mc));
        
        Collection<MapCell> unknownCells = new HashSet<>();
        newAdditions.stream().forEach(mc -> addMissingConnections(mc, otherGraph, unknownCells));
        
        newAdditions.clear();
        if (!unknownCells.isEmpty()) {
            mergeMissingMapData(otherGraph, unknownCells);
        }
    }
    
    private void addMissingConnections(MapCell myCell, GraphGameMap otherGraph
            , Collection<MapCell> unknownCells) {
        try {
            MapCell otherCell = otherGraph.getCellAt(myCell.getXCoord(), myCell.getYCoord());
            updateKnownCellContents(myCell, otherCell);
            otherGraph.mapGraph.outgoingEdgesOf(otherCell).forEach((direction) -> {
                connectDirection(myCell, direction, otherGraph, unknownCells);
            });
        } catch (NoSuchElementException ex) {
            // Other map does not know this cell
        }
    }

    private void connectDirection(MapCell myCell, CellTranslation direction
            , GraphGameMap otherGraph, Collection<MapCell> newAdditions) {
        MapCell target = otherGraph.mapGraph.getEdgeTarget(direction);
        try {
            MapCell myKnownTarget = getCellAt(target.getXCoord(), target.getYCoord());
            mapGraph.addEdge(myCell, myKnownTarget, direction);
            CellTranslation inverse = direction.generateInverse();
            mapGraph.addEdge(myKnownTarget, myCell, inverse);
        } catch (NoSuchElementException ex) {
            newAdditions.add(target);
        }
    }
    
    private void updateDijskstraPath() {
        dijkstraShortestPath = new DijkstraShortestPath<>(mapGraph);
    }

    private void updateKnownCellContents(MapCell myCell, MapCell otherCell) {
        if ((myCell.getContent() instanceof Empty)
                && !(otherCell.getContent() instanceof Empty)) {
            myCell.setContent(otherCell.getContent());
        }
    }
    
    Set<MapCell> getNeighbours(MapCell mapCell) {
        if (!mapGraph.containsVertex(mapCell)) {
            return new HashSet<>();
        }
        
        Set<CellTranslation> connections = mapGraph.outgoingEdgesOf(mapCell);
        
        return connections.parallelStream().map(c -> mapGraph.getEdgeTarget(c))
                .collect(Collectors.toSet());
    }
    
}
