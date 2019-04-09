/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.map;

import es.upm.woa.ontology.Cell;

import jade.content.Concept;
import jade.core.AID;

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
        return new MapCell() {
            @Override
            public AID getOwner() {
                return cellInfo.getOwner();
            }

            @Override
            public Concept getContent() {
                if (cellInfo.getContent() instanceof Concept) {
                    return (Concept) cellInfo.getContent();
                }
                else {
                    return null;
                }
            }

            @Override
            public int getXCoord() {
                return cellInfo.getX();
            }

            @Override
            public int getYCoord() {
                return cellInfo.getY();
            }
        };
    }
    
}
