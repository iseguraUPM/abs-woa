package es.upm.woa.group1.agent;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import es.upm.woa.group1.WoaLogger;
import es.upm.woa.group1.agent.strategy.Strategy;
import es.upm.woa.group1.map.MapCell;
import es.upm.woa.group1.map.MapCellFactory;
import es.upm.woa.group1.ontology.Group1Ontology;
import es.upm.woa.group1.ontology.NotifyUnitOwnership;
import es.upm.woa.group1.ontology.WhereAmI;
import es.upm.woa.group1.protocol.CommunicationStandard;
import es.upm.woa.group1.protocol.Conversation;
import es.upm.woa.group1.protocol.Group1CommunicationStandard;
import es.upm.woa.group1.protocol.WoaCommunicationStandard;

import es.upm.woa.ontology.Empty;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.InitalizeTribe;
import es.upm.woa.ontology.RegisterTribe;
import es.upm.woa.ontology.ResourceAccount;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 *
 * @author ISU
 */
public class AgTribe extends GroupAgent {
    
    private final int BETWEEN_REGISTRATION_RETRIES_MILLIS = 1000;
    
    private CommunicationStandard gameComStandard;
    private CommunicationStandard group1ComStandard;
    private Collection<Unit> units;
    private GraphGameMap knownMap;
    private WoaLogger logger;
    private DFAgentDescription registrationDeskServiceDescription;

    private SendMapDataSharingHelper mapDataSharingHelper;
    private SendAssignStrategyHelper assignStrategyHelper;
    
    private TribeResources tribeResources;
    
    private static final Queue<Integer> GROUP_NUMBER_QUEUE = new LinkedList<>();
    
    public AgTribe() {
        synchronized (GROUP_NUMBER_QUEUE) {
            if (GROUP_NUMBER_QUEUE.isEmpty()) {
                for (int i = 1; i <= 6; i++) {
                    GROUP_NUMBER_QUEUE.add(i);
                }
            }
        }
    }

    @Override
    protected void setup() {
        logger = new WoaLogger(getAID(), new ConsoleHandler());
        logger.setLevel(Level.FINE);
            
        try {
            
            initializeAgent();
            initializeTribe();
            
            startInformRegistrationBehaviour();
            startInformNewUnitBehaviour();
            startInformCellDetailBehaviour();
            startWhereAmIBehaviour();
            startInformUnitPositionBehaviour();
            startShareMapDataBehaviour();
            startInitializeTribeBehaviour();
        } catch (InterruptedException ex) {
            log(Level.SEVERE, "Could not register Tribe. Finalizing...");
        }
    }

