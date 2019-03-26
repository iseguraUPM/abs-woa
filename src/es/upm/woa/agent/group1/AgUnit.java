package es.upm.woa.agent.group1;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.logging.Level;
import java.util.logging.Logger;
import es.upm.woa.ontology.CreateUnit;
import es.upm.woa.ontology.GameOntology;
import jade.domain.JADEAgentManagement.CreateAgent;

/**
 *
 * @author ISU
 */
public class AgUnit extends Agent {

    public static final String UNIT = "UNIT";
    public static final String WORLD = "WORLD";
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
        addBehaviour(new Conversation(this) {
            
            @Override
            public void onStart() {
                DFAgentDescription dfd = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType(WORLD);
                dfd.addServices(sd);
                
                try {
                    // It finds agents of the required type
                    DFAgentDescription[] worldAgent;
                    worldAgent = DFService.search(myAgent, dfd);

                    if (worldAgent.length > 0) {
                        AID worldAID = (AID) worldAgent[0].getName();

                        Action action = new Action(worldAID, new CreateUnit());
                        final ConversationEnvelope newEnvelope
                                = new ConversationEnvelope(ontology, codec
                                        , action, worldAID, ACLMessage.REQUEST);
                       
                        sendMessage(newEnvelope);
                        
                        Envelope responseEnvelop = new ConversationEnvelope(ontology, codec
                                , action, worldAID, ACLMessage.UNKNOWN);
                        
                        waitReponse(responseEnvelop, new ResponseHandler() {
                            @Override
                            public void onAgree(AID sender, Concept content) {
                                System.out.println(myAgent.getLocalName()
                                        + ": received unit creation agree from " + sender.getLocalName());
                                
                                waitReponse(responseEnvelop, new ResponseHandler() {
                                    @Override
                                    public void onFailure(AID sender, Concept content) {
                                        System.out.println(myAgent.getLocalName()
                                                + ": received unit creation failure from " + sender.getLocalName());

                                    }

                                    @Override
                                    public void onInform(AID sender, Concept content) {
                                        System.out.println(myAgent.getLocalName()
                                                + ": received unit creation inform from " + sender.getLocalName());

                                    }
                                    
                                });
                            }

                            @Override
                            public void onNotUnderstood(AID sender, Concept content) {
                                System.out.println(myAgent.getLocalName() + ": received unit creation not understood from " + sender.getLocalName());
                            }

                            @Override
                            public void onRefuse(AID sender, Concept content) {
                                System.out.println(myAgent.getLocalName() + ": received unit creation refuse from " + sender.getLocalName());
                            }
                            
                            
                        });
                    }
                } catch (FIPAException e) {
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
}
