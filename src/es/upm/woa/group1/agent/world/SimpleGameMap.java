/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent.world;

import es.upm.woa.group1.map.GameMap;
import es.upm.woa.group1.map.MapCell;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 *
 * @author ISU
 */
public class SimpleGameMap implements GameMap {
    
    private final int height;
    private final int width;
    private final Set<MapCell> exploredCells;
    
    public SimpleGameMap() {
        this.height = 0;
        this.width = 0;
        this.exploredCells = new HashSet<>();
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public boolean addCell(MapCell mapCell) {
        return exploredCells.add(mapCell);
    }

    @Override
    public MapCell getCellAt(int x, int y) throws NoSuchElementException {
        return exploredCells.stream()
                .filter(cell -> cell.getXCoord() == x && cell.getYCoord() == y)
                .findAny().get();
    }

    @Override
    public Iterable<MapCell> getKnownCellsIterable() {
        return exploredCells;
    }
    
}
