package es.upm.woa.agent.group1;

// TODO: change header
/**
 * ***************************************************************
 * Agent offering the painting service: AgPainter.java
 *
 ****************************************************************
 */
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
import es.upm.woa.ontology.NotifyNewUnit;
import java.util.ArrayList;
import java.util.EmptyStackException;
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

        startUnitCreationBehaviour();
    }

    private void startUnitCreationBehaviour() {
        // Behaviors
        Action createUnitAction = new Action(getAID(), new CreateUnit());
        addBehaviour(new Conversation(this, ontology, codec, createUnitAction) {
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

                        // TODO: townhall location logic
                        if (ownerTribe == null || requesterUnit == null) {
                            respondMessage(message, ACLMessage.REFUSE);
                        } else {

                            MapCell mapCell = worldMap.getCellAt(requesterUnit.getCoordX(),
                                    requesterUnit.getCoordY());
                            boolean isOnTribeTownHall = mapCell != null
                                    && mapCell.getOwner() == ownerTribe.getId()
                                    && mapCell.getContent().equals(WorldMap.TOWN_HALL);
                            if (!isOnTribeTownHall || !ownerTribe.canAffordUnit()) {
                                respondMessage(message, ACLMessage.REFUSE);
                            } else {
                                ownerTribe.purchaseUnit();
                                respondMessage(message, ACLMessage.AGREE);
                                initiateUnitCreation(ownerTribe, message);
                            }
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
        
        Tribe newTribe = launchAgentTribe("TribeA", 0);
        if (newTribe != null) {
            handInitialTribeResources(newTribe);
        }
        
        newTribe = launchAgentTribe("TribeB", 1);
        if (newTribe != null) {
            handInitialTribeResources(newTribe);
        }
    }
    
    private void finalizeWorld() {
        activeTransactions.forEach(t -> t.rollback());
        activeTransactions.clear();
    }

    private Tribe launchAgentTribe(String tribeName, int id) {
        try {
            ContainerController cc = getContainerController();
            AgTribe newTribe = new AgTribe();
            AgentController ac = cc.acceptNewAgent(tribeName, newTribe);
            ac.start();

            Tribe newTribeRef = new Tribe(newTribe.getAID(), id);
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
            Unit newUnitRef = new Unit(newUnit.getAID(), position.getKey()
                    , position.getValue());
            
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
        notifyNewUnit.setLocation(cell);
        notifyNewUnit.setNewUnit(newUnit.getId());

        Action informNewUnitAction = new Action(ownerTribe.getAID(), notifyNewUnit);
        addBehaviour(new Conversation(this, ontology, codec, informNewUnitAction) {
            @Override
            public void onStart() {
                sendMessage(ownerTribe.getAID(), ACLMessage.INFORM, new SentMessageHandler() {
                });
            }
        });
    }
}
