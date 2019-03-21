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
import jade.domain.FIPAAgentManagement.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
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

    private static final String WORLD = "WORLD";
    private static final long serialVersionUID = 1L;
    private Ontology ontology = GameOntology.getInstance();

    protected void setup() {
        System.out.println(getLocalName() + ": has entered into the system");

        try {
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
        } catch (FIPAException e) {
            e.printStackTrace();
        }

//		BEHAVIOURS ****************************************************************
        // Adds a behavior to answer the estimation requests
        // Waits for a request and, when it arrives, answers with			  
        // the ESTIMATION and waits again.
        // If arrives a DECISION, it takes it (at this point, the painter would begin painting
        // if it is accepted...)
        addBehaviour(new CyclicBehaviour(this) {
            private static final long serialVersionUID = 1L;

            public void action() {
                // Waits for requests
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (msg != null) {
                    // TODO: check msg content
                    // TODO: create unit request logic

                    System.out.println(myAgent.getLocalName() + ": received create unit request from " + (msg.getSender()).getLocalName());
                    ACLMessage reply = msg.createReply();
			
                    reply.setContent("");
                    reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                    myAgent.send(reply);
                    System.out.println(myAgent.getLocalName() + ": answer sent -> " + reply.getContent());
                    
                    AID unitRequester = msg.getSender();
                    
                    new Thread(() -> {
                        // TODO: send to requesters tribe
                        // TODO: wait 150 hours
                        // TODO: how to launch agents in code
                        
                        //ACLMessage informNewUnit = ;
                    }).run();
                    
                } else {
                    // If no message arrives
                    block();
                }

            }

        });

    }

}
