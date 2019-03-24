package worldofagents;



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
import worldofagents.objects.Tribe;
import worldofagents.objects.Unit;
import static worldofagents.AgTribe.TRIBE;
import static worldofagents.AgUnit.UNIT;
import worldofagents.ontology.Cell;
import worldofagents.ontology.CreateUnit;
import worldofagents.ontology.GameOntology;
import worldofagents.ontology.NotifyNewUnit;

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
    private static final String messageCreateUnit = "CreateNewUnit";
    
    private static final int WAIT_NEW_AGENT_REGISTRATION_MILLIS = 500;
    
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
                        if(msg.getPerformative() == ACLMessage.INFORM) {
                            ContentElement ce = getContentManager().extractContent(msg);
                            if (ce instanceof Action) {

                                Action agAction = (Action) ce;
                                Concept conc = agAction.getAction();

                                if (conc instanceof CreateUnit) {
                                    System.out.println(getLocalName()+": received inform request from "+(msg.getSender()).getLocalName());
                                    
                                }
                            }
                        }
                    } catch (Codec.CodecException | OntologyException e) {
                        e.printStackTrace();
                    }         

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
            AgentController ac = cc.createNewAgent("TestTribe", "worldofagents.AgTribe", null);
            ac.start();
            
            doWait(WAIT_NEW_AGENT_REGISTRATION_MILLIS);
            DFAgentDescription newTribeAgent = findAgent(TRIBE, "TestTribe");
            if (newTribeAgent == null) {
                ac.kill();
                return false;
            }
            else {
                Tribe newTribe = new Tribe(newTribeAgent.getName());
                if (!tribeCollection.add(newTribe)) {
                    ac.kill();
                    return false;
                }
                else {
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
        }
        else {
            return targetTribe;
        }
    }
    
    private void handInitialTribeResources() {
        tribeCollection.stream().forEach((tribe) -> {
            for (int i = 0; i < 3; i++) {
                launchAgentUnit(tribe, "TestUnit" + i);
            }
        });
    }
    
    public boolean launchAgentUnitFromRequest(AID requesterUnitId){
        String newUnitName = UNIT;
        Tribe ownerTribe = findOwnerTribe(requesterUnitId);
        
        if(ownerTribe == null){
            return false;
        }else{
            return launchAgentUnit(ownerTribe, newUnitName);
        }
    }
    
    private Tribe findOwnerTribe(AID requesterUnAid){
        Optional<Tribe> tribe;
        tribe = tribeCollection.stream().filter(currentTribe -> currentTribe.containsUnit(requesterUnAid)).findAny();
        if(!tribe.isPresent()){
            return null;
        }else{
            return tribe.get();
        }
    }
    
    private boolean launchAgentUnit(Tribe ownerTribe, String newUnitName) {
        try {
            ContainerController cc = getContainerController();
            AgentController ac = cc.createNewAgent(newUnitName, "worldofagents.AgUnit", null);
            ac.start();
            
            doWait(WAIT_NEW_AGENT_REGISTRATION_MILLIS);
            DFAgentDescription newUnitAgent = findAgent(UNIT, newUnitName);
            if (newUnitAgent == null) {
                ac.kill();
                return false;
            }
            else {
                Unit newUnit = new Unit(newUnitAgent.getName(), 0, 0);
                if (!ownerTribe.createUnit(newUnit)) {
                    ac.kill();
                    return false;
                }
                else {
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
        
        Action agAction = new Action(ownerTribe.getId(),notifyNewUnit);
        try
        {
                // The ContentManager transforms the java objects into strings
                getContentManager().fillContent(msg, agAction);
                send(msg);
                System.out.println(getLocalName()+": INFORMS A TRIBE " + ownerTribe.getId().getName());
        }
        catch (Codec.CodecException ce)
        {
                ce.printStackTrace();
        }
        catch (OntologyException oe)
        {
                oe.printStackTrace();
        }
    }
    
    class CreateUnitBehaviour extends OneShotBehaviour {
        
        ACLMessage msg, reply ;
        
        public CreateUnitBehaviour(Agent agent, ACLMessage msg) {
            super(agent);
            this.msg = msg;
        }


        @Override
        public void action() {
            //TODO: CAMBIAR ELEGIROPCION POR LA MANERA REAL DE ELEGIR LA OPCION. Falta la logica
            int ELEGIROPCION = 2;

            ACLMessage reply = msg.createReply();
            reply.setOntology(ontology.getName());
            reply.setContent(messageCreateUnit);
            
            if(ELEGIROPCION == 0) {
                reply.setPerformative(ACLMessage.REFUSE);
                myAgent.send(reply);
                System.out.println(myAgent.getLocalName()+": Refuses to create a new unit for " + (msg.getSender()).getLocalName());
            } else if(ELEGIROPCION == 1) {
                reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                myAgent.send(reply);
                System.out.println(myAgent.getLocalName()+": Doesn't understand how to create a new unit for " + (msg.getSender()).getLocalName());
            } else if(ELEGIROPCION == 2) {
                reply.setPerformative(ACLMessage.AGREE);
                myAgent.send(reply);
                System.out.println(myAgent.getLocalName()+": Agrees to create a new unit for " + (msg.getSender()).getLocalName());
                AID unitRequester = msg.getSender();
               
                addBehaviour(new DelayBehaviour(myAgent, 15000) {
                    
                    @Override
                    public void handleElapsedTimeout() {
                        
                        AgWorld agWorld = (AgWorld) myAgent;
                            
                            boolean success = agWorld.launchAgentUnitFromRequest(unitRequester);
                            if(!success) {
                                ACLMessage newmsg = new ACLMessage(ACLMessage.FAILURE);
                                newmsg.setContent(messageCreateUnit);
                                newmsg.addReceiver(msg.getSender());
                                agWorld.send(newmsg);
                                System.out.println(agWorld.getLocalName()+": Sends failure to create a new unit to " + (msg.getSender()).getLocalName());
                             } else {
                                ACLMessage newmsg = new ACLMessage(ACLMessage.INFORM);
                                newmsg.setContent(messageCreateUnit);
                                newmsg.addReceiver(msg.getSender());
                                agWorld.send(newmsg);
                                System.out.println(agWorld.getLocalName()+": Sends inform to create a new unit to " + (msg.getSender()).getLocalName());
                            }
                        }
                });
            }
        }
    
    }
}
