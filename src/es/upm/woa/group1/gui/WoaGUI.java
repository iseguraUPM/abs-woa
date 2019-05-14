/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.gui;

/**
 *
 * @author ISU
 */
public interface WoaGUI {
    
    public static final String ACTION_EXPLOIT = "exploit";
    public static final String ACTION_NEGOTIATE = "negotiate";
    
    public static final String RESOURCE_FOOD = "food";
    public static final String RESOURCE_WOOD = "wood";
    public static final String RESOURCE_STONE = "stone";
    public static final String RESOURCE_GOLD = "gold";
    
    public static final String BUILDING_TOWNHALL = "Town Hall";
    public static final String BUILDING_FARM = "Farm";
    public static final String BUILDING_STORE = "Store";
    
    void startGame(String[] playerIds, String jsonMapData);
    
    void createAgent(String playerId, String newAgentId, int xPos, int yPos);
    
    void moveAgent(String agentId, int xPos, int yPos);
    
    void agentDies(String agentId);
    
    void startAction(String agentId, String actionType);
    
    void cancelAction(String agentId);
    
    void gainResource(String playerId, String agentId, String resourceType, int amount);
    
    void loseResource(String playerId, String agentId, String resourceType, int amount);
    
    void depleteResource(int xPos, int yPos);
    
    void createBuilding(String playerId, String buildingType);
    
    void endGame();
    
}
