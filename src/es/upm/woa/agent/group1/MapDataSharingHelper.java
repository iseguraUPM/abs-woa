/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.GameMap;
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
class MapDataSharingHelper {
    
    private final WoaAgent groupAgent;
    private final CommunicationStandard comStandard;
    private final GameMap knownMap;
    
    public MapDataSharingHelper(WoaAgent groupAgent
            , CommunicationStandard comStandard, GameMap knownMap) {
        this.groupAgent = groupAgent;
        this.comStandard = comStandard;
        this.knownMap = knownMap;
    }
    
    public void multicastMapData(AID[] receipts) {
        Action action = new Action(groupAgent.getAID(), null);
        groupAgent.addBehaviour(new Conversation(groupAgent, comStandard, action
                , Group1Ontology.SHAREMAPDATA) {
            @Override
            public void onStart() {
                sendMessage(receipts, ACLMessage.INFORM, knownMap, new SentMessageHandler() {
                    
                    @Override
                    public void onSent(String conversationID) {
                        groupAgent.log(Level.FINER, "Shared map data with " + printReceipts(receipts));
                    }
                    
                    @Override
                    public void onSentMessageError() {
                        groupAgent.log(Level.FINER, "Error sharing map data");
                    }
                    
                });
            }
        });
    }
    
    public void unicastMapData(AID receipt) {
        multicastMapData(new AID[]{receipt});
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
