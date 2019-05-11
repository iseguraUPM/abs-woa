/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.ontology.Empty;

import java.util.Set;

/**
 *
 * @author ISU
 */
class TownHallSiteEvaluator implements MapCellEvaluator {

    private final GraphGameMap graphMap;
    
    public TownHallSiteEvaluator(GraphGameMap graphMap) {
        this.graphMap = graphMap;
    }
    
    @Override
    public boolean match(MapCell candidate) {
        
        Set<MapCell> neighbours = graphMap.getNeighbours(candidate);
        
        return neighbours.parallelStream().allMatch(n -> {
            return n.getContent() instanceof Empty;
        });
    }
    
}
