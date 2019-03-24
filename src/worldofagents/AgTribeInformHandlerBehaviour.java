package worldofagents;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import worldofagents.AgTribe;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.onto.basic.Action;
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
                MessageTemplate.MatchLanguage(agTribe.getCodec().getName()),
                MessageTemplate.MatchOntology(agTribe.getOntology().getName())
            ));
        
        if (msg != null) {
            try {
                if(msg.getPerformative() == ACLMessage.INFORM){
                    ContentElement ce = agTribe.getContentManager().extractContent(msg);
                    if (ce instanceof Action){

                        Action agAction = (Action) ce;
			Concept conc = agAction.getAction();
                        
                        if (conc instanceof NotifyNewUnit){
                            System.out.println(agTribe.getLocalName()+": received inform request from "+(msg.getSender()).getLocalName());
                            NotifyNewUnit newUnitInfo = (NotifyNewUnit) conc;
                            agTribe.addUnit(newUnitInfo);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }         
            
        }else {
            // If no message arrives
            block();
        }
    }
    
    
}
