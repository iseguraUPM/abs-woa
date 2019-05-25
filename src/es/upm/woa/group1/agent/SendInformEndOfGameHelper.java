/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.protocol.CommunicationStandard;
import es.upm.woa.group1.protocol.Conversation;
import es.upm.woa.ontology.GameOntology;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import java.util.Collection;

/**
 *
 * @author Martin
 */
public class SendInformEndOfGameHelper {
    private final AgWorld groupAgent;
    private final CommunicationStandard comStandard;
    private final Collection<AID> agentsAIDCollection;    
    
    public SendInformEndOfGameHelper(AgWorld groupAgent, CommunicationStandard comStandard, Collection<AID> agentsAIDCollection) {
        this.groupAgent = groupAgent;
        this.comStandard = comStandard;
        this.agentsAIDCollection = agentsAIDCollection;
    }
    
    /**
     * Sends an "end of game" inform to every agent that has been registered
     */
    public void informEnfOfGame() {
        for (AID agentAID : agentsAIDCollection) {
            groupAgent.addBehaviour(new Conversation(groupAgent, comStandard, GameOntology.ENDOFGAME) {
                @Override
                public void onStart() {
                    sendMessage(agentAID, ACLMessage.INFORM, null, new Conversation.SentMessageHandler() {
                    });
                }     
            });  
        }       
    }
}

