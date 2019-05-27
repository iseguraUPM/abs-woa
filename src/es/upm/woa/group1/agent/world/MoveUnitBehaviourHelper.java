/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent.world;

import es.upm.woa.group1.agent.TransactionRecord;
import es.upm.woa.group1.agent.Tribe;
import es.upm.woa.group1.agent.Unit;
import es.upm.woa.group1.agent.WoaAgent;
import es.upm.woa.group1.gui.WoaGUI;
import es.upm.woa.group1.map.CellTranslation;
import es.upm.woa.group1.map.GameMap;
import es.upm.woa.group1.map.GameMapCoordinate;
import es.upm.woa.group1.map.MapCell;
import es.upm.woa.group1.protocol.CommunicationStandard;
import es.upm.woa.group1.protocol.Conversation;
import es.upm.woa.group1.protocol.Transaction;

import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.MoveToCell;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;

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
    
    private final TransactionRecord transactionRecord;
    private final TribeInfomationBroker tribeInfomationBroker;
    private final UnitMovementInformer unitMovementInformer;
    
    
    public MoveUnitBehaviourHelper(WoaAgent woaAgent, CommunicationStandard comStandard
            , WoaGUI gui, GameMap worldMap
            , TransactionRecord activeTransactions
            , TribeInfomationBroker tribeInfomationBroker
            , UnitMovementInformer unitMovementInformer) {
        this.woaAgent = woaAgent;
        this.comStandard = comStandard;
        this.gui = gui;
        this.worldMap = worldMap;
        this.transactionRecord = activeTransactions;
        this.tribeInfomationBroker = tribeInfomationBroker;
        this.unitMovementInformer = unitMovementInformer;
    }
    
    public Behaviour startMoveToCellBehaviour() {
        
        Behaviour newBehaviour = new Conversation(woaAgent, comStandard
                , GameOntology.MOVETOCELL) {
            @Override
            public void onStart() {

                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onRequest(ACLMessage message) {
                        MoveToCell dummyAction = new MoveToCell();
                        dummyAction.setTargetDirection(0);
                        final Action moveToCellAction = new Action(woaAgent.getAID(), dummyAction);
                        woaAgent.log(Level.FINER, "received unit MoveToCell"
                                + " request from " + message.getSender()
                                        .getLocalName());

                        final Tribe ownerTribe = tribeInfomationBroker
                                .findOwnerTribe(message.getSender());
                        Unit requesterUnit = tribeInfomationBroker
                                .findUnit(ownerTribe, message.getSender());

                        if (ownerTribe == null || requesterUnit == null) {
                            respondMessage(message, ACLMessage.REFUSE, moveToCellAction);
                            return;
                        }
                        
                        if (requesterUnit.isBusy()) {
                            woaAgent.log(Level.FINE, "Unit is busy. Cannot move");
                            respondMessage(message, ACLMessage.REFUSE, moveToCellAction);
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
                            respondMessage(message, ACLMessage.REFUSE, moveToCellAction);
                        } catch (Codec.CodecException | OntologyException ex) {
                            woaAgent.log(Level.WARNING, "could not receive message (" + ex + ")");
                            respondMessage(message, ACLMessage.NOT_UNDERSTOOD, moveToCellAction);
                        }

                    }

                    private void processMoveToCellAction(MoveToCell targetCell, Unit requesterUnit, ACLMessage message) throws NoSuchElementException {
                        MoveToCell dummyAction = new MoveToCell();
                        dummyAction.setTargetDirection(0);
                        final Action moveToCellAction = new Action(woaAgent.getAID(), dummyAction);
                        int translationCode = targetCell.getTargetDirection();
                        
                        initiateMoveToCell(requesterUnit, translationCode, moveToCellAction, message);
                    }

                   
                });
            }


            private void initiateMoveToCell(Unit requesterUnit, int translationCode, Action action, ACLMessage message) {
                UnitCellPositioner unitPositioner = UnitCellPositioner
                        .getInstance(worldMap);

                try {
                    Transaction moveTransaction = unitPositioner.move(myAgent, 
                             requesterUnit, translationCode, new UnitCellPositioner.UnitMovementHandler() {
                        @Override
                        public void onMove(MapCell targetCell) {
                            Tribe ownerTribe = tribeInfomationBroker
                                    .findOwnerTribe(requesterUnit.getId());

                            Cell newCell = new Cell();
                            newCell.setX(targetCell.getXCoord());
                            newCell.setY(targetCell.getYCoord());
                            newCell.setContent(newCell.getContent());

                            MoveToCell moveToCellAction = new MoveToCell();
                            moveToCellAction.setNewlyArrivedCell(newCell);
                            moveToCellAction.setTargetDirection(translationCode);

                            action.setAction(moveToCellAction);

                            respondMessage(message, ACLMessage.INFORM, action);
                            
                            gui.moveAgent(requesterUnit.getId()
                                    .getLocalName(), targetCell.getXCoord(),
                                     targetCell.getYCoord());

                            unitMovementInformer
                                    .processCellOfInterest(ownerTribe, targetCell);
                            
                            unitMovementInformer.informAboutUnitPassby(ownerTribe
                                        , targetCell);
                        }

                        @Override
                        public void onCancel() {
                            respondMessage(message, ACLMessage.FAILURE, action);
                        }
                    });

                    respondMessage(message, ACLMessage.AGREE, action);
                    transactionRecord.addTransaction(moveTransaction);

                } catch (IndexOutOfBoundsException ex) {
                    woaAgent.log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " cannot move (" + ex + ")");
                    respondMessage(message, ACLMessage.REFUSE, action);
                }
            }
        };
        
        woaAgent.addBehaviour(newBehaviour);
        
        return newBehaviour;
    }
    
    
}
