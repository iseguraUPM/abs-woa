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
import jade.content.ContentManager;
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
import static es.upm.woa.agent.group1.AgTribe.TRIBE;
import static es.upm.woa.agent.group1.AgUnit.UNIT;
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
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                // Waits for requests
                ACLMessage msg = receive(MessageTemplate.and(
                        MessageTemplate.MatchLanguage(codec.getName()),
                        MessageTemplate.MatchOntology(ontology.getName())
                ));

                if (msg != null) {
                    try {
                        if (msg.getPerformative() == ACLMessage.REQUEST) {
                            ContentElement ce = getContentManager().extractContent(msg);
                            if (ce instanceof Action) {

                                Action agAction = (Action) ce;
                                Concept conc = agAction.getAction();

                                if (conc instanceof CreateUnit) {
                                    System.out.println(getLocalName() + ": received request from " + (msg.getSender()).getLocalName());
                                    addBehaviour(new CreateUnitBehaviour(myAgent, msg));
                                }
                            }
                        }
                    } catch (Codec.CodecException | OntologyException e) {
                        e.printStackTrace();
                        ACLMessage reply = msg.createReply();
                        reply.setOntology(ontology.getName());
                        reply.setLanguage(codec.getName());
                        reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                        send(reply);
                    }

                } else {
                    block();
                }
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
            AgentController ac = cc.createNewAgent("TestTribe", "es.upm.woa.agent.group1.AgTribe", null);
            ac.start();

            doWait(WAIT_NEW_AGENT_REGISTRATION_MILLIS);
            DFAgentDescription newTribeAgent = findAgent(TRIBE, "TestTribe");
            if (newTribeAgent == null) {
                ac.kill();
                return false;
            } else {
                Tribe newTribe = new Tribe(newTribeAgent.getName());
                if (!tribeCollection.add(newTribe)) {
                    ac.kill();
                    return false;
                } else {
                    return true;
                }
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
            AgentController ac = cc.createNewAgent(newUnitName, "es.upm.woa.agent.group1.AgUnit", null);
            ac.start();

            doWait(WAIT_NEW_AGENT_REGISTRATION_MILLIS);
            DFAgentDescription newUnitAgent = findAgent(UNIT, newUnitName);
            if (newUnitAgent == null) {
                ac.kill();
                return false;
            } else {
                Unit newUnit = new Unit(newUnitAgent.getName(), 0, 0);
                if (!ownerTribe.createUnit(newUnit)) {
                    ac.kill();
                    return false;
                } else {
                    informTribeAboutNewUnit(ownerTribe, newUnit);
                    return true;
                }
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

    class CreateUnitBehaviour extends OneShotBehaviour {

        ACLMessage msg, reply;

        public CreateUnitBehaviour(Agent agent, ACLMessage msg) {
            super(agent);
            this.msg = msg;
        }

        @Override
        public void action() {

            try {
                ACLMessage reply = msg.createReply();
                reply.setOntology(ontology.getName());
                reply.setLanguage(codec.getName());
                getContentManager().fillContent(reply, new Action(getAID(), new CreateUnit()));

                AID unitRequester = msg.getSender();
                final Tribe ownerTribe = findOwnerTribe(unitRequester);
                Unit requesterUnit = findUnit(ownerTribe, unitRequester);

                // TODO: townhall location logic
                if (ownerTribe == null || requesterUnit == null || !ownerTribe.canAffordUnit()) {
                    reply.setPerformative(ACLMessage.REFUSE);
                    System.out.println(myAgent.getLocalName() + ": Refuses to create a new unit for " + (msg.getSender()).getLocalName());
                    myAgent.send(reply);
                } else {
                    ownerTribe.purchaseUnit();
                    reply.setPerformative(ACLMessage.AGREE);
                    myAgent.send(reply);

                    System.out.println(myAgent.getLocalName() + ": Agrees to create a new unit for " + (msg.getSender()).getLocalName());

                    addBehaviour(new DelayBehaviour(myAgent, 15000) {

                        @Override
                        public void handleElapsedTimeout() {

                            AgWorld agWorld = (AgWorld) myAgent;

                            boolean success = agWorld.launchAgentUnit(ownerTribe, "CreatedUnit" + ownerTribe.getNumberUnits());

                            try {
                                ACLMessage newmsg = new ACLMessage(ACLMessage.UNKNOWN);
                                newmsg.setOntology(ontology.getName());
                                newmsg.setLanguage(codec.getName());
                                getContentManager().fillContent(newmsg, new Action(getAID(), new CreateUnit()));
                                newmsg.addReceiver(msg.getSender());
                                
                                if (!success) {
                                    ownerTribe.refundUnit();
                                    newmsg.setPerformative(ACLMessage.FAILURE);
                                    System.out.println(agWorld.getLocalName() + ": Sends failure to create a new unit to " + (msg.getSender()).getLocalName());
                                } else {
                                    newmsg.setPerformative(ACLMessage.INFORM);
                                    System.out.println(agWorld.getLocalName() + ": Sends inform to create a new unit to " + (msg.getSender()).getLocalName());
                                }
                                agWorld.send(newmsg);

                            } catch (Codec.CodecException | OntologyException ex) {
                                Logger.getLogger(AgWorld.class.getName()).log(Level.SEVERE, null, ex);
                            }

                        }
                    });

                }
            } catch (Codec.CodecException | OntologyException ex) {
                Logger.getLogger(AgWorld.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
}
