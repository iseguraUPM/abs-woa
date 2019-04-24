/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.map;

import es.upm.woa.agent.group1.WoaDefinitions;

import jade.util.Logger;

import org.apache.commons.configuration2.JSONConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.logging.Level;

/**
 *
 * @author ISU
 */
public class WorldMap implements GameMap {
    
    private final int width;
    private final int height;
    
    private final Map<Integer, MapCell> mapCells;
    
    private WorldMap(int width, int height) {
        this.width = width;
        this.height = height;
        mapCells = new TreeMap<>();
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
     * at [1,1] and the max coordinate is [height,width]. The first coordinate
     * raises DOWN of the origin. The second coordinate raises RIGHT of the
     * origin. Both coordinates are always ODD or both EVEN.
     * @return an instance or null if configuration was not correct
     */
    public static WorldMap getInstance() {
        Configurations config = new Configurations();
        
        try {
            PropertiesConfiguration woaConfig
                    = config.properties(new File(WoaDefinitions.CONFIG_FILENAME));
            
            String mapConfigPath = woaConfig.getString("woa.map_directory");
            String mapConfigFilename = woaConfig.getString("woa.map_filename");
            
            JSONConfiguration mapConfig = config.fileBased(JSONConfiguration.class, new File(mapConfigPath + mapConfigFilename));
            
            int mapWidth = mapConfig.getInt("mapWidth");
            int mapHeight = mapConfig.getInt("mapHeight");
            
            WorldMap newInstance = new WorldMap(mapWidth, mapHeight);
        
            return newInstance;
            
        } catch (ConfigurationException ex) {
            Logger.getGlobal().log(Level.SEVERE, "Could not load map data ({0})", ex);
            return null;
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
        
        int index = toAbsolutePosition(mapCell.getXCoord(), mapCell.getYCoord());
        
        if (mapCells.containsKey(index))
            return false;
        
        mapCells.put(index, mapCell);
        return true;
    }
    
    
    @Override
    public MapCell getCellAt(int x, int y) throws NoSuchElementException {
        if (x < 1 || y < 1 || x > height || y > width) {
            throw new NoSuchElementException("Coordinates (" + x + "," + y
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