    private void startInformRegistrationBehaviour() {
        RegisterTribe dummyAction = new RegisterTribe();
        dummyAction.setTeamNumber(getGroupNumber());
        final Action registerTribe = new Action(this.getAID(), dummyAction);

        addBehaviour(new Conversation(this, gameComStandard, GameOntology.REGISTERTRIBE) {
            @Override
            public void onStart() {
                AID registrationDeskAID = (AID) registrationDeskServiceDescription.getName();

                sendMessage(registrationDeskAID, ACLMessage.REQUEST, registerTribe, new Conversation.SentMessageHandler() {

                    @Override
                    public void onSent(String conversationID) {

                        log(Level.FINER, "sent RegisterTribe request");
                        receiveResponse(conversationID, new Conversation.ResponseHandler() {

                            @Override
                            public void onAgree(ACLMessage response) {
                                log(Level.FINE, "receive RegisterTribe agree from "
                                    + response.getSender().getLocalName());
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
    
    private void startInformCellDetailBehaviour() {
        new ReceiveInformCellDetailBehaviourHelper(this, gameComStandard
                , knownMap).startInformCellDetailBehaviour();
    }

    private void startShareMapDataBehaviour() {
        new ReceiveShareMapDataBehaviourHelper(this,
                group1ComStandard, knownMap, (NewGraphConnection newConnection) -> {
                    log(Level.FINER, "Updated known map");
                    shareNewConnectionWithUnits(newConnection);
                }).startShareMapDataBehaviour();
    }

    private void startInformUnitPositionBehaviour() {
        new ReceiveInformUnitPositionBehaviourHelper(this, gameComStandard
                , knownMap).startInformCellDetailBehaviour();
    }
    
    
    private void startInformNewUnitBehaviour() {
        new ReceiveInformNewUnitBehaviourHelper(this, gameComStandard
                , knownMap, units
                , (Unit newUnit) -> {
                        informNewUnitOwnership(newUnit);
                        mapDataSharingHelper.unicastMapData(newUnit.getId(), knownMap);
                        
        }).startInformNewUnitBehaviour();
    }
    
    private void informNewUnitOwnership(Unit unit) {
        Action informOwnershipAction = new Action(getAID(), new NotifyUnitOwnership());
        addBehaviour(new Conversation(this, group1ComStandard
                , Group1Ontology.NOTIFYUNITOWNERSHIP) {
            @Override
            public void onStart() {
                sendMessage(unit.getId(), ACLMessage.INFORM, informOwnershipAction
                        , new Conversation.SentMessageHandler() {
                    @Override
                    public void onSent(String conversationID) {
                        log(Level.FINE, "Informed unit " + unit.getId()
                                .getLocalName() + " of ownership");
                    }
                    
                });
            }
        });
    }

    private void startWhereAmIBehaviour() {
        addBehaviour(new Conversation(this, group1ComStandard, Group1Ontology.WHEREAMI) {
            @Override
            public void onStart() {
                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onRequest(ACLMessage response) {
                        final Action whereAmIAction = new Action(getAID(), null);
                        log(Level.FINER, "received WhereAmI request from "
                                        + response.getSender().getLocalName());
                        
                        
                        AID senderAid = response.getSender();
                        Unit requesterUnit = units.parallelStream().filter(u -> u.getId()
                                .equals(senderAid)).findAny().orElse(null);
                        if (requesterUnit == null) {
                            respondMessage(response, ACLMessage.REFUSE, whereAmIAction);
                        }
                        else {
                            try {
                                MapCell knownCell = knownMap.getCellAt(requesterUnit
                                        .getCoordX(), requesterUnit.getCoordY());
                                WhereAmI whereAmI = new WhereAmI();
                                whereAmI.setXPosition(knownCell.getXCoord());
                                whereAmI.setYPosition(knownCell.getYCoord());
                                whereAmIAction.setAction(whereAmI);
                                
                                respondMessage(response, ACLMessage.INFORM, whereAmIAction);
                            } catch (NoSuchElementException ex) {
                                respondMessage(response, ACLMessage.REFUSE, whereAmIAction);
                            }
                        } 

                    }

                });
            }
        });
    }

    private void initializeAgent() throws InterruptedException {
        registrationDeskServiceDescription = null;
        
        //Finds the Registration Desk in the DF
        try {
            DFAgentDescription dfdRegistrationDesk = new DFAgentDescription();
            ServiceDescription sdRegistrationDesk = new ServiceDescription();
            sdRegistrationDesk.setType(AgRegistrationDesk.REGISTRATION_DESK);
            dfdRegistrationDesk.addServices(sdRegistrationDesk);
            // It finds agents of the required type
            DFAgentDescription[] descriptions = DFService.search(this, dfdRegistrationDesk);
            if (descriptions.length == 0) {
                log(Level.WARNING, "Registration Desk service description not found");
            } else {
                registrationDeskServiceDescription = descriptions[0];
            }
        } catch (FIPAException ex) {
            log(Level.WARNING, "Registration Desk service description not found");
        }
        
        if (registrationDeskServiceDescription == null) {
            Thread.sleep(BETWEEN_REGISTRATION_RETRIES_MILLIS);
            log(Level.INFO, "Retrying...");
            initializeAgent();
        }
        else {
            log(Level.INFO, "Registration desk found!");
        }
    }

    private int getGroupNumber() {
        synchronized (GROUP_NUMBER_QUEUE) {
            return GROUP_NUMBER_QUEUE.poll();
        }
    }
    
    private void initializeTribe() {
        gameComStandard = new WoaCommunicationStandard();
        gameComStandard.register(getContentManager());
        
        group1ComStandard = new Group1CommunicationStandard();
        group1ComStandard.register(getContentManager());

        units = new HashSet<>();
        knownMap = GraphGameMap.getInstance();
        mapDataSharingHelper = new SendMapDataSharingHelper(this, group1ComStandard);
        assignStrategyHelper = new SendAssignStrategyHelper(this, group1ComStandard);
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
            log(Level.FINER, "Cell discovery at " + newCell);
            knownMap.addCell(newCell);
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
    
    /**
     * Listen to the initial resources being sent by the registration desk
     */
    public void startInitializeTribeBehaviour() {
        addBehaviour(new Conversation(this, gameComStandard
                , GameOntology.INITALIZETRIBE) {
            @Override
            public void onStart() {
                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onInform(ACLMessage response) {
                        try {
                            ContentElement ce = getContentManager().extractContent(response);
                            if (ce instanceof Action) {

                                Action agAction = (Action) ce;
                                Concept conc = agAction.getAction();

                                if (conc instanceof InitalizeTribe) {
                                    log(Level.FINER, "receive InitializeTribe inform from "
                                            + response.getSender().getLocalName());

                                    InitalizeTribe initializeTribe = (InitalizeTribe) conc;
                                    
                                    ResourceAccount startingResources
                                            = initializeTribe.getStartingResources();
                                    
                                    tribeResources = new TribeResources(startingResources.getWood(), 
                                            startingResources.getStone(), 
                                            startingResources.getFood(), 
                                            startingResources.getGold());
                                    
                                    MapCell startingPosition = MapCellFactory
                                            .getInstance().buildCell(initializeTribe.getStartingPosition());
                                    knownMap.addCell(startingPosition);
                                    
                                    for(Object aidObject : initializeTribe.getUnitList().toArray()){
                                        AID unitAID = (AID) aidObject;
                                        Unit newUnit = new Unit(unitAID
                                                , startingPosition.getXCoord()
                                                , startingPosition.getYCoord());
                                        units.add(newUnit);
                                        mapDataSharingHelper.unicastMapData(unitAID, knownMap);
                                        informNewUnitOwnership(newUnit);
                                        
                        
                                    }
                                    
                                    executeStrategy();
                                    
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
    
    @Override
    public void log(Level logLevel, String message) {
        logger.log(logLevel, message);
    }

    private void shareNewConnectionWithUnits(NewGraphConnection newConnection) {
        mapDataSharingHelper.multicastMapData(getMyUnitsAIDs(), newConnection);
    }

    private void executeStrategy() {
        addBehaviour(new TribeStrategyBehaviour(this, GameClock.getInstance()
                , assignStrategyHelper, knownMap
                , Collections.unmodifiableCollection(units)
                , tribeResources, MapCellFinder.getInstance(knownMap)));
    }

}
