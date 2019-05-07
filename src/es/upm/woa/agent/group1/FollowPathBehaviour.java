/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.MoveToCell;

import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author ISU
 */
abstract class FollowPathBehaviour extends SimpleBehaviour {

    private final WoaAgent woaAgent;
    private final CommunicationStandard comStandard;
    private final AID worldAID;
    private final List<MapCell> path;
    
    private MapCell currentCell;
    private int next;
    private boolean finished;

    public FollowPathBehaviour(WoaAgent agent, CommunicationStandard comStandard, AID worldAID, List<MapCell> path) {
        super(agent);
        this.woaAgent = agent;
        this.comStandard = comStandard;
        this.worldAID = worldAID;
        this.path = path;
        
        this.next = 0;
        this.finished = false;
    }

    @Override
    public final void onStart() {
        if (path.isEmpty()) {
            onMoveErrorImpl("Path is empty");
        } else if (path.size() == 1) {
            finished = true;
            onArrived(path.get(0));
        } else {
            currentCell = path.get(0);
            step();
        }
    }
    
    @Override
    public void action() {
        block();
    }

    private void step() {
        next++;
        if (next < path.size()) {
            MapCell targetCell = path.get(next);
            launchMoveConversation(targetCell);
        }
        else {
            finished = true;
            onArrived(currentCell);
        }
    }

    private void launchMoveConversation(MapCell targetCell) {
        Action moveAction = createMoveToCellAction(targetCell);
        woaAgent.addBehaviour(new Conversation(myAgent, comStandard
                , moveAction, GameOntology.MOVETOCELL) {
            @Override
            public void onStart() {
                sendMessage(worldAID, ACLMessage.REQUEST, new SentMessageHandler() {
                    @Override
                    public void onSent(String conversationID) {
                        woaAgent.log(Level.FINE, "wants to move to cell "
                                + targetCell.getXCoord() + "," + targetCell.getXCoord());

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
                                        onMoveErrorImpl("Message failure");
                                    }

                                    @Override
                                    public void onInform(ACLMessage response) {
                                        woaAgent.log(Level.FINER, "receive MoveToCell inform from "
                                                + response.getSender().getLocalName());

                                        woaAgent.log(Level.FINE, "moved to cell "
                                                + targetCell.getXCoord() + ","
                                                + targetCell.getYCoord());

                                        currentCell = targetCell;
                                        if (!path.isEmpty()
                                                && currentCell != path.get(path.size()-1)) {
                                            onStep(currentCell);
                                        }
                                        
                                        step();
                                    }

                                });
                            }

                            @Override
                            public void onNotUnderstood(ACLMessage response) {
                                woaAgent.log(Level.WARNING, "receive MoveToCell not understood from "
                                        + response.getSender().getLocalName());
                                onMoveErrorImpl("Message not understood");
                            }

                            @Override
                            public void onRefuse(ACLMessage response) {
                                woaAgent.log(Level.FINER, "receive MoveToCell refuse from "
                                        + response.getSender().getLocalName());
                                onStuckImpl(currentCell);
                            }

                        });
                    }

                    @Override
                    public void onSentMessageError() {
                        onMoveErrorImpl("Could not send message");
                    }

                });
            }
        });
    }

    private Action createMoveToCellAction(MapCell targetCell) {
        Cell targetCellOntology = new Cell();
        targetCellOntology.setX(targetCell.getXCoord());
        targetCellOntology.setY(targetCell.getYCoord());

        MoveToCell moveToCell = new MoveToCell();
        moveToCell.setTarget(targetCellOntology);
        return new Action(myAgent.getAID(), moveToCell);
    }

    @Override
    public final boolean done() {
        return finished;
    }

    private void onStuckImpl(MapCell currentCell) {
        finished = true;
        onStuck(currentCell);
        block();
    }

    private void onMoveErrorImpl(String msg) {
        finished = true;
        onMoveError(msg);
        block();
    }
    
    protected abstract void onArrived(MapCell destination);

    protected abstract void onStep(MapCell currentCell);
    
    protected abstract void onStuck(MapCell currentCell);
    
    protected abstract void onMoveError(String msg);

}
