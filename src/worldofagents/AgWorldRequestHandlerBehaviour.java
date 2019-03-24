package worldofagents;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Timer;
import java.util.TimerTask;
import worldofagents.objects.Tribe;

/**
 *
 * @author ISU
 */
public class AgWorldRequestHandlerBehaviour extends CyclicBehaviour {
    
    private final AgWorld agWorld;
    private final String messageCreateUnit = "CreateNewUnit";
    
    
    public AgWorldRequestHandlerBehaviour(final AgWorld parent) {
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
            if(msg.getContent().equals(messageCreateUnit)){
                //We receive a message from a unit indicating us to create a new unit.
                //TODO: Checks and create the new unit.
                //TODO: CAMBIAR ELEGIROPCION POR LA MANERA REAL DE ELEGIR LA OPCION. Falta la logica
                int ELEGIROPCION = 2;
                
                if(ELEGIROPCION == 0){
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent(messageCreateUnit);
                    agWorld.send(reply);
                    System.out.println(agWorld.getLocalName()+": Refuses to create a new unit for " + (msg.getSender()).getLocalName());
                }else if(ELEGIROPCION == 1){
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                    reply.setContent(messageCreateUnit);
                    agWorld.send(reply);
                    System.out.println(agWorld.getLocalName()+": Doesn't understand how to create a new unit for " + (msg.getSender()).getLocalName());
                }else if(ELEGIROPCION == 2){
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.AGREE);
                    reply.setContent(messageCreateUnit);
                    agWorld.send(reply);
                    System.out.println(agWorld.getLocalName()+": Agrees to create a new unit for " + (msg.getSender()).getLocalName());
                    //TODO: Elegiropcion2 dependiendo de si se ha creado correctamente o ha habido algún problema al crear.
                    int ELEGIROPCION2 = 0;
                    if(ELEGIROPCION2 == 0){
                        ACLMessage newmsg = new ACLMessage(ACLMessage.FAILURE);
                        newmsg.setContent(messageCreateUnit);
                        newmsg.addReceiver(msg.getSender());
                        agWorld.send(newmsg);
                        System.out.println(agWorld.getLocalName()+": Sends failure to create a new unit to " + (msg.getSender()).getLocalName());
                    }else if(ELEGIROPCION2 == 1){
                        ACLMessage newmsg = new ACLMessage(ACLMessage.INFORM);
                        newmsg.setContent(messageCreateUnit);
                        newmsg.addReceiver(msg.getSender());
                        agWorld.send(newmsg);
                        System.out.println(agWorld.getLocalName()+": Sends inform to create a new unit to " + (msg.getSender()).getLocalName());
                    }
                }
                
            }else{
                System.out.println(agWorld.getLocalName() + ": received create unit request from " + (msg.getSender()).getLocalName());
                ACLMessage reply = msg.createReply();

                reply.setContent("");
                reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                myAgent.send(reply);
                System.out.println(agWorld.getLocalName() + ": answer sent -> " + reply.getContent());

                AID unitRequester = msg.getSender();
                
                new Timer().schedule(new TimerTask() {

                            @Override
                            public void run() {
                                agWorld.launchAgentUnitFromRequest(unitRequester);
                            }
                        //TODO: wait 150 seconds, in the future 150hours
                        }, 150000);
            }
          
        } else {
            // If no message arrives
            block();
        }

    }
    
}
