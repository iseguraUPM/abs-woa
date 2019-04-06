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
import es.upm.woa.ontology.MoveToCell;
import jade.util.leap.ArrayList;
import jade.util.leap.List;

/**
 *
 * @author ISU
 */
public class AgUnit extends Agent {

    public static final String WORLD = "WORLD";
    private Ontology ontology;
    private SLCodec codec;
    private DFAgentDescription worldAgentServiceDescription;

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
        //Finds the World in the DF
        try{
            DFAgentDescription dfdWorld = new DFAgentDescription();
            ServiceDescription sdWorld = new ServiceDescription();
            sdWorld.setType(WORLD);
            dfdWorld.addServices(sdWorld);
            // It finds agents of the required type
            DFAgentDescription [] descriptions = DFService.search(this, dfdWorld);
            if (descriptions.length == 0) {
                //TODO what if the world is not found
            }
            else {
                worldAgentServiceDescription = descriptions[0];
            }
        }catch (FIPAException e) {
            System.err.println(this.getLocalName() + ": caught exception " + e);
        }  
    }

    private void initializeUnit()  {
        ontology = GameOntology.getInstance();
        codec = new SLCodec();
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);
    }
    
    private void startCreateUnitBehaviour(){
        Action createUnitAction = new Action(getAID(), new CreateUnit());
        addBehaviour(new Conversation(this, ontology, codec, createUnitAction) {
            @Override
            public void onStart() {
                AID worldAID = (AID) worldAgentServiceDescription.getName();

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
        newCellPosition.setContent(new ArrayList());
        //TODO this shouldn't be mandatory
        newCellPosition.setOwner(this.getAID());
        //TODO by default 0,0
        newCellPosition.setX(2);
        newCellPosition.setY(2);
        
        MoveToCell moveToCell = new MoveToCell();
        moveToCell.setTarget(newCellPosition);
        
        Action moveToCellAction = new Action(getAID(), moveToCell);

        addBehaviour(new Conversation(this, ontology, codec, moveToCellAction){

            @Override
            public void onStart() {
                
                AID worldAID = (AID) worldAgentServiceDescription.getName();

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
