/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.GameMap;
import es.upm.woa.agent.group1.map.GameMapCoordinate;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.map.MapCellFactory;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.agent.group1.strategy.Strategy;
import es.upm.woa.agent.group1.strategy.StrategyEvent;
import es.upm.woa.agent.group1.strategy.StrategyEventDispatcher;
import es.upm.woa.ontology.Cell;
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
    private final GraphGameMap graphKnownMap;
    private final AID worldAID;
    
    private final PositionedAgentUnit agentUnit;
    
    private boolean finished;
    private final Set<MapCell> visitedCandidates;
    private MapCell nextCandidate;
    private int exploredCells;

    public FreeExploreStrategy(WoaAgent agent, CommunicationStandard comStandard
            , GraphGameMap graphGameMap, AID worldAID, PositionedAgentUnit agentUnit, StrategyEventDispatcher eventDispatcher) {
        super(agent, eventDispatcher);
        this.woaAgent = agent;
        this.comStandard = comStandard;
        this.graphKnownMap = graphGameMap;
        this.worldAID = worldAID;
        this.agentUnit = agentUnit;
        
        this.finished = false;
        this.visitedCandidates = new HashSet<>();
        this.exploredCells = 1;
    }

    @Override
    public int getPriority() {
        return MID_PRIORITY;
    }

    @Override
    public void onStart() {
        exploreNewCell();
    }

    private void exploreNewCell() {
        MapCell currentCell = agentUnit.getCurrentPosition();
        Cell destination = findAdjacentCellToExplore(graphKnownMap, currentCell);
        if (destination != null) {
            travelToNewCell(currentCell, destination, new OnMovedToNewCellHandler() {
                @Override
                public void onMoved() {
                    woaAgent.log(Level.FINE, "explored cell "
                            + destination.getX() + ","
                            + destination.getY());
                    exploredCells++;
                    finished = true;
                }

                @Override
                public void onError() {
                    woaAgent.log(Level.FINE, "error while traveling to cell "
                            + destination.getX() + ","
                            + destination.getY());
                    finished = true;
                }
            });
        } else {
            woaAgent.log(Level.FINE, "Could not find a cell to explore");
        }
        int mapSize = (graphKnownMap.getHeight()
                * graphKnownMap.getWidth()) / 2;
        woaAgent.log(Level.FINE, "Explored " + exploredCells
                + " out of " + mapSize + " cells");
    }

    private Cell findAdjacentCellToExplore(GameMap knownMap, MapCell currentPosition) {

        if (!GameMapCoordinate.isCorrectPosition(knownMap.getWidth(), knownMap.getHeight(), currentPosition.getXCoord(), currentPosition.getYCoord())) {
            return null;
        }

        nextCandidate = null;
        for (int[] translationVector : computeShuffledTranslationVectors()) {
            
            int[] targetPosition = GameMapCoordinate.applyTranslation(knownMap.getWidth(), knownMap.getHeight(), currentPosition.getXCoord(), currentPosition.getYCoord(), translationVector);
            try {
                MapCell foundTargetPosition = knownMap.getCellAt(
                        targetPosition[0], targetPosition[1]);
                if (nextCandidate == null && !visitedCandidates.contains(foundTargetPosition)) {
                    nextCandidate = foundTargetPosition;
                }
            } catch (NoSuchElementException ex) {
                nextCandidate = currentPosition;
                Cell cellToExplore = new Cell();
                cellToExplore.setX(targetPosition[0]);
                cellToExplore.setY(targetPosition[1]);
                return cellToExplore;
            }

        }

        visitedCandidates.add(currentPosition);
        if (finished || nextCandidate == null) {
            return null;
        } else {
            return findAdjacentCellToExplore(knownMap, nextCandidate);
        }
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
    public void action() {
        block();
    }

    @Override
    public void onEvent(StrategyEvent event) {

    }

    private Iterable<int[]> computeShuffledTranslationVectors() {
        List<int[]> shuffledOperators = new ArrayList<>();
        shuffledOperators.addAll(Arrays.asList(GameMapCoordinate.POS_OPERATORS));

        Collections.shuffle(shuffledOperators);

        return shuffledOperators;
    }

    private void travelToNewCell(MapCell currentCell, Cell cellToExplore, OnMovedToNewCellHandler handler) {
        if (nextCandidate == null || currentCell == nextCandidate) {
            moveToTargetCell(cellToExplore, handler);
        } else {
            moveToFarAwayTargetCell(currentCell, cellToExplore, handler);
        }
    }

    private void moveToFarAwayTargetCell(MapCell startCell, Cell cellToExplore, OnMovedToNewCellHandler handler) {
        List<MapCell> path = graphKnownMap.findShortestPath(startCell, nextCandidate);

        myAgent.addBehaviour(new FollowPathBehaviour(woaAgent, comStandard
                , worldAID, path) {
            @Override
            protected void onArrived(MapCell destination) {
                agentUnit.setCurrentPosition(destination);
                woaAgent.log(Level.FINE, "arrived to cell "
                        + destination.getXCoord() + ","
                        + destination.getYCoord());
                moveToTargetCell(cellToExplore, handler);
            }

            @Override
            protected void onStep(MapCell currentCell) {
                agentUnit.setCurrentPosition(currentCell);
                woaAgent.log(Level.FINE, "traveling to cell "
                        + nextCandidate.getXCoord() + ","
                        + nextCandidate.getYCoord() + " from "
                        + currentCell.getXCoord() + ","
                        + currentCell.getYCoord());
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

    private void moveToTargetCell(Cell targetCell, OnMovedToNewCellHandler handler) {
        Action moveAction = createMoveToCellAction(targetCell);
        myAgent.addBehaviour(new Conversation(myAgent, comStandard, moveAction, GameOntology.MOVETOCELL) {
            @Override
            public void onStart() {
                sendMessage(worldAID, ACLMessage.REQUEST, new Conversation.SentMessageHandler() {
                    @Override
                    public void onSent(String conversationID) {
                        woaAgent.log(Level.FINE, "wants to explore cell "
                                + targetCell.getX() + "," + targetCell.getY());

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
                                            updateCurrentPosition(woaAgent, response);
                                            handler.onMoved();
                                        } catch (Codec.CodecException | OntologyException | IndexOutOfBoundsException ex) {
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

    private void updateCurrentPosition(WoaAgent woaAgent, ACLMessage response)
            throws Codec.CodecException, OntologyException {
        ContentElement ce = woaAgent.getContentManager().extractContent(response);
        Action agAction = (Action) ce;
        Concept conc = agAction.getAction();
        MoveToCell targetCell = (MoveToCell) conc;
        MapCell currentCell = MapCellFactory
                .getInstance()
                .buildCell(targetCell.getTarget());
        boolean success = graphKnownMap.addCell(currentCell);
        if (!success) {
            currentCell = graphKnownMap.getCellAt(currentCell
                    .getXCoord(), currentCell.getYCoord());
        }

        agentUnit.setCurrentPosition(currentCell);
        woaAgent.log(Level.FINER, "receive MoveToCell inform from " + response.getSender().getLocalName());
    }

    private Action createMoveToCellAction(Cell targetCell) {
        MoveToCell moveToCell = new MoveToCell();
        moveToCell.setTarget(targetCell);

        return new Action(myAgent.getAID(), moveToCell);
    }

    private interface OnMovedToNewCellHandler {

        void onMoved();

        void onError();

    }
    

}
