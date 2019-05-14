/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent.world;

import es.upm.woa.group1.gui.WoaGUI;

/**
 *
 * @author ISU
 */
public class WoaGUIWrapper implements WoaGUI {
    
    private WoaGUI nullableInstance;
   
    /**
     * Wrapper class for WoaGUI interface. This is a helper so that methods do 
     * not return NullPointerException even if there is not a WoaGUI instance
     */
    public WoaGUIWrapper() {
        this.nullableInstance = null;
    }
    
    public void setGUIEndpoint(WoaGUI endpoint) {
        this.nullableInstance = endpoint;
    }

    @Override
    public void apiStartGame(String[] playerIds, String jsonMapData) {
        if (nullableInstance != null) {
            nullableInstance.apiStartGame(playerIds, jsonMapData);
        }
    }

    @Override
    public void apiCreateAgent(String playerId, String newAgentId, int xPos, int yPos) {
        if (nullableInstance != null) {
            nullableInstance.apiCreateAgent(playerId, newAgentId, xPos, yPos);
        }
    }

    @Override
    public void apiMoveAgent(String agentId, int xPos, int yPos) {
        if (nullableInstance != null) {
            nullableInstance.apiMoveAgent(agentId, xPos, yPos);
        }
    }

    @Override
    public void apiAgentDies(String agentId) {
        if (nullableInstance != null) {
            nullableInstance.apiAgentDies(agentId);
        }
    }

    @Override
    public void apiStartAction(String agentId, String actionType) {
        if (nullableInstance != null) {
            nullableInstance.apiStartAction(agentId, actionType);
        }
    }

    @Override
    public void apiCancelAction(String agentId) {
        if (nullableInstance != null) {
            nullableInstance.apiCancelAction(agentId);
        }
    }

    @Override
    public void apiGainResource(String playerId, String agentId, String resourceType, int amount) {
        if (nullableInstance != null) {
            nullableInstance.apiGainResource(playerId, agentId, resourceType, amount);
        }
    }

    @Override
    public void apiLoseResource(String playerId, String agentId, String resourceType, int amount) {
        if (nullableInstance != null) {
            nullableInstance.apiLoseResource(playerId, agentId, resourceType, amount);
        }
    }

    @Override
    public void apiDepleteResource(int xPos, int yPos) {
        if (nullableInstance != null) {
            nullableInstance.apiDepleteResource(xPos, yPos);
        }
    }

    @Override
    public void apiCreateBuilding(String playerId, String buildingType) {
        if (nullableInstance != null) {
            nullableInstance.apiCreateBuilding(playerId, buildingType);
        }
    }

    @Override
    public void apiEndGame() {
        if (nullableInstance != null) {
            nullableInstance.apiEndGame();
        }
    }
    
}
