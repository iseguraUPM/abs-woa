/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.ontology.Empty;
import jade.content.Concept;
import jade.core.AID;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author ISU
 */
public class WorldMap {
    
    public static final String TOWN_HALL = "TownHall";
    
    private final int width;
    private final int height;
    
    private final Map<Integer, MapCell> mapCells;
    
    private WorldMap(int width, int height) {
        this.width = width;
        this.height = height;
        mapCells = new TreeMap<>();
    }
    
    public int getWidth() {
       return width;
    }
    
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
    
    /**
     * Adds a map cell to the map
     * @param mapCell
     * @return if the position was not occupied by an empty cell
     */
    public boolean addCell(MapCell mapCell) {
        int index = toAbsolutePosition(mapCell.getXCoord(), mapCell.getYCoord());
        
        if (mapCells.containsKey(index))
            return false;
        
        mapCells.put(index, mapCell);
        return true;
    }
    
    /**
     * Return a cell by its coordinates (starting at 1,1)
     * @param x coordinate
     * @param y coordinate
     * @return the requested cell
     * @throws IndexOutOfBoundsException when the coordinates are past the bounds
     * of the map
     */
    public MapCell getCellAt(int x, int y) throws IndexOutOfBoundsException {
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
    
    private class EmptyMapCell implements MapCell {

        private final int x;
        private final int y;

        public EmptyMapCell(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public AID getOwner() {
            return null;
        }

        @Override
        public Concept getContent() {
            return new Empty();
        }

        @Override
        public int getXCoord() {
            return x;
        }

        @Override
        public int getYCoord() {
            return y;
        }
        
    }
    
}
