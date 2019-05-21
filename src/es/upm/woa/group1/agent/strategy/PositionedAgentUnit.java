/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent.strategy;

import es.upm.woa.group1.map.CellTranslation;
import es.upm.woa.group1.map.MapCell;

import jade.core.AID;

/**
 *
 * @author ISU
 */
public interface PositionedAgentUnit {
    
    /**
     * 
     * @return owner tribe AID
     */
    AID getTribeAID();

    MapCell getCurrentPosition();

    void updateCurrentPosition(CellTranslation direction, MapCell newPosition);
    
}
