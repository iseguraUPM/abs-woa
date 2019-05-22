package es.upm.woa.group1.agent;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import es.upm.woa.group1.WoaDefinitions;
import es.upm.woa.group1.agent.strategy.PositionedAgentUnit;
import es.upm.woa.group1.agent.strategy.StrategyEnvelop;
import es.upm.woa.group1.agent.strategy.StrategyFactory;
import es.upm.woa.group1.WoaLogger;
import es.upm.woa.group1.agent.strategy.FeedbackMessageFactory;
import es.upm.woa.group1.map.CellTranslation;
import es.upm.woa.group1.map.MapCell;
import es.upm.woa.group1.ontology.Group1Ontology;
import es.upm.woa.group1.ontology.WhereAmI;
import es.upm.woa.group1.protocol.CommunicationStandard;
import es.upm.woa.group1.protocol.Conversation;
import es.upm.woa.group1.protocol.Group1CommunicationStandard;
import es.upm.woa.group1.protocol.WoaCommunicationStandard;
import es.upm.woa.group1.agent.strategy.Strategy;
import es.upm.woa.group1.map.PathfinderGameMap;

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
public class AgUnit extends GroupAgent implements PositionedAgentUnit,
        CreateBuildingRequestHandler, CreateUnitRequestHandler,
         ExploitResourceRequestHandler {

    public static final String WORLD = "WORLD";

    private static final int MAX_REQUEST_POSITION_TRIES = 3;
    private static final int BETWEEN_REQUEST_POSITION_TRIES_TIME_MILLIS = 1000;
    private static final int BETWEEN_WORLD_RETRIES_MILLIS = 1000;

    private PathfinderGameMap knownMap;
    private CommunicationStandard gameComStandard;
    private CommunicationStandard group1ComStandard;
    private DFAgentDescription worldAgentServiceDescription;
    private MapCell currentPosition;
    private AID ownerTribe;

    private MapCellFinder constructionSiteFinder;
    private StrategicUnitBehaviour strategyBehaviour;
    private StrategyFactory strategyFactory;
    private FeedbackMessageFactory feedbackMessageFactory;

    private SendMapDataSharingHelper mapDataSharingHelper;
    private SendFeedbackUnitStatusHelper sendUnitStatusHelper;

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
            // NOTE: we don't update the contents in case they are not up-to-date
            //  with the World. We receive updates via NotifyCellDetail protocol.
            connectPath(myCell, direction);
        } catch (NoSuchElementException ex) {
            myCell = newPosition;
            if (knownMap.addCell(myCell)) {
                connectPath(myCell, direction);
            }
        }

        NewGraphConnection newConnection = new NewGraphConnection();
        newConnection.source = currentPosition;
        newConnection.target = myCell;
        newConnection.direction = direction;

        mapDataSharingHelper.unicastMapData(ownerTribe, newConnection);

        currentPosition = myCell;
        sendUnitStatusHelper.sendStatus(feedbackMessageFactory
                .envelopChangedPosition(currentPosition));
    }

    private void connectPath(MapCell myCell, CellTranslation direction) {
        knownMap.connectPath(currentPosition, myCell, direction);
        CellTranslation inverse = direction.generateInverse();
        knownMap.connectPath(myCell, currentPosition, inverse);
    }

    @Override
    public AID getTribeAID() {
        return ownerTribe;
    }

    @Override
    protected void setup() {
        logger = new WoaLogger(getAID(), new ConsoleHandler());
        logger.setLevel(Level.FINE);
        try {
            initializeAgent(() -> {
                log(Level.INFO, "Unit initialized");
                startStrategicUnitBehaviour();
                startAssignStrategyBehaviour();
            });
        } catch (InterruptedException ex) {
            log(Level.SEVERE, "Could not find World agent. Finalizing...");
        }

    }

    private void initializeAgent(OnUnitInitializedHandler handler)
            throws InterruptedException {
        worldAgentServiceDescription = null;
        //Finds the World in the DF
        try {
            DFAgentDescription dfdWorld = new DFAgentDescription();
            ServiceDescription sdWorld = new ServiceDescription();
            sdWorld.setType(WORLD);
            dfdWorld.addServices(sdWorld);
            // It finds agents of the required type
            DFAgentDescription[] descriptions = DFService.search(this, dfdWorld);
            if (descriptions.length == 0) {
                log(Level.WARNING, "World service description not found");
            } else {
                worldAgentServiceDescription = descriptions[0];
                initializeUnit(handler);
            }
        } catch (FIPAException ex) {
            log(Level.WARNING, "World service description not found");
        }

        if (worldAgentServiceDescription == null) {
            Thread.sleep(BETWEEN_WORLD_RETRIES_MILLIS);
            log(Level.INFO, "Retrying...");
            initializeAgent(handler);
        }
    }

    private void initializeUnit(OnUnitInitializedHandler handler) {
        initializeDependencies();
        startShareMapDataBehaviour();

        startInformOwnershipBehaviour(() -> {
            sendUnitStatusHelper = new SendFeedbackUnitStatusHelper(this,
                    group1ComStandard, ownerTribe);

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
        mapDataSharingHelper = new SendMapDataSharingHelper(this, group1ComStandard);
        constructionSiteFinder = MapCellFinder.getInstance(knownMap);

        strategyFactory = StrategyFactory.getInstance(this, gameComStandard,
                knownMap, worldAgentServiceDescription.getName(), this,
                constructionSiteFinder, this, this, this);

        feedbackMessageFactory = FeedbackMessageFactory.getInstance(this);
    }

    private void startInformUnitPositionBehaviour() {
        new ReceiveInformUnitPositionBehaviourHelper(this, gameComStandard,
                knownMap)
                .startInformCellDetailBehaviour();
    }

    private void startShareMapDataBehaviour() {
        new ReceiveShareMapDataBehaviourHelper(this,
                group1ComStandard, knownMap, (NewGraphConnection newConnection) -> {
                    log(Level.FINER, "Updated known map");
                }).startShareMapDataBehaviour();
    }

    private void startInformCellDetailBehaviour() {
        new ReceiveInformCellDetailBehaviourHelper(this, gameComStandard,
                knownMap).startInformCellDetailBehaviour();
    }

    private void startInformOwnershipBehaviour(OnReceivedOwnershipHandler handler) {
        addBehaviour(new Conversation(this, group1ComStandard, Group1Ontology.NOTIFYUNITOWNERSHIP) {
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
        addBehaviour(new Conversation(this, group1ComStandard, Group1Ontology.WHEREAMI) {
            @Override
            public void onStart() {
                sendMessage(ownerTribe, ACLMessage.REQUEST, whereAmIAction, new SentMessageHandler() {
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

    @Override
    public void log(Level logLevel, String message) {
        logger.log(logLevel, message);
    }

    @Override
    void onCellDiscovered(MapCell newCell) {
        try {
            MapCell knownCell = knownMap
                    .getCellAt(newCell.getXCoord(), newCell.getYCoord());
            knownCell.setContent(newCell.getContent());
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

    @Override
    public void onStartedBuilding(String buildingType) {
        sendUnitStatusHelper.sendStatus(feedbackMessageFactory.envelopStartedBuilding(buildingType));
    }

    @Override
    public void onStartedCreatingUnit() {
        sendUnitStatusHelper.sendStatus(feedbackMessageFactory.envelopStartedUnitCreation());
    }

    @Override
    public void onFinishedBuilding(String buildingType) {
        sendUnitStatusHelper.sendStatus(feedbackMessageFactory.envelopBuildingSuccess(buildingType));
    }

    @Override
    public void onErrorBuilding(String buildingType) {
        sendUnitStatusHelper.sendStatus(feedbackMessageFactory.envelopBuildingFailure(buildingType));
    }

    @Override
    public void onFinishedCreatingUnit() {
        sendUnitStatusHelper.sendStatus(feedbackMessageFactory.envelopUnitCreationSuccess());
    }

    @Override
    public void onErrorCreatingUnit() {
        sendUnitStatusHelper.sendStatus(feedbackMessageFactory.envelopUnitCreationFailure());
    }

    @Override
    public void onGainedGold(int amount) {
        sendUnitStatusHelper.sendStatus(feedbackMessageFactory.envelopGainedResource(WoaDefinitions.GOLD, amount));
    }

    @Override
    public void onGainedStone(int amount) {
        sendUnitStatusHelper.sendStatus(feedbackMessageFactory.envelopGainedResource(WoaDefinitions.STONE, amount));
    }

    @Override
    public void onGainedWood(int amount) {
        sendUnitStatusHelper.sendStatus(feedbackMessageFactory.envelopGainedResource(WoaDefinitions.WOOD, amount));
    }

    @Override
    public void onGainedFood(int amount) {
        sendUnitStatusHelper.sendStatus(feedbackMessageFactory.envelopGainedResource(WoaDefinitions.FOOD, amount));
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
