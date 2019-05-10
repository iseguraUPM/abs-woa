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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 *
 * @author ISU
 */
class ConstructionSiteFinder {
    
    private AID tribeAID;
    private GraphGameMap graphMap;
    
    private ConstructionSiteFinder() {}
    
    /**
     * 
     * @param tribeAID the tribe that needs to build
     * @param graphMap known map
     * @return an instance
     */
    static ConstructionSiteFinder getInstance(AID tribeAID, GraphGameMap graphMap) {
        ConstructionSiteFinder instance = new ConstructionSiteFinder();
        
        instance.tribeAID = tribeAID;
        instance.graphMap = graphMap;
        
        return instance;
    }
    
    /**
     * Finds a potential construction site for the required building type.
     * This method does not take into account the unknown surrounding cells
     * of the candidates. So the construction request can be refused.
     * @param buildingType that shall be built
     * @return the cell where it can be built or null if it could not be found
     */
    public MapCell findConstructionSite(String buildingType) {
        switch (buildingType) {
            case WoaDefinitions.TOWN_HALL:
                return findBuildingSite((MapCell site) -> {
                    return goodTownHallConstructionSite(site);
                });
            case WoaDefinitions.FARM:
            case WoaDefinitions.STORE:
                return findBuildingSite((MapCell site) -> {
                    return goodOtherBuildingConstructionSite(site);
                });
            default:
                return null;
        }
    }
    
    /**
     * Finds a potential construction site for the required building type.
     * Starts looking for the closest possible sites to the zero site.
     * This method does not take into account the unknown surrounding cells
     * of the candidates. So the construction request can be refused.
     * @param zero point where to find the closest construction site
     * @param buildingType that shall be built
     * @return the cell where it can be built or null if it could not be found
     */
    public MapCell findConstructionCloseTo(MapCell zero, String buildingType) {
        switch (buildingType) {
            case WoaDefinitions.TOWN_HALL:
                return ConstructionSiteFinder.this.findBuildingSite(zero, (MapCell site) -> {
                    return goodTownHallConstructionSite(site);
                });
            case WoaDefinitions.FARM:
            case WoaDefinitions.STORE:
                return ConstructionSiteFinder.this.findBuildingSite(zero, (MapCell site) -> {
                    return goodOtherBuildingConstructionSite(site);
                });
            default:
                return null;
        }
    }

    private MapCell findBuildingSite(SiteEvaluator siteEvaluator) {
        for (MapCell candidate : graphMap.getKnownCellsIterable()) {
            if(siteEvaluator.isGood(candidate)) {
                return candidate;
            }
        }
        
        return null;
    }

    private boolean goodTownHallConstructionSite(MapCell candidate) {
        Set<MapCell> neighbours = graphMap.getNeighbours(candidate);
        
        return neighbours.parallelStream().allMatch(n -> {
            return n.getContent() instanceof Empty;
        });
    }

    private boolean goodOtherBuildingConstructionSite(MapCell candidate) {
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

    private MapCell findBuildingSite(MapCell zero, SiteEvaluator siteEvaluator) {
        Queue<MapCell> candidates = new LinkedList<>();
        candidates.addAll(graphMap.getNeighbours(zero));
        Set<MapCell> discarded = new HashSet<>();
        
        return findBuildingSiteFromNeighbours(candidates, siteEvaluator, discarded);
    }

    private MapCell findBuildingSiteFromNeighbours(Queue<MapCell> nextCandidates
            , SiteEvaluator siteEvaluator, Set<MapCell> discarded) {
        if (nextCandidates.isEmpty()) {
            return null;
        }
        
        MapCell candidate = nextCandidates.poll();
        if (discarded.contains(candidate)) {
            return findBuildingSiteFromNeighbours(nextCandidates, siteEvaluator
                    , discarded);
        }
        
        
        if (siteEvaluator.isGood(candidate)) {
            return candidate;
        }
        else {
            discarded.add(candidate);
            nextCandidates.addAll(graphMap.getNeighbours(candidate));
            return findBuildingSiteFromNeighbours(nextCandidates, siteEvaluator, discarded);
        }
    }
    
    private interface SiteEvaluator {
    
        boolean isGood(MapCell site);
        
    }
    
}
