/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.agent.strategy.StrategyEnvelop;
import es.upm.woa.group1.ontology.Group1Ontology;
import es.upm.woa.group1.protocol.CommunicationStandard;
import es.upm.woa.group1.protocol.Conversation;

import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

import java.io.Serializable;
import java.util.logging.Level;

/**
 *
 * @author ISU
 */
class ReceiveAssignStrategyBehaviourHelper {
    
    private final WoaAgent woaAgent;
    private final CommunicationStandard comStandard;
    
    private final OnReceivedStrategyHandler receivedStrategyHandler;
    
    public ReceiveAssignStrategyBehaviourHelper(WoaAgent woaAgent
            , CommunicationStandard comStandard
            , OnReceivedStrategyHandler receivedStrategyHandler) {
        this.woaAgent = woaAgent;
        this.comStandard = comStandard;
        this.receivedStrategyHandler = receivedStrategyHandler;
    }
    
    /**
     * Start listening behaviour for ShareMapData agent inform.
     */
    public void startAssignStrategyBehaviour() {
        woaAgent.addBehaviour(new Conversation(woaAgent, comStandard
                , Group1Ontology.ASSIGNSTRATEGY) {
            @Override
            public void onStart() {
                woaAgent.log(Level.INFO, "listening to AssignStrategy messages");
                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onInform(ACLMessage response) {
                        try {
                            handleStrategyData(response);
                        } catch (OntologyException | Codec.CodecException ex) {
                            woaAgent.log(Level.WARNING
                                , "Error while receiving AssignStrategy message");
                        }
                    }
                    
                    @Override
                    public void onNotUnderstood(ACLMessage response) {
                        woaAgent.log(Level.WARNING
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
                woaAgent.log(Level.WARNING, "Could not retrieve strategy");
            }
        } catch (UnreadableException ex) {
            woaAgent.log(Level.WARNING, "Could not retrieve strategy (" + ex + ")");
        }
    }
    
    interface OnReceivedStrategyHandler {
    
        void onReceivedStrategy(StrategyEnvelop strategyEnvelop);
    
    }
    
}
