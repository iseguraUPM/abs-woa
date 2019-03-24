package worldofagents;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import jade.core.AID;
import worldofagents.AgTribe;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.util.logging.Level;
import java.util.logging.Logger;
import static worldofagents.AgUnitRequestUnitCreationHandlerBehaviour.MESSAGE;
import static worldofagents.AgUnitRequestUnitCreationHandlerBehaviour.WORLD;

/**
 *
 * @author ISU
 */
public class AgUnit extends Agent {
    
    public static final String UNIT = "UNIT";
    
    @Override
    protected void setup() {
        try {
            initializeAgent();
        } catch (FIPAException ex) {
            Logger.getLogger(AgTribe.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //Behaviours
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                //TODO: Cuando crea un agente (condiciones)
        myAgent.doWait(500000000);
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(WORLD);
        dfd.addServices(sd);

        try {
                // It finds agents of the required type
                DFAgentDescription[] worldAgent;
                worldAgent = DFService.search(myAgent, dfd);

                if (worldAgent.length > 0){
                    AID worldAID = (AID)worldAgent[0].getName();

                    // Sends the request to the world
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setContent(MESSAGE);
                    msg.addReceiver(worldAID);
                    myAgent.send(msg);
                    System.out.println(myAgent.getLocalName()+": Sends a request to create a new unit");

                    myAgent.doWait(50000);
                }else{
                    // TODO: What if the world isn't found (It should be) Re send the message in some time?
                    System.out.println(myAgent.getLocalName()+ ": WORLD NOT FOUND");
                    myAgent.doWait(50000);
                        
                }
                
        } catch (Exception e) {
                e.printStackTrace();
        }
            }
        });
        
        addBehaviour(new AgUnitRequestUnitCreationHandlerBehaviour(this));
        addBehaviour(new AgUnitReceiveRefuseHandlerBehaviour(this));
        addBehaviour(new AgUnitReceiveNotUnderstoodHandlerBehaviour(this));
        addBehaviour(new AgUnitReceiveAgreeHandlerBehaviour(this));
        addBehaviour(new AgUnitReceiveInformHandlerBehaviour(this));
        addBehaviour(new AgUnitReceiveFailureHandlerBehaviour(this));
    }
    
    private void initializeAgent() throws FIPAException {
        // Creates its own description
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setName(this.getName());
        sd.setType(UNIT);
        dfd.addServices(sd);
        // Registers its description in the DF
        DFService.register(this, dfd);
        System.out.println(getLocalName() + ": registered in the DF");
        dfd = null;
        sd = null;
    }
    
}
