/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.protocol;

import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.io.IOException;
import java.io.Serializable;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A behavior used for implementing interchanging messages between two or
 * more agents.
 * @author ISU
 */
public abstract class Conversation extends SimpleBehaviour {

    private final CommunicationStandard comStandard;
    private final String protocol;
    private boolean finished;

    public Conversation(Agent agent, CommunicationStandard com
            , String protocol) {
        super(agent);
        this.finished = false;
        this.comStandard = com;
        this.protocol = protocol;
    }

    /**
     * Sends a message to the required recipients
     * @param receivers
     * @param performative action to set the message
     * @param action
     * @param handler
     */
    protected void sendMessage(AID[] receivers, int performative, Action action, SentMessageHandler handler) {
        myAgent.addBehaviour(new OneShotBehaviour(myAgent) {
            @Override
            public void action() {

                ACLMessage newMsg = new ACLMessage(performative);
                String conversationID = UUID.randomUUID().toString();

                newMsg.setOntology(comStandard.getOntology().getName());
                newMsg.setLanguage(comStandard.getCodec().getName());
                newMsg.setConversationId(conversationID);
                newMsg.setProtocol(protocol);
                
                for (AID receiverAID : receivers) {
                    newMsg.addReceiver(receiverAID);
                }

                try {
                    if (action.getAction() != null) {
                        myAgent.getContentManager().fillContent(newMsg, action);
                    }
                    myAgent.send(newMsg);
                    handler.onSent(conversationID);
                } catch (Codec.CodecException | OntologyException ex) {
                    handler.onSentMessageError();
                }

            }
        });
    }
    
    protected void sendMessage(AID[] receivers, Serializable object, int performative, SentMessageHandler handler) {
        myAgent.addBehaviour(new OneShotBehaviour(myAgent) {
            @Override
            public void action() {

                ACLMessage newMsg = new ACLMessage(performative);
                String conversationID = UUID.randomUUID().toString();

                newMsg.setOntology(comStandard.getOntology().getName());
                newMsg.setLanguage(comStandard.getCodec().getName());
                newMsg.setConversationId(conversationID);
                newMsg.setProtocol(protocol);
                
                try {
                    newMsg.setContentObject(object);
                } catch (IOException ex) {
                    handler.onSentMessageError();
                    return;
                }
                
                for (AID receiverAID : receivers) {
                    newMsg.addReceiver(receiverAID);
                }

            }
        });
    }
    
    /**
     * Send message to one recipient
     * @param receiver
     * @param performative
     * @param action
     * @param handler 
     */
    protected void sendMessage(AID receiver, int performative, Action action, SentMessageHandler handler) {
        sendMessage(new AID[]{receiver}, performative, action, handler);
    }
    
    /**
     * Send message to one recipient with a serialized object
     * @param receiver
     * @param performative
     * @param object
     * @param handler 
     */
    protected void sendMessage(AID receiver, Serializable object, int performative, SentMessageHandler handler) {
        sendMessage(new AID[]{receiver}, object, performative, handler);
    }

    /**
     * Reply a message from one conversation
     * @param message
     * @param performative 
     * @param object 
     */
    protected void respondMessage(ACLMessage message, Serializable object, int performative) {
        myAgent.addBehaviour(new OneShotBehaviour(myAgent) {
            @Override
            public void action() {

                ACLMessage newMsg = message.createReply();
                
                newMsg.setPerformative(performative);
                
                try {
                    newMsg.setContentObject(object);
                } catch (IOException ex) {
                    Logger.getGlobal().log(Level.WARNING, "Could not fill contents of message ({0})", ex);
                    return;
                }

                myAgent.send(newMsg);
            }
        });
    }
    
    /**
     * Reply a message from one conversation
     * @param message
     * @param performative 
     * @param action 
     */
    protected void respondMessage(ACLMessage message, int performative, Action action) {
        myAgent.addBehaviour(new OneShotBehaviour(myAgent) {
            @Override
            public void action() {

                ACLMessage newMsg = message.createReply();
                
                newMsg.setPerformative(performative);

                try {
                    if (action.getAction() != null) {
                        myAgent.getContentManager().fillContent(newMsg, action);
                    }
                } catch (Codec.CodecException | OntologyException ex) {
                    Logger.getGlobal().log(Level.WARNING, "Could not fill contents of message ({0})", ex);
                    return;
                }

                myAgent.send(newMsg);
            }
        });
    }

