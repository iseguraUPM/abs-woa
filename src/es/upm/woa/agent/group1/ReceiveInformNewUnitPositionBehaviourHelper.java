/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.GameMap;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.map.MapCellFactory;
import es.upm.woa.agent.group1.ontology.Group1Ontology;
import es.upm.woa.agent.group1.ontology.NotifyUnitOwnership;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.NotifyCellDetail;
import es.upm.woa.ontology.NotifyNewUnit;
import es.upm.woa.ontology.NotifyUnitPosition;

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
class ReceiveInformNewUnitPositionBehaviourHelper {
    
    private final GroupAgent groupAgent;
    private final CommunicationStandard comStandard;
    private final CommunicationStandard group1ComStandard;
    private final GameMap knownMap;
    private final Collection<Unit> tribeUnits;
    private final SendMapDataSharingHelper mapDataSharingHelper;
    
    public ReceiveInformNewUnitPositionBehaviourHelper(GroupAgent groupAgent
            , CommunicationStandard comStandard
            , CommunicationStandard group1ComStandard, GameMap knownGameMap
            , Collection<Unit> tribeUnits, SendMapDataSharingHelper mapDataSharingHelper) {
        this.groupAgent = groupAgent;
        this.comStandard = comStandard;
        this.group1ComStandard = group1ComStandard;
        this.knownMap = knownGameMap;
        this.tribeUnits = tribeUnits;
        this.mapDataSharingHelper = mapDataSharingHelper;
    }
    
    /**
     * Start listening behaviour for NotifyUnitPosition agent inform.
     */
    public void startInformCellDetailBehaviour() {
        Action informNewUnitAction = new Action(groupAgent.getAID(), new NotifyNewUnit());
        groupAgent.addBehaviour(new Conversation(groupAgent, comStandard
                , informNewUnitAction, GameOntology.NOTIFYNEWUNIT) {
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
                                    MapCell knownCell;
                                    try {
                                        knownMap.getCellAt(startingPosition.getX()
                                               , startingPosition.getY());
                                    } catch (NoSuchElementException ex) {
                                        knownCell = MapCellFactory
                                                .getInstance().buildCell(startingPosition);
                                        knownMap.addCell(knownCell);
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
        Unit newUnit = new Unit(newUnitInfo.getNewUnit(), newUnitInfo.getLocation().getX(), newUnitInfo.getLocation().getY());
        tribeUnits.add(newUnit);
        informNewUnitOwnership(newUnit);
        informNewUnitOfKnownMap(newUnit);
    }
    
    private void informNewUnitOwnership(Unit unit) {
        Action informOwnershipAction = new Action(groupAgent.getAID(), new NotifyUnitOwnership());
        groupAgent.addBehaviour(new Conversation(groupAgent, group1ComStandard, informOwnershipAction
                , Group1Ontology.NOTIFYUNITOWNERSHIP) {
            @Override
            public void onStart() {
                sendMessage(unit.getId(), ACLMessage.INFORM
                        , new Conversation.SentMessageHandler() {
                    @Override
                    public void onSent(String conversationID) {
                        groupAgent.log(Level.FINE, "Informed unit " + unit.getId().getLocalName() + " of ownership");
                        informOfStartingPosition(unit);
                    }
                    
                });
            }
        });
    }

    private void informNewUnitOfKnownMap(Unit newUnit) {
        mapDataSharingHelper.unicastMapData(newUnit.getId());
    }
    
    private void informOfStartingPosition(Unit newUnit) {
        Cell knownCell = new Cell();
        knownCell.setX(newUnit.getCoordX());
        knownCell.setY(newUnit.getCoordY());
        
        NotifyCellDetail newCellDiscovery = new NotifyCellDetail();
        newCellDiscovery.setNewCell(knownCell);
        
        Action informCellDiscoveryAction = new Action(groupAgent.getAID(), newCellDiscovery);
        groupAgent.addBehaviour(new Conversation(groupAgent, comStandard, informCellDiscoveryAction
            , GameOntology.NOTIFYCELLDETAIL) {
                
                @Override
                public void onStart() {
                    
                    sendMessage(newUnit.getId(), ACLMessage.INFORM, new Conversation.SentMessageHandler() {
                        @Override
                        public void onSent(String conversationID) {
                            groupAgent.log(Level.FINER, "Informed unit "
                                    + newUnit.getId().getLocalName()
                                    + " of starting position");
                        }
                        
                    });
                    
                }
                
            });
    }
    
}
