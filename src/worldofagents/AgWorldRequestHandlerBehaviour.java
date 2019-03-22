/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package worldofagents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 *
 * @author ISU
 */
public class AgWorldRequestHandlerBehaviour extends CyclicBehaviour {
    
    private final Agent agWorld;
    
    public AgWorldRequestHandlerBehaviour(final Agent parent) {
        this.agWorld = parent;
    }

    private static final long serialVersionUID = 1L;
            
    @Override
    public void action() {
        // Waits for requests
        ACLMessage msg = agWorld.receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
        if (msg != null) {
            // TODO: check msg content
            // TODO: create unit request logic

            System.out.println(myAgent.getLocalName() + ": received create unit request from " + (msg.getSender()).getLocalName());
            ACLMessage reply = msg.createReply();

            reply.setContent("");
            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
            myAgent.send(reply);
            System.out.println(myAgent.getLocalName() + ": answer sent -> " + reply.getContent());

            AID unitRequester = msg.getSender();

            new Thread(() -> {
                // TODO: send to requesters tribe
                // TODO: wait 150 hours
                // TODO: how to launch agents in code

                //ACLMessage informNewUnit = ;
            }).run();

        } else {
            // If no message arrives
            block();
        }

    }
    
}
