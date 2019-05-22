/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.map.finder;

import es.upm.woa.group1.WoaDefinitions;
import es.upm.woa.group1.map.MapCell;

import es.upm.woa.ontology.Resource;

/**
 *
 * @author ISU
 */
public class OreResourceEvaluator implements MapCellEvaluator {

    @Override
    public boolean match(MapCell site) {
        if (site.getContent() instanceof Resource) {
            Resource resource = (Resource) site.getContent();
            
            if (resource.getResourceAmount() > 0
                    && resource.getResourceType().equals(WoaDefinitions.ORE)) {
                return true;
            }
        }
        
        return false;
    }
    
}
