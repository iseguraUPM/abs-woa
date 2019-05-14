/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.map;

import java.io.Serializable;
import java.util.NoSuchElementException;

/**
 *
 * @author ISU
 */
public interface GameMap extends Serializable {
    
    public int getHeight();
    
    public int getWidth();
    
    /**
     * Adds a map cell to the map
     * @param mapCell
     * @return if the position was not occupied
     * @throws IndexOutOfBoundsException if the coordinates exceed map bounds
     */
    public boolean addCell(MapCell mapCell);
    
    /**
     * Return a cell by its coordinates (starting at 1,1)
     * @param x coordinate
     * @param y coordinate
     * @return the requested cell
     * @throws NoSuchElementException when the was not found
     */
    public MapCell getCellAt(int x, int y) throws NoSuchElementException;
    
    public Iterable<MapCell> getKnownCellsIterable();
    
}
