package es.upm.woa.agent.group1;

// TODO: change header
/**
 * ***************************************************************
 * Agent offering the painting service: AgPainter.java
 *
 ****************************************************************
 */
import es.upm.woa.ontology.Building;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.CreateUnit;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.MoveToCell;
import es.upm.woa.ontology.NotifyNewCellDiscovery;
import es.upm.woa.ontology.NotifyNewUnit;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.onto.OntologyException;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.NoSuchElementException;
import java.util.Stack;
import javafx.util.Pair;

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
    public static final String TRIBE = "TRIBE";
    public static final String UNIT = "UNIT";

    private static final int STARTING_UNIT_NUMBER = 3;

    private static final long serialVersionUID = 1L;
    private Ontology ontology;
    private Codec codec;

    private Collection<Tribe> tribeCollection;
    private WorldMap worldMap;

    private Collection<Transaction> activeTransactions;

    // TODO: temporal initial coordinates for testing purposes
    private Stack<Pair<Integer, Integer>> initialUnitCoordinates;
    
    
    @Override
    protected void setup() {
        System.out.println(getLocalName() + ": has entered into the system");

        // TODO: temporal initial coordinates for testing purposes
        initialUnitCoordinates = new Stack<>();
        initialUnitCoordinates.add(new Pair(1, 1));
        initialUnitCoordinates.add(new Pair(2, 2));
        initialUnitCoordinates.add(new Pair(3, 3));
        initialUnitCoordinates.add(new Pair(1, 1));
        initialUnitCoordinates.add(new Pair(2, 2));
        initialUnitCoordinates.add(new Pair(3, 3));

        try {
            initializeAgent();
            initializeWorld();
        } catch (FIPAException ex) {

        }

        //startUnitCreationBehaviour();
        startMoveToCellBehaviour();
    }

    private void startUnitCreationBehaviour() {
        // Behaviors
        Action createUnitAction = new Action(getAID(), new CreateUnit());
        addBehaviour(new Conversation(this, ontology, codec, createUnitAction, "CreateUnit") {
            @Override
            public void onStart() {
                Action action = new Action(getAID(), new CreateUnit());

                listenMessages(new ResponseHandler() {
                    @Override
                    public void onRequest(ACLMessage message) {
                        System.out.println(myAgent.getLocalName()
                                + ": received unit creation request from " + message.getSender().getLocalName());

                        final Tribe ownerTribe = findOwnerTribe(message.getSender());
                        Unit requesterUnit = findUnit(ownerTribe, message.getSender());

                        if (ownerTribe == null || requesterUnit == null) {
                            respondMessage(message, ACLMessage.REFUSE);
                        } else if (!canCreateUnit(ownerTribe, requesterUnit)) {
                            respondMessage(message, ACLMessage.REFUSE);
                        } else {
                            ownerTribe.purchaseUnit();
                            respondMessage(message, ACLMessage.AGREE);
                            initiateUnitCreation(ownerTribe, message);
                        }

                    }
                });
            }

            private void initiateUnitCreation(Tribe ownerTribe, ACLMessage message) {

                DelayedTransactionalBehaviour activeTransaction
                        = new DelayedTransactionalBehaviour(myAgent, 15000) {

                    boolean finished = false;

                    @Override
                    public void commit() {
                        if (!finished) {
                            boolean success = launchNewAgentUnit(ownerTribe);
                            if (!success) {
                                ownerTribe.refundUnit();
                                System.out.println(myAgent.getLocalName()
                                        + ": refunded unit to " + ownerTribe.getAID().getLocalName());
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
                            System.out.println(myAgent.getLocalName()
                                    + ": refunded unit to " + ownerTribe.getAID().getLocalName());
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
    
    private boolean canCreateUnit(Tribe tribe, Unit requester) {
        
        if(UnitCellPositioner.getInstance(worldMap).isMoving(requester)){
            return false;
        }
        
        try {
            MapCell mapCell = worldMap.getCellAt(requester.getCoordX(),
                                    requester.getCoordY());
            return thereIsATownHall(mapCell, tribe);
        }
        catch (IndexOutOfBoundsException ex) {
            return false;
        }                   
    }

    private boolean thereIsATownHall(MapCell position, Tribe tribe) {
        if (!(position.getContent() instanceof Building)) {
            return false;
        }
        else {
            Building building = (Building) position.getContent();
            if (building.getType().size() == 0
                    || !(building.getType().get(0) instanceof String)) {
                return false;
            }
            else if (!building.getOwner().equals(tribe.getAID())) {
                return false;
            }
            else return ((String)building.getType().get(0)).equals("TownHall");
        }
    }
    
    private void initializeAgent() throws FIPAException {
        // Creates its own description
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setName(this.getName());
        sd.setType(WORLD);
        dfd.addServices(sd);
        // Registers its description in the DF
        DFService.register(this, dfd);
        System.out.println(getLocalName() + ": registered in the DF");
    }

    private void initializeWorld() {
        ontology = GameOntology.getInstance();
        codec = new SLCodec();
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

        worldMap = WorldMap.getInstance(3, 3);
        tribeCollection = new HashSet<>();

        activeTransactions = new ArrayList<>();

        Tribe newTribe = launchAgentTribe("TribeA");
        if (newTribe != null) {
            handInitialTribeResources(newTribe);
        }
        
        /*
        newTribe = launchAgentTribe("TribeB");
        if (newTribe != null) {
            handInitialTribeResources(newTribe);
        }
        */
    }

    // NOTE: will be used. Do not delete.
    private void finalizeWorld() {
        activeTransactions.forEach(t -> t.rollback());
        activeTransactions.clear();
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
            Logger.getLogger(AgWorld.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private void handInitialTribeResources(Tribe tribe) {
        for (int i = 0; i < STARTING_UNIT_NUMBER; i++) {
            launchNewAgentUnit(tribe);
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

    private boolean launchNewAgentUnit(Tribe ownerTribe) {
        try {
            ContainerController cc = getContainerController();
            AgUnit newUnit = new AgUnit();
            AgentController ac = cc.acceptNewAgent(generateNewUnitName(ownerTribe), newUnit);
            ac.start();

            // TODO: temporal initial positions for testing purposes
            Pair<Integer, Integer> position;
            try {
                position = initialUnitCoordinates.pop();
            } catch (EmptyStackException ex) {
                position = new Pair<>(0, 0);
            }
            Unit newUnitRef = new Unit(newUnit.getAID(), position.getKey(),
                     position.getValue());
            
            
            if (!ownerTribe.createUnit(newUnitRef)) {
                ac.kill();
                return false;
            } else {
                informTribeAboutNewUnit(ownerTribe, newUnitRef);
                return true;
            }

        } catch (StaleProxyException ex) {
            Logger.getLogger(AgWorld.class.getName()).log(Level.SEVERE, null, ex);
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
        cell.setOwner(this.getAID());
        notifyNewUnit.setLocation(cell);
        notifyNewUnit.setNewUnit(newUnit.getId());

        Action informNewUnitAction = new Action(ownerTribe.getAID(), notifyNewUnit);
        addBehaviour(new Conversation(this, ontology, codec, informNewUnitAction, "NotifyNewUnit") {
            @Override
            public void onStart() {
                sendMessage(ownerTribe.getAID(), ACLMessage.INFORM, new SentMessageHandler() {
                });
            }
        });
        
        processTribeKnownCell(ownerTribe, cell);
    }

    private void processTribeKnownCell(Tribe ownerTribe, Cell cell) {
        GameMap exploredTribeCells = ownerTribe.getKnownMap();
        try {
            exploredTribeCells.getCellAt(cell.getX()
                    , cell.getY());
            System.out.println("CONTIENE LA CELDA" + cell.getX() + "-" + cell.getY());
        }
        catch (NoSuchElementException ex) {
            System.out.println("NO CONTIENE LA CELDA" + cell.getX() + "-" + cell.getY());
            addNewlyExploredCell(cell, exploredTribeCells, ownerTribe);
        }
    }

    private void addNewlyExploredCell(Cell cell, GameMap exploredTribeCells, Tribe ownerTribe) {
        try {
            MapCell exploredCell = worldMap.getCellAt(cell.getX(), cell.getY());
            exploredTribeCells.addCell(exploredCell);
            informTribeAboutDiscoveredCell(ownerTribe, cell);
            int i = 0;
            for(Unit tribeUnit : ownerTribe.getUnitsIterable()){
                System.out.println(i + " UNIDAD INFORMADA DE " + cell.getX() + "-" + cell.getY());
                i++;
                informUnitAboutDiscoveredCell(tribeUnit, cell);
            }
        }
        catch (NoSuchElementException e) {
        }
    }
    
    private void informTribeAboutDiscoveredCell(Tribe ownerTribe, Cell newCell) {

        NotifyNewCellDiscovery notifyNewCellDiscovery = new NotifyNewCellDiscovery();
        
        //TODO this shouldn't be mandatory
        newCell.setOwner(this.getAID());
        notifyNewCellDiscovery.setNewCell(newCell);

        Action informNewCellDiscoveryAction = new Action(ownerTribe.getAID(), notifyNewCellDiscovery);
        addBehaviour(new Conversation(this, ontology, codec, informNewCellDiscoveryAction, "NotifyNewCellDiscovery") {
            @Override
            public void onStart() {
                sendMessage(ownerTribe.getAID(), ACLMessage.INFORM, new SentMessageHandler() {
                });
            }
        });
    }
    
    private void informUnitAboutDiscoveredCell(Unit ownerUnit, Cell newCell) {
        NotifyNewCellDiscovery notifyNewCellDiscovery = new NotifyNewCellDiscovery();
        
        //TODO this shouldn't be mandatory
        newCell.setOwner(this.getAID());
        notifyNewCellDiscovery.setNewCell(newCell);

        Action informNewCellDiscoveryAction = new Action(ownerUnit.getId(), notifyNewCellDiscovery);
        addBehaviour(new Conversation(this, ontology, codec, informNewCellDiscoveryAction, "NotifyNewCellDiscovery") {
            @Override
            public void onStart() {
                sendMessage(ownerUnit.getId(), ACLMessage.INFORM, new SentMessageHandler() {
                });
            }
        });
    }
    
    private void startMoveToCellBehaviour() {
        // Behaviors
        //TODO The response ontology is not yet defined. It needs to be changed in the future. 
        Action createUnitAction = new Action(getAID(), new CreateUnit());
        addBehaviour(new Conversation(this, ontology, codec, createUnitAction, "MoveToCell") {
            @Override
            public void onStart() {
                Action action = new Action();

                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onRequest(ACLMessage message) {
                        System.out.println(myAgent.getLocalName()
                                + ": received unit 'move to cell' request from " + message.getSender().getLocalName());

                        final Tribe ownerTribe = findOwnerTribe(message.getSender());
                        Unit requesterUnit = findUnit(ownerTribe, message.getSender());
                        
                        if (ownerTribe == null || requesterUnit == null) {
                            respondMessage(message, ACLMessage.REFUSE);
                        }else{

                            try {
                                ContentElement ce = getContentManager().extractContent(message);
                                Action agAction = (Action) ce;
                                Concept conc = agAction.getAction();
                                MoveToCell targetCell= (MoveToCell) conc;
                                MapCell mapCell = worldMap.getCellAt(targetCell
                                        .getTarget().getX(), targetCell.getTarget().getY());
                                
                                initiateMoveToCell(requesterUnit, mapCell, message);

                            } catch (Codec.CodecException | OntologyException ex) {
                                Logger.getLogger(AgTribe.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                });
            }

            private void initiateMoveToCell(Unit requesterUnit, MapCell mapCell, ACLMessage message) {
                UnitCellPositioner unitPositioner = UnitCellPositioner.getInstance(worldMap);
                if (unitPositioner.isMoving(requesterUnit)) {
                    System.out.println(myAgent.getLocalName()
                                        + ": unit already moving "
                                    + requesterUnit.getId().getLocalName());
                    respondMessage(message, ACLMessage.REFUSE);
                }
                                
                try {
                    //TODO message content must be updated
                    Transaction moveTransaction = unitPositioner.move(myAgent
                            , requesterUnit, mapCell, new UnitCellPositioner
                                    .UnitMovementHandler() {
                        @Override
                        public void onMove() {
                            Tribe ownerTribe = findOwnerTribe(requesterUnit.getId());
                            Cell newCell = new Cell();
                            newCell.setX(mapCell.getXCoord());
                            newCell.setY(mapCell.getYCoord());

                            
                            respondMessage(message, ACLMessage.INFORM);
                            
                            processTribeKnownCell(ownerTribe, newCell);
                        }

                        @Override
                        public void onCancel() {
                            System.out.println(myAgent.getLocalName()
                                        + ": refunded unit 'move to cell' to "
                                    + requesterUnit.getId().getLocalName());
                                respondMessage(message, ACLMessage.FAILURE);
                        }
                    });
                    respondMessage(message, ACLMessage.AGREE);
                    activeTransactions.add(moveTransaction);
                  
            }
            catch (IndexOutOfBoundsException ex) {
                System.out.println(myAgent.getLocalName()
                        + ": cannot move unit - " + ex);
                respondMessage(message, ACLMessage.REFUSE);
            }
        }
        });
    }
}
