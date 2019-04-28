/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.map.UnitCellPositioner;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.agent.group1.protocol.DelayedTransactionalBehaviour;
import es.upm.woa.ontology.Building;
import es.upm.woa.ontology.CreateUnit;
import es.upm.woa.ontology.GameOntology;

import jade.content.onto.basic.Action;
import jade.lang.acl.ACLMessage;

import java.util.NoSuchElementException;
import java.util.logging.Level;

/**
 *
 * @author ISU
 */
class AgWorldUnitCreationHelper {
      
    private static final int CREATE_UNIT_TICKS = 150;
    
    private final AgWorld agWorld;
    
    public AgWorldUnitCreationHelper(AgWorld agWorldInstance) {
        agWorld = agWorldInstance;
    }
    
    public void startUnitCreationBehaviour() {
        final Action createUnitAction = new Action(agWorld.getAID(), null);
        agWorld.addBehaviour(new Conversation(agWorld, agWorld.getOntology()
                , agWorld.getCodec(), createUnitAction, GameOntology.CREATEUNIT) {
            @Override
            public void onStart() {
                Action action = new Action(agWorld.getAID(), new CreateUnit());

                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onRequest(ACLMessage message) {
                        agWorld.log(Level.FINE, "received CreateUnit request from"
                                + message.getSender().getLocalName());

                        final Tribe ownerTribe = agWorld.findOwnerTribe(message.getSender());
                        Unit requesterUnit = agWorld.findUnit(ownerTribe, message.getSender());
                        if (ownerTribe == null || requesterUnit == null) {
                            respondMessage(message, ACLMessage.REFUSE);
                            return;
                        }

                        try {
                            MapCell unitPosition = agWorld.getWorldMap().getCellAt(requesterUnit
                                    .getCoordX(), requesterUnit.getCoordY());

                            if (!canCreateUnit(ownerTribe, requesterUnit,
                                     unitPosition)) {
                                respondMessage(message, ACLMessage.REFUSE);
                            } else {
                                initiateUnitCreation(requesterUnit, ownerTribe, unitPosition, message);
                            }

                        } catch (NoSuchElementException ex) {
                            agWorld.log(Level.WARNING, "Unit "
                                    + requesterUnit.getId().getLocalName() + " is at an unknown position");
                            respondMessage(message, ACLMessage.REFUSE);
                        }

                    }
                });
            }

            private void initiateUnitCreation(Unit requesterUnit, Tribe ownerTribe, MapCell unitPosition, ACLMessage message) {
                if (UnitCellPositioner.getInstance(agWorld.getWorldMap())
                        .isMoving(requesterUnit)) {
                    agWorld.log(Level.FINE, requesterUnit.getId().getLocalName()
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
                            boolean success = agWorld.launchNewAgentUnit(unitPosition, ownerTribe);
                            if (!success) {
                                ownerTribe.refundUnit();

                                respondMessage(message, ACLMessage.FAILURE);

                            } else {
                                respondMessage(message, ACLMessage.INFORM);
                            }
                        }

                        finished = true;
                    }

                    @Override
                    public void rollback() {
                        if (!finished) {
                            agWorld.log(Level.INFO, "refunded unit to "
                                    + ownerTribe.getAID().getLocalName());
                            ownerTribe.refundUnit();
                            respondMessage(message, ACLMessage.FAILURE);
                        }
                        finished = true;
                    }
                };

                agWorld.getActiveTransactions().add(activeTransaction);
                agWorld.addBehaviour(activeTransaction);
            }
        });
    }
    
    private boolean canCreateUnit(Tribe tribe, Unit requester, MapCell requesterPosition) {

        if (UnitCellPositioner.getInstance(agWorld.getWorldMap()).isMoving(requester)) {
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
    
}
