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
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
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

    private boolean finished;

    public Conversation(Agent agent) {
        super(agent);
    }

    protected void sendFirstMessage(final Envelope envelope, SentMessageHandler handler) {

        myAgent.addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {

                ACLMessage newMsg = new ACLMessage(envelope.getPerformative());
                String conversationID = UUID.randomUUID().toString();

                newMsg.setOntology(envelope.getOntology().getName());
                newMsg.setLanguage(envelope.getCodec().getName());
                newMsg.setConversationId(conversationID);
                newMsg.addReceiver(envelope.getReceiverAID());

                try {
                    myAgent.getContentManager().fillContent(newMsg, envelope.getAction());
                    myAgent.send(newMsg);
                    handler.onSent(conversationID);
                } catch (Codec.CodecException | OntologyException ex) {
                    handler.onSentMessageError();
                }

            }
        });
    }

    public void respondMessage(final Envelope envelope, ACLMessage lastMessage) {
        myAgent.addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {

                ACLMessage newMsg = lastMessage.createReply();
                newMsg.setPerformative(envelope.getPerformative());

                try {
                    myAgent.getContentManager().fillContent(newMsg, envelope.getAction());
                } catch (Codec.CodecException | OntologyException ex) {
                    Logger.getLogger(Conversation.class.getName()).log(Level.SEVERE, null, ex);
                }

                myAgent.send(newMsg);
            }
        });
    }

    protected void receiveResponse(final Envelope envelope, String conversationID, ResponseHandler handler) {

        myAgent.addBehaviour(new SimpleBehaviour(myAgent) {

            boolean received;

            @Override
            public void action() {
                MessageTemplate filter = generateMessageFilter(envelope, conversationID);

                ACLMessage response = myAgent.receive(filter);

                if (response != null) {

                    handleResponseByPerformative(handler, response);
                    received = true;
                } else {
                    handler.onResponseError();
                    block();
                }

            }

            @Override
            public boolean done() {
                return received;
            }

        });
    }

    protected void listenMessages(final Envelope envelope, ResponseHandler handler) {

        myAgent.addBehaviour(new CyclicBehaviour(myAgent) {

            @Override
            public void action() {
                MessageTemplate filter = generateMessageFilter(envelope, null);

                ACLMessage response = myAgent.receive(filter);

                if (response != null) {

                    handleResponseByPerformative(handler,
                            response);
                } else {
                    handler.onResponseError();
                    block();
                }

            }

        });

    }

    private MessageTemplate generateMessageFilter(Envelope envelope, String conversationID) {
        MessageTemplate filter = MessageTemplate
                .and(MessageTemplate.MatchLanguage(envelope.getCodec().getName()),
                        MessageTemplate.MatchOntology(envelope.getOntology().getName()));

        if (conversationID != null) {
            filter = MessageTemplate.and(filter,
                    MessageTemplate.MatchConversationId(conversationID));
        }
        if (envelope.getPerformative() != ACLMessage.UNKNOWN) {
            filter = MessageTemplate.and(filter, MessageTemplate
                    .MatchPerformative(envelope.getPerformative()));
        }

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

    @Override
    public abstract void onStart();

    @Override
    public final void action() {
        finished = true;
    }

    @Override
    public boolean done() {
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
        }

        public void onConfirm(ACLMessage response) {
        }

        public void onFailure(ACLMessage response) {
        }

        public void onRefuse(ACLMessage response) {
        }

        public void onAgree(ACLMessage response) {
        }

        public void onRequest(ACLMessage response) {
        }

        public void onNotUnderstood(ACLMessage response) {
        }

        public void onCancel(ACLMessage response) {
        }
        
        public void onResponseError() {
        }

    }

    protected interface Envelope {

        Ontology getOntology();

        int getPerformative();

        Action getAction();

        Codec getCodec();

        AID getReceiverAID();

    }

}
