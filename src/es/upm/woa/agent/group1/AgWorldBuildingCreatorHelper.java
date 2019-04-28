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
import es.upm.woa.ontology.CreateBuilding;
import es.upm.woa.ontology.GameOntology;
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
public class AgWorldBuildingCreatorHelper {
    
    private final AgWorld agWorld;
    
    public AgWorldBuildingCreatorHelper(AgWorld agWorldInstance) {
        agWorld = agWorldInstance;
    }
    
    public void startBuildingCreationBehaviour() {
        final Action createBuildingAction = new Action(agWorld.getAID(), null);
        agWorld.addBehaviour(new Conversation(agWorld, agWorld.getOntology(), agWorld.getCodec(), createBuildingAction, GameOntology.CREATEBUILDING) {
            @Override
            public void onStart() {
                Action action = new Action(agWorld.getAID(), new CreateBuilding());

                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onRequest(ACLMessage message) {
                        agWorld.log(Level.FINE, "received CreateBuilding request from"
                                + message.getSender().getLocalName());

                        final Tribe ownerTribe = agWorld.findOwnerTribe(message.getSender());
                        Unit requesterUnit = agWorld.findUnit(ownerTribe, message.getSender());
                        if (ownerTribe == null || requesterUnit == null) {
                            respondMessage(message, ACLMessage.REFUSE);
                            return;
                        }

                        try {
                            ContentElement ce = agWorld.getContentManager()
                                    .extractContent(message);
                            Action agAction = (Action) ce;
                            Concept conc = agAction.getAction();
                            CreateBuilding createBuilding = (CreateBuilding) conc;

                            String buildingType = createBuilding.getBuildingType();

                            MapCell unitPosition = agWorld
                                    .getWorldMap().getCellAt(requesterUnit
                                    .getCoordX(), requesterUnit.getCoordY());

                            if (!canCreateBuilding(buildingType
                                    , ownerTribe, requesterUnit)) {
                                respondMessage(message, ACLMessage.REFUSE);
                            } else {
                                initiateBuildingCreation(buildingType,
                                         ownerTribe, requesterUnit,
                                         unitPosition, message);
                            }

                        } catch (NoSuchElementException ex) {
                            agWorld.log(Level.WARNING, "Unit "
                                    + requesterUnit.getId().getLocalName()
                                    + " is at an unknown position");
                            respondMessage(message, ACLMessage.REFUSE);
                        } catch (Codec.CodecException | OntologyException ex) {
                            agWorld.log(Level.WARNING, "could not receive message (" + ex + ")");
                            respondMessage(message, ACLMessage.NOT_UNDERSTOOD);
                        }

                    }
                });
            }

            private void initiateBuildingCreation(String buildingType, Tribe ownerTribe,
                     Unit requesterUnit, MapCell unitPosition, ACLMessage message) {
                CellBuildingConstructor buildingConstructor = CellBuildingConstructor.getInstance();
                
                if (buildingConstructor.isBuilding(requesterUnit)) {
                    agWorld.log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " is currently building. Current construction"
                                    + " will be cancelled");
                    requesterUnit.rollbackCurrentTransaction();
                    respondMessage(message, ACLMessage.REFUSE);
                    return;
                }
                
                try {
                    Transaction buildTransaction = buildingConstructor.build(myAgent, ownerTribe, requesterUnit,
                             buildingType, unitPosition, new CellBuildingConstructor.BuildingConstructionHandler() {
                        @Override
                        public void onBuilt() {
                            agWorld.getGUIEndpoint().apiCreateBuilding(ownerTribe.getAID()
                                    .getLocalName(), buildingType);
                            respondMessage(message, ACLMessage.INFORM);
                            agWorld.informAboutKnownCellDetail(unitPosition);
                        }

                        @Override
                        public void onCancel() {
                            refundBuilding(buildingType, ownerTribe);
                            respondMessage(message, ACLMessage.FAILURE);
                        }

                       
                    });

                    ownerTribe.purchaseTownHall();
                    respondMessage(message, ACLMessage.AGREE);
                    requesterUnit.setCurrentTransaction(buildTransaction);
                    agWorld.getActiveTransactions().add(buildTransaction);
                } catch (CellBuildingConstructor.CellOccupiedException ex) {
                    agWorld.log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " cannot build on cell " + unitPosition.getXCoord()
                            + ", " + unitPosition.getYCoord() + "(" + ex + ")");
                    respondMessage(message, ACLMessage.REFUSE);
                } catch (CellBuildingConstructor.UnknownBuildingTypeException ex) {
                    agWorld.log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " cannot build on cell " + unitPosition.getXCoord()
                            + ", " + unitPosition.getYCoord() + "(" + ex + ")");
                    respondMessage(message, ACLMessage.NOT_UNDERSTOOD);
                }
            }
        });
    }
    
    
    private boolean canCreateBuilding(String buildingType, Tribe tribe, Unit requester) {
        if (UnitCellPositioner.getInstance().isMoving(requester)) {
            agWorld.log(Level.FINE, requester.getId().getLocalName()
                            + " cannot build while moving");
            return false;
        }

        return canAffordBuilding(buildingType, tribe);
    }
    
    private boolean canAffordBuilding(String buildingType, Tribe ownerTribe) {
        switch (buildingType) {
            case WoaDefinitions.TOWN_HALL:
                return ownerTribe.canAffordTownHall();
            default:
                agWorld.log(Level.WARNING, "Unknown building type: " + buildingType);
                return false;
        }
    }
    
    private void refundBuilding(String buildingType, Tribe ownerTribe) {
        switch (buildingType) {
            case WoaDefinitions.TOWN_HALL:
                ownerTribe.refundTownHall();
                break;
            default:
                agWorld.log(Level.WARNING, "Unknown building type: " + buildingType);
        }
    }
    
}
