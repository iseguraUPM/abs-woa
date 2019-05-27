/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent.world;

import es.upm.woa.group1.agent.Unit;
import es.upm.woa.group1.map.CellTranslation;
import es.upm.woa.group1.map.GameMap;
import es.upm.woa.group1.map.GameMapCoordinate;
import es.upm.woa.group1.map.MapCell;
import es.upm.woa.group1.protocol.DelayedTransactionalBehaviour;
import es.upm.woa.group1.protocol.Transaction;

import jade.core.Agent;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 *
 * @author ISU
 */
public class UnitCellPositioner {

    private static final int UNIT_MOVE_TICKS = 6;

    private static UnitCellPositioner instance;

    private final GameMap worldMap;

    private UnitCellPositioner(GameMap worldMap) {
        this.worldMap = worldMap;
    }

    /**
     *
     * @param worldMap
     * @return instance of the positioner
     */
    public static UnitCellPositioner getInstance(GameMap worldMap) {
        if (instance == null) {
            instance = new UnitCellPositioner(worldMap);
        }

        return instance;
    }

    /**
     * Moves the target unit to the new cell
     *
     * @param agent to execute movement
     * @param unit to move
     * @param translationCode defining the direction of the movement
     * @param handler
     * @return the transaction of the movement
     * @throws IndexOutOfBoundsException if the unit cannot move to the target
     * cell. This is usually because is not adjacent to its current position. It
     * can also mean that the unit is in an incorrect position.
     */
    public Transaction move(Agent agent, Unit unit, int translationCode, UnitMovementHandler handler)
            throws IndexOutOfBoundsException {

        int[] translationVector = getTranslationVectorFromCode(translationCode);

        if (translationVector == null) {

            throw new IndexOutOfBoundsException("Unit "
                    + unit.getId().getLocalName()
                    + " used an incorrect translation code");
        }

        int[] newCoordinates = GameMapCoordinate
                .applyTranslation(worldMap.getWidth(),
                         worldMap.getHeight(),
                         unit.getCoordX(),
                         unit.getCoordY(), translationVector);
        if (newCoordinates == null) {
            throw new IndexOutOfBoundsException("Unit "
                    + unit.getId().getLocalName()
                    + " cannot move in target direction");
        }
        
        MapCell cell;
        try {
            cell = worldMap.getCellAt(newCoordinates[0], newCoordinates[1]);
        } catch (NoSuchElementException ex) {
            throw new IndexOutOfBoundsException("Target cell does not exist");
        }

        if (!GameMapCoordinate.isCorrectPosition(worldMap.getWidth(),
                 worldMap.getHeight(), unit.getCoordX(), unit.getCoordY())) {
            throw new IndexOutOfBoundsException(unit.getId().getLocalName()
                    + " is at an incorrect position");
        }

        if (!isAdjacent(unit, cell, worldMap)) {
            throw new IndexOutOfBoundsException(unit.getId().getLocalName()
                    + " is not adjacent to cell [" + cell.getXCoord()
                    + "," + cell.getYCoord() + "]");
        }

        unit.setBusy();

        DelayedTransactionalBehaviour dtb = createMovementBehaviour(agent, unit,
                 cell, handler);

        agent.addBehaviour(dtb);

        return dtb;
    }

    private int[] getTranslationVectorFromCode(int translationCode) {
        if (translationCode == CellTranslation.TranslateDirection.UP.translationCode) {
            return CellTranslation.V_UP;
        } else if (translationCode == CellTranslation.TranslateDirection.RUP.translationCode) {
            return CellTranslation.V_RUP;
        } else if (translationCode == CellTranslation.TranslateDirection.RDOWN.translationCode) {
            return CellTranslation.V_RDOWN;
        } else if (translationCode == CellTranslation.TranslateDirection.DOWN.translationCode) {
            return CellTranslation.V_DOWN;
        } else if (translationCode == CellTranslation.TranslateDirection.LDOWN.translationCode) {
            return CellTranslation.V_LDOWN;
        } else if (translationCode == CellTranslation.TranslateDirection.LUP.translationCode) {
            return CellTranslation.V_LUP;
        } else {
            return null;
        }
    }

    private DelayedTransactionalBehaviour createMovementBehaviour(Agent agent,
             Unit unit, MapCell cell, UnitMovementHandler handler) {
        DelayedTransactionalBehaviour dtb
                = new DelayedTransactionalBehaviour(agent, UNIT_MOVE_TICKS) {

            boolean finished;

            @Override
            public boolean done() {
                return finished;
            }

            @Override
            public void commit() {
                if (!finished) {
                    unit.setPosition(cell.getXCoord(), cell.getYCoord());
                    unit.setFree();
                    handler.onMove(cell);
                }
                finished = true;
            }

            @Override
            public void rollback() {
                if (!finished) {
                    unit.setFree();
                    handler.onCancel();
                }
                finished = true;
            }

        };
        return dtb;
    }

    // NOTE: this assumes unit is in a correct position
    private boolean isAdjacent(Unit unit, MapCell cell, GameMap gameMap) {
        int x1 = unit.getCoordX();
        int y1 = unit.getCoordY();
        int x2 = cell.getXCoord();
        int y2 = cell.getYCoord();

        if (x1 == x2 && y1 == y2) {
            return false;
        }

        for (int[] operator : GameMapCoordinate.POS_OPERATORS) {
            int[] translatedPosition = GameMapCoordinate
                    .applyTranslation(gameMap.getWidth(), gameMap.getHeight(),
                             x1, y1, operator);

            if (translatedPosition == null) {
                // NOTE: should not reach
                return false;
            }

            if (translatedPosition[0] == x2 && translatedPosition[1] == y2) {
                return true;
            }
        }

        return false;
    }

    public interface UnitMovementHandler {

        void onMove(MapCell newCell);

        void onCancel();

    }

}
