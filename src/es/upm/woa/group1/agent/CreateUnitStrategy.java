/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.WoaDefinitions;
import es.upm.woa.group1.map.CellTranslation;
import es.upm.woa.group1.map.MapCell;
import es.upm.woa.group1.protocol.CommunicationStandard;
import es.upm.woa.group1.protocol.Conversation;
import es.upm.woa.group1.agent.strategy.Strategy;
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
class CreateUnitStrategy extends Strategy {
    
    private final WoaAgent woaAgent;
    private final CommunicationStandard comStandard;
    private final GraphGameMap graphKnownMap;
    private final AID worldAID;
    private int priority;
    
    private final PositionedAgentUnit agentUnit;
    
    private boolean finished;
    
    public CreateUnitStrategy(int priority
            , WoaAgent agent, CommunicationStandard comStandard
            , GraphGameMap graphGameMap, AID worldAID
            , PositionedAgentUnit agentUnit) {
        super(agent);
        this.woaAgent = agent;
        this.comStandard = comStandard;
        this.graphKnownMap = graphGameMap;
        this.worldAID = worldAID;
        this.agentUnit = agentUnit;
        
        this.priority = priority;
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
    
     private void createUnit() {
        startCreateUnitBehaviour(new OnCreatedUnitHandler() {
            @Override
            public void onCreatedUnit() {
                finishStrategy();
            }

            @Override
            public void onCouldntCreateUnit() {
                finishStrategy();
            }
        });
    }
     
    private void finishStrategy() {
        finished = true;
        priority = LOW_PRIORITY;
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
        moveToNearestTownHall(new OnArrivedToTownHallHandler() {
            @Override
            public void onArrivedToTownHall() {
                createUnit();
            }

            @Override
            public void onCouldntArriveToTownHall() {
               finishStrategy();
            }
        });
    }
    
    private void moveToNearestTownHall(OnArrivedToTownHallHandler handler) {
        if (agentUnit.getCurrentPosition().getContent() instanceof Building) {
            Building nearBuilding = (Building) agentUnit.getCurrentPosition()
                    .getContent();
            if (nearBuilding.getType().equals(WoaDefinitions.TOWN_HALL)
                    && nearBuilding.getOwner().equals(agentUnit.getTribeAID())) {
                handler.onArrivedToTownHall();
                return;
            }
        }
        
        List<CellTranslation> pathToTownHall
                = findShortestPathToTownHall(agentUnit.getCurrentPosition());
        
        startFollowPathBehaviour(woaAgent, pathToTownHall, handler);
    }

    private void startFollowPathBehaviour(WoaAgent woaAgent, List<CellTranslation> pathToTownHall, OnArrivedToTownHallHandler handler) {
        woaAgent.addBehaviour(new FollowPathBehaviour(woaAgent, comStandard, worldAID, agentUnit, pathToTownHall) {
            @Override
            protected void onArrived(CellTranslation direction, MapCell destination) {
                agentUnit.updateCurrentPosition(direction, destination);
                woaAgent.log(Level.FINE, "Arrived to town hall at: "
                        + destination);
                handler.onArrivedToTownHall();
            }
            
            @Override
            protected void onStep(CellTranslation direction, MapCell currentCell) {
                agentUnit.updateCurrentPosition(direction, currentCell);
                woaAgent.log(Level.FINER, "Moving towards town hall from: "
                        + currentCell);
            }
            
            @Override
            protected void onStuck(MapCell currentCell) {
                woaAgent.log(Level.FINE, "Cannot move towards town hall from: "
                        + currentCell);
                
                handler.onCouldntArriveToTownHall();
            }
            
            @Override
            protected void onMoveError(String msg) {
                woaAgent.log(Level.FINE, "Error while moving towards town hall ("
                        + msg + ")");
                
                handler.onCouldntArriveToTownHall();
            }
        });
    }
    
    private List<CellTranslation> findShortestPathToTownHall(MapCell source) {
        List<CellTranslation> shortestPath = graphKnownMap.findShortestPathTo(source, (MapCell t) -> {
            if (t.getContent() instanceof Building) {
                Building building = (Building) t.getContent();
                
                if (building.getType().isEmpty()) {
                    return false;
                }
                else {
                    // TODO: fix Town hall type hard coded
                    return building.getOwner().equals(agentUnit.getTribeAID())
                            && building.getType().equals(WoaDefinitions.TOWN_HALL);
                }
            }
            
            return false;
        });
        
        return shortestPath;
    }
    
    private void startCreateUnitBehaviour(OnCreatedUnitHandler handler) {
        
        Action createUnitAction = new Action(woaAgent.getAID(), new CreateUnit());
        woaAgent.addBehaviour(new Conversation(woaAgent, comStandard
                , GameOntology.CREATEUNIT) {
            @Override
            public void onStart() {
                woaAgent.log(Level.FINE, "Wants to create a unit");

                sendMessage(worldAID, ACLMessage.REQUEST, createUnitAction
                        , new Conversation.SentMessageHandler() {

                    @Override
                    public void onSent(String conversationID) {

                        receiveResponse(conversationID, new Conversation.ResponseHandler() {

                            @Override
                            public void onAgree(ACLMessage response) {
                                woaAgent.log(Level.FINER, "received CreateUnit agree from "
                                        + response.getSender().getLocalName());

                                receiveResponse(conversationID, new Conversation.ResponseHandler() {

                                    @Override
                                    public void onFailure(ACLMessage response) {
                                        woaAgent.log(Level.WARNING, "received CreateUnit failure from "
                                        + response.getSender().getLocalName());
                                        handler.onCouldntCreateUnit();
                                    }

                                    @Override
                                    public void onInform(ACLMessage response) {
                                        woaAgent.log(Level.FINER, "received CreateUnit inform from "
                                        + response.getSender().getLocalName());
                                        
                                        woaAgent.log(Level.FINE, "Created a unit at: "
                                                + agentUnit.getCurrentPosition().getXCoord()
                                                + "," + agentUnit.getCurrentPosition().getYCoord());
                                        handler.onCreatedUnit();
                                    }

                                });
                            }

                            @Override
                            public void onNotUnderstood(ACLMessage response) {
                                woaAgent.log(Level.WARNING, "received CreateUnit not understood from "
                                        + response.getSender().getLocalName());
                                handler.onCouldntCreateUnit();
                            }

                            @Override
                            public void onRefuse(ACLMessage response) {
                                woaAgent.log(Level.FINE, "receive CreateUnit refuse from "
                                        + response.getSender().getLocalName());
                                handler.onCouldntCreateUnit();
                            }

                        });
                    }
                });

            }
            
        });
    }

    
    private interface OnArrivedToTownHallHandler {
        
        public void onArrivedToTownHall();
        
        public void onCouldntArriveToTownHall();
        
    }
    
    private interface OnCreatedUnitHandler {
    
        public void onCreatedUnit();
        
        public void onCouldntCreateUnit();
    
    }  
    
}
