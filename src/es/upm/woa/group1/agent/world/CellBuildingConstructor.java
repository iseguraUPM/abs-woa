/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent.world;

import es.upm.woa.group1.agent.Tribe;
import es.upm.woa.group1.agent.Unit;
import es.upm.woa.group1.WoaDefinitions;
import es.upm.woa.group1.map.GameMap;
import es.upm.woa.group1.map.GameMapCoordinate;
import es.upm.woa.group1.map.MapCell;
import es.upm.woa.group1.protocol.DelayedTransactionalBehaviour;
import es.upm.woa.group1.protocol.Transaction;
import es.upm.woa.ontology.Building;
import es.upm.woa.ontology.Ground;

import jade.core.Agent;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 *
 * @author ISU
 */
public class CellBuildingConstructor {

    private static final int CREATE_TOWN_HALL_TICKS = 240;
    private static final int CREATE_STORE_TICKS = 120;
    private static final int CREATE_FARM_TICKS = 120;

    private static CellBuildingConstructor instance;

    private final GameMap worldMap;
    private final Map<Unit, Collection<MapCell>> blockedCells;

    private CellBuildingConstructor(GameMap worldMap) {
        this.worldMap = worldMap;
        this.blockedCells = new HashMap<>();
    }

    /**
     *
     * @param worldMap
     * @return instance of the building constructor
     */
    public static CellBuildingConstructor getInstance(GameMap worldMap) {
        if (instance == null) {
            instance = new CellBuildingConstructor(worldMap);
        }

        return instance;
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
        
        if (constructionSiteIsBlocked(cell)) {
            throw new CellOccupiedException("The cell is already being built on");
        }
        
        if (!(cell.getContent() instanceof Ground)) {
            throw new CellOccupiedException("The cell is already occupied"
                    + " by a resource or building");
        }
        
        if (!canPlaceBuildingOnCell(buildingType, unit, ownerTribe)) {
            throw new CellOccupiedException("The cell surroundings do not meet the requirements");
        }
        
        if (!isKnownBuildingType(buildingType)) {
            throw new UnknownBuildingTypeException("Unknown building type: "
                    + buildingType);
        }

        blockConstructionSite(cell, unit, buildingType);
        unit.setBusy();

        DelayedTransactionalBehaviour dtb = createBuildingBehaviour(agent
                , buildingType, ownerTribe, unit,
                 cell, handler);

        agent.addBehaviour(dtb);

        return dtb;
    }
    
    private boolean constructionSiteIsBlocked(MapCell position) {
        return blockedCells.entrySet().stream().anyMatch(prdct -> prdct.getValue().contains(position));
    }


    private DelayedTransactionalBehaviour createBuildingBehaviour(Agent agent, String buildingType
            , Tribe ownerTribe, Unit unit, MapCell cell, BuildingConstructionHandler handler) {
        int createBuildingTicks;
        
        switch (buildingType) {
            case WoaDefinitions.TOWN_HALL:
                createBuildingTicks = CREATE_TOWN_HALL_TICKS;
                break;
            case WoaDefinitions.STORE:
                createBuildingTicks = CREATE_STORE_TICKS;
                break;
            case WoaDefinitions.FARM:
                createBuildingTicks = CREATE_FARM_TICKS;
                break;
            default:
                return null;
        }
        
        DelayedTransactionalBehaviour dtb
                = new DelayedTransactionalBehaviour(agent, createBuildingTicks) {

            boolean finished = false;

            @Override
            public boolean done() {
                return finished;
            }

            @Override
            public void commit() {
                if (!finished) {
                    createBuilding(buildingType, cell, ownerTribe);
                    unblockConstructionSite(unit);
                    unit.setFree();
                    handler.onBuilt();
                }

                finished = true;
            }

            @Override
            public void rollback() {
                if (!finished) {
                    unblockConstructionSite(unit);
                    unit.setFree();
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
        
    private void blockConstructionSite(MapCell unitPosition, Unit requesterUnit, String buildingType) {
        Collection<MapCell> cells = new HashSet<>();
        cells.add(unitPosition);

        blockedCells.put(requesterUnit, cells);
        if (buildingType.equals(WoaDefinitions.TOWN_HALL)) {
            blockNeighbourCells(requesterUnit);
        }
    }
    
    private void blockNeighbourCells(Unit requesterUnit) {
        Collection<MapCell> cells = blockedCells.get(requesterUnit);

        for (int[] translationVector : GameMapCoordinate.POS_OPERATORS) {
            int[] adjacentPosition = GameMapCoordinate
                    .applyTranslation(worldMap.getWidth(),
                            worldMap.getHeight(), requesterUnit.getCoordX(),
                            requesterUnit.getCoordY(), translationVector);
            try {
                MapCell adjacentCell = worldMap.getCellAt(adjacentPosition[0], adjacentPosition[1]);
                cells.add(adjacentCell);
            } catch (NoSuchElementException ex) {
                // That cell does not exist
            }
        }
    }

    private void unblockConstructionSite(Unit requesterUnit) {
        blockedCells.remove(requesterUnit);
    }

    private boolean isKnownBuildingType(String buildingType) {
        return buildingType.equals(WoaDefinitions.TOWN_HALL)
                || buildingType.equals(WoaDefinitions.FARM)
                || buildingType.equals(WoaDefinitions.STORE);
    }
    
        // NOTE: not surrounded by any building
    private boolean canPlaceNewTownHall(Unit requester) {
        for (int[] translationVector : GameMapCoordinate.POS_OPERATORS) {
            int[] adjacentPosition = GameMapCoordinate.applyTranslation(worldMap.getWidth(), worldMap.getHeight(), requester.getCoordX(),
                    requester.getCoordY(), translationVector);
            try {
                MapCell adjacentCell = worldMap.getCellAt(adjacentPosition[0], adjacentPosition[1]);
                if (adjacentCell.getContent() instanceof Building) {
                    return false;
                } else if (constructionSiteIsBlocked(adjacentCell)) {
                    return false;
                }
            } catch (NoSuchElementException ex) {
                // That cell does not exist
            }
        }

        return true;
    }
    
    private boolean canPlaceBuildingOnCell(String buildingType, Unit requester, Tribe owner) {
        if (buildingType.equals(WoaDefinitions.TOWN_HALL)) {
            return canPlaceNewTownHall(requester);
        } else {
            return canPlaceNewBuilding(requester, owner);
        }
    }

    // NOTE: at least connected to a building from their tribe
    // which is connected to a Town Hall
    private boolean canPlaceNewBuilding(Unit requester, Tribe owner) {
        for (int[] translationVector : GameMapCoordinate.POS_OPERATORS) {
            int[] adjacentPosition = GameMapCoordinate.applyTranslation(worldMap
                    .getWidth(), worldMap.getHeight(), requester.getCoordX(),
                    requester.getCoordY(), translationVector);
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
