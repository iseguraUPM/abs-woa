/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.protocol.CommunicationStandard;
import es.upm.woa.group1.protocol.Conversation;
import es.upm.woa.ontology.EndOfGame;
import es.upm.woa.ontology.GameOntology;
import jade.content.onto.basic.Action;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.Collection;

/**
 *
 * @author Martin
 */
class SendInformEndOfGameHelper {
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
        Action endofgameaction = new Action();
        endofgameaction.setAction(new EndOfGame());
        
        final AID[] receipts = agentsAIDCollection.toArray(new AID[agentsAIDCollection.size()]);
        groupAgent.addBehaviour(new Conversation(groupAgent, comStandard, GameOntology.ENDOFGAME) {
            @Override
            public void onStart() {
                sendMessage(receipts, ACLMessage.INFORM, endofgameaction
                        , new Conversation.SentMessageHandler() {
                });
            }     
        });  
             
    }
}

