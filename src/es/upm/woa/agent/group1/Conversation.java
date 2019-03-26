/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ISU
 */
public abstract class Conversation extends SimpleBehaviour {
    
    private SequentialBehaviour conversationBehaviour;
    private String conversationID;
    private ACLMessage lastResponse;
    
    public Conversation(Agent agent) {
        conversationBehaviour = new SequentialBehaviour(agent);
        conversationID = UUID.randomUUID().toString();
    }
    
    @Override
    public final void action() {
        onStart();
        myAgent.addBehaviour(conversationBehaviour);
    }
    
    @Override
    public final boolean done() {
        return conversationBehaviour.done();
    }
    
    public abstract void onStart();
    
    protected void sendMessage(Envelope envelope) {
        conversationBehaviour.addSubBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                
                ACLMessage newMsg;
                if (lastResponse == null) {
                    newMsg = new ACLMessage(envelope.getPerformative());
                    
                    newMsg.setOntology(envelope.getOntology().getName());
                    newMsg.setLanguage(envelope.getCodec().getName());
                    newMsg.setConversationId(conversationID);

                    try {
                        myAgent.getContentManager().fillContent(newMsg, envelope.getAction());
                    } catch (Codec.CodecException | OntologyException ex) {
                        Logger.getLogger(Conversation.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                else {
                    newMsg = lastResponse.createReply();
                }   
                
                myAgent.send(newMsg);
            }
        });
    }
    
    protected void waitReponse(final Envelope envelope, ResponseHandler handler) {
        
        conversationBehaviour.addSubBehaviour(new OneShotBehaviour(myAgent) {
        
            @Override
            public void action() {
                MessageTemplate filter = generateMessageFilter(envelope);
                
                lastResponse = myAgent.receive(filter);
                        
                if (lastResponse != null) {
                    try {
                        ContentElement ce = myAgent.getContentManager()
                                .extractAbsContent(lastResponse);

                        if (ce instanceof Action) {

                            Action action = (Action) ce;
                            Concept content = action.getAction();

                            
                            handleResponseByPerformative(handler
                                    , lastResponse.getPerformative(), content);
                        }
                    }
                    catch (Codec.CodecException | OntologyException ex) {

                        Logger.getLogger(Conversation.class.getName()).log(Level.SEVERE, null, ex);
                        handler.onResponseError();
                    }

                }
                else {
                    handler.onResponseError();
                }
            
            }
            
            
        });
        
    }
    
    private MessageTemplate generateMessageFilter(Envelope envelope) {
        MessageTemplate filter = MessageTemplate
                .and(MessageTemplate.MatchLanguage(envelope.getCodec().getName()),
                        MessageTemplate.MatchOntology(envelope.getOntology().getName()));
        filter = MessageTemplate.and(filter
                , MessageTemplate.MatchConversationId(conversationID));
        return filter;
    }
    
    private void handleResponseByPerformative(ResponseHandler handler, int performative, Concept content) {
        switch (performative) {
            case ACLMessage.AGREE:
                handler.onAgree(content);
                break;
            case ACLMessage.REFUSE:
                handler.onRefuse(content);
                break;
            case ACLMessage.CONFIRM:
                handler.onConfirm(content);
                break;
            case ACLMessage.CANCEL:
                handler.onCancel(content);
                break;
            case ACLMessage.INFORM:
                handler.onInform(content);
                break;
            case ACLMessage.FAILURE:
                handler.onFailure(content);
                break;
            case ACLMessage.NOT_UNDERSTOOD:
            default:
                handler.onNotUnderstood(content);
                break;
        }
    }
    
    abstract class ResponseHandler {
        
        public void onInform(Concept content) {}
        
        public void onConfirm(Concept content) {}
        
        public void onFailure(Concept content) {}
        
        public void onRefuse(Concept content) {}
        
        public void onAgree(Concept content) {}
        
        public void onRequest(Concept content) {}
        
        public void onNotUnderstood(Concept content) {}
        
        public void onCancel(Concept content) {};
        
        public void onResponseError() {}
        
    }
    
    interface Envelope {
        
        Ontology getOntology();
        
        int getPerformative();
        
        Action getAction();
        
        Codec getCodec();
        
        AID getReceiverAID();
        
    }
   
}
    

