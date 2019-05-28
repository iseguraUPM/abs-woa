/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.map.finder;

import es.upm.woa.group1.map.MapCell;
import es.upm.woa.ontology.Building;

import jade.core.AID;

/**
 *
 * @author ISU
 */
public class TribeBuildingEvaluator implements MapCellEvaluator {
    
    private final AID tribeAID;
    private final String buildingType;
    
    public TribeBuildingEvaluator(AID tribeAID, String buildingType) {
        this.tribeAID = tribeAID;
        this.buildingType = buildingType;
    }
    
    @Override
    public boolean match(MapCell candidate) {
        if (candidate.getContent() instanceof Building) {
            Building townHall = (Building) candidate.getContent();
            return townHall.getType().equals(buildingType)
                    && townHall.getOwner().equals(tribeAID);
        }
        
        return false;
    }
    
}
