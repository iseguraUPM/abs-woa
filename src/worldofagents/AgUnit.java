package worldofagents;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import jade.content.AgentAction;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.logging.Level;
import java.util.logging.Logger;
import static worldofagents.AgWorld.MESSAGE_CREATE_UNIT;
import static worldofagents.AgWorld.WORLD;
import worldofagents.ontology.CreateUnit;
import worldofagents.ontology.GameOntology;

/**
 *
 * @author ISU
 */
public class AgUnit extends Agent {
    
    public static final String UNIT = "UNIT";
    private Ontology ontology;
    private SLCodec codec;
    
    @Override
    protected void setup() {
        try {
            initializeAgent();
            initializeUnit();
        } catch (FIPAException ex) {
            Logger.getLogger(AgTribe.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //Behaviours
        addBehaviour(new OneShotBehaviour(this) {
            @Override
            public void action() {
                //TODO: Cuando crea un agente (condiciones)
                DFAgentDescription dfd = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType(WORLD);
                dfd.addServices(sd);

                try {
                    // It finds agents of the required type
                    DFAgentDescription[] worldAgent;
                    worldAgent = DFService.search(myAgent, dfd);

                    if (worldAgent.length > 0){
                        AID worldAID = (AID)worldAgent[0].getName();

                        // Sends the request to the world
                        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                        msg.setOntology(ontology.getName());
                        msg.setLanguage(codec.getName());
                        
                        Action createUnitAction = new Action(worldAID, new CreateUnit());
                        getContentManager().fillContent(msg, createUnitAction);
                        
                        msg.addReceiver(worldAID);
                        myAgent.send(msg);
                        System.out.println(myAgent.getLocalName() + ": Sends a request to create a new unit");

                        addBehaviour(new RequestCreateUnitBehaviour(MessageTemplate
                                        .and(MessageTemplate.MatchLanguage(codec.getName())
                                                , MessageTemplate.MatchOntology(ontology.getName()))));

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    private void initializeAgent() throws FIPAException {
        // Creates its own description
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setName(this.getName());
        sd.setType(UNIT);
        dfd.addServices(sd);
        // Registers its description in the DF
        DFService.register(this, dfd);
        System.out.println(getLocalName() + ": registered in the DF");
        dfd = null;
        sd = null;
    }

    private void initializeUnit() {
        ontology = GameOntology.getInstance();
        codec = new SLCodec();
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);
    }
    
    class RequestCreateUnitBehaviour extends OneShotBehaviour {

        private final MessageTemplate messageTemplate;
        
        public RequestCreateUnitBehaviour(MessageTemplate template) {
            this.messageTemplate = template;
        }

        @Override
        public void action() {
            
            ACLMessage msg = receive(messageTemplate);
            if (msg != null && msg.getContent().equals(MESSAGE_CREATE_UNIT)) {
                switch (msg.getPerformative()) {
                    case ACLMessage.NOT_UNDERSTOOD:
                        System.out.println(myAgent.getLocalName()+": received unit creation not understood from "+(msg.getSender()).getLocalName());
                        break;
                    case ACLMessage.REFUSE:
                        System.out.println(myAgent.getLocalName()+": received unit creation refuse from "+(msg.getSender()).getLocalName());
                        break;
                    case ACLMessage.AGREE:
                        System.out.println(myAgent.getLocalName()+": received unit creation agree from "+(msg.getSender()).getLocalName());
                        agreedCreateUnit();
                        break;
                    default:
                        break;
                }
            }
            
        }
        
        private void agreedCreateUnit() {
            ACLMessage msg = receive(messageTemplate);
            if (msg != null && msg.getContent().equals(MESSAGE_CREATE_UNIT)) {
                switch (msg.getPerformative()) {
                    case ACLMessage.FAILURE:
                        System.out.println(myAgent.getLocalName()+": received unit creation failure from "+(msg.getSender()).getLocalName());
                        break;
                    case ACLMessage.INFORM:
                        System.out.println(myAgent.getLocalName()+": received unit creation inform from "+(msg.getSender()).getLocalName());
                        break;
                    default:
                        break;
                }
            }
        }
        
    }    
}
