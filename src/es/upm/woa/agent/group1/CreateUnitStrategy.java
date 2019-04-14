/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.GraphGameMap;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.agent.group1.strategy.Strategy;
import es.upm.woa.agent.group1.strategy.StrategyEvent;
import es.upm.woa.agent.group1.strategy.StrategyEventDispatcher;
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
public class CreateUnitStrategy extends Strategy {
    
    private int priority;
    private boolean finished;
    
    public CreateUnitStrategy(AgUnit agentUnit, StrategyEventDispatcher eventDispatcher) {
        super(agentUnit, eventDispatcher);
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
        AgUnit myAgUnit = (AgUnit) myAgent;
        List<MapCell> pathToTownHall
                = findShortestPathToTownHall(myAgUnit.getCurrentCell());
        
        startFollowPathBehaviour(myAgUnit, pathToTownHall, handler);
    }

    private void startFollowPathBehaviour(AgUnit myAgUnit, List<MapCell> pathToTownHall, OnArrivedToTownHallHandler handler) {
        myAgUnit.addBehaviour(new FollowPathBehaviour(myAgUnit, pathToTownHall) {
            @Override
            protected void onArrived(MapCell destination) {
                myAgUnit.log(Level.FINE, "Arrived to town hall at: "
                        + destination.getXCoord() + "," + destination.getYCoord());
                handler.onArrivedToTownHall();
            }
            
            @Override
            protected void onStep(MapCell currentCell) {
                myAgUnit.log(Level.FINER, "Moving towards town hall from: "
                        + currentCell.getXCoord() + "," + currentCell.getYCoord());
            }
            
            @Override
            protected void onStuck(MapCell currentCell) {
                myAgUnit.log(Level.FINE, "Cannot move towards town hall from: "
                        + currentCell.getXCoord() + "," + currentCell.getYCoord());
                
                handler.onCouldntArriveToTownHall();
            }
            
            @Override
            protected void onMoveError(String msg) {
                myAgUnit.log(Level.FINE, "Error while moving towards town hall ("
                        + msg + ")");
                
                handler.onCouldntArriveToTownHall();
            }
        });
    }
    
    private List<MapCell> findShortestPathToTownHall(MapCell source) {
        AgUnit myAgUnit = (AgUnit) myAgent;
        GraphGameMap knownMap = ((AgUnit) myAgent).getKnownMap();
        
        List<MapCell> shortestPath = knownMap.findShortestPathTo(source, (MapCell t) -> {
            if (t.getContent() instanceof Building) {
                Building building = (Building) t.getContent();
                
                if (building.getType().isEmpty()) {
                    return false;
                }
                else {
                    // TODO: fix Town hall type hard coded
                    return building.getOwner().equals(myAgUnit.getOwnerAID())
                            && building.getType().get(0).equals("TownHall");
                }
            }
            
            return false;
        });
        
        return shortestPath;
    }
    
    private void startCreateUnitBehaviour(OnCreatedUnitHandler handler) {
        final AgUnit myAgUnit = (AgUnit) myAgent;
        
        Action createUnitAction = new Action(myAgUnit.getAID(), new CreateUnit());
        myAgUnit.addBehaviour(new Conversation(myAgUnit, myAgUnit.getOntology()
                , myAgUnit.getCodec(), createUnitAction, GameOntology.CREATEUNIT) {
            @Override
            public void onStart() {
                myAgUnit.log(Level.FINE, "Wants to create a unit");
                
                AID worldAID = myAgUnit.getWorldAID();

                sendMessage(worldAID, ACLMessage.REQUEST
                        , new Conversation.SentMessageHandler() {

                    @Override
                    public void onSent(String conversationID) {

                        receiveResponse(conversationID, new Conversation.ResponseHandler() {

                            @Override
                            public void onAgree(ACLMessage response) {
                                myAgUnit.log(Level.FINER, "received CreateUnit agree from "
                                        + response.getSender().getLocalName());

                                receiveResponse(conversationID, new Conversation.ResponseHandler() {

                                    @Override
                                    public void onFailure(ACLMessage response) {
                                        myAgUnit.log(Level.WARNING, "received CreateUnit failure from "
                                        + response.getSender().getLocalName());
                                        handler.onCouldntCreateUnit();
                                    }

                                    @Override
                                    public void onInform(ACLMessage response) {
                                        myAgUnit.log(Level.FINER, "received CreateUnit inform from "
                                        + response.getSender().getLocalName());
                                        
                                        myAgUnit.log(Level.FINE, "Created a unit at: "
                                                + myAgUnit.getCurrentCell().getXCoord()
                                                + "," + myAgUnit.getCurrentCell().getYCoord());
                                        handler.onCreatedUnit();
                                    }

                                });
                            }

                            @Override
                            public void onNotUnderstood(ACLMessage response) {
                                myAgUnit.log(Level.WARNING, "received CreateUnit not understood from "
                                        + response.getSender().getLocalName());
                                handler.onCouldntCreateUnit();
                            }

                            @Override
                            public void onRefuse(ACLMessage response) {
                                myAgUnit.log(Level.FINE, "receive CreateUnit refuse from "
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
