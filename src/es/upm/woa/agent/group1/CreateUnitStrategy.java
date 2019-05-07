/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.agent.group1.strategy.Strategy;
import es.upm.woa.agent.group1.strategy.StrategyEvent;
import es.upm.woa.agent.group1.strategy.StrategyEventDispatcher;
import es.upm.woa.ontology.Building;
import es.upm.woa.ontology.CreateUnit;
import es.upm.woa.ontology.GameOntology;

import jade.content.lang.Codec;
import jade.content.onto.Ontology;
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
    
    private final PositionedAgentUnit agentUnit;
    
    private int priority;
    private boolean finished;
    
    public CreateUnitStrategy(WoaAgent agent, CommunicationStandard comStandard
            , GraphGameMap graphGameMap, AID worldAID
            , PositionedAgentUnit agentUnit
            , StrategyEventDispatcher eventDispatcher) {
        super(agent, eventDispatcher);
        this.woaAgent = agent;
        this.comStandard = comStandard;
        this.graphKnownMap = graphGameMap;
        this.worldAID = worldAID;
        this.agentUnit = agentUnit;
        
        priority = HIGH_PRIORITY;
        finished = false;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void onEvent(StrategyEvent event) {
        if (event == StrategyEvent.CREATE_UNIT) {
            priority = HIGH_PRIORITY;
        }
        else if (event == StrategyEvent.WAIT) {
            priority = LOW_PRIORITY;
        }
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
        List<MapCell> pathToTownHall
                = findShortestPathToTownHall(agentUnit.getCurrentPosition());
        
        startFollowPathBehaviour(woaAgent, pathToTownHall, handler);
    }

    private void startFollowPathBehaviour(WoaAgent woaAgent, List<MapCell> pathToTownHall, OnArrivedToTownHallHandler handler) {
        woaAgent.addBehaviour(new FollowPathBehaviour(woaAgent, comStandard, worldAID, pathToTownHall) {
            @Override
            protected void onArrived(MapCell destination) {
                agentUnit.setCurrentPosition(destination);
                woaAgent.log(Level.FINE, "Arrived to town hall at: "
                        + destination.getXCoord() + "," + destination.getYCoord());
                handler.onArrivedToTownHall();
            }
            
            @Override
            protected void onStep(MapCell currentCell) {
                agentUnit.setCurrentPosition(currentCell);
                woaAgent.log(Level.FINER, "Moving towards town hall from: "
                        + currentCell.getXCoord() + "," + currentCell.getYCoord());
            }
            
            @Override
            protected void onStuck(MapCell currentCell) {
                woaAgent.log(Level.FINE, "Cannot move towards town hall from: "
                        + currentCell.getXCoord() + "," + currentCell.getYCoord());
                
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
    
    private List<MapCell> findShortestPathToTownHall(MapCell source) {
        List<MapCell> shortestPath = graphKnownMap.findShortestPathTo(source, (MapCell t) -> {
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
                , createUnitAction, GameOntology.CREATEUNIT) {
            @Override
            public void onStart() {
                woaAgent.log(Level.FINE, "Wants to create a unit");

                sendMessage(worldAID, ACLMessage.REQUEST
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
