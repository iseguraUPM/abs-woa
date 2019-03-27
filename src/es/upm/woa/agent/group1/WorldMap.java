/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

/**
 *
 * @author ISU
 */
public class WorldMap {
    
    public static final String TOWN_HALL = "TownHall";
    
    private MapCell[][] worldMapContents;
    
    private WorldMap() {}
    
    public static WorldMap getInstance(int width, int height) {
        if (width < 1 || height < 1) {
            return null;
        }
        
        WorldMap newInstance = new WorldMap();
        newInstance.worldMapContents = new MapCell[width][height];
        
        newInstance.worldMapContents[0][0] = new MapCell() {
            @Override
            public String getContent() {
                return TOWN_HALL;
            }
        };
        
        return newInstance;
    }
    
    /**
     * Return a cell by its coordinates (starting at 1,1)
     * @param x coordinate
     * @param y coordinate
     * @return the requested cell or null if the coordinates fall out of bounds
     */
    public MapCell getCellAt(int x, int y) {
        int width = worldMapContents.length;
        if (x <1 || y < 1 || x > width || y > worldMapContents[width - 1].length) {
            return null;
        }
        
        return worldMapContents[x - 1][y - 1];
    }
    
}
