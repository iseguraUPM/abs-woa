/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.world;

import es.upm.woa.agent.group1.Tribe;
import es.upm.woa.agent.group1.Unit;
import es.upm.woa.agent.group1.WoaAgent;
import es.upm.woa.agent.group1.WoaDefinitions;
import es.upm.woa.agent.group1.gui.WoaGUI;
import es.upm.woa.agent.group1.map.GameMap;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.map.UnitCellPositioner;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.agent.group1.protocol.DelayedTransactionalBehaviour;
import es.upm.woa.agent.group1.protocol.Transaction;
import es.upm.woa.ontology.Building;
import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.CreateUnit;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.NotifyNewUnit;

import jade.content.onto.basic.Action;
import jade.lang.acl.ACLMessage;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.logging.Level;

/**
 *
 * @author ISU
 */
public class CreateUnitBehaviourHelper {
      
    private static final int CREATE_UNIT_TICKS = 150;
    
    private final WoaAgent woaAgent;
    private final CommunicationStandard comStandard;
    private final GameMap worldMap;
    private final Collection<Transaction> activeTransactions;
    private final WoaGUI gui;
    
    private final TribeInfomationBroker tribeInfomationBroker;
    private final UnitMovementInformer unitMovementInformer;
    private final UnitCreator unitCreator;
    
    public CreateUnitBehaviourHelper(WoaAgent woaAgent
            , CommunicationStandard comStandard, GameMap worldMap
            , Collection<Transaction> activeTransactions
            , WoaGUI gui
            , TribeInfomationBroker tribeInfomationBroker
            , UnitMovementInformer unitMovementInformer
            , UnitCreator unitCreator) {
        this.woaAgent = woaAgent;
        this.comStandard = comStandard;
        this.worldMap = worldMap;
        this.activeTransactions = activeTransactions;
        this.gui = gui;
        this.tribeInfomationBroker = tribeInfomationBroker;
        this.unitMovementInformer = unitMovementInformer;
        this.unitCreator = unitCreator;
    }
    
