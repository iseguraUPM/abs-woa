/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import jade.content.lang.Codec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.AID;

/**
 *
 * @author ISU
 */
public class ConversationEnvelope implements Conversation.Envelope {
    
    private final Ontology ontology;
    private final Codec codec;
    private final Action action;
    private final AID receiver;
    private final int performative;
    
    public ConversationEnvelope(Ontology ontology, Codec codec, Action action, AID receiver, int performative) {
        this.ontology = ontology;
        this.codec = codec;
        this.action = action;
        this.receiver = receiver;
        this.performative = performative;
    }

    @Override
    public Ontology getOntology() {
        return ontology;
    }

    @Override
    public int getPerformative() {
        return performative;
    }

    @Override
    public Action getAction() {
        return action;
    }

    @Override
    public Codec getCodec() {
        return codec;
    }

    @Override
    public AID getReceiverAID() {
        return receiver;
    }
    
}
