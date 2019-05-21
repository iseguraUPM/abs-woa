/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.map.MapCell;
import es.upm.woa.ontology.CellContent;
import es.upm.woa.ontology.Ground;


/**
 *
 * @author ISU
 */
class EmptyMapCell implements MapCell {
    
    private final CellContent content;
    
    private final int x;
    private final int y;

    public EmptyMapCell(int x, int y) {
        this.x = x;
        this.y = y;
        this.content = new Ground();
    }

    @Override
    public CellContent getContent() {
        return content;
    }

    @Override
    public int getXCoord() {
        return x;
    }

    @Override
    public int getYCoord() {
        return y;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof EmptyMapCell) {
            EmptyMapCell other = (EmptyMapCell) o;
            return x == other.x && y == other.y;
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 61 * hash + this.x;
        hash = 61 * hash + this.y;
        return hash;
    }

    @Override
    public void setContent(CellContent content) {
        
    }
    
}