     /**
     * Start listening behaviour for CreateUnit agent requests.
     * Unregistered tribes or units will be refused.
     */
    public void startUnitCreationBehaviour() {
        
        woaAgent.addBehaviour(new Conversation(woaAgent, comStandard
               , GameOntology.CREATEUNIT) {
            @Override
            public void onStart() {

                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onRequest(ACLMessage message) {
                        final Action createUnitAction
                                = new Action(woaAgent.getAID(), new CreateUnit());
                        woaAgent.log(Level.FINE, "received CreateUnit request from"
                                + message.getSender().getLocalName());

                        final Tribe ownerTribe = tribeInfomationBroker
                                .findOwnerTribe(message.getSender());
                        Unit requesterUnit = tribeInfomationBroker
                                .findUnit(ownerTribe, message.getSender());
                        if (ownerTribe == null || requesterUnit == null) {
                            respondMessage(message, ACLMessage.REFUSE, createUnitAction);
                            return;
                        }

                        try {
                            MapCell unitPosition = worldMap.getCellAt(requesterUnit
                                    .getCoordX(), requesterUnit.getCoordY());

                            if (!canCreateUnit(ownerTribe, requesterUnit,
                                     unitPosition)) {
                                respondMessage(message, ACLMessage.REFUSE, createUnitAction);
                            } else {
                                initiateUnitCreation(requesterUnit, ownerTribe, unitPosition, message);
                            }

                        } catch (NoSuchElementException ex) {
                            woaAgent.log(Level.WARNING, "Unit "
                                    + requesterUnit.getId().getLocalName() + " is at an unknown position");
                            respondMessage(message, ACLMessage.REFUSE, createUnitAction);
                        }

                    }
                });
            }

            private void initiateUnitCreation(Unit requesterUnit, Tribe ownerTribe, MapCell unitPosition, ACLMessage message) {
                final Action createUnitAction
                                = new Action(woaAgent.getAID(), new CreateUnit());
                
                if (UnitCellPositioner.getInstance()
                        .isMoving(requesterUnit)) {
                    woaAgent.log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " already moving. Cannot create unit");
                    respondMessage(message, ACLMessage.REFUSE, createUnitAction);
                    return;
                }
                
                // NOTE: if the unit were building it means there is not a town
                // hall. Thus, the request would already be refused later and
                // checking whether is building or not is unnecessary.
                
                ownerTribe.getResources().purchaseUnit();
                respondMessage(message, ACLMessage.AGREE, createUnitAction);
                DelayedTransactionalBehaviour activeTransaction
                        = new DelayedTransactionalBehaviour(myAgent, CREATE_UNIT_TICKS) {

                    boolean finished = false;

                    @Override
                    public boolean done() {
                        return finished;
                    }

                    @Override
                    public void commit() {
                        if (!finished) {
                            unitCreator
                                    .launchNewAgentUnit(unitPosition, ownerTribe, new OnCreatedUnitHandler() {
                                @Override
                                public void onCreatedUnit(Unit createdUnit) {
                                    respondMessage(message, ACLMessage.INFORM, createUnitAction);
                                    informTribeAboutNewUnit(ownerTribe, createdUnit);
                                    gui.apiCreateAgent(ownerTribe.getAID().getLocalName(),
                                        createdUnit.getId().getLocalName(), createdUnit.getCoordX(),
                                        createdUnit.getCoordY());
                                    unitMovementInformer.informAboutUnitPassby(ownerTribe
                                             , unitPosition);
                                    woaAgent.log(Level.FINE, "Created unit "
                                     + createdUnit.getId().getLocalName() + " at "
                                     + unitPosition);
                                }

                                @Override
                                public void onCouldNotCreateUnit() {
                                    ownerTribe.getResources().refundUnit();

                                    respondMessage(message, ACLMessage.FAILURE, createUnitAction);
                                    
                                    woaAgent.log(Level.FINE, "Could not create"
                                            + " unit for tribe "
                                            + ownerTribe.getAID().getLocalName());
                                }
                            });
                        }

                        finished = true;
                    }

                    @Override
                    public void rollback() {
                        if (!finished) {
                            woaAgent.log(Level.INFO, "refunded unit to "
                                    + ownerTribe.getAID().getLocalName());
                            ownerTribe.getResources().refundUnit();
                            respondMessage(message, ACLMessage.FAILURE, createUnitAction);
                        }
                        finished = true;
                    }
                };

                activeTransactions.add(activeTransaction);
                woaAgent.addBehaviour(activeTransaction);
            }
        });
    }
    
    private boolean canCreateUnit(Tribe tribe, Unit requester, MapCell requesterPosition) {

        if (UnitCellPositioner.getInstance().isMoving(requester)) {
            return false;
        }

        return tribe.getResources().canAffordUnit()
                && thereIsATownHall(requesterPosition, tribe);
    }
    
    private boolean thereIsATownHall(MapCell position, Tribe tribe) {
        if (!(position.getContent() instanceof Building)) {
            return false;
        } else {
            Building building = (Building) position.getContent();
            if (!building.getOwner().equals(tribe.getAID())) {
                return false;
            } else {
                return building.getType().equals(WoaDefinitions.TOWN_HALL);
            }
        }
    }
    
    
    private void informTribeAboutNewUnit(Tribe ownerTribe, Unit newUnit) {

        NotifyNewUnit notifyNewUnit = new NotifyNewUnit();
        Cell cell = new Cell();
        cell.setX(newUnit.getCoordX());
        cell.setY(newUnit.getCoordY());

        notifyNewUnit.setLocation(cell);
        notifyNewUnit.setNewUnit(newUnit.getId());

        Action informNewUnitAction = new Action(ownerTribe.getAID(), notifyNewUnit);
        woaAgent.addBehaviour(new Conversation(woaAgent, comStandard, GameOntology.NOTIFYNEWUNIT) {
            @Override
            public void onStart() {
                sendMessage(ownerTribe.getAID(), ACLMessage.INFORM
                        , informNewUnitAction, new Conversation.SentMessageHandler() {

                });
            }
        });

        try {
            MapCell discoveredCell = worldMap.getCellAt(cell.getX(), cell.getY());
            unitMovementInformer.processCellOfInterest(ownerTribe, discoveredCell);
        } catch (NoSuchElementException ex) {
            woaAgent.log(Level.WARNING, "Unit in unknown starting position (" + ex + ")");
        }
    }
    
    public interface UnitCreator {
        
        /**
         * 
         * @param unitPosition starting position for the unit
         * @param ownerTribe
         * @param handler
         */
        void launchNewAgentUnit(MapCell unitPosition, Tribe ownerTribe
                , OnCreatedUnitHandler handler);
        
    }
    
    public interface OnCreatedUnitHandler {
        
        void onCreatedUnit(Unit unit);
        
        void onCouldNotCreateUnit();
        
    }
    
}
