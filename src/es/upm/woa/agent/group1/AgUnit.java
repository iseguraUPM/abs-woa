package es.upm.woa.agent.group1;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import es.upm.woa.agent.group1.map.CellTranslation;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.ontology.Group1Ontology;
import es.upm.woa.agent.group1.ontology.NotifyUnitOwnership;
import es.upm.woa.agent.group1.ontology.WhereAmI;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.agent.group1.protocol.Group1CommunicationStandard;
import es.upm.woa.agent.group1.protocol.WoaCommunicationStandard;
import es.upm.woa.agent.group1.strategy.StrategicUnitBehaviour;
import es.upm.woa.agent.group1.strategy.Strategy;

import es.upm.woa.ontology.CreateBuilding;
import es.upm.woa.ontology.Empty;
import es.upm.woa.ontology.GameOntology;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;

import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.UnexpectedArgument;
import jade.domain.FIPAException;

import jade.lang.acl.ACLMessage;
import java.util.NoSuchElementException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

/**
 *
 * @author ISU
 */
public class AgUnit extends GroupAgent implements PositionedAgentUnit {

    public static final String WORLD = "WORLD";

    private static final int MAX_REQUEST_POSITION_TRIES = 3;
    private static final int BETWEEN_REQUEST_POSITION_TRIES_TIME_MILLIS = 1000;

    private GraphGameMap knownMap;
    private CommunicationStandard gameComStandard;
    private CommunicationStandard group1ComStandard;
    private DFAgentDescription worldAgentServiceDescription;
    private MapCell currentPosition;
    private AID ownerTribe;
    private SendMapDataSharingHelper mapDataSharingHelper;
    private MapCellFinder constructionSiteFinder;

    private StrategicUnitBehaviour strategyBehaviour;
    private StrategyFactory strategyFactory;

    private WoaLogger logger;

    @Override
    public MapCell getCurrentPosition() {
        return currentPosition;
    }

    @Override
    public void updateCurrentPosition(CellTranslation direction, MapCell newPosition) {
        MapCell myCell;
        try {
            myCell = knownMap.getCellAt(newPosition.getXCoord(),
                     newPosition.getYCoord());
            updateCellContents(myCell, newPosition);
            knownMap.connectPath(currentPosition, myCell, direction);
            CellTranslation inverse = direction.generateInverse();
            knownMap.connectPath(myCell, currentPosition, inverse);
        } catch (NoSuchElementException ex) {
            myCell = newPosition;
            if (knownMap.addCell(myCell)) {
                knownMap.connectPath(currentPosition, myCell, direction);
                CellTranslation inverse = direction.generateInverse();
                knownMap.connectPath(myCell, currentPosition, inverse);
            }
        }

        mapDataSharingHelper.unicastMapData(ownerTribe);

        currentPosition = myCell;
    }

    private void updateCellContents(MapCell myCell, MapCell updatedCell) {
        if (myCell.getContent() instanceof Empty && !(updatedCell.getContent() instanceof Empty)) {
            myCell.setContent(updatedCell.getContent());
        }
    }

    @Override
    public AID getTribeAID() {
        return ownerTribe;
    }

    @Override
    protected void setup() {
        logger = new WoaLogger(getAID(), new ConsoleHandler());
        logger.setLevel(Level.ALL);

        initializeAgent(() -> {
            log(Level.INFO, "Unit initialized");
            startStrategicUnitBehaviour();
            startAssignStrategyBehaviour();
        });

    }

    private void initializeAgent(OnUnitInitializedHandler handler) {
        //Finds the World in the DF
        try {
            DFAgentDescription dfdWorld = new DFAgentDescription();
            ServiceDescription sdWorld = new ServiceDescription();
            sdWorld.setType(WORLD);
            dfdWorld.addServices(sdWorld);
            // It finds agents of the required type
            DFAgentDescription[] descriptions = DFService.search(this, dfdWorld);
            if (descriptions.length == 0) {
                log(Level.SEVERE, "World service description not found");
            } else {
                worldAgentServiceDescription = descriptions[0];
                initializeUnit(handler);
            }
        } catch (FIPAException ex) {
            log(Level.WARNING, " the WORLD agent was not found (" + ex + ")");
        }
    }

