/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.CellTranslation;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.ontology.Group1Ontology;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.protocol.Conversation;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

import java.io.Serializable;
import java.util.NoSuchElementException;
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
            if (content instanceof NewGraphConnection) {
                groupAgent.log(Level.FINER, "Received new map connection data");
                learnNewGraphMapData((NewGraphConnection) content);
            }
            else if (content instanceof GraphGameMap) {
                groupAgent.log(Level.FINER, "Received starting map");
                knownMap.copyMapData((GraphGameMap) content);
            }
            else {
                groupAgent.log(Level.WARNING, "Could not retrieve map data");
            }
        } catch (UnreadableException ex) {
            groupAgent.log(Level.WARNING, "Could not retrieve map data (" + ex + ")");
        }
    }
    
    private void learnNewGraphMapData(NewGraphConnection newConnection) {
        MapCell mySource;
        try {
            mySource = knownMap.getCellAt(newConnection.source.getXCoord()
                    , newConnection.source.getYCoord());
        } catch (NoSuchElementException ex) {
            mySource = newConnection.source;
            knownMap.addCell(mySource);
        }
        
        MapCell myTarget;
        try {
            myTarget = knownMap.getCellAt(newConnection.target.getXCoord()
                    , newConnection.target.getYCoord());
        } catch (NoSuchElementException ex) {
            myTarget = newConnection.target;
            knownMap.addCell(myTarget);
        }
        
        knownMap.connectPath(mySource, myTarget, newConnection.direction);
        CellTranslation inverse = newConnection.direction.generateInverse();
        knownMap.connectPath(myTarget, mySource, inverse);
        
        updatedMapHandler.onMapUpdated(newConnection);
    }
    
    interface OnUpdatedMapHandler {
    
        void onMapUpdated(NewGraphConnection newConnection);
    
    }
    
}
