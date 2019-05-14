/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.map.CellTranslation;
import es.upm.woa.group1.map.GameMap;
import es.upm.woa.group1.map.MapCell;
import es.upm.woa.group1.ontology.Group1Ontology;
import es.upm.woa.group1.protocol.CommunicationStandard;
import es.upm.woa.group1.protocol.Conversation;

import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.io.Serializable;

import java.util.logging.Level;

/**
 *
 * @author ISU
 */
class SendMapDataSharingHelper {
    
    private final WoaAgent groupAgent;
    private final CommunicationStandard comStandard;
    
    public SendMapDataSharingHelper(WoaAgent groupAgent
            , CommunicationStandard comStandard) {
        this.groupAgent = groupAgent;
        this.comStandard = comStandard;
    }
    
    public void multicastMapData(AID[] receipts, Serializable mapData) {
        groupAgent.addBehaviour(new Conversation(groupAgent, comStandard
                , Group1Ontology.SHAREMAPDATA) {
            @Override
            public void onStart() {
                sendMessage(receipts, mapData, ACLMessage.INFORM, new SentMessageHandler() {
                    
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
    
    public void unicastMapData(AID receipt, Serializable mapData) {
        multicastMapData(new AID[]{receipt}, mapData);
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
