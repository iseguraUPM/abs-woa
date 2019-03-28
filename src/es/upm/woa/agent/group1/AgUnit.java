package es.upm.woa.agent.group1;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.util.logging.Level;
import java.util.logging.Logger;
import es.upm.woa.ontology.CreateUnit;
import es.upm.woa.ontology.GameOntology;

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

        //Behaviors
        Action createUnitAction = new Action(getAID(), new CreateUnit());
        addBehaviour(new Conversation(this, ontology, codec, createUnitAction) {

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

                        sendMessage(worldAID, ACLMessage.REQUEST
                                , new SentMessageHandler() {
                            
                            @Override
                            public void onSent(String conversationID) {

                                receiveResponse(conversationID, new ResponseHandler() {
                                    
                                    @Override
                                    public void onAgree(ACLMessage response) {
                                        System.out.println(myAgent.getLocalName()
                                                + ": received unit creation agree from " + response.getSender().getLocalName());

                                        receiveResponse(conversationID, new ResponseHandler() {
                                            
                                            @Override
                                            public void onFailure(ACLMessage response) {
                                                System.out.println(myAgent.getLocalName()
                                                        + ": received unit creation failure from " + response.getSender().getLocalName());
                                            }

                                            @Override
                                            public void onInform(ACLMessage response) {
                                                System.out.println(myAgent.getLocalName()
                                                        + ": received unit creation inform from " + response.getSender().getLocalName());
                                            }

                                        });
                                    }

                                    @Override
                                    public void onNotUnderstood(ACLMessage response) {
                                        System.out.println(myAgent.getLocalName() + ": received unit creation not understood from " + response.getSender().getLocalName());
                                    }

                                    @Override
                                    public void onRefuse(ACLMessage response) {
                                        System.out.println(myAgent.getLocalName() + ": received unit creation refuse from " + response.getSender().getLocalName());
                                    }

                                });
                            }
                        });

                    }
                } catch (FIPAException e) {
                    System.err.println(myAgent.getLocalName() + ": caught exception " + e);
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
    }

    private void initializeUnit() {
        ontology = GameOntology.getInstance();
        codec = new SLCodec();
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);
    }
}
