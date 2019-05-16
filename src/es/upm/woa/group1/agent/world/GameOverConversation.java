/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent.world;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 *
 * @author ISU
 */
public class GameOverConversation extends SimpleBehaviour
        implements GameOverResource {
    
    public GameOverConversation(Agent agent) {
        super(agent);
    }
    
    @Override
    public void action() {
        
        MessageTemplate filter = MessageTemplate.MatchAll();
        
        ACLMessage msg = myAgent.receive(filter);

        if (msg != null) {
            
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);

            myAgent.send(msg);
        } else {
            block();
        }
    }

    @Override
    public boolean done() {
        return false;
    }
    
}
