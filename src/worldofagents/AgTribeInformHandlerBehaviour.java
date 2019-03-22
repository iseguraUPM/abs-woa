/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package worldofagents;

import jade.content.ContentElement;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import worldofagents.ontology.NotifyNewUnit;

/**
 *
 * @author Martin
 */
public class AgTribeInformHandlerBehaviour extends CyclicBehaviour{

    private static final long serialVersionUID = 1L;
    private final AgTribe agTribe;

    public AgTribeInformHandlerBehaviour(final AgTribe parent){
        this.agTribe = parent;
    }

    @Override
    public void action() {
        ACLMessage msg = agTribe.receive(MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchOntology(agTribe.getOntology().getName())));
        
        if (msg != null) {
            try {
                ContentElement ce = agTribe.getContentManager().extractContent(msg);
                
                if (ce instanceof NotifyNewUnit){
                    NotifyNewUnit newUnitInfo = (NotifyNewUnit) ce;
                    System.out.println(agTribe.getLocalName()+": received inform request from "+(msg.getSender()).getLocalName());    
                    //agTribe.addUnit(newUnitInfo);
                }
            } catch (Exception e) {
            }         
            
        }else {
            // If no message arrives
            block();
        }
    }
    
    
}
