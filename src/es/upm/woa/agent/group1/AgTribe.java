package es.upm.woa.agent.group1;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import es.upm.woa.agent.group1.map.GameMap;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.ontology.Group1Ontology;
import es.upm.woa.agent.group1.ontology.NotifyUnitOwnership;
import es.upm.woa.agent.group1.ontology.WhereAmI;
import es.upm.woa.agent.group1.protocol.Conversation;

import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.NotifyCellDetail;
import es.upm.woa.ontology.NotifyNewUnit;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

import java.util.Collection;
import java.util.HashSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 *
 * @author ISU
 */
public class AgTribe extends GroupAgent {
    
    private static final int TRIBE_KNOWN_MAP_SIZE = 4;
    
    private Ontology gameOntology;
    private Ontology group1Ontology;
    private Codec codec;
    private Collection<Unit> units;
    private GameMap knownMap;
    private Handler logHandler;

    @Override
    protected void setup() {       
        logHandler = new ConsoleHandler();
        logHandler.setLevel(Level.ALL);
        
        initializeAgent();
        initializeTribe();
        
        startInformNewUnitBehaviour();
        new ReceiveInformCellDetailBehaviourHelper(this, gameOntology
                , codec, knownMap).startInformCellDetailBehaviour();
        startWhereAmIBehaviour();
        new ReceiveInformUnitPositionBehaviourHelper(this, gameOntology, codec, knownMap).startInformCellDetailBehaviour();
    }
    
    
    private void startInformNewUnitBehaviour() {
        Action informNewUnitAction = new Action(getAID(), new NotifyNewUnit());
        addBehaviour(new Conversation(this, gameOntology, codec, informNewUnitAction, GameOntology.NOTIFYNEWUNIT) {
            @Override
            public void onStart() {
                listenMessages(new ResponseHandler() {
                    @Override
                    public void onInform(ACLMessage response) {
                        try {
                            ContentElement ce = getContentManager().extractContent(response);
                            if (ce instanceof Action) {
                                
                                Action agAction = (Action) ce;
                                Concept conc = agAction.getAction();
                                
                                if (conc instanceof NotifyNewUnit) {
                                    log(Level.FINER, "receive NotifyNewUnit inform from "
                                        + response.getSender().getLocalName());
                                    NotifyNewUnit newUnitInfo = (NotifyNewUnit) conc;
                                    log(Level.FINE, "new unit created at " + 
                                            + newUnitInfo.getLocation().getX()
                                            + ", "
                                            + newUnitInfo.getLocation().getY());
                                    registerNewUnit(newUnitInfo);
                                }
                            }
                        } catch (Codec.CodecException | OntologyException ex) {
                            log(Level.WARNING, "could not receive message"
                                    + " (" + ex + ")");
                        }

                    }

                });
            }
        });
    }
    
    private void registerNewUnit(NotifyNewUnit newUnitInfo) {
        Unit newUnit = new Unit(newUnitInfo.getNewUnit(), newUnitInfo.getLocation().getX(), newUnitInfo.getLocation().getY());
        units.add(newUnit);
        informNewUnitOwnership(newUnit);
        informNewUnitOfKnownMap(newUnit);
    }
    
