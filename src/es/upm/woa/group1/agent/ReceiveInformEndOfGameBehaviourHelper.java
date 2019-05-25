/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.protocol.CommunicationStandard;
import es.upm.woa.group1.protocol.Conversation;
import es.upm.woa.ontology.GameOntology;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.wrapper.StaleProxyException;
import java.util.logging.Level;

/**
 *
 * @author Martin
 */
public class ReceiveInformEndOfGameBehaviourHelper {
    
    private final GroupAgent groupAgent;
    private final CommunicationStandard comStandard;
    
    public ReceiveInformEndOfGameBehaviourHelper(
            GroupAgent groupAgent,
            CommunicationStandard comStandard){
        this.groupAgent = groupAgent;
        this.comStandard = comStandard;
    }
    
    /**
    * Start listening behaviour for EndOfGame agent inform.
    */
    public void startEndOfGameBehaviour() {
        groupAgent.addBehaviour(new Conversation(groupAgent, comStandard
                , GameOntology.ENDOFGAME) {
            @Override
            public void onStart() {
                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onInform(ACLMessage response) {
                        try {
                            DFService.deregister(myAgent);
                        } catch (FIPAException ex) {
                             groupAgent.log(Level.WARNING, 
                                     "could not deregister in the DF (" + ex + ")");
                        }
                        
                        try {
                            groupAgent.getContainerController().kill();
                            groupAgent.log(Level.FINE, "could suicide successfully");
                        } catch (StaleProxyException ex) {
                            groupAgent.log(Level.WARNING, "could not suicide (" + ex + ")");
                        }
                    }

                });
            }
        });
    }
}
