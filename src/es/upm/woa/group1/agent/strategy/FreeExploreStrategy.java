/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent.strategy;

import es.upm.woa.group1.agent.ExplorationRequestHandler;
import es.upm.woa.group1.agent.WoaAgent;
import es.upm.woa.group1.map.CellTranslation;
import es.upm.woa.group1.map.MapCell;
import es.upm.woa.group1.map.MapCellFactory;
import es.upm.woa.group1.map.PathfinderGameMap;
import es.upm.woa.group1.protocol.CommunicationStandard;
import es.upm.woa.group1.protocol.Conversation;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.MoveToCell;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;

/**
 *
 * @author ISU
 */
class FreeExploreStrategy extends Strategy {

    private final WoaAgent woaAgent;
    private final CommunicationStandard comStandard;
    private final PathfinderGameMap graphKnownMap;
    private final AID worldAID;
    private final int priority;
    
    private final PositionedAgentUnit agentUnit;
    private final ExplorationRequestHandler explorationRequestHandler;
    
    private boolean finishedRound;
    private boolean finishExploration;
    private final Set<MapCell> visitedCandidates;
    private MapCell nextCandidate;

    public FreeExploreStrategy(int priority, WoaAgent agent, CommunicationStandard comStandard
            , PathfinderGameMap graphGameMap, AID worldAID, PositionedAgentUnit agentUnit
    , ExplorationRequestHandler explorationRequestHandler) {
        super(agent);
        this.woaAgent = agent;
        this.comStandard = comStandard;
        this.graphKnownMap = graphGameMap;
        this.worldAID = worldAID;
        this.agentUnit = agentUnit;
        this.explorationRequestHandler = explorationRequestHandler;
        
        this.priority = priority;
        this.finishedRound = false;
        this.finishExploration = false;
        this.visitedCandidates = new HashSet<>();
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void onStart() {
        exploreNewCell();
    }

    private void exploreNewCell() {
        MapCell currentCell = agentUnit.getCurrentPosition();
        CellTranslation direction = findDirectionToExplore(currentCell);
        if (direction != null) {
            travelToNewCell(currentCell, direction, new OnMovedToNewCellHandler() {
                @Override
                public void onMoved() {
                    woaAgent.log(Level.FINE, "explored cell "
                            + agentUnit.getCurrentPosition());
                    finishedRound = true;
                }

                @Override
                public void onError() {
                    woaAgent.log(Level.FINE, "error while traveling on direction "
                            + direction);
                    finishedRound = true;
                }
            });
        } else {
            explorationRequestHandler.onFinishedExploration();
            woaAgent.log(Level.INFO, "Could not find a cell to explore");
            finishedRound = true;
            finishExploration = true;
        }
    }

    private CellTranslation findDirectionToExplore(MapCell currentPosition) {
        nextCandidate = null;
        for (CellTranslation.TranslateDirection direction
                : computeShuffledTranslationVectors()) {
            CellTranslation translationDirection = new CellTranslation(direction);
            
            MapCell connectedNeighbour
                    = graphKnownMap.getMapCellOnDirection(currentPosition
                            , translationDirection);
            if (connectedNeighbour != null) {
                if (nextCandidate == null && !visitedCandidates.contains(connectedNeighbour)) {
                    nextCandidate = connectedNeighbour;
                }
            }
            else {
                nextCandidate = currentPosition;
                return translationDirection;
            }
        }

        visitedCandidates.add(currentPosition);
        if (finishedRound || nextCandidate == null) {
            return null;
        } else {
            return findDirectionToExplore(nextCandidate);
        }
    }

    @Override
    public boolean done() {
        return finishedRound;
    }

    @Override
    protected void resetStrategy() {
        finishedRound = false;
    }

    @Override
    public void action() {
        block();
    }
    
    private Iterable<CellTranslation.TranslateDirection> computeShuffledTranslationVectors() {
        List<CellTranslation.TranslateDirection> shuffledOperators = new ArrayList<>();
        shuffledOperators.addAll(Arrays.asList(CellTranslation.TranslateDirection.values()));

        Collections.shuffle(shuffledOperators);

        return shuffledOperators;
    }

    private void travelToNewCell(MapCell currentCell, CellTranslation direction, OnMovedToNewCellHandler handler) {
        if (nextCandidate == null || currentCell.equals(nextCandidate)) {
            moveInDirection(direction, handler);
        } else {
            moveToFarAwayTargetCell(currentCell, direction, handler);
        }
    }

    private void moveToFarAwayTargetCell(MapCell startCell, CellTranslation lastTranslation, OnMovedToNewCellHandler handler) {
        List<CellTranslation> path = graphKnownMap.findShortestPath(startCell, nextCandidate);

        myAgent.addBehaviour(new FollowPathBehaviour(woaAgent, comStandard
                , worldAID, agentUnit, path) {
            @Override
            protected void onArrived(CellTranslation direction, MapCell destination) {
                agentUnit.updateCurrentPosition(direction, destination);
                woaAgent.log(Level.FINE, "arrived to cell "
                        + destination);
                moveInDirection(lastTranslation, handler);
            }

            @Override
            protected void onStep(CellTranslation direction, MapCell currentCell) {
                agentUnit.updateCurrentPosition(direction, currentCell);
                woaAgent.log(Level.FINE, "traveling to cell "
                        + nextCandidate + " from "
                        + currentCell);
            }

            @Override
            protected void onStuck(MapCell currentCell) {
                handler.onError();
            }

            @Override
            protected void onMoveError(String msg) {
                handler.onError();
            }
        });
    }

    private void moveInDirection(CellTranslation direction, OnMovedToNewCellHandler handler) {
        Action moveAction = createMoveToCellAction(direction);
        myAgent.addBehaviour(new Conversation(myAgent, comStandard, GameOntology.MOVETOCELL) {
            @Override
            public void onStart() {
                sendMessage(worldAID, ACLMessage.REQUEST, moveAction
                        , new Conversation.SentMessageHandler() {
                    @Override
                    public void onSent(String conversationID) {
                        woaAgent.log(Level.FINE, "wants to explore from "
                                + agentUnit.getCurrentPosition() + " "
                                + direction);

                        receiveResponse(conversationID, new Conversation.ResponseHandler() {

                            @Override
                            public void onAgree(ACLMessage response) {
                                woaAgent.log(Level.FINER, "receive MoveToCell agree from "
                                        + response.getSender().getLocalName());

                                receiveResponse(conversationID, new Conversation.ResponseHandler() {

                                    @Override
                                    public void onFailure(ACLMessage response) {
                                        woaAgent.log(Level.WARNING, "receive MoveToCell failure from "
                                                + response.getSender().getLocalName());
                                        handler.onError();
                                    }

                                    @Override
                                    public void onInform(ACLMessage response) {
                                        try {
                                            updateCurrentPosition(woaAgent, direction, response);
                                            handler.onMoved();
                                        } catch (Codec.CodecException | OntologyException
                                                | IndexOutOfBoundsException | NoSuchElementException ex) {
                                            woaAgent.log(Level.WARNING, "cannot retrieve new"
                                                    + " position. Unit"
                                                    + " position desync"
                                                    + " (" + ex + ")"
                                                    + response.getSender().getLocalName());
                                            handler.onError();
                                        }
                                    }

                                });
                            }

                            @Override
                            public void onNotUnderstood(ACLMessage response) {
                                woaAgent.log(Level.WARNING, "receive MoveToCell not understood from "
                                        + response.getSender().getLocalName());
                                handler.onError();
                            }

                            @Override
                            public void onRefuse(ACLMessage response) {
                                woaAgent.log(Level.FINER, "receive MoveToCell refuse from "
                                        + response.getSender().getLocalName());
                                handler.onError();
                            }

                        });
                    }

                    @Override
                    public void onSentMessageError() {
                        handler.onError();
                    }

                });
            }
        });
    }

    private void updateCurrentPosition(WoaAgent woaAgent
            , CellTranslation operation, ACLMessage response)
            throws Codec.CodecException, OntologyException {
        ContentElement ce = woaAgent.getContentManager().extractContent(response);
        Action agAction = (Action) ce;
        Concept conc = agAction.getAction();
        MoveToCell targetCell = (MoveToCell) conc;
        
        MapCell newPosition = MapCellFactory
                .getInstance()
                .buildCell(targetCell.getNewlyArrivedCell());

        agentUnit.updateCurrentPosition(operation, newPosition);
        woaAgent.log(Level.FINER, "receive MoveToCell inform from " + response.getSender().getLocalName());
    }

    private Action createMoveToCellAction(CellTranslation operation) {
        MoveToCell moveToCell = new MoveToCell();
        moveToCell.setTargetDirection(operation.getTranslationCode());

        return new Action(myAgent.getAID(), moveToCell);
    }

    @Override
    public boolean isOneShot() {
        return finishExploration;
    }

    private interface OnMovedToNewCellHandler {

        void onMoved();

        void onError();

    }
    

}
