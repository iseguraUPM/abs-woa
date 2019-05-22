/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.map.GameMap;
import es.upm.woa.group1.protocol.CommunicationStandard;
import es.upm.woa.group1.protocol.Conversation;
import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.NotifyNewUnit;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.lang.acl.ACLMessage;
import java.util.Collection;

import java.util.NoSuchElementException;
import java.util.logging.Level;

/**
 *
 * @author ISU
 */
class ReceiveInformNewUnitBehaviourHelper {
    
    private final GroupAgent groupAgent;
    private final CommunicationStandard comStandard;
    private final GameMap knownMap;
    private final Collection<Unit> tribeUnits;
    
    private final OnUnitRegisteredHandler unitRegisteredHandler;
    
    public ReceiveInformNewUnitBehaviourHelper(GroupAgent groupAgent
            , CommunicationStandard comStandard
            , GameMap knownGameMap
            , Collection<Unit> tribeUnits
            , OnUnitRegisteredHandler handler) {
        this.groupAgent = groupAgent;
        this.comStandard = comStandard;
        this.knownMap = knownGameMap;
        this.tribeUnits = tribeUnits;
        
        this.unitRegisteredHandler = handler;
    }
    
    /**
     * Start listening behaviour for NotifyUnitPosition agent inform.
     */
    public void startInformNewUnitBehaviour() {
        groupAgent.addBehaviour(new Conversation(groupAgent, comStandard
                , GameOntology.NOTIFYNEWUNIT) {
            @Override
            public void onStart() {
                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onInform(ACLMessage response) {
                        try {
                            ContentElement ce = groupAgent.getContentManager()
                                    .extractContent(response);
                            if (ce instanceof Action) {
                                
                                Action agAction = (Action) ce;
                                Concept conc = agAction.getAction();
                                
                                if (conc instanceof NotifyNewUnit) {
                                    groupAgent.log(Level.FINER, "receive NotifyNewUnit inform from "
                                        + response.getSender().getLocalName());
                                    NotifyNewUnit newUnitInfo = (NotifyNewUnit) conc;
                                    groupAgent.log(Level.FINE, "new unit created at " + 
                                            + newUnitInfo.getLocation().getX()
                                            + ", "
                                            + newUnitInfo.getLocation().getY());
                                    Cell startingPosition = newUnitInfo
                                            .getLocation();
                                    // TODO: this cell should be indicated on
                                    // game start with the rest of resources
                                    try {
                                        knownMap.getCellAt(startingPosition.getX()
                                               , startingPosition.getY());
                                    } catch (NoSuchElementException ex) {
                                        groupAgent.log(Level.WARNING
                                                , "Unknown unit starting position. Unit position desync");
                                    }
                                    
                                    registerNewUnit(newUnitInfo);
                                }
                            }
                        } catch (Codec.CodecException | OntologyException ex) {
                            groupAgent.log(Level.WARNING, "could not receive message"
                                    + " (" + ex + ")");
                        }

                    }

                });
            }
        });
    }
        
    private void registerNewUnit(NotifyNewUnit newUnitInfo) {
        Unit newUnit = new TribeUnit(newUnitInfo.getNewUnit(), newUnitInfo.getLocation().getX(), newUnitInfo.getLocation().getY());
        tribeUnits.add(newUnit);
        unitRegisteredHandler.onUnitRegistered(newUnit);
    }
    
    public interface OnUnitRegisteredHandler {
        
        void onUnitRegistered(Unit newUnit);
        
    }
    
}