    private void initializeUnit(OnUnitInitializedHandler handler) {
        initializeDependencies();
        startShareMapDataBehaviour();

        startInformOwnershipBehaviour(() -> {
            requestUnitPosition(MAX_REQUEST_POSITION_TRIES, () -> {
                startInformCellDetailBehaviour();
                startInformUnitPositionBehaviour();
                handler.onUnitInitialized();
            });
        });
    }

    private void initializeDependencies() {
        gameComStandard = new WoaCommunicationStandard();
        gameComStandard.register(getContentManager());

        group1ComStandard = new Group1CommunicationStandard();
        group1ComStandard.register(getContentManager());

        knownMap = GraphGameMap.getInstance();
        mapDataSharingHelper = new SendMapDataSharingHelper(this, group1ComStandard, knownMap);
        constructionSiteFinder = MapCellFinder.getInstance(knownMap);
        strategyFactory = StrategyFactory.getInstance(this, gameComStandard,
                 knownMap, worldAgentServiceDescription.getName(), this,
                 constructionSiteFinder);
    }

    private void startInformUnitPositionBehaviour() {
        new ReceiveInformUnitPositionBehaviourHelper(this, gameComStandard,
                 knownMap)
                .startInformCellDetailBehaviour();
    }

    private void startShareMapDataBehaviour() {
        new ReceiveShareMapDataBehaviourHelper(this,
                group1ComStandard, knownMap, () -> {
                    log(Level.FINE, "Updated known map");
                }).startShareMapDataBehaviour();
    }

    private void startInformCellDetailBehaviour() {
        new ReceiveInformCellDetailBehaviourHelper(this, gameComStandard,
                 knownMap).startInformCellDetailBehaviour();
    }

    private void startInformOwnershipBehaviour(OnReceivedOwnershipHandler handler) {
        Action informOwnershipAction = new Action(getAID(), new NotifyUnitOwnership());
        addBehaviour(new Conversation(this, group1ComStandard, informOwnershipAction, Group1Ontology.NOTIFYUNITOWNERSHIP) {
            @Override
            public void onStart() {
                listenMessages(new ResponseHandler() {
                    @Override
                    public void onInform(ACLMessage response) {
                        log(Level.FINE, "Registered owner tribe: "
                                + response.getSender().getLocalName());
                        ownerTribe = response.getSender();
                        handler.onReceivedOwnership();
                    }
                });
            }
        });
    }

    private void requestUnitPosition(int tries, OnReceivedStartingPositionHandler handler) {
        if (tries > 0) {
            attemptCurrentPositionRequest(tries, handler);
        } else {
            log(Level.WARNING, "Could not obtain current position from tribe");
        }
    }

    private void attemptCurrentPositionRequest(int tries, OnReceivedStartingPositionHandler handler) {
        final Action whereAmIAction = new Action(getAID(), new WhereAmI());
        addBehaviour(new Conversation(this, group1ComStandard, whereAmIAction, Group1Ontology.WHEREAMI) {
            @Override
            public void onStart() {
                sendMessage(ownerTribe, ACLMessage.REQUEST, new SentMessageHandler() {
                    @Override
                    public void onSent(String conversationID) {

                        receiveResponse(conversationID, new ResponseHandler() {
                            @Override
                            public void onInform(ACLMessage response) {
                                log(Level.FINER, "receive WhereAmI inform from "
                                        + response.getSender().getLocalName());
                                try {
                                    ContentElement ce = getContentManager()
                                            .extractContent(response);

                                    Action action = (Action) ce;
                                    WhereAmI whereAmI = (WhereAmI) action
                                            .getAction();

                                    currentPosition = knownMap
                                            .getCellAt(whereAmI
                                                    .getXPosition(),
                                                     whereAmI.getYPosition());
                                    log(Level.FINE, "Starting position at "
                                            + currentPosition);
                                    
                                    handler.onReceivedStartingPosition();

                                } catch (Codec.CodecException | NoSuchElementException
                                        | OntologyException ex) {
                                    log(Level.WARNING, "Could not retrieve starting position (" + ex + ")");
                                }
                            }

                            @Override
                            public void onRefuse(ACLMessage message) {
                                log(Level.WARNING, "Received WhereAmI refuse from "
                                        + message.getSender().getLocalName());
                            }

                        });

                    }

                    @Override
                    public void onSentMessageError() {
                        block(BETWEEN_REQUEST_POSITION_TRIES_TIME_MILLIS);
                        requestUnitPosition(tries - 1, handler);
                    }

                });
            }
        });
    }

