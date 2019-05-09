package es.upm.woa.agent.group1;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import static es.upm.woa.agent.group1.AgUnit.WORLD;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.ontology.Group1Ontology;
import es.upm.woa.agent.group1.ontology.NotifyUnitOwnership;
import es.upm.woa.agent.group1.ontology.WhereAmI;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.agent.group1.protocol.DelayTickBehaviour;
import es.upm.woa.agent.group1.protocol.Group1CommunicationStandard;
import es.upm.woa.agent.group1.protocol.WoaCommunicationStandard;
import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.CreateBuilding;
import es.upm.woa.ontology.Empty;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.InitalizeTribe;
import es.upm.woa.ontology.NotifyCellDetail;
import es.upm.woa.ontology.NotifyNewUnit;
import es.upm.woa.ontology.NotifyUnitPosition;
import es.upm.woa.ontology.RegisterTribe;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 *
 * @author ISU
 */
public class AgTribe extends GroupAgent {
    public static final String REGISTRATION_DESK = "REGISTRATION_DESK";
    
    private int tribeNumber;
    private CommunicationStandard gameComStandard;
    private CommunicationStandard group1ComStandard;
    private Collection<Unit> units;
    private GraphGameMap knownMap;
    private WoaLogger logger;
    private DFAgentDescription registrationDeskServiceDescription;

    private SendMapDataSharingHelper mapDataSharingHelper;
    private DelayTickBehaviour delayedShareMapDataBehaviour;

    @Override
    protected void setup() {
        logger = new WoaLogger(getAID(), new ConsoleHandler());
        logger.setLevel(Level.ALL);
        
        initializeAgent();
        initializeTribe();
        
        startInformRegisteringBehaviour();
        startInformNewUnitBehaviour();
        startInformCellDetailBehaviour();
        startWhereAmIBehaviour();
        startInformUnitPositionBehaviour();
        startShareMapDataBehaviour();
    }

    private void startInformRegisteringBehaviour() {
        final Action registerTribe = new Action(this.getAID(), new RegisterTribe());

        addBehaviour(new Conversation(this, gameComStandard, registerTribe, GameOntology.REGISTERTRIBE) {
            @Override
            public void onStart() {
                AID registrationDeskAID = (AID) registrationDeskServiceDescription.getName();

                sendMessage(registrationDeskAID, ACLMessage.REQUEST, new Conversation.SentMessageHandler() {

                    @Override
                    public void onSent(String conversationID) {

                        log(Level.FINER, "sent RegisterTribe request");
                        receiveResponse(conversationID, new Conversation.ResponseHandler() {

                            @Override
                            public void onAgree(ACLMessage response) {
                                try {
                                    handleRegisterTribeMessage(response);
                                    log(Level.FINE, "receive RegisterTribe agree from "
                                        + response.getSender().getLocalName() + "and this tribe's number is " + tribeNumber);
                                } catch (Codec.CodecException | OntologyException ex) {
                                    log(Level.WARNING, "could not receive message"
                                            + " (" + ex + ")");
                                }
                            }

                            @Override
                            public void onNotUnderstood(ACLMessage response) {
                                log(Level.FINE, "receive RegisterTribe not understood from "
                                      + response.getSender().getLocalName());
                            }

                            @Override
                            public void onRefuse(ACLMessage response) {
                                log(Level.FINE, "receive RegisterTribe refuse from "
                                        + response.getSender().getLocalName());
                            }

                        });
                    }
                });

            }

        });
    }
    
    private void handleRegisterTribeMessage(ACLMessage response)
            throws OntologyException, Codec.CodecException {
        ContentElement ce = this.getContentManager().extractContent(response);
        if (ce instanceof Action) {

            Action agAction = (Action) ce;
            Concept conc = agAction.getAction();

            if (conc instanceof RegisterTribe) {
                RegisterTribe registerTribe = (RegisterTribe) conc;
                tribeNumber = registerTribe.getTeamNumber();              
            }
        }
    }
    
    private void startInformCellDetailBehaviour() {
        new ReceiveInformCellDetailBehaviourHelper(this, gameComStandard
                , knownMap).startInformCellDetailBehaviour();
    }

    private void startShareMapDataBehaviour() {
        new ReceiveShareMapDataBehaviourHelper(this,
                group1ComStandard, knownMap, () -> {
                    log(Level.FINE, "Updated known map");
                    shareMapDataWithUnits();
                    
                }).startShareMapDataBehaviour();
    }

