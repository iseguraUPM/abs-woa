/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.agent.strategy.FeedbackMessageEnvelop;
import es.upm.woa.group1.ontology.Group1Ontology;
import es.upm.woa.group1.protocol.CommunicationStandard;
import es.upm.woa.group1.protocol.Conversation;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.logging.Level;

/**
 *
 * @author ISU
 */
class SendFeedbackUnitStatusHelper {

    private final WoaAgent woaAgent;
    private final CommunicationStandard comStandard;
    private final AID tribeAid;

    public SendFeedbackUnitStatusHelper(WoaAgent woaAgent,
             CommunicationStandard comStandard, AID tribeAid) {
        this.woaAgent = woaAgent;
        this.comStandard = comStandard;
        this.tribeAid = tribeAid;
    }

    public void sendStatus(FeedbackMessageEnvelop feedbackMessageEnvelop) {
        woaAgent.addBehaviour(new Conversation(woaAgent, comStandard,
                 Group1Ontology.FEEDBACKUNITSTATUS) {
            @Override
            public void onStart() {
                sendMessage(tribeAid, feedbackMessageEnvelop, ACLMessage.INFORM,
                         new SentMessageHandler() {
                    @Override
                    public void onSent(String conversationID) {
                        woaAgent.log(Level.FINE, "Feedback unit"
                                + " status to "
                                + tribeAid.getLocalName());
                    }

                    @Override
                    public void onSentMessageError() {
                        woaAgent.log(Level.WARNING, "Error sending unit status");
                    }

                });
            }
        });
    }

}
