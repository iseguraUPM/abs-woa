/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.map;

import es.upm.woa.group1.agent.Tribe;
import es.upm.woa.group1.agent.Unit;
import es.upm.woa.group1.agent.WoaDefinitions;
import es.upm.woa.group1.protocol.DelayedTransactionalBehaviour;
import es.upm.woa.group1.protocol.Transaction;
import es.upm.woa.ontology.Building;
import es.upm.woa.ontology.Empty;

import jade.core.Agent;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author ISU
 */
public class CellBuildingConstructor {

    private static final int CREATE_TOWN_HALL_TICKS = 240;

    private static CellBuildingConstructor instance;

    private final Set<Unit> builderUnits;
    private final Set<MapCell> constructionSites;

    private CellBuildingConstructor() {
        this.builderUnits = new HashSet<>();
        this.constructionSites = new HashSet<>();
    }

    /**
     *
     * @return instance of the building constructor
     */
    public static CellBuildingConstructor getInstance() {
        if (instance == null) {
            instance = new CellBuildingConstructor();
        }

        return instance;
    }

    /**
     *
     * @param unit
     * @return the unit is currently building in a cell
     */
    public boolean isBuilding(Unit unit) {
        return builderUnits.contains(unit);
    }

    /**
     * Builds the requested type of building in the
     *
     * @param agent to execute movement
     * @param ownerTribe of the building
     * @param unit that wants to build
     * @param buildingType
     * @param cell to where the building is located
     * @param handler
     * @return the transaction of the movement
     * @throws CellBuildingConstructor.CellOccupiedException if the cell is
     * either being built on or occupied by another building or resource
     * @throws CellBuildingConstructor.UnknownBuildingTypeException when try
     * to build an unknown type of building
     */
    public Transaction build(Agent agent, Tribe ownerTribe, Unit unit, String buildingType,
             MapCell cell, BuildingConstructionHandler handler)
            throws CellOccupiedException, UnknownBuildingTypeException {
        
        if (constructionSites.contains(cell)) {
            throw new CellOccupiedException("The cell is already being built on");
        }
        
        if (!(cell.getContent() instanceof Empty)) {
            throw new CellOccupiedException("The cell is already occupied"
                    + " by a resource or building");
        }
        
        if (!isKnownBuildingType(buildingType)) {
            throw new UnknownBuildingTypeException("Unknown building type: "
                    + buildingType);
        }
        
        setAsBuilding(unit, cell);

        DelayedTransactionalBehaviour dtb = createBuildingBehaviour(agent
                , buildingType, ownerTribe, unit,
                 cell, handler);

        agent.addBehaviour(dtb);

        return dtb;
    }

    private DelayedTransactionalBehaviour createBuildingBehaviour(Agent agent, String buildingType
            , Tribe ownerTribe, Unit unit, MapCell cell, BuildingConstructionHandler handler) {
        DelayedTransactionalBehaviour dtb
                = new DelayedTransactionalBehaviour(agent, CREATE_TOWN_HALL_TICKS) {

            boolean finished = false;

            @Override
            public boolean done() {
                return finished;
            }

            @Override
            public void commit() {
                if (!finished) {
                    createBuilding(buildingType, cell, ownerTribe);
                    unsetAsBuilding(unit, cell);
                    handler.onBuilt();
                }

                finished = true;
            }

            @Override
            public void rollback() {
                if (!finished) {
                    unsetAsBuilding(unit, cell);
                    handler.onCancel();
                }
                finished = true;
            }

        };
        
        return dtb;
    }
    
    // NOTE: must check building type before
    private void createBuilding(String buildingType, MapCell newBuildingLocation, Tribe ownerTribe) {
        Building newBuilding = new Building();
        newBuilding.setOwner(ownerTribe.getAID());
        newBuildingLocation.setContent(newBuilding);
        
        switch (buildingType) {
            case WoaDefinitions.TOWN_HALL:
            case WoaDefinitions.STORE:
            case WoaDefinitions.FARM:
                newBuilding.setType(buildingType);
                break;
            default:
                throw new UnknownError("Unknown building type: " + buildingType);
        }
    }

    private void setAsBuilding(Unit builderUnit, MapCell targetCell) {
        constructionSites.add(targetCell);
        builderUnits.add(builderUnit);
    }

    private void unsetAsBuilding(Unit builderUnit, MapCell targetCell) {
        constructionSites.remove(targetCell);
        builderUnits.remove(builderUnit);
    }

    private boolean isKnownBuildingType(String buildingType) {
        return buildingType.equals(WoaDefinitions.TOWN_HALL)
                || buildingType.equals(WoaDefinitions.FARM)
                || buildingType.equals(WoaDefinitions.STORE);
    }

    public interface BuildingConstructionHandler {

        void onBuilt();

        void onCancel();

    }
    
    public class CellOccupiedException extends Exception {
        
        private final String msg;
        
        public CellOccupiedException(String msg) {
            this.msg = msg;
        }
        
        @Override
        public String toString() {
            return msg;
        }
    }
    
    public class UnknownBuildingTypeException extends Exception {
        
        private final String msg;
        
        public UnknownBuildingTypeException(String msg) {
            this.msg = msg;
        }
        
        @Override
        public String toString() {
            return msg;
        }
    }

}
