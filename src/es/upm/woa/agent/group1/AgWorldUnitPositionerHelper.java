/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.CellBuildingConstructor;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.map.UnitCellPositioner;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.agent.group1.protocol.Transaction;
import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.MoveToCell;

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
public class AgWorldUnitPositionerHelper {
    
    private final AgWorld agWorld;
    
    public AgWorldUnitPositionerHelper(AgWorld agWorldInstance) {
        agWorld = agWorldInstance;
    }
    
    public void startMoveToCellBehaviour() {
        final Action moveToCellAction = new Action(agWorld.getAID(), null);
        agWorld.addBehaviour(new Conversation(agWorld, agWorld.getOntology()
                , agWorld.getCodec(), moveToCellAction, GameOntology.MOVETOCELL) {
            @Override
            public void onStart() {

                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onRequest(ACLMessage message) {
                        agWorld.log(Level.FINE, "received unit MoveToCell"
                                + " request from " + message.getSender()
                                        .getLocalName());

                        final Tribe ownerTribe = agWorld.findOwnerTribe(message.getSender());
                        Unit requesterUnit = agWorld.findUnit(ownerTribe, message.getSender());

                        if (ownerTribe == null || requesterUnit == null) {
                            respondMessage(message, ACLMessage.REFUSE);
                            return;
                        }
                        
                        

                        try {
                            ContentElement ce = agWorld
                                    .getContentManager().extractContent(message);
                            Action agAction = (Action) ce;
                            Concept conc = agAction.getAction();
                            MoveToCell targetCell = (MoveToCell) conc;

                            MapCell mapCell = agWorld
                                    .getWorldMap().getCellAt(targetCell
                                    .getTarget().getX(), targetCell.getTarget()
                                            .getY());
                            
                            initiateMoveToCell(requesterUnit, mapCell, moveToCellAction, message);

                        } catch (NoSuchElementException ex) {
                            agWorld.log(Level.WARNING, "Unit "
                                    + requesterUnit.getId().getLocalName() + " is at an unknown position");
                            respondMessage(message, ACLMessage.REFUSE);
                        } catch (Codec.CodecException | OntologyException ex) {
                            agWorld.log(Level.WARNING, "could not receive message (" + ex + ")");
                            respondMessage(message, ACLMessage.NOT_UNDERSTOOD);
                        }

                    }
                });
            }

            private void initiateMoveToCell(Unit requesterUnit, MapCell mapCell, Action action, ACLMessage message) {
                UnitCellPositioner unitPositioner = UnitCellPositioner
                        .getInstance();
                if (unitPositioner.isMoving(requesterUnit)) {
                    agWorld.log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " already moving. Cannot move again");
                    respondMessage(message, ACLMessage.REFUSE);
                    return;
                }
                
                if (CellBuildingConstructor.getInstance()
                        .isBuilding(requesterUnit)) {
                    agWorld.log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " is currently building. Current construction"
                                    + " will be cancelled");
                    respondMessage(message, ACLMessage.REFUSE);
                    return;
                }

                try {
                    Transaction moveTransaction = unitPositioner.move(myAgent, agWorld.getWorldMap(),
                             requesterUnit, mapCell, new UnitCellPositioner.UnitMovementHandler() {
                        @Override
                        public void onMove() {
                            Tribe ownerTribe = agWorld.findOwnerTribe(requesterUnit.getId());

                            Cell newCell = new Cell();
                            newCell.setX(mapCell.getXCoord());
                            newCell.setY(mapCell.getYCoord());
                            newCell.setContent(mapCell.getContent());

                            MoveToCell moveToCellAction = new MoveToCell();
                            moveToCellAction.setTarget(newCell);

                            action.setAction(moveToCellAction);

                            respondMessage(message, ACLMessage.INFORM);
                            agWorld.getGUIEndpoint().apiMoveAgent(requesterUnit.getId()
                                    .getLocalName(), mapCell.getXCoord(),
                                     mapCell.getYCoord());

                            agWorld.processTribeKnownCell(ownerTribe, newCell);
                            
                            agWorld.informAboutUnitPassby(ownerTribe
                                        , mapCell);
                        }

                        @Override
                        public void onCancel() {
                            respondMessage(message, ACLMessage.FAILURE);
                        }
                    });

                    respondMessage(message, ACLMessage.AGREE);
                    agWorld.getActiveTransactions().add(moveTransaction);

                } catch (IndexOutOfBoundsException ex) {
                    agWorld.log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " cannot move to cell " + mapCell.getXCoord()
                            + ", " + mapCell.getYCoord() + "(" + ex + ")");
                    respondMessage(message, ACLMessage.REFUSE);
                }
            }
        });
    }
    
}
