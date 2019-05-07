package es.upm.woa.agent.group1;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.ontology.Group1Ontology;
import es.upm.woa.agent.group1.ontology.NotifyUnitOwnership;
import es.upm.woa.agent.group1.ontology.WhereAmI;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.agent.group1.protocol.Group1CommunicationStandard;
import es.upm.woa.agent.group1.protocol.WoaCommunicationStandard;
import es.upm.woa.agent.group1.strategy.StrategicUnitBehaviour;
import es.upm.woa.agent.group1.strategy.StrategyEventDispatcher;

import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.CreateBuilding;
import es.upm.woa.ontology.CreateUnit;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.MoveToCell;

import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;

import java.util.NoSuchElementException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 *
 * @author ISU
 */
public class AgUnit extends GroupAgent implements PositionedAgentUnit {

    public static final String WORLD = "WORLD";

    private static final int MAX_REQUEST_POSITION_TRIES = 3;
    private static final int BETWEEN_REQUEST_POSITION_TRIES_TIME_MILLIS = 1000;
    
    // TODO: temporary
    private static final int UNIT_KNOWN_MAP_SIZE = 4;
    private static int UNIT_TEST_MODE = 0;

    private GraphGameMap knownMap;
    private CommunicationStandard gameComStandard;
    private CommunicationStandard group1ComStandard;
    private DFAgentDescription worldAgentServiceDescription;
    private MapCell currentPosition;
    private AID ownerTribe;
    private StrategyEventDispatcher eventDispatcher;

    private Handler logHandler;

    @Override
    public MapCell getCurrentPosition() {
        return currentPosition;
    }

    @Override
    public void setCurrentPosition(MapCell currentPosition) {
        this.currentPosition = currentPosition;
    }

    @Override
    public AID getTribeAID() {
        return ownerTribe;
    }


