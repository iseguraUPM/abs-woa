package es.upm.woa.agent.group1;

// TODO: change header
/**
 * ***************************************************************
 * Agent offering the painting service: AgPainter.java
 *
 ****************************************************************
 */
import es.upm.woa.agent.group1.gui.WoaGUI;
import es.upm.woa.agent.group1.gui.WoaGUIFactory;
import es.upm.woa.agent.group1.map.CellBuildingConstructor;
import es.upm.woa.agent.group1.map.GameMap;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.map.UnitCellPositioner;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.agent.group1.protocol.DelayedTransactionalBehaviour;
import es.upm.woa.agent.group1.protocol.Transaction;
import es.upm.woa.ontology.Building;
import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.CreateBuilding;
import es.upm.woa.ontology.CreateUnit;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.MoveToCell;
import es.upm.woa.ontology.NotifyCellDetail;
import es.upm.woa.ontology.NotifyNewUnit;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.onto.OntologyException;

import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

// TODO: change docs
/**
 * This agent has the following functionality:
 * <ul>
 * <li>TODO</li>
 * </ul>
 *
 * @author Iñaki Segura
 *
 */
public class AgWorld extends Agent {

    public static final String WORLD = "WORLD";

    private static final int STARTING_UNIT_NUMBER = 1;
    private static final int CREATE_UNIT_TICKS = 150;

    private static final long serialVersionUID = 1L;
    private Ontology ontology;
    private Codec codec;

    private Collection<Tribe> tribeCollection;
    private GameMap worldMap;
    private WoaGUIWrapper guiEndpoint;

    // TODO: temporal solution before registration
    private List<String> startingTribeNames;

    private Collection<Transaction> activeTransactions;

    private Handler logHandler;

    @Override
    protected void setup() {
        logHandler = new ConsoleHandler();
        logHandler.setLevel(Level.FINE);
        log(Level.INFO, "has entered the system");

        // TODO: temp
        startingTribeNames = new ArrayList<>();
        startingTribeNames.add("TribeA");
        startingTribeNames.add("TribeB");
        startingTribeNames.add("TribeC");
        startingTribeNames.add("TribeD");
        startingTribeNames.add("TribeE");
        startingTribeNames.add("TribeF");

        initializeAgent();
        if (!initializeWorld()) {
            try {
                getContainerController().kill();
            } catch (StaleProxyException ex) {
                log(Level.SEVERE, "Could not shut down agent properly");
            }
            return;
        }

        startUnitCreationBehaviour();
        startMoveToCellBehaviour();
        startTownHallCreationBehaviour();
    }

    private void initializeAgent() {
        try {
            // Creates its own description
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setName(this.getName());
            sd.setType(WORLD);
            dfd.addServices(sd);
            // Registers its description in the DF
            DFService.register(this, dfd);
            log(Level.INFO, "registered in the DF");
        } catch (FIPAException ex) {
            log(Level.WARNING, "could not register in the DF (" + ex + ")");
        }
    }

