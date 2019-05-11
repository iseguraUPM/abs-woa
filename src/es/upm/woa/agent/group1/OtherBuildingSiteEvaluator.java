/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.ontology.Building;
import es.upm.woa.ontology.Empty;

import jade.core.AID;

import java.util.Set;

/**
 *
 * @author ISU
 */
class OtherBuildingSiteEvaluator implements MapCellEvaluator {

    private final GraphGameMap graphMap;
    private final AID tribeAID;
    
    public OtherBuildingSiteEvaluator(GraphGameMap graphGameMap, AID tribeAID) {
        this.graphMap = graphGameMap;
        this.tribeAID = tribeAID;
    }
    
    @Override
    public boolean match(MapCell candidate) {
        Set<MapCell> neighbours = graphMap.getNeighbours(candidate);
        
        return neighbours.parallelStream().allMatch((MapCell n) -> {
            if (n.getContent() instanceof Building) {
                Building neighbourBuilding = (Building) n.getContent();
                return neighbourBuilding.getOwner().equals(tribeAID);
            }
            else {
                return n.getContent() instanceof Empty;
            }
        });
    }
    
}
