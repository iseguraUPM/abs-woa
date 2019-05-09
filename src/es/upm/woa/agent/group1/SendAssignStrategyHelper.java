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

import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

import java.util.logging.Level;

/**
 *
 * @author ISU
 */
class SendAssignStrategyHelper {
    
    private final WoaAgent groupAgent;
    private final CommunicationStandard comStandard;
    
    public SendAssignStrategyHelper(WoaAgent groupAgent
            , CommunicationStandard comStandard) {
        this.groupAgent = groupAgent;
        this.comStandard = comStandard;
    }
    
    public void multicastStrategy(AID[] receipts, String strategy) {
        AssignStrategy assignStrategyAction = new AssignStrategy();
        assignStrategyAction.setStrategy(strategy);
        
        Action action = new Action(groupAgent.getAID(), assignStrategyAction);
        groupAgent.addBehaviour(new Conversation(groupAgent, comStandard, action
                , Group1Ontology.ASSIGNSTRATEGY ) {
            @Override
            public void onStart() {
                sendMessage(receipts, ACLMessage.INFORM, new SentMessageHandler() {
                    
                    @Override
                    public void onSent(String conversationID) {
                        groupAgent.log(Level.FINER, "Assigned "  + strategy + " to "+ printReceipts(receipts));
                    }
                    
                    @Override
                    public void onSentMessageError() {
                        groupAgent.log(Level.FINER, "Error sending strategy");
                    }
                    
                });
            }
        });
    }
    
    public void unicastStrategy(AID receipt, String strategy) {
        multicastStrategy(new AID[]{receipt}, strategy);
    }
    
    private String printReceipts(AID[] receipts) {
        StringBuilder stringBuilder = new StringBuilder();
        for (AID receipt : receipts) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append(receipt.getLocalName());
        }
        
        return stringBuilder.toString();
    }
    
}
