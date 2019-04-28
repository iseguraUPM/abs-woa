/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.map;

import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.Empty;
import es.upm.woa.ontology.Resource;

import jade.content.Concept;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

/**
 *
 * @author ISU
 */
public class MapCellFactory {

    private MapCellFactory() {

    }

    public static MapCellFactory getInstance() {
        return new MapCellFactory();
    }

    public MapCell buildCell(Cell cellInfo) {
        Concept content;
        if (cellInfo.getContent() instanceof Concept) {
            content = (Concept) cellInfo.getContent();
        } else {
            content = new Empty();
        }

        return new SimpleMapCell(content,
                 cellInfo.getX(), cellInfo.getY());
    }

    public MapCell buildCellFromImmutableNode(HierarchicalConfiguration<ImmutableNode> tileNode) {
        int x = tileNode.getInt("x");
        int y = tileNode.getInt("y");
        String resourceType = tileNode.getString("resource");

        // TODO: other resource types
        Concept resourceContent;
        if (resourceType.equals("Ground")) {
            resourceContent = new Empty();
        } else {
            resourceContent = new Resource();
        }

        MapCell mapCell = new SimpleMapCell(resourceContent, x, y);

        return mapCell;
    }

    private class SimpleMapCell implements MapCell {

        private Concept content;
        private final int x;
        private final int y;
        
        public SimpleMapCell(Concept content, int x, int y) {
            this.content = content;
            this.x = x;
            this.y = y;
        }

        @Override
        public Concept getContent() {
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
        public void setContent(Concept content) {
            this.content = content;
        }

    }
}
