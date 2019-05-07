/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.GameMap;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.map.MapCellFactory;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.Empty;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.NotifyCellDetail;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.lang.acl.ACLMessage;

import java.util.NoSuchElementException;
import java.util.logging.Level;

/**
 *
 * @author ISU
 */
class ReceiveInformCellDetailBehaviourHelper {
    
    private final GroupAgent groupAgent;
    private final Ontology ontology;
    private final Codec codec;
    private final GameMap knownMap;
    
    public ReceiveInformCellDetailBehaviourHelper(GroupAgent groupAgent
            , Ontology ontology, Codec codec, GameMap knownMap) {
        this.groupAgent = groupAgent;
        this.ontology = ontology;
        this.codec = codec;
        this.knownMap = knownMap;
    }
    
    public void startInformCellDetailBehaviour() {
        Action informCellDetailAction = new Action(groupAgent.getAID(), null);
        groupAgent.addBehaviour(new Conversation(groupAgent, ontology, codec
                , informCellDetailAction, GameOntology.NOTIFYCELLDETAIL) {
            @Override
            public void onStart() {
                groupAgent.log(Level.INFO, "listening to NotifyCellDetail messages");
                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onInform(ACLMessage response) {
                        try {
                            handleNotifyCellDetailMessage(response);
                        } catch (Codec.CodecException | OntologyException ex) {
                            groupAgent.log(Level.WARNING, "could not receive message"
                                    + " (" + ex + ")");
                        }

                    }

                });
            }
        });
    }

    private void handleNotifyCellDetailMessage(ACLMessage response)
            throws OntologyException, Codec.CodecException {
        ContentElement ce = groupAgent.getContentManager().extractContent(response);
        if (ce instanceof Action) {

            Action agAction = (Action) ce;
            Concept conc = agAction.getAction();

            if (conc instanceof NotifyCellDetail) {
                groupAgent.log(Level.FINER, "receive NotifyCellDetail inform from "
                        + response.getSender().getLocalName());

                NotifyCellDetail cellDetail = (NotifyCellDetail) conc;

                Cell informedCell = cellDetail.getNewCell();
                boolean success = knownMap
                        .addCell(MapCellFactory.getInstance()
                                .buildCell(informedCell));
                try {
                    MapCell knownCell = knownMap
                            .getCellAt(informedCell.getX(),
                                     informedCell.getY());
                    if (success) {
                        groupAgent.onCellDiscovered(knownCell);
                        
                    } else {
                        updateCellContents(knownCell, informedCell);
                        groupAgent.onCellUpdated(knownCell);
                    }
                    
                } catch (NoSuchElementException ex) {
                    // Should not reach
                    groupAgent.log(Level.WARNING, "Could not retrieve known cell");
                }
                return;
            }
        }
        
        groupAgent.log(Level.WARNING, "Could not retrieve cell detail action");
    }
    
    private void updateCellContents(MapCell knownCell, Cell updatedCell) {
        if (knownCell.getContent() instanceof Empty) {
            knownCell.setContent((Concept) updatedCell.getContent());
        }
    }
}
