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
import es.upm.woa.agent.group1.map.GameMap;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.agent.group1.protocol.Transaction;
import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.NotifyCellDetail;
import es.upm.woa.ontology.NotifyNewUnit;
import es.upm.woa.ontology.NotifyUnitPosition;

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
    
    /// NOTE: this methods must be package-private
    
    GameMap getWorldMap() {
        return worldMap;
    }

    Ontology getOntology() {
        return ontology;
    }

    Codec getCodec() {
        return codec;
    }
    
    Collection<Transaction> getActiveTransactions() {
        return activeTransactions;
    }
    
    WoaGUI getGUIEndpoint() {
        return guiEndpoint;
    }

    /// !NOTE

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

        new AgWorldUnitCreationHelper(this).startUnitCreationBehaviour();
        new AgWorldUnitPositionerHelper(this).startMoveToCellBehaviour();
        new AgWorldBuildingCreatorHelper(this).startBuildingCreationBehaviour();
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

    Tribe findOwnerTribe(AID requesterUnAid) {
        Optional<Tribe> tribe;
        tribe = tribeCollection.stream().filter(currentTribe -> currentTribe.getUnit(requesterUnAid) != null).findAny();
        if (!tribe.isPresent()) {
            return null;
        } else {
            return tribe.get();
        }
    }

    Unit findUnit(Tribe ownerTribe, AID unitAID) {
        return ownerTribe.getUnit(unitAID);
    }

    boolean launchNewAgentUnit(MapCell startingPosition, Tribe ownerTribe) {
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

    void processTribeKnownCell(Tribe ownerTribe, Cell cell) {
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
            addNewlyExploredCell(cell, exploredTribeCells);
        }
    }

    private void addNewlyExploredCell(Cell cell, GameMap exploredTribeCells) {
        try {
            MapCell exploredCell = worldMap.getCellAt(cell.getX(), cell.getY());

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
                    targetTribe.getUnitsIterable()
                            .forEach(u -> receipts.add(u.getId()));
                    broadcastNotifyCellDetail(receipts
                            .toArray(new AID[receipts.size()]), ontologyCell);
                }
            });
        } catch (NoSuchElementException ex) {
            log(Level.WARNING, "Unknown cell at " + cell.getX() +"," + cell.getY());
        }
    }
    
    void informAboutKnownCellDetail(MapCell updatedCell) {
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
                targetTribe.getUnitsIterable()
                        .forEach(u -> receipts.add(u.getId()));
                broadcastNotifyCellDetail(receipts
                        .toArray(new AID[receipts.size()]), ontologyCell);
            }
            catch (NoSuchElementException ex) {
                // Tribe does not know cell
            }
        });
    }
    
    private void broadcastNotifyCellDetail(AID[] receipts, Cell cell) {
        NotifyCellDetail notifyCellDetail = new NotifyCellDetail();

        notifyCellDetail.setNewCell(cell);

        Action informCellDetailAction = new Action(getAID(), notifyCellDetail);
        addBehaviour(new Conversation(this, ontology, codec, informCellDetailAction, GameOntology.NOTIFYCELLDETAIL) {
            @Override
            public void onStart() {
                sendMessage(receipts, ACLMessage.INFORM, new Conversation.SentMessageHandler() {
                });
            }
        });
    }
    
    void informAboutUnitPassby(Tribe ownerTribe, MapCell position) {
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
                targetTribe.getUnitsIterable()
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

        Action informUnitPositionAction = new Action(getAID(), notifyUnitPosition);
        addBehaviour(new Conversation(this, ontology, codec, informUnitPositionAction, GameOntology.NOTIFYUNITPOSITION) {
            @Override
            public void onStart() {
                sendMessage(receipts, ACLMessage.INFORM, new Conversation.SentMessageHandler() {
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

}
