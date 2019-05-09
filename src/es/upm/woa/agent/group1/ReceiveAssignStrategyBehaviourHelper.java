/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.ontology.AssignStrategy;
import es.upm.woa.agent.group1.ontology.Group1Ontology;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.protocol.Conversation;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.lang.acl.ACLMessage;

import java.util.logging.Level;

/**
 *
 * @author ISU
 */
class ReceiveAssignStrategyBehaviourHelper {
    
    private final GroupAgent groupAgent;
    private final CommunicationStandard comStandard;
    
    private final OnReceivedStrategyHandler receivedStrategyHandler;
    
    public ReceiveAssignStrategyBehaviourHelper(GroupAgent groupAgent
            , CommunicationStandard comStandard
            , OnReceivedStrategyHandler receivedStrategyHandler) {
        this.groupAgent = groupAgent;
        this.comStandard = comStandard;
        this.receivedStrategyHandler = receivedStrategyHandler;
    }
    
    /**
     * Start listening behaviour for ShareMapData agent inform.
     */
    public void startAssignStrategyBehaviour() {
        Action informCellDetailAction = new Action(groupAgent.getAID(), null);
        groupAgent.addBehaviour(new Conversation(groupAgent, comStandard
                , informCellDetailAction, Group1Ontology.ASSIGNSTRATEGY) {
            @Override
            public void onStart() {
                groupAgent.log(Level.INFO, "listening to AssignStrategy messages");
                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onInform(ACLMessage response) {
                        try {
                            handleStrategyData(response);
                        } catch (OntologyException | Codec.CodecException ex) {
                            groupAgent.log(Level.WARNING
                                , "Error while receiving AssignStrategy message");
                        }
                    }
                    
                    @Override
                    public void onNotUnderstood(ACLMessage response) {
                        groupAgent.log(Level.WARNING
                                , "Error while receiving AssignStrategy message");
                    }

                });
            }
        });
    }

    private void handleStrategyData(ACLMessage response)
            throws OntologyException, Codec.CodecException {
        ContentElement ce = groupAgent.getContentManager().extractContent(response);
        if (ce instanceof Action) {

            Action agAction = (Action) ce;
            Concept conc = agAction.getAction();

            if (conc instanceof AssignStrategy) {
                groupAgent.log(Level.FINER, "receive AssignStrategy inform from "
                        + response.getSender().getLocalName());

                AssignStrategy assignStrategy = (AssignStrategy) conc;

                
                receivedStrategyHandler
                        .onReceivedStrategy(assignStrategy.getStrategy());
            }
        }
    }
    
    interface OnReceivedStrategyHandler {
    
        void onReceivedStrategy(String strategy);
    
    }
    
}
