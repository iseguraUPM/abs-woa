/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

/**
 *
 * @author ISU
 */
public interface CreateBuildingRequestHandler {
    
    void onStartedBuilding(String buildingType);
    
    void onFinishedBuilding(String buildingType);
    
    void onErrorBuilding(String buildingType);
    
}
