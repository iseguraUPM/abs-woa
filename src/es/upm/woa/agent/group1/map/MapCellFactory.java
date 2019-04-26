/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.map;

import com.fasterxml.jackson.databind.JsonNode;
import es.upm.woa.ontology.Cell;

import jade.content.Concept;
import jade.core.AID;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.upm.woa.ontology.Empty;
import es.upm.woa.ontology.Resource;
import java.util.ArrayList;
import java.util.List;
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
        }
        else {
            content = new Empty();
        }
        
        return new SimpleMapCell(cellInfo.getOwner(), content
                , cellInfo.getX(), cellInfo.getY());
    }
    
    public MapCell buildCellFromImmutableNode(HierarchicalConfiguration<ImmutableNode> tileNode) {
        int x = tileNode.getInt("x");
        int y = tileNode.getInt("y");
        String resourceType = tileNode.getString("resource");
        
        // TODO: other resource types
        Concept resourceContent;
        if (resourceType.equals("Ground")) {
            resourceContent = new Empty();
        }
        else {
            resourceContent = new Resource();
        }
        
        MapCell mapCell = new SimpleMapCell(resourceContent, x, y);
        
        return mapCell;
    }
    
    private class SimpleMapCell implements MapCell {
        
        private AID owner;
        private Concept content;
        private int x;
        private int y;
        
        public SimpleMapCell(Concept content, int x, int y) {
            this.owner = null;
            this.content = content;
            this.x = x;
            this.y = y;
        }
        
        public SimpleMapCell(AID owner, Concept content, int x, int y) {
            this(content, x, y);
            this.owner = owner;
        }
        
         @Override
            public AID getOwner() {
                return owner;
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