    private boolean initializeWorld() {
        ontology = GameOntology.getInstance();
        codec = new SLCodec();
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

        guiEndpoint = new WoaGUIWrapper();
        try {
            WoaGUI woaGUI = WoaGUIFactory.getInstance().getGUI();
            guiEndpoint.setGUIEndpoint(woaGUI);
        } catch (IOException ex) {
            log(Level.WARNING, "Could not connect to GUI endpoint");
        }

        WorldMapConfigurator configurator;
        try {
            configurator = WorldMapConfigurator
                    .getInstance();

            worldMap = configurator.generateWorldMap();
        } catch (ConfigurationException ex) {
            log(Level.SEVERE, "Could not load the configuration");
            return false;
        }

        tribeCollection = new HashSet<>();
        activeTransactions = new ArrayList<>();

        log(Level.INFO, "Starting game...");

        // TODO: temp
        final int MAX_TRIBES = 1;

        // TODO: not the right way to initiate the game. Should be after registering
        // all tribes and giving the resources.
        String[] tribeNames = startingTribeNames.subList(0, MAX_TRIBES).toArray(new String[MAX_TRIBES]);

        try {
            guiEndpoint.apiStartGame(tribeNames,
                     configurator.getMapConfigurationContents());
        } catch (IOException ex) {
            log(Level.SEVERE, "Could not load the map configuration");
            return false;
        }

        for (int i = 0; i < MAX_TRIBES; i++) {
            String tribeName = startingTribeNames.get(i);
            Tribe newTribe = launchAgentTribe(tribeName);
            if (newTribe != null) {
                try {
                    MapCell townHallCell = configurator.addNewTribe(worldMap,
                             newTribe.getAID());
                    handInitialTribeResources(townHallCell, newTribe);
                } catch (ConfigurationException ex) {
                    log(Level.SEVERE, "Could not add new tribe: "
                            + newTribe.getAID().getLocalName() + " (" + ex + ")");
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    protected void takeDown() {
        finalizeAgent();
        rollbackUnfinishedTransactions();
    }

    private void rollbackUnfinishedTransactions() {
        activeTransactions.forEach(t -> t.rollback());
        activeTransactions.clear();
    }

    private void finalizeAgent() {
        try {
            DFService.deregister(this);
        } catch (FIPAException ex) {
            log(Level.WARNING, "could not deregister in the DF (" + ex + ")");
        }
    }

    private void startUnitCreationBehaviour() {
        final Action createUnitAction = new Action(getAID(), null);
        addBehaviour(new Conversation(this, ontology, codec, createUnitAction, GameOntology.CREATEUNIT) {
            @Override
            public void onStart() {
                Action action = new Action(getAID(), new CreateUnit());

                listenMessages(new ResponseHandler() {
                    @Override
                    public void onRequest(ACLMessage message) {
                        log(Level.FINE, "received CreateUnit request from"
                                + message.getSender().getLocalName());

                        final Tribe ownerTribe = findOwnerTribe(message.getSender());
                        Unit requesterUnit = findUnit(ownerTribe, message.getSender());
                        if (ownerTribe == null || requesterUnit == null) {
                            respondMessage(message, ACLMessage.REFUSE);
                            return;
                        }

                        try {
                            MapCell unitPosition = worldMap.getCellAt(requesterUnit
                                    .getCoordX(), requesterUnit.getCoordY());

                            if (!canCreateUnit(ownerTribe, requesterUnit,
                                     unitPosition)) {
                                respondMessage(message, ACLMessage.REFUSE);
                            } else {
                                initiateUnitCreation(requesterUnit, ownerTribe, unitPosition, message);
                            }

                        } catch (NoSuchElementException ex) {
                            log(Level.WARNING, "Unit "
                                    + requesterUnit.getId().getLocalName() + " is at an unknown position");
                            respondMessage(message, ACLMessage.REFUSE);
                        }

                    }
                });
            }

            private void initiateUnitCreation(Unit requesterUnit, Tribe ownerTribe, MapCell unitPosition, ACLMessage message) {
                if (UnitCellPositioner.getInstance(worldMap).isMoving(requesterUnit)) {
                    log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " already moving. Cannot create unit");
                    respondMessage(message, ACLMessage.REFUSE);
                    return;
                }
                
                // NOTE: if the unit were building it means there is not a town
                // hall. Thus, the request would already be refused later and
                // checking whether is building or not is unnecessary.
                
                ownerTribe.purchaseUnit();
                respondMessage(message, ACLMessage.AGREE);
                DelayedTransactionalBehaviour activeTransaction
                        = new DelayedTransactionalBehaviour(myAgent, CREATE_UNIT_TICKS) {

                    boolean finished = false;

                    @Override
                    public boolean done() {
                        return finished;
                    }

                    @Override
                    public void commit() {
                        if (!finished) {
                            boolean success = launchNewAgentUnit(unitPosition, ownerTribe);
                            if (!success) {
                                ownerTribe.refundUnit();

                                respondMessage(message, ACLMessage.FAILURE);

                            } else {
                                respondMessage(message, ACLMessage.INFORM);
                            }
                        }

                        finished = true;
                    }

                    @Override
                    public void rollback() {
                        if (!finished) {
                            log(Level.INFO, "refunded unit to "
                                    + ownerTribe.getAID().getLocalName());
                            ownerTribe.refundUnit();
                            respondMessage(message, ACLMessage.FAILURE);
                        }
                        finished = true;
                    }
                };

                activeTransactions.add(activeTransaction);
                addBehaviour(activeTransaction);
            }
        });
    }

    private boolean canCreateUnit(Tribe tribe, Unit requester, MapCell requesterPosition) {

        if (UnitCellPositioner.getInstance(worldMap).isMoving(requester)) {
            return false;
        }

        return tribe.canAffordUnit() && thereIsATownHall(requesterPosition, tribe);
    }

    private boolean thereIsATownHall(MapCell position, Tribe tribe) {
        if (!(position.getContent() instanceof Building)) {
            return false;
        } else {
            Building building = (Building) position.getContent();
            if (!building.getOwner().equals(tribe.getAID())) {
                return false;
            } else {
                return building.getType().equals(WoaDefinitions.TOWN_HALL);
            }
        }
    }

    private Tribe launchAgentTribe(String tribeName) {
        try {
            ContainerController cc = getContainerController();
            AgTribe newTribe = new AgTribe();
            AgentController ac = cc.acceptNewAgent(tribeName, newTribe);
            ac.start();

            Tribe newTribeRef = new Tribe(newTribe.getAID());
            if (!tribeCollection.add(newTribeRef)) {
                ac.kill();
                return null;
            } else {
                return newTribeRef;
            }

        } catch (StaleProxyException ex) {
            log(Level.WARNING, "could not launch tribe " + tribeName + " (" + ex
                    + ")");
            return null;
        }
    }

    private void handInitialTribeResources(MapCell townHallCell, Tribe tribe) {
        for (int i = 0; i < STARTING_UNIT_NUMBER; i++) {
            launchNewAgentUnit(townHallCell, tribe);
        }
    }

    private Tribe findOwnerTribe(AID requesterUnAid) {
        Optional<Tribe> tribe;
        tribe = tribeCollection.stream().filter(currentTribe -> currentTribe.getUnit(requesterUnAid) != null).findAny();
        if (!tribe.isPresent()) {
            return null;
        } else {
            return tribe.get();
        }
    }

    private Unit findUnit(Tribe ownerTribe, AID unitAID) {
        return ownerTribe.getUnit(unitAID);
    }

    private boolean launchNewAgentUnit(MapCell startingPosition, Tribe ownerTribe) {
        try {
            ContainerController cc = getContainerController();
            AgUnit newUnit = new AgUnit();
            AgentController ac = cc.acceptNewAgent(generateNewUnitName(ownerTribe), newUnit);
            ac.start();

            Unit newUnitRef = new Unit(newUnit.getAID(),
                     startingPosition.getXCoord(), startingPosition.getYCoord());

            if (!ownerTribe.createUnit(newUnitRef)) {
                ac.kill();
                return false;
            } else {
                informTribeAboutNewUnit(ownerTribe, newUnitRef);
                guiEndpoint.apiCreateAgent(ownerTribe.getAID().getLocalName(),
                         newUnitRef.getId().getLocalName(), newUnitRef.getCoordX(),
                         newUnitRef.getCoordY());
                return true;
            }

        } catch (StaleProxyException ex) {
            log(Level.WARNING, "could not launch new unit (" + ex
                    + ")");
            return false;
        }
    }

    private String generateNewUnitName(Tribe ownerTribe) {
        return ownerTribe.getUnitNamePrefix() + ownerTribe.getNumberUnits();
    }

    private void informTribeAboutNewUnit(Tribe ownerTribe, Unit newUnit) {

        NotifyNewUnit notifyNewUnit = new NotifyNewUnit();
        Cell cell = new Cell();
        cell.setX(newUnit.getCoordX());
        cell.setY(newUnit.getCoordY());

        //TODO this shouldn't be mandatory
        notifyNewUnit.setLocation(cell);
        notifyNewUnit.setNewUnit(newUnit.getId());

        Action informNewUnitAction = new Action(ownerTribe.getAID(), notifyNewUnit);
        addBehaviour(new Conversation(this, ontology, codec, informNewUnitAction, GameOntology.NOTIFYNEWUNIT) {
            @Override
            public void onStart() {
                sendMessage(ownerTribe.getAID(), ACLMessage.INFORM, new SentMessageHandler() {

                });
            }
        });

        try {
            MapCell discoveredCell = worldMap.getCellAt(cell.getX(), cell.getY());
            cell.setContent(discoveredCell.getContent());
            processTribeKnownCell(ownerTribe, cell);
        } catch (NoSuchElementException ex) {
            log(Level.WARNING, "Unit in unknown starting position (" + ex + ")");
        }
    }

    private void processTribeKnownCell(Tribe ownerTribe, Cell cell) {
        GameMap exploredTribeCells = ownerTribe.getKnownMap();
        try {
            exploredTribeCells.getCellAt(cell.getX(),
                     cell.getY());
            log(Level.FINER, ownerTribe.getAID().getLocalName()
                    + " already knows cell "
                    + cell.getX() + "," + cell.getY());
        } catch (NoSuchElementException ex) {
            log(Level.FINER, ownerTribe.getAID().getLocalName()
                    + " discovered cell "
                    + cell.getX() + "," + cell.getY());
            addNewlyExploredCell(cell, exploredTribeCells, ownerTribe);
        }
    }

    private void addNewlyExploredCell(Cell cell, GameMap exploredTribeCells, Tribe ownerTribe) {
        try {
            MapCell exploredCell = worldMap.getCellAt(cell.getX(), cell.getY());
            exploredTribeCells.addCell(exploredCell);
            informTribeAboutDiscoveredCell(ownerTribe, cell);
            for (Unit tribeUnit : ownerTribe.getUnitsIterable()) {
                log(Level.FINER, tribeUnit.getId().getLocalName()
                        + " was informed of cell "
                        + cell.getX() + "," + cell.getY());
                informUnitAboutDiscoveredCell(tribeUnit, cell);
            }
        } catch (NoSuchElementException e) {
        }
    }

    private void informTribeAboutDiscoveredCell(Tribe ownerTribe, Cell newCell) {

        NotifyCellDetail notifyNewCellDiscovery = new NotifyCellDetail();

        notifyNewCellDiscovery.setNewCell(newCell);

        Action informNewCellDiscoveryAction = new Action(ownerTribe.getAID(), notifyNewCellDiscovery);
        addBehaviour(new Conversation(this, ontology, codec, informNewCellDiscoveryAction, GameOntology.NOTIFYCELLDETAIL) {
            @Override
            public void onStart() {
                sendMessage(ownerTribe.getAID(), ACLMessage.INFORM, new SentMessageHandler() {
                });
            }
        });
    }

    private void informUnitAboutDiscoveredCell(Unit ownerUnit, Cell newCell) {
        NotifyCellDetail notifyNewCellDiscovery = new NotifyCellDetail();

        notifyNewCellDiscovery.setNewCell(newCell);

        Action informNewCellDiscoveryAction = new Action(ownerUnit.getId(), notifyNewCellDiscovery);
        addBehaviour(new Conversation(this, ontology, codec, informNewCellDiscoveryAction, GameOntology.NOTIFYCELLDETAIL) {
            @Override
            public void onStart() {
                sendMessage(ownerUnit.getId(), ACLMessage.INFORM, new SentMessageHandler() {
                });
            }
        });
    }

    private void startMoveToCellBehaviour() {
        final Action moveToCellAction = new Action(getAID(), null);
        addBehaviour(new Conversation(this, ontology, codec, moveToCellAction, GameOntology.MOVETOCELL) {
            @Override
            public void onStart() {

                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onRequest(ACLMessage message) {
                        log(Level.FINE, "received unit MoveToCell"
                                + " request from " + message.getSender()
                                        .getLocalName());

                        final Tribe ownerTribe = findOwnerTribe(message.getSender());
                        Unit requesterUnit = findUnit(ownerTribe, message.getSender());

                        if (ownerTribe == null || requesterUnit == null) {
                            respondMessage(message, ACLMessage.REFUSE);
                            return;
                        }
                        
                        

                        try {
                            ContentElement ce = getContentManager().extractContent(message);
                            Action agAction = (Action) ce;
                            Concept conc = agAction.getAction();
                            MoveToCell targetCell = (MoveToCell) conc;

                            MapCell mapCell = worldMap.getCellAt(targetCell
                                    .getTarget().getX(), targetCell.getTarget().getY());
                            
                            initiateMoveToCell(requesterUnit, mapCell, moveToCellAction, message);

                        } catch (NoSuchElementException ex) {
                            log(Level.WARNING, "Unit "
                                    + requesterUnit.getId().getLocalName() + " is at an unknown position");
                            respondMessage(message, ACLMessage.REFUSE);
                        } catch (Codec.CodecException | OntologyException ex) {
                            log(Level.WARNING, "could not receive message (" + ex + ")");
                            respondMessage(message, ACLMessage.NOT_UNDERSTOOD);
                        }

                    }
                });
            }

            private void initiateMoveToCell(Unit requesterUnit, MapCell mapCell, Action action, ACLMessage message) {
                UnitCellPositioner unitPositioner = UnitCellPositioner.getInstance(worldMap);
                if (unitPositioner.isMoving(requesterUnit)) {
                    log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " already moving. Cannot move again");
                    respondMessage(message, ACLMessage.REFUSE);
                    return;
                }
                
                if (CellBuildingConstructor.getInstance()
                        .isBuilding(requesterUnit)) {
                    log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " is currently building. Current construction"
                                    + " will be cancelled");
                    requesterUnit.rollbackCurrentTransaction();
                    respondMessage(message, ACLMessage.REFUSE);
                    return;
                }

                try {
                    Transaction moveTransaction = unitPositioner.move(myAgent,
                             requesterUnit, mapCell, new UnitCellPositioner.UnitMovementHandler() {
                        @Override
                        public void onMove() {
                            Tribe ownerTribe = findOwnerTribe(requesterUnit.getId());

                            Cell newCell = new Cell();
                            newCell.setX(mapCell.getXCoord());
                            newCell.setY(mapCell.getYCoord());
                            newCell.setContent(mapCell.getContent());

                            MoveToCell moveToCellAction = new MoveToCell();
                            moveToCellAction.setTarget(newCell);

                            action.setAction(moveToCellAction);

                            respondMessage(message, ACLMessage.INFORM);
                            guiEndpoint.apiMoveAgent(requesterUnit.getId()
                                    .getLocalName(), mapCell.getXCoord(),
                                     mapCell.getYCoord());

                            processTribeKnownCell(ownerTribe, newCell);
                        }

                        @Override
                        public void onCancel() {
                            respondMessage(message, ACLMessage.FAILURE);
                        }
                    });

                    respondMessage(message, ACLMessage.AGREE);
                    activeTransactions.add(moveTransaction);

                } catch (IndexOutOfBoundsException ex) {
                    log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " cannot move to cell " + mapCell.getXCoord()
                            + ", " + mapCell.getYCoord() + "(" + ex + ")");
                    respondMessage(message, ACLMessage.REFUSE);
                }
            }
        });
    }


    private void startTownHallCreationBehaviour() {
        final Action createBuildingAction = new Action(getAID(), null);
        addBehaviour(new Conversation(this, ontology, codec, createBuildingAction, GameOntology.CREATEBUILDING) {
            @Override
            public void onStart() {
                Action action = new Action(getAID(), new CreateBuilding());

                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onRequest(ACLMessage message) {
                        log(Level.FINE, "received CreateBuilding request from"
                                + message.getSender().getLocalName());

                        final Tribe ownerTribe = findOwnerTribe(message.getSender());
                        Unit requesterUnit = findUnit(ownerTribe, message.getSender());
                        if (ownerTribe == null || requesterUnit == null) {
                            respondMessage(message, ACLMessage.REFUSE);
                            return;
                        }

                        try {
                            ContentElement ce = getContentManager().extractContent(message);
                            Action agAction = (Action) ce;
                            Concept conc = agAction.getAction();
                            CreateBuilding createBuilding = (CreateBuilding) conc;

                            String buildingType = createBuilding.getBuildingType();

                            MapCell unitPosition = worldMap.getCellAt(requesterUnit
                                    .getCoordX(), requesterUnit.getCoordY());

                            if (!canCreateBuilding(buildingType
                                    , ownerTribe, requesterUnit)) {
                                respondMessage(message, ACLMessage.REFUSE);
                            } else {
                                initiateBuildingCreation(buildingType,
                                         ownerTribe, requesterUnit,
                                         unitPosition, message);
                            }

                        } catch (NoSuchElementException ex) {
                            log(Level.WARNING, "Unit "
                                    + requesterUnit.getId().getLocalName()
                                    + " is at an unknown position");
                            respondMessage(message, ACLMessage.REFUSE);
                        } catch (Codec.CodecException | OntologyException ex) {
                            log(Level.WARNING, "could not receive message (" + ex + ")");
                            respondMessage(message, ACLMessage.NOT_UNDERSTOOD);
                        }

                    }
                });
            }

            private void initiateBuildingCreation(String buildingType, Tribe ownerTribe,
                     Unit requesterUnit, MapCell unitPosition, ACLMessage message) {
                CellBuildingConstructor buildingConstructor = CellBuildingConstructor.getInstance();
                
                if (buildingConstructor.isBuilding(requesterUnit)) {
                    log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " is currently building. Current construction"
                                    + " will be cancelled");
                    requesterUnit.rollbackCurrentTransaction();
                    respondMessage(message, ACLMessage.REFUSE);
                    return;
                }
                
                try {
                    Transaction buildTransaction = buildingConstructor.build(myAgent, ownerTribe, requesterUnit,
                             buildingType, unitPosition, new CellBuildingConstructor.BuildingConstructionHandler() {
                        @Override
                        public void onBuilt() {
                            guiEndpoint.apiCreateBuilding(ownerTribe.getAID()
                                    .getLocalName(), buildingType);
                            respondMessage(message, ACLMessage.INFORM);
                        }

                        @Override
                        public void onCancel() {
                            refundBuilding(buildingType, ownerTribe);
                            respondMessage(message, ACLMessage.FAILURE);
                        }

                       
                    });

                    ownerTribe.purchaseTownHall();
                    respondMessage(message, ACLMessage.AGREE);
                    requesterUnit.setCurrentTransaction(buildTransaction);
                    activeTransactions.add(buildTransaction);
                } catch (CellBuildingConstructor.CellOccupiedException ex) {
                    log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " cannot build on cell " + unitPosition.getXCoord()
                            + ", " + unitPosition.getYCoord() + "(" + ex + ")");
                    respondMessage(message, ACLMessage.REFUSE);
                } catch (CellBuildingConstructor.UnknownBuildingTypeException ex) {
                    log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " cannot build on cell " + unitPosition.getXCoord()
                            + ", " + unitPosition.getYCoord() + "(" + ex + ")");
                    respondMessage(message, ACLMessage.NOT_UNDERSTOOD);
                }
            }
        });
    }

    private boolean canCreateBuilding(String buildingType, Tribe tribe, Unit requester) {
        if (UnitCellPositioner.getInstance(worldMap).isMoving(requester)) {
            log(Level.FINE, requester.getId().getLocalName()
                            + " cannot build while moving");
            return false;
        }

        return canAffordBuilding(buildingType, tribe);
    }
    
    private boolean canAffordBuilding(String buildingType, Tribe ownerTribe) {
        switch (buildingType) {
            case WoaDefinitions.TOWN_HALL:
                return ownerTribe.canAffordTownHall();
            default:
                log(Level.WARNING, "Unknown building type: " + buildingType);
                return false;
        }
    }
    
    private void refundBuilding(String buildingType, Tribe ownerTribe) {
        switch (buildingType) {
            case WoaDefinitions.TOWN_HALL:
                ownerTribe.refundTownHall();
                break;
            default:
                log(Level.WARNING, "Unknown building type: " + buildingType);
        }
    }
    
    
    private void log(Level logLevel, String message) {
        String compMsg = getLocalName() + ": " + message;
        if (logHandler.isLoggable(new LogRecord(logLevel, compMsg))) {
            System.out.println(compMsg);
        }
    }

}
