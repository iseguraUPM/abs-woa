/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.ontology.Group1Ontology;
import es.upm.woa.agent.group1.ontology.ShareMapData;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.protocol.Conversation;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.lang.acl.ACLMessage;

import java.util.logging.Level;

/**
 *
 * @author ISU
 */
class ReceiveShareMapDataBehaviourHelper {
    
    private final GroupAgent groupAgent;
    private final CommunicationStandard comStandard;
    private final GraphGameMap knownMap;
    
    public ReceiveShareMapDataBehaviourHelper(GroupAgent groupAgent
            , CommunicationStandard comStandard, GraphGameMap knownMap) {
        this.groupAgent = groupAgent;
        this.comStandard = comStandard;
        this.knownMap = knownMap;
    }
    
    /**
     * Start listening behaviour for NotifyCellDetail agent inform.
     */
    public void startShareMapDataBehaviour() {
        Action informCellDetailAction = new Action(groupAgent.getAID(), null);
        groupAgent.addBehaviour(new Conversation(groupAgent, comStandard
                , informCellDetailAction, Group1Ontology.SHAREMAPDATA) {
            @Override
            public void onStart() {
                groupAgent.log(Level.INFO, "listening to ShareMapData messages");
                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onInform(ACLMessage response) {
                        try {
                            handleShareMapDataInform(response);
                        } catch (Codec.CodecException | OntologyException ex) {
                            groupAgent.log(Level.WARNING, "could not receive message"
                                    + " (" + ex + ")");
                        }

                    }

                });
            }
        });
    }

    private void handleShareMapDataInform(ACLMessage response)
            throws OntologyException, Codec.CodecException {
        ContentElement ce = groupAgent.getContentManager().extractContent(response);
        if (ce instanceof Action) {

            Action agAction = (Action) ce;
            Concept conc = agAction.getAction();

            if (conc instanceof ShareMapData) {
                groupAgent.log(Level.FINER, "receive NotifyCellDetail inform from "
                        + response.getSender().getLocalName());

                ShareMapData mapDataAction = (ShareMapData) conc;

                if (mapDataAction.getKnownMap() instanceof GraphGameMap) {
                    learnNewGraphMapData((GraphGameMap) mapDataAction.getKnownMap());
                }
                else {
                    groupAgent.log(Level.WARNING, "Could not retrieve map data");
                }
                return;
            }
        }
        
        groupAgent.log(Level.WARNING, "Could not retrieve map data action");
    }
    
    private void learnNewGraphMapData(GraphGameMap otherGameMap) {
        knownMap.mergeMapData(otherGameMap);
    }
    
}
