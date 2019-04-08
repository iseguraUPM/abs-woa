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
    
    private static final int UNIT_MOVE_TIME_MILLIS = 600;
    
    private static UnitCellPositioner instance;
    
    private final WorldMap worldMap;
    private final Set<Unit> movingUnits;
    
    private final static int[][] POS_OPERATORS =
    {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}, {2, 0}, {0, 2}};
    
    private UnitCellPositioner(WorldMap worldMap) {
        this.worldMap = worldMap;
        this.movingUnits = new HashSet<>();
    }
    
    /**
     * 
     * @param worldMap
     * @return instance of the positioner
     */
    public static UnitCellPositioner getInstance(WorldMap worldMap) {
        if (instance == null) {
            instance = new UnitCellPositioner(worldMap);
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
     * @param unit to move
     * @param cell to position the unit
     * @param handler
     * @return the transaction of the movement
     * @throws IndexOutOfBoundsException if the unit cannot move to the 
     * target cell. This is usually because is not adjacent to its current
     * position. It can also mean that the unit is in an incorrect position.
     */
    public Transaction move(Agent agent, Unit unit, MapCell cell, UnitMovementHandler handler)
            throws IndexOutOfBoundsException {
        if (!isCorrectPosition(unit.getCoordX(), unit.getCoordY())) {
            throw new IndexOutOfBoundsException(unit.getId().getLocalName() +
                    " is at an incorrect position");
        }
        
        if (!isAdjacent(unit, cell)) {
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
                new DelayedTransactionalBehaviour(agent, UNIT_MOVE_TIME_MILLIS) {
            
            boolean finished;
            
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
    
    // This checks that a position is inside the borders and both coordinates are
    // odd or both are even numbers.
    private boolean isCorrectPosition(int x, int y) {
        return x >= 1 && y >= 1
                && x <= worldMap.getHeight() && y <= worldMap.getWidth()
                && (x + y) % 2 == 0;
    }
    
    // NOTE: this assumes unit is in a correct position
    private boolean isAdjacent(Unit unit, MapCell cell) {
        int x1 = unit.getCoordX();
        int y1 = unit.getCoordX();
        int x2 = cell.getXCoord();
        int y2 = cell.getYCoord();
        
        if (x1 == x2 && y1 == y2) {
            return false;
        }
        
        for (int[] operator : POS_OPERATORS) {
            int newX = x1 + operator[0];
            int newY = y1 + operator[1];
            
            if (newX == x2 && newY == y2)
                return true;
            
            int[] correctedPos = correctPosition(newX, newY);
            if (correctedPos[0] == x2 && correctedPos[1] == y2) {
                return true;
            }
        }
        
        return false;
    }
    
    private int [] correctPosition(int x, int y) {
        int[] pos = new int[] {x, y};
        
        if (x >= 1 && y >= 1
                && x <= worldMap.getHeight() && y <= worldMap.getWidth()) {
            return pos;
        }
        
        // Case: position outside square map by the lower right corner
        if (x > worldMap.getHeight() && y > worldMap.getWidth()) {
            pos[0] = 1;
            pos[1] = 1;
            return pos;
        }
        
        if (x < 1 && y % 2 == 0) {
            pos[0] = closestLowerEven(worldMap.getHeight());
        }
        else if (x < 1) {
            pos[0] = closestLowerOdd(worldMap.getHeight());
        }
        
        if (x > worldMap.getHeight() && y % 2 == 0) {
            pos[0] = 2;
        }
        if (x > worldMap.getHeight()) {
            pos[0] = 1;
        }
        
        if (y < 1 && x % 2 == 0) {
            pos[1] = closestLowerEven(worldMap.getWidth());
        }
        else if (y < 1) {
            pos[1] = closestLowerOdd(worldMap.getWidth());
        }
        
        if (y > worldMap.getWidth() && x % 2 == 0) {
            pos[1] = 2;
        }
        if (x > worldMap.getWidth()) {
            pos[1] = 1;
        }
        
        return pos;
    }
    
    private int closestLowerEven(int number) {
        if (number % 2 == 0) {
            return number;
        }
        else {
            return number - 1;
        }
    }
    
    private int closestLowerOdd(int number) {
        if (number % 2 != 0) {
            return number;
        }
        else {
            return number - 1;
        }
    }
    
    public interface UnitMovementHandler {
        
        void onMove();
        
        void onCancel();
        
    }
    
}
