/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package worldofagents.agunit;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Date;

/**
 *
 * @author Juan Pablo
 */
public class AgUnitReceiveFailureHandlerBehaviour extends CyclicBehaviour {
    //ESTE REFUSE SE PUEDE USAR PARA TODOS LOS REFUSES, CON DIFERENTES CAMPOS EN EL CONTENIDO DEL MENSAJE.
    private final Agent agUnit;
    private final String messageCreateUnit = "CreateNewUnit";
    
    public AgUnitReceiveFailureHandlerBehaviour(final Agent parent) {
        this.agUnit = parent;
    }
 
    private static final long serialVersionUID =1L;
    AID ag;
    
    @Override    
    public void action() {
        ACLMessage msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.FAILURE));
        if (msg != null) {
            if(msg.getContent().equals(messageCreateUnit)){
                System.out.println(myAgent.getLocalName()+": received unit creation failure from "+(msg.getSender()).getLocalName());
            }
        }else {
            // If no message arrives
            block();
        }

    }
}
