/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.map;

import es.upm.woa.agent.group1.Unit;
import es.upm.woa.agent.group1.protocol.DelayedTransactionalBehaviour;
import es.upm.woa.agent.group1.protocol.Transaction;

import jade.core.Agent;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author ISU
 */
public class UnitCellPositioner {
    
    private static final int UNIT_MOVE_TICKS = 6;
    
    private static UnitCellPositioner instance;
    
    private final Set<Unit> movingUnits;
    
    private UnitCellPositioner() {
        this.movingUnits = new HashSet<>();
    }
    
    /**
     * 
     * @return instance of the positioner
     */
    public static UnitCellPositioner getInstance() {
        if (instance == null) {
            instance = new UnitCellPositioner();
        }
        
        return instance;
    }
    
    /**
     * 
     * @param unit
     * @return the unit is currently undergoing positioning
     */
    public boolean isMoving(Unit unit) {
        return movingUnits.contains(unit);
    }
    
    /**
     * Moves the target unit to the new cell
     * @param agent to execute movement
     * @param gameMap where to perform movement
     * @param unit to move
     * @param cell to position the unit
     * @param handler
     * @return the transaction of the movement
     * @throws IndexOutOfBoundsException if the unit cannot move to the 
     * target cell. This is usually because is not adjacent to its current
     * position. It can also mean that the unit is in an incorrect position.
     */
    public Transaction move(Agent agent, GameMap gameMap, Unit unit, MapCell cell, UnitMovementHandler handler)
            throws IndexOutOfBoundsException {
        if (!GameMapCoordinate.isCorrectPosition(gameMap.getWidth()
                , gameMap.getHeight(), unit.getCoordX(), unit.getCoordY())) {
            throw new IndexOutOfBoundsException(unit.getId().getLocalName() +
                    " is at an incorrect position");
        }
        
        if (!isAdjacent(unit, cell, gameMap)) {
            throw new IndexOutOfBoundsException(unit.getId().getLocalName() +
                    " is not adjacent to cell [" + cell.getXCoord()
                    + "," + cell.getYCoord() + "]");
        }
        
        setAsMoving(unit);
        
        DelayedTransactionalBehaviour dtb = createMovementBehaviour(agent, unit
                , cell, handler);
        
        agent.addBehaviour(dtb);
        
        return dtb;
    }

    private DelayedTransactionalBehaviour createMovementBehaviour(Agent agent
            , Unit unit, MapCell cell, UnitMovementHandler handler) {
        DelayedTransactionalBehaviour dtb =
                new DelayedTransactionalBehaviour(agent, UNIT_MOVE_TICKS) {
            
            boolean finished;
            
            @Override
            public boolean done() {
                return finished;
            }
            
            @Override
            public void commit() {
                if (!finished) {
                    unit.setPosition(cell.getXCoord(), cell.getYCoord());
                    unsetAsMoving(unit);
                    handler.onMove();
                }
                finished = true;
            }

            @Override
            public void rollback() {
                if (!finished) {
                    unsetAsMoving(unit);
                    handler.onCancel();
                }
                finished = true;
            }
            
        };
        return dtb;
    }
    
    private void setAsMoving(Unit movingUnit) {
        movingUnits.add(movingUnit);
    }
    
    private void unsetAsMoving(Unit movingUnit) {
        movingUnits.remove(movingUnit);
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
                    .applyTranslation(gameMap.getWidth(), gameMap.getHeight()
                            , x1, y1, operator);
            
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
        
        void onMove();
        
        void onCancel();
        
    }
    
}
