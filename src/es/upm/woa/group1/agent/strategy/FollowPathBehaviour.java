/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent.strategy;

import es.upm.woa.group1.agent.WoaAgent;
import es.upm.woa.group1.map.CellTranslation;
import es.upm.woa.group1.map.MapCell;
import es.upm.woa.group1.map.MapCellFactory;
import es.upm.woa.group1.protocol.CommunicationStandard;
import es.upm.woa.group1.protocol.Conversation;
import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.MoveToCell;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
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
    private final List<CellTranslation> pathOperations;

    private final PositionedAgentUnit agentUnit;

    private int next;
    private boolean finished;

    public FollowPathBehaviour(WoaAgent agent, CommunicationStandard comStandard,
            AID worldAID, PositionedAgentUnit agentUnit, List<CellTranslation> path) {
        super(agent);
        this.woaAgent = agent;
        this.comStandard = comStandard;
        this.worldAID = worldAID;
        this.pathOperations = path;
        this.agentUnit = agentUnit;

        this.next = 0;
        this.finished = false;
    }

    @Override
    public final void onStart() {
        if (pathOperations.isEmpty()) {
            onMoveErrorImpl("Path is empty");
        } else {
            step();
        }
    }

    @Override
    public void action() {
        block();
    }

    private void step() {
        if (next < pathOperations.size()) {
            CellTranslation operation = pathOperations.get(next);
            next++;
            launchMoveConversation(operation);
        } else {
            finished = true;
        }
        
    }

    private void launchMoveConversation(CellTranslation operation) {
        Action moveAction = createMoveToCellAction(operation);
        woaAgent.addBehaviour(new Conversation(myAgent, comStandard,
                GameOntology.MOVETOCELL) {
            @Override
            public void onStart() {
                sendMessage(worldAID, ACLMessage.REQUEST, moveAction, new SentMessageHandler() {
                    @Override
                    public void onSent(String conversationID) {
                        woaAgent.log(Level.FINE, "wants to move from "
                                + agentUnit.getCurrentPosition() + " "
                                + operation);

                        handleOnSentMessage(conversationID);
                    }

                    @Override
                    public void onSentMessageError() {
                        onMoveErrorImpl("Could not send message");
                    }

                });
            }

            private void handleOnSentMessage(String conversationID) {
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
                                try {
                                    woaAgent.log(Level.FINER, "receive MoveToCell inform from "
                                            + response.getSender().getLocalName());
                                    
                                    Cell newPosition = extractCellFromMessage(response);
                                    
                                    if (next < pathOperations.size()) {
                                        onStep(operation, MapCellFactory.getInstance()
                                                .buildCell(newPosition));
                                    }
                                    else {
                                        onArrived(operation, MapCellFactory.getInstance()
                                                .buildCell(newPosition));
                                    }
                                    
                                    step();
                                } catch (Codec.CodecException | OntologyException ex) {
                                    onMoveErrorImpl("Could not receive the new position");
                                }
                                
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
                        onStuckImpl(agentUnit.getCurrentPosition());
                    }
                    
                });
            }
        });
    }

    private Cell extractCellFromMessage(ACLMessage response)
            throws Codec.CodecException, OntologyException {
        ContentElement ce = woaAgent.getContentManager().extractContent(response);
        if (ce instanceof Action) {

            Action agAction = (Action) ce;
            Concept conc = agAction.getAction();

            if (conc instanceof MoveToCell) {
                MoveToCell cellDetail = (MoveToCell) conc;

                return cellDetail.getNewlyArrivedCell();
            }
        }

        throw new OntologyException("Message contents are empty");
    }

    private Action createMoveToCellAction(CellTranslation operation) {
        MoveToCell moveToCell = new MoveToCell();
        moveToCell.setTargetDirection(operation.getTranslationCode());
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

    protected abstract void onArrived(CellTranslation direction, MapCell destination);

    protected abstract void onStep(CellTranslation direction, MapCell currentCell);

    protected abstract void onStuck(MapCell currentCell);

    protected abstract void onMoveError(String msg);

}
