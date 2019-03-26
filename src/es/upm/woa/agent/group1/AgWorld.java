package es.upm.woa.agent.group1;

// TODO: change header
/**
 * ***************************************************************
 * Agent offering the painting service: AgPainter.java
 *
 ****************************************************************
 */
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
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
        addBehaviour(new Conversation(this) {
            @Override
            public void onStart() {
                Action action = new Action(getAID(), new CreateUnit());

                final ConversationEnvelope envelope = new ConversationEnvelope(ontology,
                         codec, action, getAID(), ACLMessage.REQUEST);

                listenMessages(envelope, new ResponseHandler() {
                    @Override
                    public void onRequest(ACLMessage message) {
                        System.out.println(myAgent.getLocalName()
                                        + ": received unit creation request from " + message.getSender().getLocalName());
                        
                        final Tribe ownerTribe = findOwnerTribe(message.getSender());
                        Unit requesterUnit = findUnit(ownerTribe, message.getSender());

                        // TODO: townhall location logic
                        if (ownerTribe == null || requesterUnit == null || !ownerTribe.canAffordUnit()) {
                            respondMessage(new ConversationEnvelope(ontology, codec, action, message.getSender(), ACLMessage.REFUSE), message);
                        } else {
                            ownerTribe.purchaseUnit();
                            respondMessage(new ConversationEnvelope(ontology
                                    , codec, action, message.getSender(), ACLMessage.AGREE), message);

                            addBehaviour(new DelayBehaviour(myAgent, 15000) {

                                @Override
                                public void handleElapsedTimeout() {

                                    AgWorld agWorld = (AgWorld) myAgent;

                                    boolean success = agWorld.launchAgentUnit(ownerTribe, "CreatedUnit" + ownerTribe.getNumberUnits());
                                    if (!success) {
                                        ownerTribe.refundUnit();
                                        respondMessage(new ConversationEnvelope(ontology, codec, action, message.getSender(), ACLMessage.FAILURE), message);

                                    }
                                    else {
                                        respondMessage(new ConversationEnvelope(ontology, codec, action, message.getSender(), ACLMessage.INFORM), message);
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

    private DFAgentDescription findAgent(String type, String agentName) {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(type);
        dfd.addServices(sd);

        try {
            return searchAgentDescription(dfd, agentName);
        } catch (FIPAException ex) {
            Logger.getLogger(AgWorld.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private DFAgentDescription searchAgentDescription(DFAgentDescription searchDescription, String agentName) throws FIPAException {
        DFAgentDescription[] foundTribes;
        foundTribes = DFService.search(this, searchDescription);

        if (foundTribes.length == 0) {
            return null;
        }

        int i = 0;
        DFAgentDescription targetTribe = foundTribes[i];
        while (i < foundTribes.length && !targetTribe.getName().getLocalName().equals(agentName)) {
            if (++i < foundTribes.length) {
                targetTribe = foundTribes[i];
            }
        }

        if (foundTribes.length == 0 || !targetTribe.getName().getLocalName().equals(agentName)) {
            return null;
        } else {
            return targetTribe;
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

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(ownerTribe.getId());
        msg.setOntology(ontology.getName());
        msg.setLanguage(codec.getName());

        NotifyNewUnit notifyNewUnit = new NotifyNewUnit();
        Cell cell = new Cell();
        cell.setX(newUnit.getCoordX());
        cell.setY(newUnit.getCoordY());
        notifyNewUnit.setLocation(cell);
        notifyNewUnit.setNewUnit(newUnit.getId());

        Action agAction = new Action(ownerTribe.getId(), notifyNewUnit);
        try {
            // The ContentManager transforms the java objects into strings
            getContentManager().fillContent(msg, agAction);
            send(msg);
            System.out.println(getLocalName() + ": INFORMS A TRIBE " + ownerTribe.getId().getName());
        } catch (Codec.CodecException | OntologyException ce) {
            ce.printStackTrace();
        }
    }
}
