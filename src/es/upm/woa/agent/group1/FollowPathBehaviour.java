/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.MoveToCell;

import jade.content.onto.basic.Action;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;

import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author ISU
 */
public abstract class FollowPathBehaviour extends Behaviour {

    private final List<MapCell> path;
    private MapCell currentCell;
    private int next;
    private boolean finished;

    public FollowPathBehaviour(AgUnit agentUnit, List<MapCell> path) {
        super(agentUnit);
        this.path = path;
        this.finished = false;
    }

    @Override
    public final void onStart() {
        if (path.isEmpty()) {
            
            onMoveErrorImpl("Path is empty");
            finished = true;
        } else if (path.size() == 1) {
            onArrived(path.get(0));
            finished = true;
        } else {
            currentCell = path.get(0);
            next = 0;
        }
    }

    @Override
    public final void action() {
        if (!finished) {
            step();
        }
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
        AgUnit agentUnit = (AgUnit) myAgent;
        
        Action moveAction = createMoveToCellAction(targetCell);
        agentUnit.addBehaviour(new Conversation(myAgent, agentUnit.getOntology()
                , agentUnit.getCodec(), moveAction, GameOntology.MOVETOCELL) {
            @Override
            public void onStart() {
                sendMessage(agentUnit.getWorldAID(), ACLMessage.REQUEST, new SentMessageHandler() {
                    @Override
                    public void onSent(String conversationID) {
                        agentUnit.log(Level.FINE, "wants to move to cell "
                                + targetCell.getXCoord() + "," + targetCell.getXCoord());

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
                                        onMoveErrorImpl("Message failure");
                                    }

                                    @Override
                                    public void onInform(ACLMessage response) {
                                        agentUnit.log(Level.FINER, "receive MoveToCell inform from "
                                                + response.getSender().getLocalName());

                                        agentUnit.log(Level.FINE, "moved to cell "
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
                                agentUnit.log(Level.WARNING, "receive MoveToCell not understood from "
                                        + response.getSender().getLocalName());
                                onMoveErrorImpl("Message not understood");
                            }

                            @Override
                            public void onRefuse(ACLMessage response) {
                                agentUnit.log(Level.FINER, "receive MoveToCell refuse from "
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
