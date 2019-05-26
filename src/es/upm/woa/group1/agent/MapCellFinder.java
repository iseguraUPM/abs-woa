/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.map.finder.LocationFinder;
import es.upm.woa.group1.map.finder.MapCellEvaluator;
import es.upm.woa.group1.map.MapCell;
import es.upm.woa.group1.map.PathfinderGameMap;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 *
 * @author ISU
 */
class MapCellFinder implements LocationFinder {
    
    private PathfinderGameMap graphMap;
    
    private MapCellFinder() {}
    
    /**
     * 
     * @param graphMap known map
     * @return an instance
     */
    static MapCellFinder getInstance(PathfinderGameMap graphMap) {
        MapCellFinder instance = new MapCellFinder();
        
        instance.graphMap = graphMap;
        
        return instance;
    }
    
    @Override
    public MapCell findMatchingSite(MapCellEvaluator evaluator) {
        return findMatch(evaluator);
    }
    
    @Override
    public MapCell findMatchingSiteCloseTo(MapCell zero, MapCellEvaluator evaluator) {
        return findMatchCloseTo(zero, evaluator);
    }

    private MapCell findMatch(MapCellEvaluator siteEvaluator) {
        for (MapCell candidate : graphMap.getKnownCellsIterable()) {
            if(siteEvaluator.match(candidate)) {
                return candidate;
            }
        }
        
        return null;
    }
    
    private MapCell findMatchCloseTo(MapCell zero, MapCellEvaluator evaluator) {
        if (evaluator.match(zero)) {
            return zero;
        }
        
        Queue<MapCell> candidates = new LinkedList<>();
        candidates.addAll(graphMap.getNeighbours(zero));
        Set<MapCell> discarded = new HashSet<>();
        
        return findMatchFromNeighbours(candidates, evaluator, discarded);
    }

    private MapCell findMatchFromNeighbours(Queue<MapCell> nextCandidates
            , MapCellEvaluator evaluator, Set<MapCell> discarded) {
        if (nextCandidates.isEmpty()) {
            return null;
        }
        
        MapCell candidate = nextCandidates.poll();
        if (discarded.contains(candidate)) {
            return findMatchFromNeighbours(nextCandidates, evaluator
                    , discarded);
        }
        
        
        if (evaluator.match(candidate)) {
            return candidate;
        }
        else {
            discarded.add(candidate);
            nextCandidates.addAll(graphMap.getNeighbours(candidate));
            return findMatchFromNeighbours(nextCandidates, evaluator, discarded);
        }
    }
    
    
}
