/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import java.util.NoSuchElementException;

/**
 *
 * @author ISU
 */
public interface GameMap {
    
    public int getHeight();
    
    public int getWidth();
    
    /**
     * Adds a map cell to the map
     * @param mapCell
     * @return if the position was not occupied by an empty cell
     */
    public boolean addCell(MapCell mapCell);
    
    /**
     * Return a cell by its coordinates (starting at 1,1)
     * @param x coordinate
     * @param y coordinate
     * @return the requested cell
     * @throws IndexOutOfBoundsException when the was not found
     */
    public MapCell getCellAt(int x, int y) throws NoSuchElementException;
    
    public Iterable<MapCell> getKnownCellsIterable();
    
}
