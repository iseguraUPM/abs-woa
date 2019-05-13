package es.upm.woa.agent.group1;

// TODO: change header
/**
 * ***************************************************************
 * Agent offering the painting service: AgPainter.java
 *
 ****************************************************************
 */
import es.upm.woa.agent.group1.world.WoaGUIWrapper;
import es.upm.woa.agent.group1.world.TribeInfomationBroker;
import es.upm.woa.agent.group1.world.UnitMovementInformer;
import es.upm.woa.agent.group1.world.CreateUnitBehaviourHelper;
import es.upm.woa.agent.group1.world.MoveUnitBehaviourHelper;
import es.upm.woa.agent.group1.world.CreateBuildingBehaviourHelper;
import es.upm.woa.agent.group1.map.WorldMapConfigurator;
import es.upm.woa.agent.group1.gui.WoaGUI;
import es.upm.woa.agent.group1.gui.WoaGUIFactory;
import es.upm.woa.agent.group1.map.GameMap;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.agent.group1.protocol.Transaction;
import es.upm.woa.agent.group1.protocol.WoaCommunicationStandard;
import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.NotifyCellDetail;
import es.upm.woa.ontology.NotifyUnitPosition;

import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

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
        CreateBuildingBehaviourHelper.KnownPositionInformer
        , StartGameInformer
        , CreateUnitBehaviourHelper.UnitCreator
        , UnitMovementInformer
        , TribeInfomationBroker {

    public static final String WORLD = "WORLD";

    private static final int STARTING_UNIT_NUMBER = 3;

    private static final long serialVersionUID = 1L;
    private CommunicationStandard woaComStandard;

    private Collection<Tribe> tribeCollection;
    private GameMap worldMap;
    private WoaGUIWrapper guiEndpoint;
    private WorldMapConfigurator woaConfigurator;
    private TribeResources initialTribeResources;

    // TODO: temporal solution before registration
    private List<String> startingTribeNames;

    private Collection<Transaction> activeTransactions;

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
            
            startingTribeNames = new ArrayList<>();
            startingTribeNames.add("TribeA");
            startingTribeNames.add("TribeB");
            startingTribeNames.add("TribeC");
            startingTribeNames.add("TribeD");
            startingTribeNames.add("TribeE");
            startingTribeNames.add("TribeF");
            
            
        } catch (FIPAException ex) {
            log(Level.SEVERE, "could not register in the DF (" + ex + ")");
        }
    }

    private void initializeWorld() {
        woaComStandard = new WoaCommunicationStandard();
        woaComStandard.register(getContentManager());
        
        tribeCollection = new HashSet<>();
        activeTransactions = new ArrayList<>();

        connectToGuiEndpoint();
        
        try {
            woaConfigurator = WorldMapConfigurator
                    .getInstance();
            
            log(Level.INFO, "Generating world map...");
            worldMap = woaConfigurator.generateWorldMap();
            
            initialTribeResources = woaConfigurator.getInitialResources();
    
        } catch (ConfigurationException ex) {
            log(Level.SEVERE, "Could not load the configuration");
        }
        
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

    private void launchAgentRegistrationDesk() {
        try {
            ContainerController cc = getContainerController();
            AgRegistrationDesk agRegistrationDesk
                    = new AgRegistrationDesk(startingTribeNames
                            , initialTribeResources, tribeCollection, this);
            AgentController ac = cc.acceptNewAgent("Registartion Desk", agRegistrationDesk);
            ac.start();
        } catch (StaleProxyException ex) {
            log(Level.WARNING, "could not launch tribe Registration Desk (" + ex
                    + ")");
        }
    }

    private void launchInitialTribeUnits(MapCell townHallCell, Tribe tribe) {
        for (int i = 0; i < STARTING_UNIT_NUMBER; i++) {
            launchNewAgentUnit(townHallCell, tribe
                    , new CreateUnitBehaviourHelper.OnCreatedUnitHandler() {
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
        tribe = tribeCollection.parallelStream()
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
    public void launchNewAgentUnit(MapCell startingPosition, Tribe ownerTribe
            , CreateUnitBehaviourHelper.OnCreatedUnitHandler handler) {
        try {
            ContainerController cc = getContainerController();
            AgUnit newUnit = new AgUnit();
            AgentController ac = cc.acceptNewAgent(generateNewUnitName(ownerTribe), newUnit);
            ac.start();

            Unit newUnitRef = new Unit(newUnit.getAID(),
                     startingPosition.getXCoord(), startingPosition.getYCoord());

            if (!ownerTribe.createUnit(newUnitRef)) {
                ac.kill();
                handler.onCouldNotCreateUnit();
            } else {
                handler.onCreatedUnit(newUnitRef);
            }

        } catch (StaleProxyException ex) {
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
                    + cell.getXCoord()+ "," + cell.getYCoord());
        } catch (NoSuchElementException ex) {
            log(Level.FINER, ownerTribe.getAID().getLocalName()
                    + " discovered cell "
                    + cell.getXCoord()+ "," + cell.getYCoord());
            addNewlyExploredCell(cell, exploredTribeCells);
        }
    }

    private void addNewlyExploredCell(MapCell exploredCell, GameMap exploredTribeCells) {
        Cell ontologyCell = new Cell();
        ontologyCell.setX(exploredCell.getXCoord());
        ontologyCell.setY(exploredCell.getYCoord());
        ontologyCell.setContent(exploredCell.getContent());

        List<AID> receipts = new ArrayList<>();
        tribeCollection.forEach((targetTribe) -> {
            try {
                targetTribe.getKnownMap().getCellAt(exploredCell.getXCoord()
                        , exploredCell.getYCoord());
                // Already knows cell
            }
            catch (NoSuchElementException ex) {
                exploredTribeCells.addCell(exploredCell);
                receipts.add(targetTribe.getAID());
                targetTribe.getUnits()
                        .forEach(u -> receipts.add(u.getId()));
                multicastNotifyCellDetail(receipts
                        .toArray(new AID[receipts.size()]), ontologyCell);
            }
        });
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
                targetTribe.getKnownMap().getCellAt(updatedCell.getXCoord()
                        , updatedCell.getYCoord());
                receipts.add(targetTribe.getAID());
                targetTribe.getUnits()
                        .forEach(u -> receipts.add(u.getId()));
                multicastNotifyCellDetail(receipts
                        .toArray(new AID[receipts.size()]), ontologyCell);
            }
            catch (NoSuchElementException ex) {
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
                targetTribe.getKnownMap().getCellAt(position.getXCoord()
                        , position.getYCoord());
                receipts.add(targetTribe.getAID());
                targetTribe.getUnits()
                        .forEach(u -> receipts.add(u.getId()));
                broadcastNotifyUnitPosition(receipts
                        .toArray(new AID[receipts.size()]), ownerTribe, ontologyCell);
            }
            catch (NoSuchElementException ex) {
                // Tribe does not know cell
            }
        });
    }
    
    private void broadcastNotifyUnitPosition(AID[] receipts, Tribe ownerTribe, Cell position) {
        NotifyUnitPosition notifyUnitPosition = new NotifyUnitPosition();

        notifyUnitPosition.setTribeId(ownerTribe.getAID().getLocalName());
        notifyUnitPosition.setCell(position);
        
        Action action = new Action(getAID(), notifyUnitPosition);

        addBehaviour(new Conversation(this, woaComStandard, GameOntology.NOTIFYUNITPOSITION) {
            @Override
            public void onStart() {
                sendMessage(receipts, ACLMessage.INFORM, action, new Conversation.SentMessageHandler() {
                });
            }
        });
    }
    
    @Override
    public void log(Level logLevel, String message) {
        logger.log(logLevel, message);
    }

    @Override
    public void startGame() {
        tribeCollection.forEach((Tribe tribe) -> {
            try {
                MapCell startingCell = woaConfigurator
                        .getNewTribeInitialCell(worldMap, tribe.getAID());
                launchInitialTribeUnits(startingCell, tribe);

                initializeTribe(tribe, initialTribeResources, startingCell);
            } catch (ConfigurationException ex) {
                log(Level.SEVERE, "Could not launch tribes");
            }
        });


        try {
            guiEndpoint.apiStartGame(startingTribeNames.toArray(new String[startingTribeNames.size()]),
                    woaConfigurator.getMapConfigurationContents());
            tribeCollection.parallelStream().forEach((Tribe tribe) -> {
                tribe.getUnits().forEach((unit) -> {
                    guiEndpoint.apiCreateAgent(tribe.getAID().getLocalName()
                            , unit.getId().getLocalName(), unit.getCoordX()
                            , unit.getCoordY());
                });
            });
        } catch (IOException ex) {
            log(Level.WARNING, "Could not load configuration to the GUI"
                    + " endpoint");
        }

        startWorldBehaviours();
    }

    private void startWorldBehaviours() {
        new CreateUnitBehaviourHelper(this, woaComStandard
                , worldMap, activeTransactions, guiEndpoint, this, this, this)
                .startUnitCreationBehaviour();
        new MoveUnitBehaviourHelper(this, woaComStandard, guiEndpoint
                , worldMap, activeTransactions, this, this).startMoveToCellBehaviour();
        new CreateBuildingBehaviourHelper(this, woaComStandard, guiEndpoint
                , worldMap, activeTransactions, this, this)
                .startBuildingCreationBehaviour();
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
     * Sends an inform with the initial resources
     * to every tribe that has been registered
     */
    private void initializeTribe(Tribe tribe, TribeResources initialTribeResources
            , MapCell initialMapCell) {
        new SendInformInitializeTribeHelper(this, woaComStandard
                , tribe.getAID(), initialTribeResources, tribe.getUnits(), initialMapCell)
                .initializeTribe();
    }

}
