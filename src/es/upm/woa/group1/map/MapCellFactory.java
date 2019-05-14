/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.map;

import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.CellContent;
import es.upm.woa.ontology.Empty;
import es.upm.woa.ontology.Resource;
import java.io.Serializable;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

/**
 *
 * @author ISU
 */
public class MapCellFactory implements Serializable {

    private MapCellFactory() {

    }

    public static MapCellFactory getInstance() {
        return new MapCellFactory();
    }

    public MapCell buildCell(Cell cellInfo) {
        CellContent content = cellInfo.getContent() == null ? new Empty()
                : cellInfo.getContent();
        
        return new SimpleMapCell(content,
                 cellInfo.getX(), cellInfo.getY());
    }

    public MapCell buildCellFromImmutableNode(HierarchicalConfiguration<ImmutableNode> tileNode) {
        int x = tileNode.getInt("x");
        int y = tileNode.getInt("y");
        String resourceType = tileNode.getString("resource");

        // TODO: other resource types
        CellContent resourceContent;
        if (resourceType.equals("Ground")) {
            resourceContent = new Empty();
        } else {
            resourceContent = new Resource();
        }

        MapCell mapCell = new SimpleMapCell(resourceContent, x, y);

        return mapCell;
    }

    private class SimpleMapCell implements MapCell {

        private CellContent content;
        private final int x;
        private final int y;
        
        public SimpleMapCell(CellContent content, int x, int y) {
            this.content = content;
            this.x = x;
            this.y = y;
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
        public void setContent(CellContent content) {
            this.content = content;
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof SimpleMapCell) {
                SimpleMapCell smc = (SimpleMapCell) o;
                return x == smc.x && y == smc.y;
            }
            else {
                return super.equals(o);
            }
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + this.x;
            hash = 53 * hash + this.y;
            return hash;
        }
        
        @Override
        public String toString() {
            return "[" + x + "," + y + "]";
        }

    }
}