    private void startWhereAmIBehaviour() {
        final Action whereAmIAction = new Action(getAID(), new WhereAmI());
        addBehaviour(new Conversation(this, group1Ontology, codec, whereAmIAction, Group1Ontology.WHEREAMI) {
            @Override
            public void onStart() {
                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onRequest(ACLMessage response) {
                        log(Level.FINER, "received WhereAmI request from "
                                        + response.getSender().getLocalName());
                        
                        
                        AID senderAid = response.getSender();
                        Unit requesterUnit = units.stream().filter(u -> u.getId()
                                .equals(senderAid)).findAny().orElse(null);
                        if (requesterUnit == null) {
                            respondMessage(response, ACLMessage.REFUSE);
                        }
                        else {
                            WhereAmI whereAmI = new WhereAmI();
                            whereAmI.setXCoord(requesterUnit.getCoordX());
                            whereAmI.setYCoord(requesterUnit.getCoordY());
                            
                            whereAmIAction.setAction(whereAmI);
                            
                            respondMessage(response, ACLMessage.INFORM);
                        } 

                    }

                });
            }
        });
    }

    private void initializeAgent() {
        
    }

    private void initializeTribe() {
        gameOntology = GameOntology.getInstance();
        group1Ontology = Group1Ontology.getInstance();
        codec = new SLCodec();
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(gameOntology);
        getContentManager().registerOntology(group1Ontology);

        units = new HashSet<>();
        knownMap = GraphGameMap.getInstance(TRIBE_KNOWN_MAP_SIZE
                , TRIBE_KNOWN_MAP_SIZE);
    }
        
    private void informNewUnitOwnership(Unit unit) {
        Action informOwnershipAction = new Action(getAID(), new NotifyUnitOwnership());
        addBehaviour(new Conversation(this, group1Ontology, codec, informOwnershipAction
                , Group1Ontology.NOTIFYUNITOWNERSHIP) {
            @Override
            public void onStart() {
                sendMessage(unit.getId(), ACLMessage.INFORM
                        , new Conversation.SentMessageHandler() {
                    @Override
                    public void onSent(String conversationID) {
                        log(Level.FINE, "Informed unit " + unit.getId().getLocalName() + " of ownership");
                    }
                    
                });
            }
        });
    }

    private void informNewUnitOfKnownMap(Unit newUnit) {
        for (MapCell knownCell : knownMap.getKnownCellsIterable()) {
            if (knownCell.getXCoord() != newUnit.getCoordX()
                    || knownCell.getYCoord() != newUnit.getCoordY()) {
                informOfKnownMapCell(newUnit, knownCell);
            }
        }
    }
    
    private void informOfKnownMapCell(Unit newUnit, MapCell cell) {
        Cell knownCell = new Cell();
        knownCell.setX(cell.getXCoord());
        knownCell.setY(cell.getYCoord());
        knownCell.setContent(cell.getContent());
        
        NotifyCellDetail newCellDiscovery = new NotifyCellDetail();
        newCellDiscovery.setNewCell(knownCell);
        
        Action informCellDiscoveryAction = new Action(getAID(), newCellDiscovery);
        addBehaviour(new Conversation(this, gameOntology, codec, informCellDiscoveryAction
            , GameOntology.NOTIFYCELLDETAIL) {
                
                @Override
                public void onStart() {
                    
                    sendMessage(newUnit.getId(), ACLMessage.INFORM, new SentMessageHandler() {
                        @Override
                        public void onSent(String conversationID) {
                            log(Level.FINER, "Informed unit "
                                    + newUnit.getId().getLocalName()
                                    + " of location " + cell.getXCoord()
                                    + "," + cell.getYCoord());
                        }
                        
                    });
                    
                }
                
            });
    }
    
    @Override
    void log(Level logLevel, String message) {
        String compMsg = getLocalName() + ": " + message;
        if (logHandler.isLoggable(new LogRecord(logLevel, compMsg))) {
            System.out.println(compMsg);
        }
    }

    @Override
    void onCellDiscovered(MapCell newCell) {
        log(Level.FINER, "Cell discovery at "
                                + newCell.getXCoord()
                                + ","
                                + newCell.getYCoord());
    }

    @Override
    void onCellUpdated(MapCell updatedCell) {
        log(Level.FINER, "Cell updated at "
                                + updatedCell.getXCoord()
                                + ","
                                + updatedCell.getYCoord());
    }

    @Override
    void onUnitPassby(MapCell cell, String tribeId) {
        log(Level.FINER, "Unit from tribe " + tribeId + " at "
                                + cell.getXCoord()
                                + ","
                                + cell.getYCoord());
    }


}