    private void startInformUnitPositionBehaviour() {
        new ReceiveInformUnitPositionBehaviourHelper(this, gameComStandard
                , knownMap).startInformCellDetailBehaviour();
    }
    
    
    private void startInformNewUnitBehaviour() {
        Action informNewUnitAction = new Action(getAID(), new NotifyNewUnit());
        addBehaviour(new Conversation(this, gameComStandard, informNewUnitAction, GameOntology.NOTIFYNEWUNIT) {
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
        addBehaviour(new Conversation(this, group1ComStandard, whereAmIAction, Group1Ontology.WHEREAMI) {
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
        //Finds the Registration Desk in the DF
        try {
            DFAgentDescription dfdRegistrationDesk = new DFAgentDescription();
            ServiceDescription sdRegistrationDesk = new ServiceDescription();
            sdRegistrationDesk.setType(REGISTRATION_DESK);
            dfdRegistrationDesk.addServices(sdRegistrationDesk);
            // It finds agents of the required type
            DFAgentDescription[] descriptions = DFService.search(this, dfdRegistrationDesk);
            if (descriptions.length == 0) {
                log(Level.SEVERE, "Registration Desk service description not found");
            } else {
                registrationDeskServiceDescription = descriptions[0];
            }
        } catch (FIPAException ex) {
            log(Level.WARNING, " the REGISTRATION_DESK agent was not found (" + ex + ")");
        }
        
    }

    private void initializeTribe() {
        gameComStandard = new WoaCommunicationStandard();
        gameComStandard.register(getContentManager());
        
        group1ComStandard = new Group1CommunicationStandard();
        group1ComStandard.register(getContentManager());

        units = new HashSet<>();
        knownMap = GraphGameMap.getInstance();
        mapDataSharingHelper = new SendMapDataSharingHelper(this, group1ComStandard, knownMap);
    }
        
    private void informNewUnitOwnership(Unit unit) {
        Action informOwnershipAction = new Action(getAID(), new NotifyUnitOwnership());
        addBehaviour(new Conversation(this, group1ComStandard, informOwnershipAction
                , Group1Ontology.NOTIFYUNITOWNERSHIP) {
            @Override
            public void onStart() {
                sendMessage(unit.getId(), ACLMessage.INFORM
                        , new Conversation.SentMessageHandler() {
                    @Override
                    public void onSent(String conversationID) {
                        log(Level.FINE, "Informed unit " + unit.getId().getLocalName() + " of ownership");
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
        
        Action informCellDiscoveryAction = new Action(getAID(), newCellDiscovery);
        addBehaviour(new Conversation(this, gameComStandard, informCellDiscoveryAction
            , GameOntology.NOTIFYCELLDETAIL) {
                
                @Override
                public void onStart() {
                    
                    sendMessage(newUnit.getId(), ACLMessage.INFORM, new SentMessageHandler() {
                        @Override
                        public void onSent(String conversationID) {
                            log(Level.FINER, "Informed unit "
                                    + newUnit.getId().getLocalName()
                                    + " of starting position");
                        }
                        
                    });
                    
                }
                
            });
    }
    
    @Override
    public void log(Level logLevel, String message) {
        logger.log(logLevel, message);
    }

    @Override
    void onCellDiscovered(MapCell newCell) {
        try {
            MapCell knownCell = knownMap
                    .getCellAt(newCell.getXCoord(), newCell.getYCoord());
            if (knownCell.getContent() instanceof Empty && !(newCell.getContent() instanceof Empty)) {
                knownCell.setContent(newCell.getContent());
            }
            log(Level.FINER, "Cell updated at " + newCell);
        } catch (NoSuchElementException ex) {
            //log(Level.FINER, "Cell discovery at " + newCell);
            // We do nothing. Other units will send us the information
        }
    }

    @Override
    void onUnitPassby(MapCell cell, String tribeId) {
        log(Level.FINER, "Unit from tribe " + tribeId + " at "
                                + cell);
    }

    private AID[] getMyUnitsAIDs() {
        return units.stream().map(unit -> unit.getId())
                .collect(Collectors.toList()).toArray(new AID[units.size()]);
    }

    private synchronized void shareMapDataWithUnits() {
        if (delayedShareMapDataBehaviour == null) {
            delayedShareMapDataBehaviour = new DelayTickBehaviour(this, 100) {
                
                @Override
                protected void handleElapsedTimeout() {
                    mapDataSharingHelper.multicastMapData(getMyUnitsAIDs());
                    delayedShareMapDataBehaviour = null;
                }
                
            };
            addBehaviour(delayedShareMapDataBehaviour);
        }
    }


}
