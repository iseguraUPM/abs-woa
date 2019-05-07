/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.GameMap;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.map.UnitCellPositioner;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.agent.group1.protocol.DelayedTransactionalBehaviour;
import es.upm.woa.agent.group1.protocol.Transaction;
import es.upm.woa.ontology.Building;
import es.upm.woa.ontology.CreateUnit;
import es.upm.woa.ontology.GameOntology;

import jade.content.lang.Codec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.lang.acl.ACLMessage;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.logging.Level;

/**
 *
 * @author ISU
 */
class CreateUnitBehaviourHelper {
      
    private static final int CREATE_UNIT_TICKS = 150;
    
    private final WoaAgent woaAgent;
    private final Ontology ontology;
    private final Codec codec;
    private final GameMap worldMap;
    private final Collection<Transaction> activeTransactions;
    
    private final TribeInfomationBroker tribeInfomationBroker;
    private final UnitMovementInformer unitMovementInformer;
    private final UnitCreator unitCreator;
    
    public CreateUnitBehaviourHelper(WoaAgent woaAgent, Ontology ontology
            , Codec codec, GameMap worldMap
            , Collection<Transaction> activeTransactions
            , TribeInfomationBroker tribeInfomationBroker
            , UnitMovementInformer unitMovementInformer
            , UnitCreator unitCreator) {
        this.woaAgent = woaAgent;
        this.ontology = ontology;
        this.codec = codec;
        this.worldMap = worldMap;
        this.activeTransactions = activeTransactions;
        this.tribeInfomationBroker = tribeInfomationBroker;
        this.unitMovementInformer = unitMovementInformer;
        this.unitCreator = unitCreator;
    }
    
     /**
     * Start listening behaviour for CreateUnit agent requests.
     * Unregistered tribes or units will be refused.
     */
    public void startUnitCreationBehaviour() {
        final Action createUnitAction = new Action(woaAgent.getAID(), null);
        woaAgent.addBehaviour(new Conversation(woaAgent, ontology
                , codec, createUnitAction, GameOntology.CREATEUNIT) {
            @Override
            public void onStart() {
                Action action = new Action(woaAgent.getAID(), new CreateUnit());

                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onRequest(ACLMessage message) {
                        woaAgent.log(Level.FINE, "received CreateUnit request from"
                                + message.getSender().getLocalName());

                        final Tribe ownerTribe = tribeInfomationBroker
                                .findOwnerTribe(message.getSender());
                        Unit requesterUnit = tribeInfomationBroker
                                .findUnit(ownerTribe, message.getSender());
                        if (ownerTribe == null || requesterUnit == null) {
                            respondMessage(message, ACLMessage.REFUSE);
                            return;
                        }

                        try {
                            MapCell unitPosition = worldMap.getCellAt(requesterUnit
                                    .getCoordX(), requesterUnit.getCoordY());

                            if (!canCreateUnit(ownerTribe, requesterUnit,
                                     unitPosition)) {
                                respondMessage(message, ACLMessage.REFUSE);
                            } else {
                                initiateUnitCreation(requesterUnit, ownerTribe, unitPosition, message);
                            }

                        } catch (NoSuchElementException ex) {
                            woaAgent.log(Level.WARNING, "Unit "
                                    + requesterUnit.getId().getLocalName() + " is at an unknown position");
                            respondMessage(message, ACLMessage.REFUSE);
                        }

                    }
                });
            }

            private void initiateUnitCreation(Unit requesterUnit, Tribe ownerTribe, MapCell unitPosition, ACLMessage message) {
                if (UnitCellPositioner.getInstance()
                        .isMoving(requesterUnit)) {
                    woaAgent.log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " already moving. Cannot create unit");
                    respondMessage(message, ACLMessage.REFUSE);
                    return;
                }
                
                // NOTE: if the unit were building it means there is not a town
                // hall. Thus, the request would already be refused later and
                // checking whether is building or not is unnecessary.
                
                ownerTribe.purchaseUnit();
                respondMessage(message, ACLMessage.AGREE);
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
                            boolean success = unitCreator
                                    .launchNewAgentUnit(unitPosition, ownerTribe);
                            if (!success) {
                                ownerTribe.refundUnit();

                                respondMessage(message, ACLMessage.FAILURE);

                            } else {
                                respondMessage(message, ACLMessage.INFORM);
                                unitMovementInformer.informAboutUnitPassby(ownerTribe
                                        , unitPosition);
                            }
                        }

                        finished = true;
                    }

                    @Override
                    public void rollback() {
                        if (!finished) {
                            woaAgent.log(Level.INFO, "refunded unit to "
                                    + ownerTribe.getAID().getLocalName());
                            ownerTribe.refundUnit();
                            respondMessage(message, ACLMessage.FAILURE);
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

        return tribe.canAffordUnit() && thereIsATownHall(requesterPosition, tribe);
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
    
    interface UnitCreator {
        
        /**
         * 
         * @param unitPosition
         * @param ownerTribe
         * @return if the creation of the unit agent was successful
         */
        boolean launchNewAgentUnit(MapCell unitPosition, Tribe ownerTribe);
        
    }
    
}
