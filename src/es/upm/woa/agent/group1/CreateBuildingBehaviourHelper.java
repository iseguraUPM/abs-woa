/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.gui.WoaGUI;
import es.upm.woa.agent.group1.map.CellBuildingConstructor;
import es.upm.woa.agent.group1.map.GameMap;
import es.upm.woa.agent.group1.map.GameMapCoordinate;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.map.UnitCellPositioner;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.agent.group1.protocol.Transaction;
import es.upm.woa.ontology.Building;
import es.upm.woa.ontology.CreateBuilding;
import es.upm.woa.ontology.Empty;
import es.upm.woa.ontology.GameOntology;

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
public class CreateBuildingBehaviourHelper {
    
    private final WoaAgent woaAgent;
    private final CommunicationStandard comStandard;
    private final WoaGUI gui;
    private final GameMap worldMap;
    private final Collection<Transaction> activeTransactions;
    
    private final KnownPositionInformer knownPositionInformHandler;
    private final TribeInfomationBroker tribeInfomationHandler;
    
    CreateBuildingBehaviourHelper(WoaAgent woaAgent
            , CommunicationStandard comStandard, WoaGUI gui, GameMap worldMap
            , Collection<Transaction> activeTransactions
            , KnownPositionInformer knownPositionInformHandler
            , TribeInfomationBroker tribeInfomationHandler) {
        this.woaAgent = woaAgent;
        this.comStandard = comStandard;
        this.gui = gui;
        this.worldMap = worldMap;
        this.activeTransactions = activeTransactions;
        this.knownPositionInformHandler = knownPositionInformHandler;
        this.tribeInfomationHandler = tribeInfomationHandler;
    }
    
