/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent.world;

import es.upm.woa.group1.agent.Tribe;
import es.upm.woa.group1.agent.Unit;
import es.upm.woa.group1.agent.WoaAgent;
import es.upm.woa.group1.WoaDefinitions;
import es.upm.woa.group1.agent.TransactionRecord;
import es.upm.woa.group1.gui.WoaGUI;
import es.upm.woa.group1.map.GameMap;
import es.upm.woa.group1.map.GameMapCoordinate;
import es.upm.woa.group1.map.MapCell;
import es.upm.woa.group1.protocol.CommunicationStandard;
import es.upm.woa.group1.protocol.Conversation;
import es.upm.woa.group1.protocol.Transaction;

import es.upm.woa.ontology.Building;
import es.upm.woa.ontology.CreateBuilding;
import es.upm.woa.ontology.GameOntology;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
    private final int resourceCapUpgrade;

    private final TransactionRecord transactionRecord;
    private final KnownPositionInformer knownPositionInformHandler;
    private final TribeInfomationBroker tribeInfomationHandler;

    public CreateBuildingBehaviourHelper(WoaAgent woaAgent,
            CommunicationStandard comStandard, WoaGUI gui, GameMap worldMap,
            int resourceCapUpgrade,
            TransactionRecord activeTransactions,
            KnownPositionInformer knownPositionInformHandler,
            TribeInfomationBroker tribeInfomationHandler) {
        this.woaAgent = woaAgent;
        this.comStandard = comStandard;
        this.gui = gui;
        this.worldMap = worldMap;
        this.resourceCapUpgrade = resourceCapUpgrade;
        this.transactionRecord = activeTransactions;
        this.knownPositionInformHandler = knownPositionInformHandler;
        this.tribeInfomationHandler = tribeInfomationHandler;
    }

    /**
     * Start listening behaviour for CreateBuilding agent requests. Unregistered
     * tribes or units will be refused.
     *
     * @return
     */
    public Behaviour startBuildingCreationBehaviour() {

        Behaviour newBehaviour = new Conversation(woaAgent, comStandard, GameOntology.CREATEBUILDING) {
            @Override
            public void onStart() {
                Action action = new Action(woaAgent.getAID(), new CreateBuilding());

                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onRequest(ACLMessage message) {
                        CreateBuilding dummyAction = new CreateBuilding();
                        dummyAction.setBuildingType("");
                        Action createBuildingAction = new Action(woaAgent.getAID(), dummyAction);

                        woaAgent.log(Level.FINER, "received CreateBuilding request from"
                                + message.getSender().getLocalName());

                        final Tribe ownerTribe = tribeInfomationHandler
                                .findOwnerTribe(message.getSender());
                        Unit requesterUnit = tribeInfomationHandler
                                .findUnit(ownerTribe, message.getSender());
                        if (ownerTribe == null || requesterUnit == null) {
                            respondMessage(message, ACLMessage.REFUSE, action);
                            return;
                        }

                        if (requesterUnit.isBusy()) {
                            woaAgent.log(Level.FINE, "Unit already busy. Cannot build");
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
                            if (!canAffordBuilding(buildingType, ownerTribe)) {
                                respondMessage(message, ACLMessage.REFUSE, createBuildingAction);
                            } else {
                                initiateBuildingCreation(buildingType,
                                        ownerTribe, requesterUnit,
                                        unitPosition, message);
                            }

                        } catch (NoSuchElementException ex) {
                            woaAgent.log(Level.WARNING, "Unit "
                                    + requesterUnit.getId().getLocalName()
                                    + " is at an unknown position");
                            respondMessage(message, ACLMessage.REFUSE, createBuildingAction);
                        } catch (Codec.CodecException | OntologyException ex) {
                            woaAgent.log(Level.WARNING, "could not receive message (" + ex + ")");
                            respondMessage(message, ACLMessage.NOT_UNDERSTOOD, createBuildingAction);
                        }

                    }
                });
            }

            private void initiateBuildingCreation(String buildingType, Tribe ownerTribe,
                    Unit requesterUnit, MapCell unitPosition, ACLMessage message) {
                CreateBuilding dummyAction = new CreateBuilding();
                dummyAction.setBuildingType("");
                Action createBuildingAction = new Action(woaAgent.getAID(), dummyAction);

                CellBuildingConstructor buildingConstructor = CellBuildingConstructor.getInstance(worldMap);
                try {

                    Transaction buildTransaction = buildingConstructor.build(myAgent, ownerTribe, requesterUnit,
                            buildingType, unitPosition, new CellBuildingConstructor.BuildingConstructionHandler() {
                        @Override
                        public void onBuilt() {
                            gui.createBuilding(ownerTribe.getAID()
                                    .getLocalName(), buildingType);
                            ownerTribe.getResources().upgradeStorageSpace(resourceCapUpgrade);
                            respondMessage(message, ACLMessage.INFORM, createBuildingAction);
                            knownPositionInformHandler
                                    .informAboutKnownCellDetail(unitPosition);
                        }

                        @Override
                        public void onCancel() {
                            refundBuilding(ownerTribe, buildingType, requesterUnit);
                            respondMessage(message, ACLMessage.FAILURE, createBuildingAction);
                        }

                    });

                    if (!purchaseBuilding(ownerTribe, buildingType, requesterUnit)) {
                        respondMessage(message, ACLMessage.FAILURE, createBuildingAction);
                    }

                    respondMessage(message, ACLMessage.AGREE, createBuildingAction);
                    transactionRecord.addTransaction(buildTransaction);
                } catch (CellBuildingConstructor.CellOccupiedException ex) {
                    woaAgent.log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " cannot build on cell " + unitPosition.getXCoord()
                            + ", " + unitPosition.getYCoord() + "(" + ex + ")");

                    respondMessage(message, ACLMessage.REFUSE, createBuildingAction);
                } catch (CellBuildingConstructor.UnknownBuildingTypeException ex) {
                    woaAgent.log(Level.FINE, requesterUnit.getId().getLocalName()
                            + " cannot build on cell " + unitPosition.getXCoord()
                            + ", " + unitPosition.getYCoord() + "(" + ex + ")");
                    respondMessage(message, ACLMessage.NOT_UNDERSTOOD, createBuildingAction);
                }
            }

        };

        woaAgent.addBehaviour(newBehaviour);

        return newBehaviour;
    }

    private boolean purchaseBuilding(Tribe ownerTribe, String buildingType, Unit requesterUnit) {
        switch (buildingType) {
            case WoaDefinitions.TOWN_HALL:
                return purchaseTownHall(ownerTribe, requesterUnit);
            case WoaDefinitions.FARM:
                return purchaseFarm(ownerTribe, requesterUnit);
            case WoaDefinitions.STORE:
                return purchaseStore(ownerTribe, requesterUnit);
            default:
                woaAgent.log(Level.WARNING, "Unknown building type: " + buildingType);
                return false;
        }
    }

    private boolean canAffordBuilding(String buildingType, Tribe ownerTribe) {
        switch (buildingType) {
            case WoaDefinitions.TOWN_HALL:
                return ownerTribe.getResources().canAffordTownHall();
            default:
                woaAgent.log(Level.WARNING, "Unknown building type: " + buildingType);
                return false;
        }
    }

    private void refundBuilding(Tribe ownerTribe, String buildingType,
            Unit requesterUnit) {
        switch (buildingType) {
            case WoaDefinitions.TOWN_HALL:
                refundTownHall(ownerTribe, requesterUnit);
                break;
            case WoaDefinitions.STORE:
                refundStore(ownerTribe, requesterUnit);
                break;
            case WoaDefinitions.FARM:
                refundFarm(ownerTribe, requesterUnit);
                break;
            default:
                woaAgent.log(Level.WARNING, "Unknown building type: " + buildingType);
        }
    }

    private void refundTownHall(Tribe ownerTribe, Unit requesterUnit) {
        ownerTribe.getResources().refundTownHall();
        gui.gainResource(ownerTribe.getAID().getLocalName(),
                requesterUnit.getId().getLocalName(),
                WoaGUI.RESOURCE_GOLD, WoaDefinitions.TOWN_HALL_GOLD_COST);
        gui.gainResource(ownerTribe.getAID().getLocalName(),
                requesterUnit.getId().getLocalName(),
                WoaGUI.RESOURCE_STONE, WoaDefinitions.TOWN_HALL_STONE_COST);
        gui.gainResource(ownerTribe.getAID().getLocalName(),
                requesterUnit.getId().getLocalName(),
                WoaGUI.RESOURCE_WOOD, WoaDefinitions.TOWN_HALL_WOOD_COST);
    }

    private void refundStore(Tribe ownerTribe, Unit requesterUnit) {
        ownerTribe.getResources().refundStore();
        gui.gainResource(ownerTribe.getAID().getLocalName(),
                requesterUnit.getId().getLocalName(),
                WoaGUI.RESOURCE_GOLD, WoaDefinitions.STORE_GOLD_COST);
        gui.gainResource(ownerTribe.getAID().getLocalName(),
                requesterUnit.getId().getLocalName(),
                WoaGUI.RESOURCE_STONE, WoaDefinitions.STORE_STONE_COST);
        gui.gainResource(ownerTribe.getAID().getLocalName(),
                requesterUnit.getId().getLocalName(),
                WoaGUI.RESOURCE_WOOD, WoaDefinitions.STORE_WOOD_COST);
    }

    private void refundFarm(Tribe ownerTribe, Unit requesterUnit) {
        ownerTribe.getResources().refundFarm();
        gui.gainResource(ownerTribe.getAID().getLocalName(),
                requesterUnit.getId().getLocalName(),
                WoaGUI.RESOURCE_GOLD, WoaDefinitions.FARM_GOLD_COST);
        gui.gainResource(ownerTribe.getAID().getLocalName(),
                requesterUnit.getId().getLocalName(),
                WoaGUI.RESOURCE_STONE, WoaDefinitions.FARM_STONE_COST);
        gui.gainResource(ownerTribe.getAID().getLocalName(),
                requesterUnit.getId().getLocalName(),
                WoaGUI.RESOURCE_WOOD, WoaDefinitions.FARM_WOOD_COST);
    }

    private boolean purchaseTownHall(Tribe ownerTribe, Unit requesterUnit) {
        boolean success = ownerTribe.getResources().purchaseTownHall();
        if (!success) {
            return false;
        }

        gui.loseResource(ownerTribe.getAID().getLocalName(),
                requesterUnit.getId().getLocalName(),
                WoaGUI.RESOURCE_GOLD, WoaDefinitions.TOWN_HALL_GOLD_COST);
        gui.loseResource(ownerTribe.getAID().getLocalName(),
                requesterUnit.getId().getLocalName(),
                WoaGUI.RESOURCE_STONE, WoaDefinitions.TOWN_HALL_STONE_COST);
        gui.loseResource(ownerTribe.getAID().getLocalName(),
                requesterUnit.getId().getLocalName(),
                WoaGUI.RESOURCE_WOOD, WoaDefinitions.TOWN_HALL_WOOD_COST);

        return true;
    }

    private boolean purchaseFarm(Tribe ownerTribe, Unit requesterUnit) {
        boolean success = ownerTribe.getResources().purchaseFarm();
        if (!success) {
            return false;
        }

        gui.loseResource(ownerTribe.getAID().getLocalName(),
                requesterUnit.getId().getLocalName(),
                WoaGUI.RESOURCE_GOLD, WoaDefinitions.FARM_GOLD_COST);
        gui.loseResource(ownerTribe.getAID().getLocalName(),
                requesterUnit.getId().getLocalName(),
                WoaGUI.RESOURCE_STONE, WoaDefinitions.FARM_STONE_COST);
        gui.loseResource(ownerTribe.getAID().getLocalName(),
                requesterUnit.getId().getLocalName(),
                WoaGUI.RESOURCE_WOOD, WoaDefinitions.FARM_WOOD_COST);

        return true;
    }

    private boolean purchaseStore(Tribe ownerTribe, Unit requesterUnit) {
        boolean success = ownerTribe.getResources().purchaseStore();
        if (!success) {
            return false;
        }

        gui.loseResource(ownerTribe.getAID().getLocalName(),
                requesterUnit.getId().getLocalName(),
                WoaGUI.RESOURCE_GOLD, WoaDefinitions.STORE_GOLD_COST);
        gui.loseResource(ownerTribe.getAID().getLocalName(),
                requesterUnit.getId().getLocalName(),
                WoaGUI.RESOURCE_STONE, WoaDefinitions.STORE_STONE_COST);
        gui.loseResource(ownerTribe.getAID().getLocalName(),
                requesterUnit.getId().getLocalName(),
                WoaGUI.RESOURCE_WOOD, WoaDefinitions.STORE_WOOD_COST);

        return true;
    }

}
