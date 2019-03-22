package worldofagents;

// TODO: change header
/**
 * ***************************************************************
 * Agent offering the painting service: AgPainter.java
 *
 ****************************************************************
 */
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Profile;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import jade.wrapper.State;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import static worldofagents.AgTribe.TRIBE;
import static worldofagents.AgUnit.UNIT;
import worldofagents.ontology.GameOntology;

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
    
    private static final long serialVersionUID = 1L;
    private Ontology ontology; 
    private Collection<Tribe> tribes;

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
        addBehaviour(new AgWorldRequestHandlerBehaviour(this));

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
            
        tribes = new HashSet<>();
        
        if (launchAgentTribe()) {
            handInitialTribeResources();
        }
    }
    
    private boolean launchAgentTribe() {
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
                Tribe newTribe = new Tribe(newTribeAgent.getName().toString());
                if (!tribes.add(newTribe)) {
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
                targetTribe = foundTribes[++i];
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
        for (Tribe tribe : tribes) {
            for (int i = 0; i < 3; i++) {
                launchAgentUnit(tribe, "TestUnit" + i);
            }
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
                Unit newUnit = new Unit(newUnitAgent.getName().toString(), 0, 0);
                if (!ownerTribe.createUnit(newUnit)) {
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

}
