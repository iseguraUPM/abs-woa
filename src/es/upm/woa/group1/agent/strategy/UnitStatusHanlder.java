/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent.strategy;

import jade.core.AID;

/**
 *
 * @author ISU
 */
public interface UnitStatusHanlder {

    void onChangedPosition(AID unitAID, int xCoord, int yCoord);

    void onStartedBuilding(AID unitAID, String buildingType);

    void onFinishedBuilding(AID unitAID, String buildingType, boolean success);

    void onStartingUnitCreation(AID unitAID);

    void onFinishedUnitCreation(AID unitAID, boolean success);

    void onExploitedResource(AID unitAID, String resourceType, int amount);
    
}
