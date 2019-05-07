/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.MapCell;

import jade.core.AID;

/**
 *
 * @author ISU
 */
interface PositionedAgentUnit {
    
    AID getTribeAID();

    MapCell getCurrentPosition();

    void setCurrentPosition(MapCell mapCell);
    
}
