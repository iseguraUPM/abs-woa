package es.upm.woa.agent.group1;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import es.upm.woa.agent.group1.map.GraphGameMap;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.map.MapCellFactory;
import es.upm.woa.agent.group1.ontology.Group1Ontology;
import es.upm.woa.agent.group1.ontology.NotifyUnitOwnership;
import es.upm.woa.agent.group1.ontology.WhereAmI;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.agent.group1.strategy.StrategicUnitBehaviour;
import es.upm.woa.agent.group1.strategy.StrategyEventDispatcher;

import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.CreateUnit;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.MoveToCell;
import es.upm.woa.ontology.NotifyNewCellDiscovery;

import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
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
public class AgUnit extends Agent {

    public static final String WORLD = "WORLD";

    private static final int MAX_REQUEST_POSITION_TRIES = 3;
    private static final int BETWEEN_REQUEST_POSITION_TRIES_TIME_MILLIS = 1000;

    private GraphGameMap knownMap;
    private Ontology gameOntology;
    private Ontology group1Ontology;
    private SLCodec codec;
    private DFAgentDescription worldAgentServiceDescription;
    private MapCell currentPosition;
    private AID ownerTribe;
    private StrategyEventDispatcher eventDispatcher;

    private Handler logHandler;

    /// NOTE: this methods must be package-private
    GraphGameMap getKnownMap() {
        return knownMap;
    }

    Ontology getOntology() {
        return gameOntology;
    }

    Codec getCodec() {
        return codec;
    }

    AID getWorldAID() {
        return worldAgentServiceDescription.getName();
    }

    MapCell getCurrentCell() {
        return currentPosition;
    }
    
    void setCurrentCell(MapCell currentPosition) {
        this.currentPosition = currentPosition;
    }

    AID getOwnerAID() {
        return ownerTribe;
    }

    /// !NOTE
    @Override
    protected void setup() {
        logHandler = new ConsoleHandler();
        logHandler.setLevel(Level.FINER);

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
        gameOntology = GameOntology.getInstance();
        group1Ontology = Group1Ontology.getInstance();
        codec = new SLCodec();
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(gameOntology);
        getContentManager().registerOntology(group1Ontology);
        
        knownMap = GraphGameMap.getInstance(6, 6);

        startInformNewCellDiscoveryBehaviour();
        startInformOwnershipBehaviour(() -> {
            requestUnitPosition(MAX_REQUEST_POSITION_TRIES, () -> {
                handler.onUnitInitialized();
            });
        });
    }

    private void startCreateUnitBehaviour() {
        Action createUnitAction = new Action(getAID(), new CreateUnit());
        addBehaviour(new Conversation(this, gameOntology, codec, createUnitAction, GameOntology.CREATEUNIT) {
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

    private void startMoveToCellBehaviour() {

        Cell newCellPosition = new Cell();
        //TODO by default 0,0
        newCellPosition.setX(2);
        newCellPosition.setY(2);

        MoveToCell moveToCell = new MoveToCell();
        moveToCell.setTarget(newCellPosition);

        Action moveToCellAction = new Action(getAID(), moveToCell);
        addBehaviour(new Conversation(this, gameOntology, codec, moveToCellAction, GameOntology.MOVETOCELL) {

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

    private void startInformNewCellDiscoveryBehaviour() {
        Action informNewCellDiscoveryAction = new Action(getAID(), new NotifyNewCellDiscovery());
        addBehaviour(new Conversation(this, gameOntology, codec, informNewCellDiscoveryAction, GameOntology.NOTIFYNEWCELLDISCOVERY) {
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

                                if (conc instanceof NotifyNewCellDiscovery) {
                                    log(Level.FINER, "receive NotifyNewCellDiscovery inform from "
                                            + response.getSender().getLocalName());

                                    NotifyNewCellDiscovery newCellInfo = (NotifyNewCellDiscovery) conc;

                                    

                                    boolean success = knownMap
                                            .addCell(MapCellFactory.getInstance()
                                            .buildCell(newCellInfo.getNewCell()));
                                    if (success) {
                                        log(Level.FINER, "Cell discovery at "
                                            + newCellInfo.getNewCell().getX()
                                            + ","
                                            + newCellInfo.getNewCell().getY());
                                    }
                                    else {
                                        log(Level.FINER, "Already knew cell at "
                                            + newCellInfo.getNewCell().getX()
                                            + ","
                                            + newCellInfo.getNewCell().getY());
                                    }
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

    private void startInformOwnershipBehaviour(OnReceivedOwnershipHandler handler) {
        Action informOwnershipAction = new Action(getAID(), new NotifyUnitOwnership());
        addBehaviour(new Conversation(this, group1Ontology, codec, informOwnershipAction, Group1Ontology.NOTIFYUNITOWNERSHIP) {
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
        addBehaviour(new Conversation(this, group1Ontology, codec, whereAmIAction, Group1Ontology.WHEREAMI) {
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

    void log(Level logLevel, String message) {
        String compMsg = getLocalName() + ": " + message;
        if (logHandler.isLoggable(new LogRecord(logLevel, compMsg))) {
            System.out.println(compMsg);
        }
    }

    private void startStrategy() {
        StrategicUnitBehaviour unitBehaviour = new StrategicUnitBehaviour(this);
        unitBehaviour.addStrategy(new CreateUnitStrategy(this, eventDispatcher));
        addBehaviour(unitBehaviour);
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
