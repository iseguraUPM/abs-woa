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
import es.upm.woa.ontology.CreateBuilding;
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
class CreateBuildingStrategy extends Strategy {
    
    private final WoaAgent woaAgent;
    private final CommunicationStandard comStandard;
    private final GraphGameMap graphKnownMap;
    private final AID worldAID;
    
    private final String buildingType;
    private MapCell constructionSite;
    private final ConstructionSiteFinder constructionSiteFinder;
    
    private final PositionedAgentUnit agentUnit;
    
    private final int priority;
    private boolean finished;
    
    CreateBuildingStrategy(WoaAgent agent, CommunicationStandard comStandard
            , GraphGameMap graphGameMap, AID worldAID
            , PositionedAgentUnit agentUnit, String buildingType
            , MapCell constructionSite) {
        super(agent);
        this.woaAgent = agent;
        this.comStandard = comStandard;
        this.graphKnownMap = graphGameMap;
        this.worldAID = worldAID;
        this.buildingType = buildingType;
        this.constructionSite = constructionSite;
        this.constructionSiteFinder = null;
        this.agentUnit = agentUnit;
        
        this.priority = HIGH_PRIORITY;
        this.finished = false;
    }
    
    CreateBuildingStrategy(WoaAgent agent, CommunicationStandard comStandard
            , GraphGameMap graphGameMap, AID worldAID
            , PositionedAgentUnit agentUnit, String buildingType
            , ConstructionSiteFinder constructionSiteFinder) {
        super(agent);
        this.woaAgent = agent;
        this.comStandard = comStandard;
        this.graphKnownMap = graphGameMap;
        this.worldAID = worldAID;
        this.buildingType = buildingType;
        this.constructionSite = null;
        this.constructionSiteFinder = constructionSiteFinder;;
        this.agentUnit = agentUnit;
        
        this.priority = HIGH_PRIORITY;
        this.finished = false;
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
    
    private void createBuilding() {
        startCreateUnitBehaviour(new OnCreatedUnitHandler() {
            @Override
            public void onCreatedBuilding() {
                finishStrategy();
            }

            @Override
            public void onCouldntCreateBuilding() {
                finishStrategy();
            }
        });
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
        woaAgent.log(Level.FINE, 
                "starting create building strategy");
        
        if (constructionSite == null) {
            constructionSite = findCandidateConstructionSite();
            if (constructionSite == null) {
                woaAgent.log(Level.WARNING, "Could not find a " + buildingType
                        + " construction site close to "
                        + agentUnit.getCurrentPosition());
                finishStrategy();
                return;
            }
        }
       
        moveToConstructionSite(new OnArrivedToConstructionSiteHandler() {
            @Override
            public void onArrivedToConstuctionSite() {
                createBuilding();
            }

            @Override
            public void onCouldntArriveToConstructionSite() {
               finishStrategy();
            }
        });
    }
    
    private void moveToConstructionSite(OnArrivedToConstructionSiteHandler handler) {
        List<CellTranslation> pathToConstructionSite
                = findShortestPathToConstructionSite();
        
        startFollowPathBehaviour(pathToConstructionSite, handler);
    }

    private void startFollowPathBehaviour(List<CellTranslation> pathToTownHall
            , OnArrivedToConstructionSiteHandler handler) {
        woaAgent.addBehaviour(new FollowPathBehaviour(woaAgent, comStandard
                , worldAID, agentUnit, pathToTownHall) {
            @Override
            protected void onArrived(CellTranslation direction, MapCell destination) {
                agentUnit.updateCurrentPosition(direction, destination);
                woaAgent.log(Level.FINE, "Arrived to construction site at: "
                        + destination);
                handler.onArrivedToConstuctionSite();
            }
            
            @Override
            protected void onStep(CellTranslation direction, MapCell currentCell) {
                agentUnit.updateCurrentPosition(direction, currentCell);
                woaAgent.log(Level.FINER, "Moving towards construction site from: "
                        + currentCell);
            }
            
            @Override
            protected void onStuck(MapCell currentCell) {
                woaAgent.log(Level.FINE, "Cannot move towards construction site"
                        + " from: " + currentCell);
                
                handler.onCouldntArriveToConstructionSite();
            }
            
            @Override
            protected void onMoveError(String msg) {
                woaAgent.log(Level.FINE, "Error while moving towards"
                        + " construction site (" + msg + ")");
                
                handler.onCouldntArriveToConstructionSite();
            }
        });
    }
    
    private List<CellTranslation> findShortestPathToConstructionSite() {
        List<CellTranslation> shortestPath
                = graphKnownMap.findShortestPath(agentUnit.getCurrentPosition()
                        , constructionSite);
        
        return shortestPath;
    }
    
    private void startCreateUnitBehaviour(OnCreatedUnitHandler handler) {
        CreateBuilding createBuilding = new CreateBuilding();
        createBuilding.setBuildingType(buildingType);
        
        Action createUnitAction = new Action(woaAgent.getAID(), createBuilding);
        woaAgent.addBehaviour(new Conversation(woaAgent, comStandard
                , createUnitAction, GameOntology.CREATEBUILDING) {
            @Override
            public void onStart() {
                woaAgent.log(Level.FINE, "Wants to create a building of "
                        + buildingType);

                sendMessage(worldAID, ACLMessage.REQUEST
                        , new Conversation.SentMessageHandler() {

                    @Override
                    public void onSent(String conversationID) {

                        receiveResponse(conversationID, new Conversation.ResponseHandler() {

                            @Override
                            public void onAgree(ACLMessage response) {
                                woaAgent.log(Level.FINER, "received CreateBuilding agree from "
                                        + response.getSender().getLocalName());

                                receiveResponse(conversationID, new Conversation.ResponseHandler() {

                                    @Override
                                    public void onFailure(ACLMessage response) {
                                        woaAgent.log(Level.WARNING, "received CreateBuilding failure from "
                                        + response.getSender().getLocalName());
                                        handler.onCouldntCreateBuilding();
                                    }

                                    @Override
                                    public void onInform(ACLMessage response) {
                                        woaAgent.log(Level.FINER, "received CreateBuilding inform from "
                                        + response.getSender().getLocalName());
                                        
                                        woaAgent.log(Level.FINE, "Created a building of "
                                                + buildingType + " at: "
                                                + agentUnit.getCurrentPosition());
                                        handler.onCreatedBuilding();
                                    }

                                });
                            }

                            @Override
                            public void onNotUnderstood(ACLMessage response) {
                                woaAgent.log(Level.WARNING, "received CreateUnit not understood from "
                                        + response.getSender().getLocalName());
                                handler.onCouldntCreateBuilding();
                            }

                            @Override
                            public void onRefuse(ACLMessage response) {
                                woaAgent.log(Level.FINE, "receive CreateUnit refuse from "
                                        + response.getSender().getLocalName());
                                handler.onCouldntCreateBuilding();
                            }

                        });
                    }
                });

            }
            
        });
    }

    private MapCell findCandidateConstructionSite() {
        return constructionSiteFinder
                .findConstructionCloseTo(agentUnit.getCurrentPosition()
                        , buildingType);
    }

    private interface OnArrivedToConstructionSiteHandler {
        
        public void onArrivedToConstuctionSite();
        
        public void onCouldntArriveToConstructionSite();
        
    }
    
    private interface OnCreatedUnitHandler {
    
        public void onCreatedBuilding();
        
        public void onCouldntCreateBuilding();
    
    }
    
}
