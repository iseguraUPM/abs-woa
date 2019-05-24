/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.map.finder;

import es.upm.woa.group1.map.MapCell;
import es.upm.woa.group1.map.PathfinderGameMap;
import es.upm.woa.ontology.Building;

import java.util.Set;

/**
 *
 * @author ISU
 */
public class TownHallSiteEvaluator implements MapCellEvaluator {

    private final PathfinderGameMap graphMap;
    
    public TownHallSiteEvaluator(PathfinderGameMap graphMap) {
        this.graphMap = graphMap;
    }
    
    @Override
    public boolean match(MapCell candidate) {
        
        Set<MapCell> neighbours = graphMap.getNeighbours(candidate);
        
        return neighbours.stream().allMatch(n -> {
            return !(n.getContent() instanceof Building);
        });
    }
    
}
