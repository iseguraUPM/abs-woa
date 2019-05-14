/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.map.MapCell;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 *
 * @author ISU
 */
class MapCellFinder {
    
    private GraphGameMap graphMap;
    
    private MapCellFinder() {}
    
    /**
     * 
     * @param graphMap known map
     * @return an instance
     */
    static MapCellFinder getInstance(GraphGameMap graphMap) {
        MapCellFinder instance = new MapCellFinder();
        
        instance.graphMap = graphMap;
        
        return instance;
    }
    
    /**
     * Finds a map cell site that matches the conditions.
     * This method does not take into account the unknown surrounding cells
     * of the candidates.
     * @param evaluator
     * @return a cell that matches the conditions or null if it could not be found
     */
    public MapCell findMatchingSite(MapCellEvaluator evaluator) {
        return findMatch(evaluator);
    }
    
    /**
     * Finds a map cell site that matches the conditions.
     * Starts looking for the closest possible sites to the zero site.
     * This method does not take into account the unknown surrounding cells
     * of the candidates.
     * @param zero point where to find the closest construction site
     * @param evaluator
     * @return the closest cell that matches conditions
     * or null if it could not be found
     */
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
