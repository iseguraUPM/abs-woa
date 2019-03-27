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
import jade.content.onto.OntologyException;
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

// TODO: change docs
/**
 * This agent has the following functionality:
 * <ul>
 * <li> It registers itself in the DF as PAINTER
 * <li> Waits for requests of its painting service
 * <li> If any estimation request arrives, it answers with a random value
 * <li> Finally, it waits for the client answer
 * </ul>
 *
 * @author Ricardo Imbert, UPM
 * @version $Date: 2009/04/01 13:45:18 $ $Revision: 1.1 $
 *
 */
public class AgWorld extends Agent {

    public static final String WORLD = "WORLD";
    public static final String TRIBE = "TRIBE";
    public static final String UNIT = "UNIT";

    private static final int WAIT_NEW_AGENT_REGISTRATION_MILLIS = 500;
    private static final int STARTING_UNIT_NUMBER = 3;

    private static final long serialVersionUID = 1L;
    private Ontology ontology;
    private Codec codec;

    private Collection<Tribe> tribeCollection;

    @Override
    protected void setup() {
        System.out.println(getLocalName() + ": has entered into the system");

        try {
            initializeAgent();
            initializeWorld();

        } catch (FIPAException e) {
            e.printStackTrace();
        }

//		BEHAVIOURS ****************************************************************
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
                        if (ownerTribe == null || requesterUnit == null || !ownerTribe.canAffordUnit()) {
                            respondMessage(message, ACLMessage.REFUSE);
                        } else {
                            ownerTribe.purchaseUnit();
                            respondMessage(message, ACLMessage.AGREE);

                            addBehaviour(new DelayBehaviour(myAgent, 15000) {

                                @Override
                                public void handleElapsedTimeout() {

                                    boolean success = launchAgentUnit(ownerTribe, "CreatedUnit" + ownerTribe.getNumberUnits());
                                    if (!success) {
                                        ownerTribe.refundUnit();
                                        respondMessage(message, ACLMessage.FAILURE);

                                    }
                                    else {
                                        respondMessage(message, ACLMessage.INFORM);
                                    }
                                }
                            });
                            
                        }
                    }
                });
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
        dfd = null;
        sd = null;
    }

    private void initializeWorld() {
        ontology = GameOntology.getInstance();
        codec = new SLCodec();
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

        tribeCollection = new HashSet<>();

        if (launchAgentTribe()) {
            handInitialTribeResources();
        }
    }

    public boolean launchAgentTribe() {
        try {
            ContainerController cc = getContainerController();
            AgTribe newTribe = new AgTribe();
            AgentController ac = cc.acceptNewAgent("TestTribe", newTribe);
            ac.start();

            Tribe newTribeRef = new Tribe(newTribe.getAID());
            if (!tribeCollection.add(newTribeRef)) {
                ac.kill();
                return false;
            } else {
                return true;
            }

        } catch (StaleProxyException ex) {
            Logger.getLogger(AgWorld.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    private void handInitialTribeResources() {
        tribeCollection.stream().forEach((tribe) -> {
            for (int i = 0; i < STARTING_UNIT_NUMBER; i++) {
                launchAgentUnit(tribe, "TestUnit" + i);
            }
        });
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

    private boolean launchAgentUnit(Tribe ownerTribe, String newUnitName) {
        try {
            ContainerController cc = getContainerController();
            AgUnit newUnit = new AgUnit();
            AgentController ac = cc.acceptNewAgent(newUnitName, newUnit);
            ac.start();

            Unit newUnitRef = new Unit(newUnit.getAID(), 0, 0);
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

    private void informTribeAboutNewUnit(Tribe ownerTribe, Unit newUnit) {

        NotifyNewUnit notifyNewUnit = new NotifyNewUnit();
        Cell cell = new Cell();
        cell.setX(newUnit.getCoordX());
        cell.setY(newUnit.getCoordY());
        notifyNewUnit.setLocation(cell);
        notifyNewUnit.setNewUnit(newUnit.getId());

        Action informNewUnitAction = new Action(ownerTribe.getId(), notifyNewUnit);
        addBehaviour(new Conversation(this, ontology, codec, informNewUnitAction) {
            @Override
            public void onStart() {
                sendMessage(ownerTribe.getId(), ACLMessage.INFORM, new SentMessageHandler() {});
            }
        });
    }
}
