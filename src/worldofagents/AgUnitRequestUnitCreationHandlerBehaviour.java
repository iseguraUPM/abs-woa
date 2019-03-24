package worldofagents;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


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
public class AgUnitRequestUnitCreationHandlerBehaviour extends CyclicBehaviour {
    
    private final Agent agUnit;
    public final static String WORLD = "WORLD";
    public final static String MESSAGE = "CreateNewUnit";
    
    
    public AgUnitRequestUnitCreationHandlerBehaviour(final Agent parent) {
        this.agUnit = parent;
    }
 
    private static final long serialVersionUID =1L;
    AID ag;
    
    @Override
    public void action(){
        //TODO: Cuando crea un agente (condiciones)
        myAgent.doWait(500000000);
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(WORLD);
        dfd.addServices(sd);

        try{
                // It finds agents of the required type
                DFAgentDescription[] worldAgent = new DFAgentDescription[1];
                worldAgent = DFService.search(myAgent, dfd);

                if (worldAgent.length > 0){
                    ag = (AID)worldAgent[0].getName();

                    // Sends the request to the world
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setContent(MESSAGE);
                    msg.addReceiver(ag);
                    myAgent.send(msg);
                    System.out.println(myAgent.getLocalName()+": Sends a request to create a new unit");

                    myAgent.doWait(50000);
                }else{
                    // TODO: What if the world isn't found (It should be) Re send the message in some time?
                    System.out.println(myAgent.getLocalName()+ ": WORLD NOT FOUND");
                    myAgent.doWait(50000);
                        
                }
                
        }catch (Exception e){
                e.printStackTrace();
        }
    }
}
