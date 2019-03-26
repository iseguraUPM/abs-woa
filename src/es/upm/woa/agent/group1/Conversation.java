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
import jade.core.MicroRuntime;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CompositeBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.leap.Collection;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ISU
 */
public abstract class Conversation extends SequentialBehaviour {
    
    private String conversationID;
    private ACLMessage lastResponse;
    
    public Conversation(Agent agent) {
        super(agent);
    }
    
    protected void sendMessage(final Envelope envelope) { 
        if (getCurrent() != null && getCurrent() instanceof WaitBehaviour) {
            removeSubBehaviour(getCurrent());
        }
        
        addSubBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                
                ACLMessage newMsg;
                if (lastResponse == null) {
                    newMsg = new ACLMessage(envelope.getPerformative());
                    conversationID = UUID.randomUUID().toString();
                    
                    newMsg.setOntology(envelope.getOntology().getName());
                    newMsg.setLanguage(envelope.getCodec().getName());
                    newMsg.setConversationId(conversationID);
                    newMsg.addReceiver(envelope.getReceiverAID());
                }
                else {
                    newMsg = lastResponse.createReply();
                    newMsg.setPerformative(envelope.getPerformative());
                    lastResponse = null;
                }   
                
                try {
                    myAgent.getContentManager().fillContent(newMsg, envelope.getAction());
                } catch (Codec.CodecException | OntologyException ex) {
                    Logger.getLogger(Conversation.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                myAgent.send(newMsg);
            }
        });
        
        addSubBehaviour(new WaitBehaviour());
    }
    
    protected void waitReponse(final Envelope envelope, ResponseHandler handler) {
        if (getCurrent() != null && getCurrent() instanceof WaitBehaviour) {
            removeSubBehaviour(getCurrent());
        }
        
        addSubBehaviour(new SimpleBehaviour(myAgent) {
            
            boolean received;
        
            @Override
            public void action() {
                MessageTemplate filter = generateMessageFilter(envelope);
                
                ACLMessage response = myAgent.receive(filter);
                        
                if (response != null) {
                    
                    handleResponseByPerformative(handler
                                    , response);
                    lastResponse = response;
                    received = true;
                    block();
                }
                else {
                    handler.onResponseError();
                    block();
                }
            
            }
            
            @Override
            public boolean done() {
                return received;
            }
            
            
        });
        
        addSubBehaviour(new WaitBehaviour());
    }
    
    private MessageTemplate generateMessageFilter(Envelope envelope) {
        MessageTemplate filter = MessageTemplate
                .and(MessageTemplate.MatchLanguage(envelope.getCodec().getName()),
                        MessageTemplate.MatchOntology(envelope.getOntology().getName()));
        
        if (conversationID != null) {
            filter = MessageTemplate.and(filter
                    , MessageTemplate.MatchConversationId(conversationID));
        }
        if (envelope.getPerformative() != ACLMessage.UNKNOWN) {
            filter = MessageTemplate.and(filter, MessageTemplate
                    .MatchPerformative(envelope.getPerformative()));
        }
        
        return filter;
    }
    
    private void handleResponseByPerformative(ResponseHandler handler
            , ACLMessage response) {
        
        try {
            if (response.getContent() != null) {
                ContentElement ce = myAgent.getContentManager()
                        .extractContent(response);
                if (ce instanceof Action) {

                Action action = (Action) ce;
                Concept content = action.getAction();

                switch (response.getPerformative()) {
                    case ACLMessage.REQUEST:
                        handler.onRequest(response.getSender(), content);
                        break;
                    case ACLMessage.AGREE:
                        handler.onAgree(response.getSender(), content);
                        break;
                    case ACLMessage.REFUSE:
                        handler.onRefuse(response.getSender(), content);
                        break;
                    case ACLMessage.CONFIRM:
                        handler.onConfirm(response.getSender(), content);
                        break;
                    case ACLMessage.CANCEL:
                        handler.onCancel(response.getSender(), content);
                        break;
                    case ACLMessage.INFORM:
                        handler.onInform(response.getSender(), content);
                        break;
                    case ACLMessage.FAILURE:
                        handler.onFailure(response.getSender(), content);
                        break;
                    case ACLMessage.NOT_UNDERSTOOD:
                    default:
                        handler.onNotUnderstood(response.getSender(), content);
                        break;
                }
            }

            }
        }
        catch (Codec.CodecException | OntologyException ex) {

            Logger.getLogger(Conversation.class.getName()).log(Level.SEVERE, null, ex);
            handler.onResponseError();
        }
        
    }
    
    abstract class ResponseHandler {
        
        public void onInform(AID sender, Concept content) {}
        
        public void onConfirm(AID sender, Concept content) {}
        
        public void onFailure(AID sender, Concept content) {}
        
        public void onRefuse(AID sender, Concept content) {}
        
        public void onAgree(AID sender, Concept content) {}
        
        public void onRequest(AID sender, Concept content) {}
        
        public void onNotUnderstood(AID sender, Concept content) {}
        
        public void onCancel(AID sender, Concept content) {};
        
        public void onResponseError() {}
        
    }
    
    interface Envelope {
        
        Ontology getOntology();
        
        int getPerformative();
        
        Action getAction();
        
        Codec getCodec();
        
        AID getReceiverAID();
        
    }
    
    private class WaitBehaviour extends SimpleBehaviour {

        @Override
        public void action() {
            block();
        }

        @Override
        public boolean done() {
            return false;
        }
        
    }
   
}
    

