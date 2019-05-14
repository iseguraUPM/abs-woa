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
    public void startGame(String[] playerIds, String jsonMapData) {
        if (nullableInstance != null) {
            nullableInstance.startGame(playerIds, jsonMapData);
        }
    }

    @Override
    public void createAgent(String playerId, String newAgentId, int xPos, int yPos) {
        if (nullableInstance != null) {
            nullableInstance.createAgent(playerId, newAgentId, xPos, yPos);
        }
    }

    @Override
    public void moveAgent(String agentId, int xPos, int yPos) {
        if (nullableInstance != null) {
            nullableInstance.moveAgent(agentId, xPos, yPos);
        }
    }

    @Override
    public void agentDies(String agentId) {
        if (nullableInstance != null) {
            nullableInstance.agentDies(agentId);
        }
    }

    @Override
    public void startAction(String agentId, String actionType) {
        if (nullableInstance != null) {
            nullableInstance.startAction(agentId, actionType);
        }
    }

    @Override
    public void cancelAction(String agentId) {
        if (nullableInstance != null) {
            nullableInstance.cancelAction(agentId);
        }
    }

    @Override
    public void gainResource(String playerId, String agentId, String resourceType, int amount) {
        if (nullableInstance != null) {
            nullableInstance.gainResource(playerId, agentId, resourceType, amount);
        }
    }

    @Override
    public void loseResource(String playerId, String agentId, String resourceType, int amount) {
        if (nullableInstance != null) {
            nullableInstance.loseResource(playerId, agentId, resourceType, amount);
        }
    }

    @Override
    public void depleteResource(int xPos, int yPos) {
        if (nullableInstance != null) {
            nullableInstance.depleteResource(xPos, yPos);
        }
    }

    @Override
    public void createBuilding(String playerId, String buildingType) {
        if (nullableInstance != null) {
            nullableInstance.createBuilding(playerId, buildingType);
        }
    }

    @Override
    public void endGame() {
        if (nullableInstance != null) {
            nullableInstance.endGame();
        }
    }
    
}
