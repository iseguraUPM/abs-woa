/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.map.finder;

import es.upm.woa.group1.map.PathfinderGameMap;
import es.upm.woa.group1.map.MapCell;
import es.upm.woa.ontology.Building;
import es.upm.woa.ontology.Ground;

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
        if (!(candidate.getContent() instanceof Ground)) {
            return false;
        }

        Set<MapCell> neighbours = graphMap.getNeighbours(candidate);

        return neighbours.stream().anyMatch((MapCell n) -> {
            if (n.getContent() instanceof Building) {
                Building neighbourBuilding = (Building) n.getContent();
                return neighbourBuilding.getOwner().equals(tribeAID);
            }

            return false;
        });
    }

}
