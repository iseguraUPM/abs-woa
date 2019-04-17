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

    private boolean finished;
    private final Set<MapCell> visitedCandidates;
    private MapCell nextCandidate;
    private int exploredCells;

    public FreeExploreStrategy(AgUnit agent, StrategyEventDispatcher eventDispatcher) {
        super(agent, eventDispatcher);
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
        AgUnit myAgUnit = (AgUnit) myAgent;
        MapCell currentCell = myAgUnit.getCurrentCell();
        Cell destination = findAdjacentCellToExplore(myAgUnit.getKnownMap(), currentCell);
        if (destination != null) {
            travelToNewCell(currentCell, destination, new OnMovedToNewCellHandler() {
                @Override
                public void onMoved() {
                    myAgUnit.log(Level.FINE, "explored cell "
                            + destination.getX() + ","
                            + destination.getY());
                    exploredCells++;
                    finished = true;
                }

                @Override
                public void onError() {
                    myAgUnit.log(Level.FINE, "error while traveling to cell "
                            + destination.getX() + ","
                            + destination.getY());
                    finished = true;
                }
            });
        } else {
            myAgUnit.log(Level.FINE, "Could not find a cell to explore");
        }
        int mapSize = (myAgUnit.getKnownMap().getHeight()
                * myAgUnit.getKnownMap().getWidth()) / 2;
        myAgUnit.log(Level.FINE, "Explored " + exploredCells
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
        final AgUnit agentUnit = (AgUnit) myAgent;
        List<MapCell> path = agentUnit.getKnownMap().findShortestPath(startCell, nextCandidate);

        agentUnit.addBehaviour(new FollowPathBehaviour(agentUnit, path) {
            @Override
            protected void onArrived(MapCell destination) {
                agentUnit.setCurrentCell(destination);
                agentUnit.log(Level.FINE, "arrived to cell "
                        + destination.getXCoord() + ","
                        + destination.getYCoord());
                moveToTargetCell(cellToExplore, handler);
            }

            @Override
            protected void onStep(MapCell currentCell) {
                agentUnit.setCurrentCell(currentCell);
                agentUnit.log(Level.FINE, "traveling to cell "
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
        final AgUnit agentUnit = (AgUnit) myAgent;

        Action moveAction = createMoveToCellAction(targetCell);
        agentUnit.addBehaviour(new Conversation(myAgent, agentUnit.getOntology(), agentUnit.getCodec(), moveAction, GameOntology.MOVETOCELL) {
            @Override
            public void onStart() {
                sendMessage(agentUnit.getWorldAID(), ACLMessage.REQUEST, new Conversation.SentMessageHandler() {
                    @Override
                    public void onSent(String conversationID) {
                        agentUnit.log(Level.FINE, "wants to explore cell "
                                + targetCell.getX() + "," + targetCell.getY());

                        receiveResponse(conversationID, new Conversation.ResponseHandler() {

                            @Override
                            public void onAgree(ACLMessage response) {
                                agentUnit.log(Level.FINER, "receive MoveToCell agree from "
                                        + response.getSender().getLocalName());

                                receiveResponse(conversationID, new Conversation.ResponseHandler() {

                                    @Override
                                    public void onFailure(ACLMessage response) {
                                        agentUnit.log(Level.WARNING, "receive MoveToCell failure from "
                                                + response.getSender().getLocalName());
                                        handler.onError();
                                    }

                                    @Override
                                    public void onInform(ACLMessage response) {
                                        try {
                                            updateCurrentPosition(agentUnit, response);
                                            handler.onMoved();
                                        } catch (Codec.CodecException | OntologyException | IndexOutOfBoundsException ex) {
                                            agentUnit.log(Level.WARNING, "cannot retrieve new"
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
                                agentUnit.log(Level.WARNING, "receive MoveToCell not understood from "
                                        + response.getSender().getLocalName());
                                handler.onError();
                            }

                            @Override
                            public void onRefuse(ACLMessage response) {
                                agentUnit.log(Level.FINER, "receive MoveToCell refuse from "
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

    private void updateCurrentPosition(AgUnit agentUnit, ACLMessage response)
            throws Codec.CodecException, OntologyException {
        ContentElement ce = agentUnit.getContentManager().extractContent(response);
        Action agAction = (Action) ce;
        Concept conc = agAction.getAction();
        MoveToCell targetCell = (MoveToCell) conc;
        MapCell currentCell = MapCellFactory
                .getInstance()
                .buildCell(targetCell.getTarget());
        boolean success = agentUnit.getKnownMap().addCell(currentCell);
        if (!success) {
            currentCell = agentUnit.getKnownMap().getCellAt(currentCell
                    .getXCoord(), currentCell.getYCoord());
        }

        agentUnit.setCurrentCell(currentCell);
        agentUnit.log(Level.FINER, "receive MoveToCell inform from " + response.getSender().getLocalName());
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
