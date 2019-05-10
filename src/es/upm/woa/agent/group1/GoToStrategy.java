/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.CellTranslation;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.agent.group1.strategy.Strategy;
import static es.upm.woa.agent.group1.strategy.Strategy.HIGH_PRIORITY;
import static es.upm.woa.agent.group1.strategy.Strategy.LOW_PRIORITY;
import es.upm.woa.ontology.Building;
import es.upm.woa.ontology.CreateUnit;
import es.upm.woa.ontology.GameOntology;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author ISU
 */
public class GoToStrategy extends Strategy {
    
    private final WoaAgent woaAgent;
    private final CommunicationStandard comStandard;
    private final GraphGameMap graphKnownMap;
    private final AID worldAID;
    private final MapCell destination;
    
    private final PositionedAgentUnit agentUnit;
    
    private int priority;
    private boolean finished;
    
    GoToStrategy(WoaAgent agent, CommunicationStandard comStandard
            , GraphGameMap graphGameMap, AID worldAID, MapCell destination
            , PositionedAgentUnit agentUnit) {
        super(agent);
        this.woaAgent = agent;
        this.comStandard = comStandard;
        this.graphKnownMap = graphGameMap;
        this.worldAID = worldAID;
        this.destination = destination;
        this.agentUnit = agentUnit;
        
        priority = HIGH_PRIORITY;
        finished = false;
    }

    @Override
    public int getPriority() {
        return priority;
    }
    
    @Override
    public boolean isOneShot() {
        return true;
    }

    @Override
    public void action() {
        block();
    }
     
    private void finishStrategy() {
        finished = true;
    }

    @Override
    public boolean done() {
        return finished;
    }
    
    
    @Override
    protected void resetStrategy() {
        finished = false;
    }
    
    @Override
    public void onStart() {
        AgUnit myAgUnit = (AgUnit) myAgent;
        myAgUnit.log(Level.FINE, 
                "starting create unit strategy");
        moveToDestination(destination);
    }
    
    private void moveToDestination(MapCell destination) {
        if (agentUnit.getCurrentPosition().equals(destination)) {
            woaAgent.log(Level.FINE, "Already at destination : "
                        + destination);
            finishStrategy();
            return;
        }
        
        List<CellTranslation> pathToTownHall
                = findShortestPathToDestination(agentUnit.getCurrentPosition(), destination);
        
        startFollowPathBehaviour(woaAgent, pathToTownHall);
    }

    private void startFollowPathBehaviour(WoaAgent woaAgent, List<CellTranslation> pathToTownHall) {
        woaAgent.addBehaviour(new FollowPathBehaviour(woaAgent, comStandard
                , worldAID, agentUnit, pathToTownHall) {
            @Override
            protected void onArrived(CellTranslation direction, MapCell destination) {
                agentUnit.updateCurrentPosition(direction, destination);
                woaAgent.log(Level.FINE, "Arrived to destination at: "
                        + destination);
                finishStrategy();
            }
            
            @Override
            protected void onStep(CellTranslation direction, MapCell currentCell) {
                agentUnit.updateCurrentPosition(direction, currentCell);
                woaAgent.log(Level.FINER, "Moving towards destination from: "
                        + currentCell);
            }
            
            @Override
            protected void onStuck(MapCell currentCell) {
                woaAgent.log(Level.FINE, "Cannot move towards destination from: "
                        + currentCell);
                finishStrategy();
            }
            
            @Override
            protected void onMoveError(String msg) {
                woaAgent.log(Level.FINE, "Error while moving towards destination ("
                        + msg + ")");
                finishStrategy();
            }
        });
    }
    
    private List<CellTranslation> findShortestPathToDestination(MapCell source, MapCell destination) {
        List<CellTranslation> shortestPath = graphKnownMap.findShortestPath(source, destination);
        
        return shortestPath;
    }
    
}
