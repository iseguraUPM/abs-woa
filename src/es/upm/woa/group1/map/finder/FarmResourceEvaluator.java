/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.map.finder;

import es.upm.woa.group1.WoaDefinitions;
import es.upm.woa.group1.map.MapCell;
import es.upm.woa.ontology.Building;

import jade.core.AID;

/**
 *
 * @author ISU
 */
public class FarmResourceEvaluator implements MapCellEvaluator {
    
    private final AID tribeAID;
    
    public FarmResourceEvaluator(AID tribeAID) {
        this.tribeAID = tribeAID;
    }

    @Override
    public boolean match(MapCell site) {
        if (site.getContent() instanceof Building) {
            Building building = (Building) site.getContent();
            
            if (building.getType().equals(WoaDefinitions.FARM)
                    && building.getOwner().equals(tribeAID)) {
                return true;
            }
        }
        
        return false;
    }
    
}