    /**
     * Start listening behaviour for CreateBuilding agent requests.
     * Unregistered tribes or units will be refused.
     */
    public void startBuildingCreationBehaviour() {
        CreateBuilding dummyAction = new CreateBuilding();
        dummyAction.setBuildingType("");
        final Action createBuildingAction = new Action(woaAgent.getAID(), dummyAction);
        woaAgent.addBehaviour(new Conversation(woaAgent, comStandard, createBuildingAction, GameOntology.CREATEBUILDING) {
            @Override
            public void onStart() {
                Action action = new Action(woaAgent.getAID(), new CreateBuilding());

                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onRequest(ACLMessage message) {
                        woaAgent.log(Level.FINE, "received CreateBuilding request from"
                                + message.getSender().getLocalName());

                        final Tribe ownerTribe = tribeInfomationHandler
                                .findOwnerTribe(message.getSender());
                        Unit requesterUnit = tribeInfomationHandler
                                .findUnit(ownerTribe, message.getSender());
                        if (ownerTribe == null || requesterUnit == null) {
                            respondMessage(message, ACLMessage.REFUSE);
                            return;
                        }

                        try {
                            ContentElement ce = woaAgent.getContentManager()
                                    .extractContent(message);
                            Action agAction = (Action) ce;
                            Concept conc = agAction.getAction();
                            CreateBuilding createBuilding = (CreateBuilding) conc;

                            String buildingType = createBuilding.getBuildingType();

                            MapCell unitPosition = worldMap.getCellAt(requesterUnit
                                    .getCoordX(), requesterUnit.getCoordY());

                            action.setAction(conc);
                            if (!canCreateBuilding(buildingType
                                    , ownerTribe, requesterUnit)) {
                                respondMessage(message, ACLMessage.REFUSE);
                            } else {
                                initiateBuildingCreation(buildingType,
                                         ownerTribe, requesterUnit,
                                         unitPosition, message);
                            }

                        } catch (NoSuchElementException ex) {
                            woaAgent.log(Level.WARNING, "Unit "
                                    + requesterUnit.getId().getLocalName()
                                    + " is at an unknown position");
                            respondMessage(message, ACLMessage.REFUSE);
                        } catch (Codec.CodecException | OntologyException ex) {
                            woaAgent.log(Level.WARNING, "could not receive message (" + ex + ")");
                            respondMessage(message, ACLMessage.NOT_UNDERSTOOD);
                        }

                    }
                });
            }

            private void initiateBuildingCreation(String buildingType, Tribe ownerTribe,
                     Unit requesterUnit, MapCell unitPosition, ACLMessage message) {
                CellBuildingConstructor buildingConstructor = CellBuildingConstructor.getInstance();
                
                if (buildingConstructor.isBuilding(requesterUnit)) {
                    woaAgent.log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " is currently building. Current construction"
                                    + " will be cancelled");
                    respondMessage(message, ACLMessage.REFUSE);
                    return;
                }
                
                try {
                    Transaction buildTransaction = buildingConstructor.build(myAgent, ownerTribe, requesterUnit,
                             buildingType, unitPosition, new CellBuildingConstructor.BuildingConstructionHandler() {
                        @Override
                        public void onBuilt() {
                            gui.apiCreateBuilding(ownerTribe.getAID()
                                    .getLocalName(), buildingType);
                            respondMessage(message, ACLMessage.INFORM);
                            knownPositionInformHandler
                                    .informAboutKnownCellDetail(unitPosition);
                        }

                        @Override
                        public void onCancel() {
                            refundBuilding(buildingType, ownerTribe);
                            respondMessage(message, ACLMessage.FAILURE);
                        }

                       
                    });

                    ownerTribe.purchaseTownHall();
                    respondMessage(message, ACLMessage.AGREE);
                    activeTransactions.add(buildTransaction);
                } catch (CellBuildingConstructor.CellOccupiedException ex) {
                    woaAgent.log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " cannot build on cell " + unitPosition.getXCoord()
                            + ", " + unitPosition.getYCoord() + "(" + ex + ")");
                    respondMessage(message, ACLMessage.REFUSE);
                } catch (CellBuildingConstructor.UnknownBuildingTypeException ex) {
                    woaAgent.log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " cannot build on cell " + unitPosition.getXCoord()
                            + ", " + unitPosition.getYCoord() + "(" + ex + ")");
                    respondMessage(message, ACLMessage.NOT_UNDERSTOOD);
                }
            }
        });
    }
    
    
    private boolean canCreateBuilding(String buildingType, Tribe tribe, Unit requester) {
        if (UnitCellPositioner.getInstance().isMoving(requester)) {
            woaAgent.log(Level.FINE, requester.getId().getLocalName()
                            + " cannot build while moving");
            return false;
        }
        
        if (!canAffordBuilding(buildingType, tribe)) {
            return false;
        }

        return canPlaceBuildingOnCell(buildingType, requester, tribe);
    }
    
    private boolean canPlaceBuildingOnCell(String buildingType, Unit requester, Tribe owner) {
        if (buildingType.equals(WoaDefinitions.TOWN_HALL)) {
            return canPlaceNewTownHall(requester);
        }
        else {
            return canPlaceNewBuilding(requester, owner);
        }
    }
    
    private boolean canAffordBuilding(String buildingType, Tribe ownerTribe) {
        switch (buildingType) {
            case WoaDefinitions.TOWN_HALL:
                return ownerTribe.canAffordTownHall();
            default:
                woaAgent.log(Level.WARNING, "Unknown building type: " + buildingType);
                return false;
        }
    }
    
    private void refundBuilding(String buildingType, Tribe ownerTribe) {
        switch (buildingType) {
            case WoaDefinitions.TOWN_HALL:
                ownerTribe.refundTownHall();
                break;
            default:
                woaAgent.log(Level.WARNING, "Unknown building type: " + buildingType);
        }
    }

    private boolean canPlaceNewTownHall(Unit requester) {
        for (int[] translationVector : GameMapCoordinate.POS_OPERATORS) {
            int[] adjacentPosition = GameMapCoordinate.applyTranslation(worldMap.getWidth(), worldMap.getHeight(), requester.getCoordX()
                    , requester.getCoordY(), translationVector);
            try {
                MapCell adjacentCell = worldMap.getCellAt(adjacentPosition[0], adjacentPosition[1]);
                if (!(adjacentCell.getContent() instanceof Empty)) {
                    return false;
                }
            } catch (NoSuchElementException ex) {
                // That cell does not exist
            }
        }
        
        return true;
    }

    private boolean canPlaceNewBuilding(Unit requester, Tribe owner) {
        for (int[] translationVector : GameMapCoordinate.POS_OPERATORS) {
            int[] adjacentPosition = GameMapCoordinate.applyTranslation(worldMap
                    .getWidth(), worldMap.getHeight(), requester.getCoordX()
                    , requester.getCoordY(), translationVector);
            try {
                MapCell adjacentCell = worldMap.getCellAt(adjacentPosition[0], adjacentPosition[1]);
                if (isBuildingFromOwner(adjacentCell, owner)) {
                    return true;
                }
            } catch (NoSuchElementException ex) {
                // That cell does not exist
            }
        }
        
        return false;
    }

    private boolean isBuildingFromOwner(MapCell adjacentCell, Tribe owner) {
        if (adjacentCell.getContent() instanceof Building) {
            Building existingBuilding = (Building) adjacentCell.getContent();
            return existingBuilding.getOwner().equals(owner.getAID());
        }
        
        return false;
    }
    
    interface KnownPositionInformer {
        
        /**
         * Inform all tribes that may know the cell about its details.
         * @param knownPosition 
         */
        void informAboutKnownCellDetail(MapCell knownPosition);
        
    }
    
}
