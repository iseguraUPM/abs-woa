/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package worldofagents;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
