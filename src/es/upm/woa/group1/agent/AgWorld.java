package es.upm.woa.group1.agent;

// TODO: change header
/**
 * ***************************************************************
 * Agent offering the painting service: AgPainter.java
 *
 ****************************************************************
 */
import es.upm.woa.group1.WoaDefinitions;
import es.upm.woa.group1.WoaLogger;
import es.upm.woa.group1.agent.world.WoaGUIWrapper;
import es.upm.woa.group1.agent.world.TribeInfomationBroker;
import es.upm.woa.group1.agent.world.UnitMovementInformer;
import es.upm.woa.group1.agent.world.CreateUnitBehaviourHelper;
import es.upm.woa.group1.agent.world.MoveUnitBehaviourHelper;
import es.upm.woa.group1.agent.world.CreateBuildingBehaviourHelper;
import es.upm.woa.group1.WoaConfigurator;
import es.upm.woa.group1.agent.world.ExploitResourceBehaviourHelper;
import es.upm.woa.group1.agent.world.GameOverConversation;
import es.upm.woa.group1.agent.world.GameOverResource;
import es.upm.woa.group1.agent.world.KnownPositionInformer;
import es.upm.woa.group1.gui.WoaGUI;
import es.upm.woa.group1.gui.WoaGUIFactory;
import es.upm.woa.group1.map.GameMap;
import es.upm.woa.group1.map.MapCell;
import es.upm.woa.group1.protocol.CommunicationStandard;
import es.upm.woa.group1.protocol.Conversation;
import es.upm.woa.group1.protocol.DelayTickBehaviour;
import es.upm.woa.group1.protocol.Transaction;
import es.upm.woa.group1.protocol.WoaCommunicationStandard;

import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.NotifyCellDetail;
import es.upm.woa.ontology.NotifyUnitPosition;

