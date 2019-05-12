/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.GameMap;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.map.MapCellFactory;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.NotifyUnitPosition;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.lang.acl.ACLMessage;

import java.util.NoSuchElementException;
import java.util.logging.Level;

/**
 *
 * @author ISU
 */
class ReceiveInformUnitPositionBehaviourHelper {
    
    private final GroupAgent groupAgent;
    private final CommunicationStandard comStandard;
    private final GameMap knownMap;
    
    public ReceiveInformUnitPositionBehaviourHelper(GroupAgent groupAgent
            , CommunicationStandard comStandard, GameMap knownGameMap) {
        this.groupAgent = groupAgent;
        this.comStandard = comStandard;
        this.knownMap = knownGameMap;
    }
    
    /**
     * Start listening behaviour for NotifyUnitPosition agent inform.
     */
    public void startInformCellDetailBehaviour() {
        groupAgent.addBehaviour(new Conversation(groupAgent, comStandard
                ,  GameOntology.NOTIFYUNITPOSITION) {
            @Override
            public void onStart() {
                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onInform(ACLMessage response) {
                        try {
                            handleNotifyUnitPositionMessage(response);
                        } catch (Codec.CodecException | OntologyException ex) {
                            groupAgent.log(Level.WARNING, "could not receive message"
                                    + " (" + ex + ")");
                        }

                    }

                });
            }
        });
    }

    private void handleNotifyUnitPositionMessage(ACLMessage response)
            throws OntologyException, Codec.CodecException {
        ContentElement ce = groupAgent.getContentManager().extractContent(response);
        if (ce instanceof Action) {

            Action agAction = (Action) ce;
            Concept conc = agAction.getAction();

            if (conc instanceof NotifyUnitPosition) {
                groupAgent.log(Level.FINER, "receive NotifyUnitPosition inform from "
                        + response.getSender().getLocalName());

                NotifyUnitPosition unitPosition = (NotifyUnitPosition) conc;

                Cell informedCell = unitPosition.getCell();
                try {
                    MapCell knownCell = knownMap
                            .getCellAt(informedCell.getX(),
                                     informedCell.getY());
                    groupAgent.onUnitPassby(knownCell, unitPosition.getTribeId());
                } catch (NoSuchElementException ex) {
                    // Should not reach
                    groupAgent.log(Level.WARNING, "Unit passed by an unknown position");
                }
                
            }
        }
    }
    
    
}
