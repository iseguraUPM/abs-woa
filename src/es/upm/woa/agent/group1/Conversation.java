/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.ontology.CreateUnit;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ISU
 */
public abstract class Conversation extends SimpleBehaviour {
    
    private SequentialBehaviour conversationBehaviour;
    
    @Override
    public final void action() {
        
    }
    
    @Override
    public final boolean done() {
        return false;
    }
    
    public abstract void onStart();
    
    protected void sendMessage(Envelope envelope) {
        conversationBehaviour.addSubBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                Agent senderAgent = envelope.getSender();
                ACLMessage newMsg = new ACLMessage(envelope.getPerformative());
                
                newMsg.setOntology(envelope.getOntology().getName());
                newMsg.setLanguage(envelope.getCodec().getName());
                
                try {
                    senderAgent.getContentManager().fillContent(newMsg, envelope.getAction());
                } catch (Codec.CodecException | OntologyException ex) {
                    Logger.getLogger(Conversation.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                newMsg.addReceiver(envelope.getReceiverAID());
                
                senderAgent.send(newMsg);
            }
        });
    }
    
    protected void waitReponse(Envelope envelope, ResponseHandler handler) {
        
        conversationBehaviour.addSubBehaviour(new ParallelBehaviour(envelope.getSender()
                , ParallelBehaviour.WHEN_ANY) {
        

            @Override
            public void onStart() {
                MessageTemplate filter = MessageTemplate
                    .and(MessageTemplate.MatchLanguage(envelope.getCodec().getName()),
                         MessageTemplate.MatchOntology(envelope.getOntology().getName()));
                
                addSubBehaviour(new ReceiveResponseBehaviour(myAgent, filter, ACLMessage.INFORM) {
                    @Override
                    public void onReceive() {
                        handler.onInform();
                    }
                    
                    @Override
                    public void onError() {
                        handler.onResponseError();
                    }
                    
                });
                
                addSubBehaviour(new ReceiveResponseBehaviour(myAgent, filter, ACLMessage.NOT_UNDERSTOOD) {
                    @Override
                    public void onReceive() {
                        handler.onNotUnderstood();
                    }
                    
                    @Override
                    public void onError() {
                        handler.onResponseError();
                    }
                    
                });
                
                addSubBehaviour(new ReceiveResponseBehaviour(myAgent, filter, ACLMessage.FAILURE) {
                    @Override
                    public void onReceive() {
                        handler.onFailure();
                    }
                    
                    @Override
                    public void onError() {
                        handler.onResponseError();
                    }
                    
                });
                
                addSubBehaviour(new ReceiveResponseBehaviour(myAgent, filter, ACLMessage.AGREE) {
                    @Override
                    public void onReceive() {
                        handler.onAgree();
                    }
                    
                    @Override
                    public void onError() {
                        handler.onResponseError();
                    }
                    
                });
            }
            
            
        });
        
    }
    
    abstract class ResponseHandler {
        
        public void onInform() {}
        
        public void onFailure() {}
        
        public void onReject() {}
        
        public void onAgree() {}
        
        public void onRequest() {}
        
        public void onNotUnderstood() {}
        
        public void onResponseError() {}
        
    }
    
    
    interface Envelope {
        
        Agent getSender();
        
        Ontology getOntology();
        
        int getPerformative();
        
        Action getAction();
        
        Codec getCodec();
        
        AID getReceiverAID();
        
    }
    
    private abstract class ReceiveResponseBehaviour extends OneShotBehaviour {

        private MessageTemplate filter;
        private int performative;
        
        public ReceiveResponseBehaviour(Agent agent, MessageTemplate filter, int performative) {
            super(agent);
            this.filter = filter;
            this.performative = performative;
        }
        
        public abstract void onReceive();
        
        public abstract void onError();
        
        @Override
        public final void action() {
            ACLMessage response = myAgent.receive(MessageTemplate.and(filter
                                , MessageTemplate.MatchPerformative(performative)));
                        
                if (response != null) {
                    try {
                        ContentElement ce = myAgent.getContentManager()
                                .extractContent(response);

                        if (ce instanceof Action) {

                            Action agAction = (Action) ce;
                            Concept conc = agAction.getAction();

                            if (conc instanceof CreateUnit) {
                                onReceive();
                            }
                        }
                    } catch (Codec.CodecException | OntologyException ex) {

                        Logger.getLogger(Conversation.class.getName()).log(Level.SEVERE, null, ex);
                        onError();
                    }

                }
                else {
                    onError();
                }
            
                
        }
        
    }
   
}
    