import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.ConsoleHandler;

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
public class AgWorld extends WoaAgent implements
        KnownPositionInformer,
        StartGameInformer,
        CreateUnitBehaviourHelper.UnitCreator,
        UnitMovementInformer,
        TribeInfomationBroker,
        TransactionRecord {

    public static final String WORLD = "WORLD";

    private static final int STARTING_UNIT_NUMBER = 3;
    private static final int CLEAR_TRANSACTION_TIME_MILLIS = 1000;
    private static final int WAIT_BEFORE_STOP_BEHAVIOURS_MILLIS = 5000;

    private static final long serialVersionUID = 1L;
    private CommunicationStandard woaComStandard;

    private Collection<Tribe> tribeCollection;
    private GameMap worldMap;
    private WoaGUIWrapper guiEndpoint;
    private WoaConfigurator woaConfigurator;
    private TribeResources initialTribeResources;

    private Collection<Transaction> activeTransactions;
    private Collection<Behaviour> worldBehaviours;
    private Collection<AgentController> worldUnits;

    private AgentController agentRegistrationDesk;
    private boolean finalizing;

    private WoaLogger logger;

    @Override
    protected void setup() {
        logger = new WoaLogger(getAID(), new ConsoleHandler());
        logger.setLevel(Level.FINE);
        log(Level.INFO, "has entered the system");

        initializeAgent();
        initializeWorld();

        launchAgentRegistrationDesk();
    }

    @Override
    public void addBehaviour(Behaviour behaviour) {
        if (behaviour instanceof GameOverResource) {
            worldBehaviours.add(behaviour);
            super.addBehaviour(behaviour);
        } else if (!finalizing) {
            worldBehaviours.add(behaviour);
            super.addBehaviour(behaviour);
        }
    }

    @Override
    protected void takeDown() {
        log(Level.INFO, "Taking down...");
        worldUnits.forEach((AgentController unit) -> {
            try {
                unit.kill();
            } catch (StaleProxyException ex) {
                try {
                    log(Level.WARNING, "Could not terminate " + unit.getName());
                } catch (StaleProxyException ex1) {
                    log(Level.WARNING, "Could not terminate unit");
                }
            }
        });
        worldUnits.clear();
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
            log(Level.SEVERE, "could not register in the DF (" + ex + ")");
        }
    }

    private void initializeWorld() {
        finalizing = false;

        woaComStandard = new WoaCommunicationStandard();
        woaComStandard.register(getContentManager());

        tribeCollection = new HashSet<>();
        activeTransactions = new ArrayList<>();
        worldBehaviours = new ArrayList<>();
        worldUnits = new ArrayList<>();

        connectToGuiEndpoint();

        try {
            woaConfigurator = WoaConfigurator
                    .getInstance();

            log(Level.INFO, "Generating world map...");
            worldMap = woaConfigurator.generateWorldMap();

            initialTribeResources = woaConfigurator.getInitialResources();

        } catch (ConfigurationException ex) {
            log(Level.SEVERE, "Could not load the configuration");
        }

    }

    protected void finalizeGame() {
        log(Level.WARNING, "Finalizing game...");
        deregisterAgent();
        cleanupActiveTransactions();
        rollbackUnfinishedTransactions();
        new SendInformEndOfGameHelper(this, woaComStandard, getAllRegisteredAgentsAIDs())
                .informEnfOfGame();
        finalizing = true;
        stopGameBehaviours();
        guiEndpoint.endGame();
    }

    private Collection<AID> getAllRegisteredAgentsAIDs() {
        List<AID> agentsCollection = new ArrayList<>();

        tribeCollection.stream().map((currentTribe) -> {
            agentsCollection.add(currentTribe.getAID());
            return currentTribe.getUnits();
        }).forEach((tribeUnits) -> {
            tribeUnits.stream().forEach((currentUnit) -> {
                agentsCollection.add(currentUnit.getId());
            });
        });

        return agentsCollection;
    }

    private void rollbackUnfinishedTransactions() {
        log(Level.WARNING, "Rolling back active transactions...");
        activeTransactions.forEach(t -> t.rollback());
        activeTransactions.clear();
    }

    private void deregisterAgent() {
        try {
            DFService.deregister(this);
        } catch (FIPAException ex) {
            log(Level.WARNING, "could not deregister in the DF (" + ex + ")");
        }
    }

    private void launchAgentRegistrationDesk() {
        try {
            ContainerController cc = getContainerController();
            AgRegistrationDesk agRegistrationDesk
                    = new AgRegistrationDesk(initialTribeResources,
                            tribeCollection, this, woaConfigurator);
            agentRegistrationDesk = cc.acceptNewAgent("Registartion Desk", agRegistrationDesk);
            agentRegistrationDesk.start();
        } catch (StaleProxyException ex) {
            log(Level.WARNING, "could not launch tribe Registration Desk (" + ex
                    + ")");
        }
    }

    private void launchInitialTribeUnits(MapCell townHallCell, Tribe tribe) {
        for (int i = 0; i < STARTING_UNIT_NUMBER; i++) {
            launchNewAgentUnit(townHallCell, tribe,
                    new CreateUnitBehaviourHelper.OnCreatedUnitHandler() {
                @Override
                public void onCreatedUnit(Unit unit) {
                    log(Level.FINE, "Created initial unit "
                            + unit.getId().getLocalName() + " at "
                            + townHallCell);
                }

                @Override
                public void onCouldNotCreateUnit() {
                    log(Level.WARNING, "could not launch initial unit for tribe"
                            + tribe.getAID().getLocalName());
                }
            });
        }
    }

    @Override
    public Tribe findOwnerTribe(AID requesterUnAid) {
        Optional<Tribe> tribe;
        tribe = tribeCollection.stream()
                .filter(currentTribe -> currentTribe.getUnit(requesterUnAid) != null).findAny();
        if (!tribe.isPresent()) {
            return null;
        } else {
            return tribe.get();
        }
    }

    @Override
    public Unit findUnit(Tribe ownerTribe, AID unitAID) {
        return ownerTribe.getUnit(unitAID);
    }

    @Override
    public void launchNewAgentUnit(MapCell startingPosition, Tribe ownerTribe,
            CreateUnitBehaviourHelper.OnCreatedUnitHandler handler) {
        try {
            Agent newUnit = getAgentClass(ownerTribe.getTribeNumber()).newInstance();
            ContainerController cc = getContainerController();
            AgentController ac = cc.acceptNewAgent(generateNewUnitName(ownerTribe), newUnit);
            worldUnits.add(ac);
            ac.start();

            Unit newUnitRef = new TribeUnit(newUnit.getAID(),
                    startingPosition.getXCoord(), startingPosition.getYCoord());

            if (!ownerTribe.createUnit(newUnitRef)) {
                ac.kill();
                handler.onCouldNotCreateUnit();
            } else {
                handler.onCreatedUnit(newUnitRef);
            }

        } catch (StaleProxyException ex) {
            handler.onCouldNotCreateUnit();
        } catch (InstantiationException | IllegalAccessException ex) {
            log(Level.SEVERE, "Could not instantiate agent unit class");
            handler.onCouldNotCreateUnit();
        }
    }

    private String generateNewUnitName(Tribe ownerTribe) {
        return ownerTribe.getUnitNamePrefix() + ownerTribe.getNumberUnits();
    }

    @Override
    public void processCellOfInterest(Tribe ownerTribe, MapCell cell) {
        GameMap exploredTribeCells = ownerTribe.getKnownMap();
        try {
            exploredTribeCells.getCellAt(cell.getXCoord(),
                    cell.getYCoord());
            log(Level.FINER, ownerTribe.getAID().getLocalName()
                    + " already knows cell "
                    + cell.getXCoord() + "," + cell.getYCoord());
        } catch (NoSuchElementException ex) {
            log(Level.FINER, ownerTribe.getAID().getLocalName()
                    + " discovered cell "
                    + cell.getXCoord() + "," + cell.getYCoord());
            addNewlyExploredCell(ownerTribe, cell, exploredTribeCells);
        }
    }

    private void addNewlyExploredCell(Tribe targetTribe, MapCell exploredCell, GameMap exploredTribeCells) {
        Cell ontologyCell = new Cell();
        ontologyCell.setX(exploredCell.getXCoord());
        ontologyCell.setY(exploredCell.getYCoord());
        ontologyCell.setContent(exploredCell.getContent());

        List<AID> receipts = new ArrayList<>();

        try {
            targetTribe.getKnownMap().getCellAt(exploredCell.getXCoord(),
                    exploredCell.getYCoord());
            // Already knows cell
        } catch (NoSuchElementException ex) {
            exploredTribeCells.addCell(exploredCell);
            receipts.add(targetTribe.getAID());
            targetTribe.getUnits()
                    .forEach(u -> receipts.add(u.getId()));
            multicastNotifyCellDetail(receipts
                    .toArray(new AID[receipts.size()]), ontologyCell);
        }
    }

    @Override
    public void informAboutKnownCellDetail(MapCell updatedCell) {
        Cell ontologyCell = new Cell();
        ontologyCell.setX(updatedCell.getXCoord());
        ontologyCell.setY(updatedCell.getYCoord());
        ontologyCell.setContent(updatedCell.getContent());

        List<AID> receipts = new ArrayList<>();
        tribeCollection.forEach((targetTribe) -> {
            try {
                targetTribe.getKnownMap().getCellAt(updatedCell.getXCoord(),
                        updatedCell.getYCoord());
                receipts.add(targetTribe.getAID());
                targetTribe.getUnits()
                        .forEach(u -> receipts.add(u.getId()));
                multicastNotifyCellDetail(receipts
                        .toArray(new AID[receipts.size()]), ontologyCell);
            } catch (NoSuchElementException ex) {
                // Tribe does not know cell
            }
        });
    }

    private void multicastNotifyCellDetail(AID[] receipts, Cell cell) {
        NotifyCellDetail notifyCellDetail = new NotifyCellDetail();
        notifyCellDetail.setNewCell(cell);

        Action action = new Action(getAID(), notifyCellDetail);
        addBehaviour(new Conversation(this, woaComStandard, GameOntology.NOTIFYCELLDETAIL) {
            @Override
            public void onStart() {
                sendMessage(receipts, ACLMessage.INFORM, action, new Conversation.SentMessageHandler() {
                });
            }
        });
    }

    @Override
    public void informAboutUnitPassby(Tribe ownerTribe, MapCell position) {
        Cell ontologyCell = new Cell();
        ontologyCell.setX(position.getXCoord());
        ontologyCell.setY(position.getYCoord());
        ontologyCell.setContent(position.getContent());

        List<AID> receipts = new ArrayList<>();
        tribeCollection.forEach((targetTribe) -> {
            try {
                targetTribe.getKnownMap().getCellAt(position.getXCoord(),
                        position.getYCoord());
                receipts.add(targetTribe.getAID());
                targetTribe.getUnits()
                        .forEach(u -> receipts.add(u.getId()));
                broadcastNotifyUnitPosition(receipts
                        .toArray(new AID[receipts.size()]), ownerTribe, ontologyCell);
            } catch (NoSuchElementException ex) {
                // Tribe does not know cell
            }
        });
    }

    private void broadcastNotifyUnitPosition(AID[] receipts, Tribe ownerTribe, Cell position) {
        /*NotifyUnitPosition notifyUnitPosition = new NotifyUnitPosition();

        notifyUnitPosition.setTribeId(ownerTribe.getAID().getLocalName());
        notifyUnitPosition.setCell(position);

        Action action = new Action(getAID(), notifyUnitPosition);

        addBehaviour(new Conversation(this, woaComStandard, GameOntology.NOTIFYUNITPOSITION) {
            @Override
            public void onStart() {
                sendMessage(receipts, ACLMessage.INFORM, action, new Conversation.SentMessageHandler() {
                });
            }
        });*/
    }

    @Override
    public void log(Level logLevel, String message) {
        logger.log(logLevel, message);
    }

    @Override
    public void startGame() {
        try {
            agentRegistrationDesk.kill();
            agentRegistrationDesk = null;
        } catch (StaleProxyException ex) {
            log(Level.WARNING, "Could not terminate agent registration desk");
        }

        for (int i = 1; i <= 6; i++) {
            final int tribeNumber = i;
            Tribe registeredTribe = tribeCollection.stream()
                    .filter(tribe -> tribe.getTribeNumber() == tribeNumber).findAny()
                    .orElse(null);
            try {
                MapCell startingCell = woaConfigurator
                        .getNewTribeInitialCell(worldMap);
                if (registeredTribe != null) {
                    launchInitialTribeUnits(startingCell, registeredTribe);
                    initializeTribe(registeredTribe, initialTribeResources
                            , startingCell);
                }
            } catch (ConfigurationException ex) {
                log(Level.SEVERE, "Could not launch tribes");
            }
        }

        try {
            Collection<String> startingTribeNames = computeTribeNamesForGUI();
            guiEndpoint.startGame(startingTribeNames.toArray(new String[startingTribeNames.size()]),
                    woaConfigurator.getMapConfigurationContents());
            tribeCollection.forEach(tribe -> {
                tribe.getUnits().forEach(unit -> {
                    guiEndpoint.createAgent(tribe.getAID().getLocalName(),
                                unit.getId().getLocalName(), unit.getCoordX(),
                                unit.getCoordY());
                });
            });
        } catch (IOException ex) {
            log(Level.WARNING, "Could not load configuration to the GUI"
                    + " endpoint");
        }

        startWorldBehaviours();
    }

    protected Collection<String> computeTribeNamesForGUI() {
        Collection<String> startingTribeNames = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            final int tribeNumber = i;
            Tribe registeredTribe = tribeCollection.stream()
                    .filter(tribe -> tribe.getTribeNumber() == tribeNumber).findAny()
                    .orElse(null);
            if (registeredTribe == null) {
                startingTribeNames.add("Tribe" + tribeNumber);
            } else {
                startingTribeNames.add(registeredTribe.getAID().getLocalName());
            }
        }

        return startingTribeNames;
    }

    private void startWorldBehaviours() {
        startGameBehaviours();
        startGameOverBehaviour();
    }

    protected void startGameBehaviours() {
        worldBehaviours.add(new CreateUnitBehaviourHelper(this, woaComStandard,
                worldMap, this, guiEndpoint, this, this, this)
                .startUnitCreationBehaviour());
        worldBehaviours.add(new MoveUnitBehaviourHelper(this, woaComStandard, guiEndpoint,
                worldMap, this, this, this).startMoveToCellBehaviour());
        worldBehaviours.add(new CreateBuildingBehaviourHelper(this, woaComStandard, guiEndpoint,
                worldMap, woaConfigurator.getStoreUpgradeAmount(), this, this, this)
                .startBuildingCreationBehaviour());
        worldBehaviours.add(new ExploitResourceBehaviourHelper(this,
                woaComStandard, worldMap, this, guiEndpoint, this,
                this, this, this).startExploitResourcesBehaviour());
    }

    private void connectToGuiEndpoint() {
        guiEndpoint = new WoaGUIWrapper();
        try {
            WoaGUI woaGUI = WoaGUIFactory.getInstance().getGUI();
            guiEndpoint.setGUIEndpoint(woaGUI);
        } catch (IOException ex) {
            log(Level.WARNING, "Could not connect to GUI endpoint");
        }
    }

    /**
     * Sends an inform with the initial resources to every tribe that has been
     * registered
     */
    private void initializeTribe(Tribe tribe, TribeResources initialTribeResources,
            MapCell initialMapCell) {
        new SendInformInitializeTribeHelper(this, woaComStandard,
                tribe.getAID(), initialTribeResources, tribe.getUnits(),
                initialMapCell, worldMap, woaConfigurator.getResourceCap(),
                woaConfigurator.getStoreUpgradeAmount())
                .initializeTribe();
    }

    private Class<? extends Agent> getAgentClass(int tribeNumber) {
        String agentUnitClassPath = MessageFormat
                .format(WoaDefinitions.AGENT_CLASS_PATH_TEMPLATE, tribeNumber)
                .concat(WoaDefinitions.AGENT_UNIT_CLASS_NAME);
        try {
            return (Class<? extends Agent>) Class.forName(agentUnitClassPath);
        } catch (ClassNotFoundException ex) {
            if (tribeNumber != 1) {
                log(Level.WARNING, "Could not find agent unit class "
                        + agentUnitClassPath
                        + ". Using group 1 agent units instead");
                log(Level.INFO, "");
                return getAgentClass(1);
            } else {
                log(Level.SEVERE, "Could not find agent unit class " + agentUnitClassPath);
                return null;
            }
        }
    }

    private void startGameOverBehaviour() {
        int gameTime = woaConfigurator.getGameTicks();

        addBehaviour(new GameOverBehaviour(this, gameTime) {

            @Override
            protected void handleElapsedTimeout() {
                addBehaviour(new GameOverConversation(myAgent));
                finalizeGame();
            }
        });
    }

    protected void cleanupActiveTransactions() {
        activeTransactions.removeIf(transaction -> transaction.done());
    }

    private void stopGameBehaviours() {
        int tickDelta = woaConfigurator.getTickMillis();

        addBehaviour(new GameOverBehaviour(this,
                WAIT_BEFORE_STOP_BEHAVIOURS_MILLIS / tickDelta) {
            @Override
            protected void handleElapsedTimeout() {
                log(Level.WARNING, "Stopping game behaviours...");
                worldBehaviours.stream().forEach(behaviour -> removeBehaviour(behaviour));
                worldBehaviours.clear();
            }
        });
    }

    @Override
    public void addTransaction(Transaction newTransaction) {
        if (!finalizing) {
            cleanupActiveTransactions();
            activeTransactions.add(newTransaction);
        }
    }

    private abstract class GameOverBehaviour extends DelayTickBehaviour
            implements GameOverResource {

        public GameOverBehaviour(Agent a, long tickTimeout) {
            super(a, tickTimeout);
        }

    }

}
