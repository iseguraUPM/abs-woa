/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.gui.WoaGUI;
import es.upm.woa.agent.group1.map.CellBuildingConstructor;
import es.upm.woa.agent.group1.map.CellTranslation;
import es.upm.woa.agent.group1.map.GameMap;
import es.upm.woa.agent.group1.map.GameMapCoordinate;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.map.UnitCellPositioner;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
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

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.logging.Level;

/**
 *
 * @author ISU
 */
public class MoveUnitBehaviourHelper {
    
    private final WoaAgent woaAgent;
    private final CommunicationStandard comStandard;
    private final WoaGUI gui;
    private final GameMap worldMap;
    private final Collection<Transaction> activeTransactions;
    
    private final TribeInfomationBroker tribeInfomationBroker;
    private final UnitMovementInformer unitMovementInformer;
    
    
    MoveUnitBehaviourHelper(WoaAgent woaAgent, CommunicationStandard comStandard
            , WoaGUI gui, GameMap worldMap
            , Collection<Transaction> activeTransactions
            , TribeInfomationBroker tribeInfomationBroker
            , UnitMovementInformer unitMovementInformer) {
        this.woaAgent = woaAgent;
        this.comStandard = comStandard;
        this.gui = gui;
        this.worldMap = worldMap;
        this.activeTransactions = activeTransactions;
        this.tribeInfomationBroker = tribeInfomationBroker;
        this.unitMovementInformer = unitMovementInformer;
    }
    
    public void startMoveToCellBehaviour() {
        final Action moveToCellAction = new Action(woaAgent.getAID(), new MoveToCell());
        woaAgent.addBehaviour(new Conversation(woaAgent, comStandard
                , moveToCellAction, GameOntology.MOVETOCELL) {
            @Override
            public void onStart() {

                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onRequest(ACLMessage message) {
                        woaAgent.log(Level.FINE, "received unit MoveToCell"
                                + " request from " + message.getSender()
                                        .getLocalName());

                        final Tribe ownerTribe = tribeInfomationBroker
                                .findOwnerTribe(message.getSender());
                        Unit requesterUnit = tribeInfomationBroker
                                .findUnit(ownerTribe, message.getSender());

                        if (ownerTribe == null || requesterUnit == null) {
                            respondMessage(message, ACLMessage.REFUSE);
                            return;
                        }
                        
                        

                        try {
                            ContentElement ce = woaAgent
                                    .getContentManager().extractContent(message);
                            Action agAction = (Action) ce;
                            Concept conc = agAction.getAction();
                            MoveToCell targetCell = (MoveToCell) conc;
                            processMoveToCellAction(targetCell, requesterUnit, message);

                        } catch (NoSuchElementException ex) {
                            woaAgent.log(Level.WARNING, "Unit "
                                    + requesterUnit.getId().getLocalName() + " is at an unknown position");
                            respondMessage(message, ACLMessage.REFUSE);
                        } catch (Codec.CodecException | OntologyException ex) {
                            woaAgent.log(Level.WARNING, "could not receive message (" + ex + ")");
                            respondMessage(message, ACLMessage.NOT_UNDERSTOOD);
                        }

                    }

                    private void processMoveToCellAction(MoveToCell targetCell, Unit requesterUnit, ACLMessage message) throws NoSuchElementException {
                        int translationCode = targetCell.getTargetDirection();
                        int[] translationVector = getTranslationVectorFromCode(translationCode);
                        
                        if (translationVector == null) {
                            woaAgent.log(Level.FINE, "Unit "
                                    + requesterUnit.getId().getLocalName()
                                    + " used an incorrect translation code");
                            respondMessage(message, ACLMessage.NOT_UNDERSTOOD);
                        }
                        
                        
                        int[] newCoordinates = GameMapCoordinate
                                .applyTranslation(worldMap.getWidth()
                                        , worldMap.getHeight()
                                        , requesterUnit.getCoordX()
                                        , requesterUnit.getCoordY(), translationVector);
                        if (newCoordinates == null) {
                            woaAgent.log(Level.FINE, "Unit "
                                    + requesterUnit.getId().getLocalName()
                                    + " cannot move in target direction");
                            respondMessage(message, ACLMessage.REFUSE);
                        }
                        
                        
                        MapCell mapCell = worldMap.getCellAt(newCoordinates[0], newCoordinates[1]);
                        
                        initiateMoveToCell(requesterUnit, translationCode, mapCell, moveToCellAction, message);
                    }

                   
                });
            }
            
            private int [] getTranslationVectorFromCode(int translationCode) {
                if (translationCode == CellTranslation.TranslateDirection.UP.translationCode) {
                    return CellTranslation.V_UP;
                }
                else if (translationCode == CellTranslation.TranslateDirection.RUP.translationCode) {
                    return CellTranslation.V_RUP;
                }
                else if (translationCode == CellTranslation.TranslateDirection.RDOWN.translationCode) {
                    return CellTranslation.V_RDOWN;
                }
                else if (translationCode == CellTranslation.TranslateDirection.DOWN.translationCode) {
                    return CellTranslation.V_DOWN;
                }
                else if (translationCode == CellTranslation.TranslateDirection.LDOWN.translationCode) {
                    return CellTranslation.V_LDOWN;
                }
                else if (translationCode == CellTranslation.TranslateDirection.LUP.translationCode) {
                    return CellTranslation.V_LUP;
                }
                else {
                    return null;
                }
            }

            private void initiateMoveToCell(Unit requesterUnit, int translationCode, MapCell mapCell, Action action, ACLMessage message) {
                UnitCellPositioner unitPositioner = UnitCellPositioner
                        .getInstance();
                if (unitPositioner.isMoving(requesterUnit)) {
                    woaAgent.log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " already moving. Cannot move again");
                    respondMessage(message, ACLMessage.REFUSE);
                    return;
                }
                
                if (CellBuildingConstructor.getInstance()
                        .isBuilding(requesterUnit)) {
                    woaAgent.log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " is currently building. Current construction"
                                    + " will be cancelled");
                    respondMessage(message, ACLMessage.REFUSE);
                    return;
                }

