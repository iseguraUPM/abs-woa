/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.map.PathfinderGameMap;
import es.upm.woa.group1.map.MapCellEvaluator;
import es.upm.woa.group1.map.MapCell;
import es.upm.woa.ontology.Building;

import jade.core.AID;

import java.util.Set;

/**
 *
 * @author ISU
 */
public class OtherBuildingSiteEvaluator implements MapCellEvaluator {

    private final PathfinderGameMap graphMap;
    private final AID tribeAID;
    
    public OtherBuildingSiteEvaluator(PathfinderGameMap graphGameMap, AID tribeAID) {
        this.graphMap = graphGameMap;
        this.tribeAID = tribeAID;
    }
    
    @Override
    public boolean match(MapCell candidate) {
        Set<MapCell> neighbours = graphMap.getNeighbours(candidate);
        
        return neighbours.parallelStream().anyMatch((MapCell n) -> {
            if (n.getContent() instanceof Building) {
                Building neighbourBuilding = (Building) n.getContent();
                return neighbourBuilding.getOwner().equals(tribeAID);
            }
            else {
                return false;
            }
        });
    }
    
}
