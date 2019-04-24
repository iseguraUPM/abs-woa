/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.gui;

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
    
    void apiStartGame(String[] playerIds, String jsonMapData);
    
    void apiCreateAgent(String playerId, String newAgentId, int xPos, int yPos);
    
    void apiMoveAgent(String agentId, int xPos, int yPos);
    
    void apiAgentDies(String agentId);
    
    void apiStartAction(String agentId, String actionType);
    
    void apiCancelAction(String agentId);
    
    void apiGainResource(String playerId, String agentId, String resourceType, int amount);
    
    void apiLoseResource(String playerId, String agentId, String resourceType, int amount);
    
    void apiDepleteResource(int xPos, int yPos);
    
    void apiCreateBuilding(String playerId, String buildingType);
    
    void apiEndGame();
    
}