                try {
                    Transaction moveTransaction = unitPositioner.move(myAgent, worldMap,
                             requesterUnit, mapCell, new UnitCellPositioner.UnitMovementHandler() {
                        @Override
                        public void onMove() {
                            Tribe ownerTribe = tribeInfomationBroker
                                    .findOwnerTribe(requesterUnit.getId());

                            Cell newCell = new Cell();
                            newCell.setX(mapCell.getXCoord());
                            newCell.setY(mapCell.getYCoord());
                            newCell.setContent(mapCell.getContent());

                            MoveToCell moveToCellAction = new MoveToCell();
                            moveToCellAction.setNewlyArrivedCell(newCell);
                            moveToCellAction.setTargetDirection(translationCode);

                            action.setAction(moveToCellAction);

                            respondMessage(message, ACLMessage.INFORM);
                            gui.apiMoveAgent(requesterUnit.getId()
                                    .getLocalName(), mapCell.getXCoord(),
                                     mapCell.getYCoord());

                            unitMovementInformer
                                    .processCellOfInterest(ownerTribe, mapCell);
                            
                            unitMovementInformer.informAboutUnitPassby(ownerTribe
                                        , mapCell);
                        }

                        @Override
                        public void onCancel() {
                            respondMessage(message, ACLMessage.FAILURE);
                        }
                    });

                    respondMessage(message, ACLMessage.AGREE);
                    activeTransactions.add(moveTransaction);

                } catch (IndexOutOfBoundsException ex) {
                    woaAgent.log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " cannot move to cell " + mapCell.getXCoord()
                            + ", " + mapCell.getYCoord() + "(" + ex + ")");
                    respondMessage(message, ACLMessage.REFUSE);
                }
            }
        });
    }
    
    
}