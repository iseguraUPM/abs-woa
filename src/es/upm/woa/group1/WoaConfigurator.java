/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1;

import es.upm.woa.group1.agent.TribeResources;
import es.upm.woa.group1.map.GameMap;
import es.upm.woa.group1.map.MapCell;
import es.upm.woa.group1.map.MapCellFactory;

import jade.core.AID;

import org.apache.commons.configuration2.JSONConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ISU
 */
public class WoaConfigurator {
    
    private final static String CONFIG_FILENAME = "woa.properties";
    
    private static WoaConfigurator instance;
    
    private Queue<Integer[]> initialTribePositions;
    private Configurations configs;
    
    private String mapFilePath;
    private int registrationTimeMillis;
    private int gameTicks;
    private int tickMillis;
    private String guiEndpoint;
    private int maxTribes;
    private int resourceCap;
    private int storeUpgradeAmount;
    
    /**
     * 
     * @return new instance
     * @throws ConfigurationException if the configuration file does not meet
     *  the required format
     */
    public static WoaConfigurator getInstance()
            throws ConfigurationException {
        if (instance == null) {
            instance = new WoaConfigurator();

            instance.configs = new Configurations();

            PropertiesConfiguration properties
                    = instance.configs.properties(new File(CONFIG_FILENAME));
            
            instance.mapFilePath = properties.getString("map_path");
            instance.registrationTimeMillis = properties.getInt("reg_millis");
            instance.gameTicks = properties.getInt("game_ticks");
            instance.guiEndpoint = properties.getString("gui_endpoint");
            instance.tickMillis = properties.getInt("tick_millis");
            instance.maxTribes = properties.getInt("max_tribes");
            instance.resourceCap = properties.getInt("resource_cap");
            instance.storeUpgradeAmount = properties.getInt("store_upgrade_amount");
        }

        return instance;
    }
    
    public int getRegistrationTimeMillis() {
        return registrationTimeMillis;
    }
    
    public int getGameTicks() {
        return gameTicks;
    }
    
    public int getTickMillis() {
        return tickMillis;
    }
    
    public String getGuiEndpoint() {
        return guiEndpoint;
    }
    
    public int getMaxTribeNumber() {
        return maxTribes;
    }
    
    public int getResourceCap() {
        return resourceCap;
    }
    
    public int getStoreUpgradeAmount() {
        return storeUpgradeAmount;
    }

    /**
     * 
     * @return the string-formatted content of the entire map JSON configuration
     *  file
     * @throws FileNotFoundException if the file does not exist
     * @throws IOException if there was any problem during the reading of such
     *  file
     */
    public String getMapConfigurationContents() throws FileNotFoundException
            , IOException {
        FileInputStream fis = null;
        try {
            File mapConfigurationFile = new File(mapFilePath);
            fis = new FileInputStream(mapConfigurationFile);
            byte[] data = new byte[(int) mapConfigurationFile.length()];
            fis.read(data);
            fis.close();
        
            return new String(data, StandardCharsets.UTF_8);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                    Logger.getGlobal().log(Level.SEVERE
                            , "Could not close stream ({0})", ex);
                }
            }
        }
    }
    
    /**
     * 
     * @return the resource-populated world map
     * @throws ConfigurationException if the configuration file does not meet
     *  the required format
     */
    public GameMap generateWorldMap() throws ConfigurationException {
        File mapConfigurationFile = new File(mapFilePath);
        JSONConfiguration mapConfig = configs.fileBased(JSONConfiguration.class
                , mapConfigurationFile);
            
        int mapWidth = mapConfig.getInt("mapWidth");
        int mapHeight = mapConfig.getInt("mapHeight");
        
        if (mapWidth % 2 != 0 || mapHeight % 2 != 0) {
            throw new UnsupportedOperationException("Map dimensions must be even sized");
        }

        WorldMap worldMap = WorldMap.getInstance(mapWidth, mapHeight);
        
        List<HierarchicalConfiguration<ImmutableNode>> mapTiles
                = mapConfig.configurationsAt("tiles");
        mapTiles.stream().map((tile) -> MapCellFactory.getInstance()
                .buildCellFromImmutableNode(tile)).forEachOrdered((mapCell) -> {
                    try {
                        worldMap.addCell(mapCell);
                    } catch (IndexOutOfBoundsException ex) {
                        Logger.getGlobal().log(Level.WARNING
                                , "Could not add cell to world map ({0})", ex);
                    }
        });
        
        
        return worldMap;
    }
    
    /**
     * 
     * @return the resource-populated world map
     * @throws ConfigurationException if the configuration file does not meet
     *  the required format
     */
    public TribeResources getInitialResources() throws ConfigurationException {
        File mapConfigurationFile = new File(mapFilePath);
        JSONConfiguration initialConfig = configs.fileBased(JSONConfiguration.class
                , mapConfigurationFile);
        
        HierarchicalConfiguration<ImmutableNode> configList
                = initialConfig.configurationAt("initialResources");
        
        int startingFood = configList.getInt("food");
        int startingGold = configList.getInt("gold");
        int startingStone = configList.getInt("stone");
        int startingWood = configList.getInt("wood");
       
        return new TribeResources(resourceCap, startingWood, startingStone
                , startingFood, startingGold);
    }
    
    /**
     * 
     * @param gameMap will have a new town hall where the tribe is placed
     * @param tribeAID of the tribe to be added
     * @return the location of the initial cell
     * @throws ConfigurationException if any of the conditions do not meet to
     *  add a new tribe:
     *  - The JSON configuration format is as required
     *  - There are tribe slots left (max. 6)
     *  - The position configured is not occupied by a building or resource
     *  - The position configured is already present in the map
     */
    public MapCell getNewTribeInitialCell(GameMap gameMap, AID tribeAID) throws ConfigurationException {
        if (initialTribePositions == null) {
            initialTribePositions = loadInitialTribePositions();
        }
        
        if (initialTribePositions.isEmpty()) {
            throw new ConfigurationException("There are no tribe positions left");
        }
        
        Integer[] position = initialTribePositions.poll();
        if (position.length != 2) {
            throw new ConfigurationException("Faulty tribe position");
        }
        
        try {
            return getInitialCell(gameMap, position, tribeAID);
        } catch (NoSuchElementException ex) {
            throw new ConfigurationException("The starting position does not fit"
                    + "in the map");
        }
    }

    private MapCell getInitialCell(GameMap gameMap, Integer[] position, AID tribeAID)
            throws ConfigurationException, NoSuchElementException {
        MapCell targetCell = gameMap.getCellAt(position[0], position[1]);
        
        return targetCell;
    }

    private Queue<Integer[]> loadInitialTribePositions()
            throws ConfigurationException {
        Queue<Integer[]> initialPositions = new LinkedList<>();
        
        File mapConfigurationFile = new File(mapFilePath);
        JSONConfiguration mapConfig = configs.fileBased(JSONConfiguration.class
                , mapConfigurationFile);
        
        List<HierarchicalConfiguration<ImmutableNode>> initialPositionsConfiguration
                = mapConfig.configurationsAt("initialPositions");
        initialPositionsConfiguration.forEach((initialPosition) -> {
            initialPositions.add(loadPositionFromNode(initialPosition));
        });
        
        return initialPositions;
    }
    
    private Integer[] loadPositionFromNode(HierarchicalConfiguration<ImmutableNode> node) {
        int x = node.getInt("x");
        int y = node.getInt("y");
        
        return new Integer[]{x, y};
    }
}