    // TODO: move to strategy
    private void startCreateTownHallBehaviour() {
        CreateBuilding createBuilding = new CreateBuilding();
        createBuilding.setBuildingType(WoaDefinitions.TOWN_HALL);
        Action createTownHall = new Action(getAID(), createBuilding);
        addBehaviour(new Conversation(this, gameComStandard, createTownHall, GameOntology.CREATEBUILDING) {
            @Override
            public void onStart() {
                AID worldAID = (AID) worldAgentServiceDescription.getName();

                sendMessage(worldAID, ACLMessage.REQUEST, new Conversation.SentMessageHandler() {

                    @Override
                    public void onSent(String conversationID) {

                        log(Level.FINER, "sent CreateBuuilding request");
                        receiveResponse(conversationID, new Conversation.ResponseHandler() {

                            @Override
                            public void onAgree(ACLMessage response) {
                                log(Level.FINER, "received CreateTownHall agree from "
                                        + response.getSender().getLocalName());

                                receiveResponse(conversationID, new Conversation.ResponseHandler() {

                                    @Override
                                    public void onFailure(ACLMessage response) {
                                        log(Level.WARNING, "received CreateTownHall failure from "
                                                + response.getSender().getLocalName());
                                    }

                                    @Override
                                    public void onInform(ACLMessage response) {
                                        log(Level.FINER, "received CreateTownHall inform from "
                                                + response.getSender().getLocalName());
                                        log(Level.FINE, "created town hall at "
                                                + currentPosition.getXCoord()
                                                + "," + currentPosition.getYCoord());
                                    }

                                });
                            }

                            @Override
                            public void onNotUnderstood(ACLMessage response) {
                                log(Level.WARNING, "received CreateTownHall not understood from "
                                        + response.getSender().getLocalName());
                            }

                            @Override
                            public void onRefuse(ACLMessage response) {
                                log(Level.FINE, "receive CreateTownHall refuse from "
                                        + response.getSender().getLocalName());
                            }

                        });
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

    private void startAssignStrategyBehaviour() {
        new ReceiveAssignStrategyBehaviourHelper(this, group1ComStandard,
                 (StrategyEnvelop strategyEnvelop) -> {
                    try {
                        Strategy incomingStrategy = strategyFactory.getStrategy(strategyEnvelop);
                        strategyBehaviour.addStrategy(incomingStrategy);
                    } catch (UnexpectedArgument ex) {
                        log(Level.WARNING, "Error while retrieving new strategy ("
                                + ex + ")");
                    }
                }).startAssignStrategyBehaviour();
    }

    private void startStrategicUnitBehaviour() {
        strategyBehaviour = new StrategicUnitBehaviour(this);
        addBehaviour(strategyBehaviour);
    }

    private interface OnReceivedOwnershipHandler {

        void onReceivedOwnership();

    }

    private interface OnReceivedStartingPositionHandler {

        void onReceivedStartingPosition();

    }

    private interface OnUnitInitializedHandler {

        void onUnitInitialized();

    }

}
