/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.agent.strategy.FeedbackMessageEnvelop;
import es.upm.woa.group1.agent.strategy.FeedbackMessageFactory;
import es.upm.woa.group1.agent.strategy.UnitStatusHanlder;
import es.upm.woa.group1.ontology.Group1Ontology;
import es.upm.woa.group1.protocol.CommunicationStandard;
import es.upm.woa.group1.protocol.Conversation;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

import java.io.Serializable;
import java.util.logging.Level;

/**
 *
 * @author ISU
 */
class ReceiveFeedbackUnitStatusHelper {

    private final CommunicationStandard comStandard;
    private final WoaAgent woaAgent;
    private final UnitStatusHanlder feedbackMessageHandler;

    public ReceiveFeedbackUnitStatusHelper(WoaAgent agent
            , CommunicationStandard comStandard
            , UnitStatusHanlder unitStatusHanlder) {
        this.comStandard = comStandard;
        this.woaAgent = agent;
        this.feedbackMessageHandler = unitStatusHanlder;
    }
    
    public void startFeedbackUnitBehaviour() {

        woaAgent.addBehaviour(new Conversation(woaAgent, comStandard
                , Group1Ontology.FEEDBACKUNITSTATUS) {
            @Override
            public void onStart() {
                listenMessages(new ResponseHandler() {
                    @Override
                    public void onInform(ACLMessage response) {
                        handleFeedbackMessage(response);
                    }

                    @Override
                    public void onNotUnderstood(ACLMessage response) {
                        woaAgent.log(Level.WARNING,
                                 "Error while receiving AssignStrategy message");
                    }

                });
            }
        });

    }
    
    private void handleFeedbackMessage(ACLMessage response) {
        try {
            Serializable content = response.getContentObject();
            if (content instanceof FeedbackMessageEnvelop) {
                FeedbackMessageEnvelop feedEnvelop = (FeedbackMessageEnvelop) content;
                FeedbackMessageFactory.handleMessage(feedEnvelop, feedbackMessageHandler);
            } else {
                woaAgent.log(Level.WARNING, "Could not retrieve strategy");
            }
        } catch (UnreadableException ex) {
            woaAgent.log(Level.WARNING, "Could not retrieve strategy (" + ex + ")");
        }
    }

}
