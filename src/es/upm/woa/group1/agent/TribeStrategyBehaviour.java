/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.map.finder.TownHallSiteEvaluator;
import es.upm.woa.group1.agent.strategy.StrategyFactory;
import es.upm.woa.group1.WoaDefinitions;
import es.upm.woa.group1.agent.strategy.Strategy;
import es.upm.woa.group1.map.MapCell;
import es.upm.woa.group1.map.PathfinderGameMap;

import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;

import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 *
 * @author ISU
 */
final class TribeStrategyBehaviour extends SimpleBehaviour {
    
    private static final int FIRST_PHASE = 0;
    private static final int SECOND_PHASE = 1;
    private static final int THIRD_PHASE = 2;
    
    private final WoaAgent agent;
    private final Ticker ticker;
    private final SendAssignStrategyHelper strategyHelper;
    private final PathfinderGameMap graphMap;
    private final Collection<Unit> unitCollection;
    private final TribeResources resourceAccount;
    private final MapCellFinder mapCellFinder;
    
    private long elapsedTicks;
    private int phase;
    
    private final Set<Unit> exporers;
    private Unit townHallConstructor;
    
    public TribeStrategyBehaviour(WoaAgent agent, Ticker ticker
            , SendAssignStrategyHelper strategyHelper
            , PathfinderGameMap graphMap
            , Collection<Unit> unitCollection
            , TribeResources resourceAccount
            , MapCellFinder mapCellFinder) {
        super(agent);
        this.agent = agent;
        this.ticker = ticker;
        this.strategyHelper = strategyHelper;
        this.graphMap = graphMap;
        this.unitCollection = unitCollection;
        this.resourceAccount = resourceAccount;
        this.mapCellFinder = mapCellFinder;
        
        this.elapsedTicks = ticker.getCurrentTick();
        this.phase = FIRST_PHASE;
        
        this.exporers = new HashSet<>();
        this.townHallConstructor = null;
    }

    @Override
    public void action() {
        if (elapsedTicks < ticker.getCurrentTick()) {
            performRound();
            elapsedTicks = ticker.getCurrentTick();
        }
    }

    @Override
    public boolean done() {
        return false;
    }

    private void performRound() {
        checkPhase();
        switch (phase) {
            case FIRST_PHASE:
                performFirstPhase();
                break;
            case SECOND_PHASE:
                performSecondPhase();
                break;
            case THIRD_PHASE:
                break;
            default:
                    break;
        }
    }

    private void checkPhase() {
        if (phase == FIRST_PHASE && resourceAccount.canAffordTownHall()) {
            phase = SECOND_PHASE;
        }
    }

    private void performFirstPhase() {
        if (exporers.isEmpty()) {
            exporers.addAll(unitCollection);
            strategyHelper.multicastStrategy(collectAIDs(exporers)
                    , StrategyFactory.envelopFreeExploreStrategy(Strategy.MID_PRIORITY));
        }
    }
    
    private void performSecondPhase() {
        if (townHallConstructor == null) {
            try {
                assignTownHallConstructor();
                strategyHelper.unicastStrategy(townHallConstructor.getId()
                        , StrategyFactory
                                .envelopCreateBuildingStrategy(Strategy.HIGH_PRIORITY
                                        , WoaDefinitions.TOWN_HALL));
            } catch (NoSuchElementException ex) {
                agent.log(Level.WARNING, "No construction site found");
            }
        }
    }
    
    private AID[] collectAIDs(Collection<Unit> units) {
        return units.stream().map(unit -> unit.getId())
                .collect(Collectors.toList())
                .toArray(new AID[unitCollection.size()]);
    }

    private void assignTownHallConstructor() {
        townHallConstructor = unitCollection.parallelStream().min((Unit t, Unit t1) -> {
            MapCell unitPosition = graphMap.getCellAt(t.getCoordX(), t.getCoordY());
            
            
            MapCell siteCandidate = mapCellFinder
                    .findMatchingSiteCloseTo(unitPosition
                            , new TownHallSiteEvaluator(graphMap));
            
            if (unitPosition.equals(siteCandidate)) {
                return 0;
            }
            
            int distance
                    = graphMap.findShortestPath(unitPosition
                            , siteCandidate).size();
            
            if (distance == 0) {
                return Integer.MAX_VALUE;
            }
            else {
                return distance;
            }
        }).get();
    }
    
}