    /**
     * Listen to a message from a given conversation
     * @param conversationID
     * @param handler 
     */
    protected void receiveResponse(String conversationID, ResponseHandler handler) {

        myAgent.addBehaviour(new SimpleBehaviour(myAgent) {

            boolean received;

            @Override
            public void action() {
                MessageTemplate filter = generateMessageFilter(conversationID);

                ACLMessage response = myAgent.receive(filter);

                if (response != null) {

                    handleResponseByPerformative(handler, response);
                    received = true;
                } else {
                    block();
                }

            }

            @Override
            public boolean done() {
                return received;
            }

        });
    }

    /**
     * Listen to a given number of messages
     * @param messageLimit of messages to listen. If it is less than zero
     * it will listen forever.
     * @param handler 
     */
    protected void listenMessages(int messageLimit, ResponseHandler handler) {
        myAgent.addBehaviour(new SimpleBehaviour(myAgent) {
            
            int limit = messageLimit;

            @Override
            public void action() {
                MessageTemplate filter = generateMessageFilter();

                ACLMessage response = myAgent.receive(filter);
                
                
                if (response != null) {
                    
                    handleResponseByPerformative(handler,
                            response);
                    
                    if (limit > 0)
                        limit--;
                } else {
                    block();
                }
            }

            @Override
            public boolean done() {
                return limit == 0;
            }
        });
    }
    
    /**
     * Listen to messages indefinitely
     * @param handler 
     */
    protected void listenMessages(ResponseHandler handler) {
        listenMessages(-1, handler);
    }

    private MessageTemplate generateMessageFilter(String conversationID) {
        return MessageTemplate.and(generateMessageFilter(),
                MessageTemplate.MatchConversationId(conversationID)); 
    }
    
    private MessageTemplate generateMessageFilter() {
        MessageTemplate filter = MessageTemplate
                .and(MessageTemplate.MatchLanguage(comStandard.getCodec().getName()),
                        MessageTemplate.MatchOntology(comStandard.getOntology().getName()));
        filter = MessageTemplate.and(filter
                , MessageTemplate.MatchProtocol(protocol));
        
        return filter;
    }

    private void handleResponseByPerformative(ResponseHandler handler,
            ACLMessage response) {
        switch (response.getPerformative()) {
            case ACLMessage.REQUEST:
                handler.onRequest(response);
                break;
            case ACLMessage.AGREE:
                handler.onAgree(response);
                break;
            case ACLMessage.REFUSE:
                handler.onRefuse(response);
                break;
            case ACLMessage.CONFIRM:
                handler.onConfirm(response);
                break;
            case ACLMessage.CANCEL:
                handler.onCancel(response);
                break;
            case ACLMessage.INFORM:
                handler.onInform(response);
                break;
            case ACLMessage.FAILURE:
                handler.onFailure(response);
                break;
            case ACLMessage.NOT_UNDERSTOOD:
            default:
                handler.onNotUnderstood(response);
                break;
        }
    }
    
    private void logUnhandledMessage(String type, AID source) {
        Logger.getGlobal().log(Level.WARNING, "PROTOCOL: {0} from {1} to {2} not handled"
                , new Object[]{type, source.getLocalName(), myAgent.getAID().getLocalName()});
    }

    /**
     * Must override in order to instantiate a conversation. Here the conversation
     * methods for message handling are placed.
     */
    @Override
    public abstract void onStart();

    @Override
    public final void action() {
        finished = true;
    }

    @Override
    public final boolean done() {
        return finished;
    }

    protected abstract class SentMessageHandler {

        public void onSent(String conversationID) {
        }

        public void onSentMessageError() {
        }

    }

    protected abstract class ResponseHandler {

        public void onInform(ACLMessage response) {
            logUnhandledMessage("INFORM", response.getSender());
        }

        public void onConfirm(ACLMessage response) {
            logUnhandledMessage("CONFIRM", response.getSender());
        }

        public void onFailure(ACLMessage response) {
            logUnhandledMessage("FAILURE", response.getSender());
        }

        public void onRefuse(ACLMessage response) {
            logUnhandledMessage("REFUSE", response.getSender());
        }

        public void onAgree(ACLMessage response) {
            logUnhandledMessage("AGREE", response.getSender());
        }

        public void onRequest(ACLMessage response) {
            logUnhandledMessage("REQUEST", response.getSender());
        }

        public void onNotUnderstood(ACLMessage response) {
            logUnhandledMessage("NOT UNDERSTOOD", response.getSender());
        }

        public void onCancel(ACLMessage response) {
            logUnhandledMessage("CANCEL", response.getSender());
        }

    }

}
