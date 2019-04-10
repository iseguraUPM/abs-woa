/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.GameMap;
import es.upm.woa.agent.group1.map.MapCell;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

/**
 *
 * @author ISU
 */
class TribeMap implements GameMap {
    
    private final Set<MapCell> mapCells;
    private int maxHeight;
    private int minHeight;
    private int maxWidth;
    private int minWidth;
    
    public TribeMap() {
        mapCells = new TreeSet<>(new KnownCellComparator());
        maxHeight = 0;
        minWidth = 0;
        maxWidth = 0;
        minWidth = 0;
    }

    @Override
    public int getHeight() {
        return maxHeight - minHeight;
    }

    @Override
    public int getWidth() {
        return maxWidth - minWidth;
    }

    @Override
    public boolean addCell(MapCell mapCell) {
        boolean success = mapCells.add(mapCell);
        if (!success) {
            return false;
        } else {
            updateSize(mapCell);
            return true;
        }
    }

    private void updateSize(MapCell mapCell) {
        maxHeight = mapCell.getXCoord() > maxHeight ? mapCell.getXCoord() : maxHeight;
        minHeight = mapCell.getXCoord() < minHeight ? mapCell.getXCoord() : minHeight;
        maxWidth = mapCell.getYCoord() > maxWidth ? mapCell.getYCoord() : maxWidth;
        minWidth = mapCell.getYCoord() < minWidth ? mapCell.getYCoord() : minWidth;
    }

    @Override
    public MapCell getCellAt(int x, int y) throws NoSuchElementException {
        MapCell foundCell = mapCells.stream().filter((mapCell) -> mapCell.getXCoord() == x && mapCell.getYCoord() == y).findAny().get();
        return foundCell;
    }

    @Override
    public Iterable<MapCell> getKnownCellsIterable() {
        return mapCells;
    }
    
    public Stack<MapCell> traceRoute(MapCell target) {
        return new Stack<>();
    }
    
    private class KnownCellComparator implements Comparator<MapCell> {

        // Same coords = same cell
        // else oldest goes first
        @Override
        public int compare(MapCell c1, MapCell c2) {
            return (int) getDistance(c1, c2);
        }
        
        private double getDistance(MapCell c1, MapCell c2) {
            final double dx = c1.getXCoord() - c2.getXCoord(); 
            final double dy = c1.getYCoord() - c2.getYCoord();

            return Math.sqrt(dx*dx + dy*dy);
        }
        
    }
}
