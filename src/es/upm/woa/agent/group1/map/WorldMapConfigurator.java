/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.map;

import es.upm.woa.agent.group1.TribeResources;
import es.upm.woa.agent.group1.WoaDefinitions;
import es.upm.woa.ontology.Empty;

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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ISU
 */
public class WorldMapConfigurator {
    
    private Stack<Integer[]> initialTribePositions;
    private Configurations configs;
    private File mapConfigurationFile;
    
    /**
     * 
     * @return new instance
     * @throws ConfigurationException if the configuration file does not meet
     *  the required format
     */
    public static WorldMapConfigurator getInstance()
            throws ConfigurationException {
        WorldMapConfigurator newInstance = new WorldMapConfigurator();
        
        newInstance.configs = new Configurations();
        
        PropertiesConfiguration woaConfig
                = newInstance.configs.properties(new File(WoaDefinitions.CONFIG_FILENAME));

        String mapConfigPath = woaConfig.getString("woa.map_directory");
        String mapConfigFilename = woaConfig.getString("woa.map_filename");

        newInstance.mapConfigurationFile = new File(mapConfigPath + mapConfigFilename);

        return newInstance;
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
        JSONConfiguration mapConfig = configs.fileBased(JSONConfiguration.class
                , mapConfigurationFile);
            
        int mapWidth = mapConfig.getInt("mapWidth");
        int mapHeight = mapConfig.getInt("mapHeight");

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
        JSONConfiguration initialConfig = configs.fileBased(JSONConfiguration.class
                , mapConfigurationFile);
        
        HierarchicalConfiguration<ImmutableNode> configList
                = initialConfig.configurationAt("initialResources");
        
        TribeResources tribeResources = new TribeResources();
        tribeResources.setFood(configList.getInt("food"));
        tribeResources.setGold(configList.getInt("gold"));
        tribeResources.setStone(configList.getInt("stone"));
        tribeResources.setWood(configList.getInt("wood"));
       
        return tribeResources;
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
    public MapCell addNewTribe(GameMap gameMap, AID tribeAID) throws ConfigurationException {
        if (initialTribePositions == null) {
            initialTribePositions = loadInitialTribePositions();
        }
        
        if (initialTribePositions.empty()) {
            throw new ConfigurationException("There are no tribe positions left");
        }
        
        Integer[] position = initialTribePositions.pop();
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
        if (!(targetCell.getContent() instanceof Empty)) {
            throw new ConfigurationException("Ocupied cell");
        }
        
        return targetCell;
    }

    private Stack<Integer[]> loadInitialTribePositions()
            throws ConfigurationException {
        Stack<Integer[]> initialPositionStack = new Stack<>();
        
        JSONConfiguration mapConfig = configs.fileBased(JSONConfiguration.class
                , mapConfigurationFile);
        
        List<HierarchicalConfiguration<ImmutableNode>> initialPositionsConfiguration
                = mapConfig.configurationsAt("initialPositions");
        initialPositionsConfiguration.forEach((initialPosition) -> {
            initialPositionStack.add(loadPositionFromNode(initialPosition));
        });
        
        return initialPositionStack;
    }
    
    private Integer[] loadPositionFromNode(HierarchicalConfiguration<ImmutableNode> node) {
        int x = node.getInt("x");
        int y = node.getInt("y");
        
        return new Integer[]{x, y};
    }
}
