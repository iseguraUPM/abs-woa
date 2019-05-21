/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.map;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;

/**
 *
 * @author ISU
 */
public interface PathfinderGameMap extends GameMap {
    
    /**
     * Compute the shortest path to the target cell.
     * @param source
     * @param target
     * @return the sequence of translations required that make the path
     * or an empty list if there is not a viable path.
     */
    List<CellTranslation> findShortestPath(MapCell source, MapCell target);
    
    /**
     * Compute the shortest path to the nearest candidate that passes the
     * predicate filter.
     * @param source 
     * @param filterPredicate the predicate to filter candidate cells
     * @return the sequence of translations required that make the path
     * or an empty list if there is not a viable path.
     */
    List<CellTranslation> findShortestPathTo(MapCell source
            , Predicate<MapCell> filterPredicate);
    
    
    /**
     * Adds a connection between two existing cells
     * @param from
     * @param to
     * @param translation
     * @return if the connection was added and did not exist before
     * @throws NoSuchElementException if the map did not contain both cells
     */
    boolean connectPath(MapCell from, MapCell to, CellTranslation translation)
            throws NoSuchElementException;
    
    /**
     * Return the cell adjacent in the selected direction from a given source.
     * @param source map cell
     * @param direction where the connection should be
     * @return the target cell or null if does not exist
     */
    MapCell getMapCellOnDirection(MapCell source, CellTranslation direction);
    
    /**
     * Get surrounding cells of the target cell.
     * @param mapCell
     * @return the set of adjacent surrounding cells.
     */
    Set<MapCell> getNeighbours(MapCell mapCell);
    
    /**
     * Copies all the cell and connections data from other map
     * @param otherGameMap 
     */
    void copyMapData(PathfinderGameMap otherGameMap);
    
}
