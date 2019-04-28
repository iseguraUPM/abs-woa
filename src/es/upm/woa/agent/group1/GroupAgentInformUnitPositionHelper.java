/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.map.MapCellFactory;
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
class GroupAgentInformUnitPositionHelper {
    
    private final GroupAgent groupAgent;
    
    public GroupAgentInformUnitPositionHelper(GroupAgent groupAgent) {
        this.groupAgent = groupAgent;
    }
    
    public void startInformCellDetailBehaviour() {
        Action informUnitPositionAction = new Action(groupAgent.getAID(), null);
        groupAgent.addBehaviour(new Conversation(groupAgent, groupAgent.getOntology(), groupAgent.getCodec()
                , informUnitPositionAction, GameOntology.NOTIFYUNITPOSITION) {
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

                // NOTE: if a world mistakenly sends us an unknown position
                // we take it as a gift
                Cell informedCell = unitPosition.getCell();
                boolean success = groupAgent.getKnownMap()
                        .addCell(MapCellFactory.getInstance()
                                .buildCell(informedCell));
                try {
                    MapCell knownCell = groupAgent.getKnownMap()
                            .getCellAt(informedCell.getX(),
                                     informedCell.getY());
                    if (success) {
                        groupAgent.onCellDiscovered(knownCell);   
                    }
                    
                    groupAgent.onUnitPassby(knownCell, unitPosition.getTribeId()); 
                } catch (NoSuchElementException ex) {
                    // Should not reach
                    groupAgent.log(Level.WARNING, "Could not retrieve known cell");
                }
                
            }
        }
    }
    
    
}
