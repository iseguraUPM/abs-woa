package es.upm.woa.agent.group1;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import es.upm.woa.ontology.Cell;
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
    private DFAgentDescription[] worldAgent;

    @Override
    protected void setup() {
        try {
            initializeAgent();
            initializeUnit();
        } catch (FIPAException ex) {
            Logger.getLogger(AgTribe.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if(checkNewUnitIsNeeded()){
            startCreateUnitBehaviour();
        }else if(checkMoveToCellIsNeeded()){
            startMoveToCellBehaviour();
        }   
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

    private void initializeUnit()  {
        ontology = GameOntology.getInstance();
        codec = new SLCodec();
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);
        
        //Finds the World in the DF
        try{
            DFAgentDescription dfdWorld = new DFAgentDescription();
            ServiceDescription sdWorld = new ServiceDescription();
            sdWorld.setType(WORLD);
            dfdWorld.addServices(sdWorld);
            // It finds agents of the required type
            worldAgent = DFService.search(this, dfdWorld);
            if (worldAgent.length == 0) {
                //TODO what if the world is not found
            }
        }catch (FIPAException e) {
            System.err.println(this.getLocalName() + ": caught exception " + e);
        }  
    }
    
    private void startCreateUnitBehaviour(){
        Action createUnitAction = new Action(getAID(), new CreateUnit());
        addBehaviour(new Conversation(this, ontology, codec, createUnitAction) {
            @Override
            public void onStart() {
                AID worldAID = (AID) worldAgent[0].getName();

                sendMessage(worldAID, ACLMessage.REQUEST
                        , new Conversation.SentMessageHandler() {

                    @Override
                    public void onSent(String conversationID) {

                        receiveResponse(conversationID, new Conversation.ResponseHandler() {

                            @Override
                            public void onAgree(ACLMessage response) {
                                System.out.println(myAgent.getLocalName()
                                        + ": received unit creation agree from " + response.getSender().getLocalName());

                                receiveResponse(conversationID, new Conversation.ResponseHandler() {

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
            
        });
    }
    
    private void startMoveToCellBehaviour(){
        Cell newCellPosition = new Cell();
        //TODO by default 0,0
        newCellPosition.setX(0);
        newCellPosition.setY(0);
        Action moveToCell = new Action(getAID(), newCellPosition);
        addBehaviour(new Conversation(this, ontology, codec, moveToCell){

            @Override
            public void onStart() {
                
                AID worldAID = (AID) worldAgent[0].getName();
                System.out.println(getLocalName() + ": request move to cell");
                sendMessage(worldAID, ACLMessage.REQUEST
                        , new Conversation.SentMessageHandler() {
                    @Override
                    public void onSent(String conversationID) {

                        receiveResponse(conversationID, new Conversation.ResponseHandler() {

                            @Override
                            public void onAgree(ACLMessage response) {
                                System.out.println(myAgent.getLocalName()
                                        + ": received unit 'move to cell' agree from " + response.getSender().getLocalName());

                                receiveResponse(conversationID, new Conversation.ResponseHandler() {

                                    @Override
                                    public void onFailure(ACLMessage response) {
                                        System.out.println(myAgent.getLocalName()
                                                + ": received unit 'move to cell' failure from " + response.getSender().getLocalName());
                                    }

                                    @Override
                                    public void onInform(ACLMessage response) {
                                        System.out.println(myAgent.getLocalName()
                                                + ": received unit 'move to cell' inform from " + response.getSender().getLocalName());
                                    }

                                });
                            }

                            @Override
                            public void onNotUnderstood(ACLMessage response) {
                                System.out.println(myAgent.getLocalName() + ": received unit 'move to cell' not understood from " + response.getSender().getLocalName());
                            }

                            @Override
                            public void onRefuse(ACLMessage response) {
                                System.out.println(myAgent.getLocalName() + ": received unit 'move to cell' refuse from " + response.getSender().getLocalName());
                            }

                        });
                    }
                });        
            }
        });
    }
    
    private boolean checkNewUnitIsNeeded(){
        return false;
    }
    
    private boolean checkMoveToCellIsNeeded(){
        return true;
    }
}