    @Override
    protected void setup() {
        logHandler = new ConsoleHandler();
        logHandler.setLevel(Level.ALL);

        initializeAgent(() -> {
            log(Level.INFO, "Unit initialized");
            //startCreateUnitBehaviour(); -> to strategy
            //startMoveToCellBehaviour(); -> to strategy 
            startStrategy();
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
        gameComStandard = new WoaCommunicationStandard();
        gameComStandard.register(getContentManager());
        
        group1ComStandard = new Group1CommunicationStandard();
        group1ComStandard.register(getContentManager());

        knownMap = GraphGameMap.getInstance(UNIT_KNOWN_MAP_SIZE, UNIT_KNOWN_MAP_SIZE);

        new ReceiveInformCellDetailBehaviourHelper(this, gameComStandard
                , knownMap).startInformCellDetailBehaviour();
        startInformOwnershipBehaviour(() -> {
            requestUnitPosition(MAX_REQUEST_POSITION_TRIES, () -> {
                handler.onUnitInitialized();
                new ReceiveInformUnitPositionBehaviourHelper(this, gameComStandard
                        , knownMap)
                        .startInformCellDetailBehaviour();
            });
        });
    }

    // TODO: remove. Do not copy code
    private void startCreateUnitBehaviour() {
        Action createUnitAction = new Action(getAID(), new CreateUnit());
        addBehaviour(new Conversation(this, gameComStandard, createUnitAction, GameOntology.CREATEUNIT) {
            @Override
            public void onStart() {
                AID worldAID = (AID) worldAgentServiceDescription.getName();

                sendMessage(worldAID, ACLMessage.REQUEST, new Conversation.SentMessageHandler() {

                    @Override
                    public void onSent(String conversationID) {

                        receiveResponse(conversationID, new Conversation.ResponseHandler() {

                            @Override
                            public void onAgree(ACLMessage response) {
                                log(Level.FINER, "received CreateUnit agree from "
                                        + response.getSender().getLocalName());

                                receiveResponse(conversationID, new Conversation.ResponseHandler() {

                                    @Override
                                    public void onFailure(ACLMessage response) {
                                        log(Level.WARNING, "received CreateUnit failure from "
                                                + response.getSender().getLocalName());
                                    }

                                    @Override
                                    public void onInform(ACLMessage response) {
                                        log(Level.FINER, "received CreateUnit inform from "
                                                + response.getSender().getLocalName());
                                    }

                                });
                            }

                            @Override
                            public void onNotUnderstood(ACLMessage response) {
                                log(Level.WARNING, "received CreateUnit not understood from "
                                        + response.getSender().getLocalName());
                            }

                            @Override
                            public void onRefuse(ACLMessage response) {
                                log(Level.FINE, "receive CreateUnit refuse from "
                                        + response.getSender().getLocalName());
                            }

                        });
                    }
                });

            }

        });
    }

    // TODO: remove. Do not copy code
    private void startMoveToCellBehaviour() {

        Cell newCellPosition = new Cell();
        //TODO by default 0,0
        newCellPosition.setX(2);
        newCellPosition.setY(2);

        MoveToCell moveToCell = new MoveToCell();
        moveToCell.setTarget(newCellPosition);

        Action moveToCellAction = new Action(getAID(), moveToCell);
        addBehaviour(new Conversation(this, gameComStandard, moveToCellAction, GameOntology.MOVETOCELL) {

            @Override
            public void onStart() {

                AID worldAID = (AID) worldAgentServiceDescription.getName();

                sendMessage(worldAID, ACLMessage.REQUEST, new Conversation.SentMessageHandler() {
                    @Override
                    public void onSent(String conversationID) {
                        log(Level.FINE, "wants to move to cell "
                                + newCellPosition.getX() + "," + newCellPosition.getY());

                        receiveResponse(conversationID, new Conversation.ResponseHandler() {

                            @Override
                            public void onAgree(ACLMessage response) {
                                log(Level.FINER, "receive MoveToCell agree from "
                                        + response.getSender().getLocalName());

                                receiveResponse(conversationID, new Conversation.ResponseHandler() {

                                    @Override
                                    public void onFailure(ACLMessage response) {
                                        log(Level.WARNING, "receive MoveToCell failure from "
                                                + response.getSender().getLocalName());
                                    }

                                    @Override
                                    public void onInform(ACLMessage response) {

                                        log(Level.FINER, "receive MoveToCell inform from "
                                                + response.getSender().getLocalName());

                                        log(Level.FINE, "moved to cell "
                                                + newCellPosition.getX() + ","
                                                + newCellPosition.getY());
                                    }

                                });
                            }

                            @Override
                            public void onNotUnderstood(ACLMessage response) {
                                log(Level.WARNING, "receive MoveToCell not understood from "
                                        + response.getSender().getLocalName());
                            }

                            @Override
                            public void onRefuse(ACLMessage response) {
                                log(Level.FINE, "receive MoveToCell refuse from "
                                        + response.getSender().getLocalName());
                            }

                        });
                    }
                });
            }
        });
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
                                try {
                                    ContentElement ce = getContentManager().extractContent(response);
                                    if (ce instanceof Action) {

                                        Action agAction = (Action) ce;
                                        Concept conc = agAction.getAction();

                                        if (conc instanceof WhereAmI) {
                                            log(Level.FINER, "receive WhereAmI inform from "
                                                    + response.getSender().getLocalName());

                                            WhereAmI where = (WhereAmI) conc;
                                            int x = where.getXCoord();
                                            int y = where.getYCoord();

                                            try {
                                                MapCell position = knownMap.getCellAt(x, y);
                                                currentPosition = position;
                                                handler.onReceivedStartingPosition();
                                            } catch (NoSuchElementException ex) {
                                                log(Level.FINE, "Current position unknown (" + ex + ")");
                                                block(BETWEEN_REQUEST_POSITION_TRIES_TIME_MILLIS);
                                                // Try again
                                                requestUnitPosition(tries - 1, handler);
                                            }
                                        }
                                    }
                                } catch (Codec.CodecException | OntologyException ex) {
                                    log(Level.WARNING, "could not receive message"
                                            + " (" + ex + ")");
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

    private void startStrategy() {
        // TODO: the test mode is so the first unit creates a town hall and
        //  the second one explores
        if (UNIT_TEST_MODE++ == 0) {
            startCreateTownHallBehaviour();
        }
        else if (UNIT_TEST_MODE == 1) {
            StrategicUnitBehaviour unitBehaviour = new StrategicUnitBehaviour(this);
            
            //unitBehaviour.addStrategy(new CreateUnitStrategy(this, eventDispatcher));
            addFreeExploreStrategy(unitBehaviour);
            addBehaviour(unitBehaviour);
        }
    }

    private void addFreeExploreStrategy(StrategicUnitBehaviour unitBehaviour) {
        unitBehaviour.addStrategy(new FreeExploreStrategy(this, gameComStandard, knownMap, worldAgentServiceDescription.getName(), this, eventDispatcher));
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
