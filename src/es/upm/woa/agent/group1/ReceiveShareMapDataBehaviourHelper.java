/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.ontology.Group1Ontology;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.protocol.Conversation;

import jade.content.onto.basic.Action;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

import java.io.Serializable;
import java.util.logging.Level;

/**
 *
 * @author ISU
 */
class ReceiveShareMapDataBehaviourHelper {
    
    private final GroupAgent groupAgent;
    private final CommunicationStandard comStandard;
    private final GraphGameMap knownMap;
    
    private final OnUpdatedMapHandler updatedMapHandler;
    
    public ReceiveShareMapDataBehaviourHelper(GroupAgent groupAgent
            , CommunicationStandard comStandard, GraphGameMap knownMap
            , OnUpdatedMapHandler updatedMapHandler) {
        this.groupAgent = groupAgent;
        this.comStandard = comStandard;
        this.knownMap = knownMap;
        this.updatedMapHandler = updatedMapHandler;
    }
    
    /**
     * Start listening behaviour for ShareMapData agent inform.
     */
    public void startShareMapDataBehaviour() {
        groupAgent.addBehaviour(new Conversation(groupAgent, comStandard
                , Group1Ontology.SHAREMAPDATA) {
            @Override
            public void onStart() {
                groupAgent.log(Level.INFO, "listening to ShareMapData messages");
                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onInform(ACLMessage response) {
                        handleShareMapDataInform(response);
                    }
                    
                    @Override
                    public void onNotUnderstood(ACLMessage response) {
                        groupAgent.log(Level.WARNING
                                , "Error while receiving ShareMapData message");
                    }

                });
            }
        });
    }

    private void handleShareMapDataInform(ACLMessage response) {
        try {
            Serializable content = response.getContentObject();
            if (content instanceof GraphGameMap) {
                groupAgent.log(Level.FINER, "Received shared map data");
                learnNewGraphMapData((GraphGameMap) content);
            }
            else {
                groupAgent.log(Level.WARNING, "Could not retrieve map data");
            }
        } catch (UnreadableException ex) {
            groupAgent.log(Level.WARNING, "Could not retrieve map data (" + ex + ")");
        }
    }
    
    private void learnNewGraphMapData(GraphGameMap otherGameMap) {
        knownMap.mergeMapData(otherGameMap);
        updatedMapHandler.onMapUpdated();
    }
    
    interface OnUpdatedMapHandler {
    
        void onMapUpdated();
    
    }
    
}
