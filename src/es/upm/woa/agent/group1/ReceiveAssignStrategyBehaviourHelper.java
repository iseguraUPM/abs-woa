/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.ontology.Group1Ontology;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.ontology.NotifyCellDetail;

import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

import java.io.Serializable;
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
        groupAgent.addBehaviour(new Conversation(groupAgent, comStandard
                , Group1Ontology.ASSIGNSTRATEGY) {
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
        try {
            Serializable content = response.getContentObject();
            if (content instanceof StrategyEnvelop) {
                receivedStrategyHandler
                        .onReceivedStrategy((StrategyEnvelop) content);
            }
            else {
                groupAgent.log(Level.WARNING, "Could not retrieve strategy");
            }
        } catch (UnreadableException ex) {
            groupAgent.log(Level.WARNING, "Could not retrieve strategy (" + ex + ")");
        }
    }
    
    interface OnReceivedStrategyHandler {
    
        void onReceivedStrategy(StrategyEnvelop strategyEnvelop);
    
    }
    
}
