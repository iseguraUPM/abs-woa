/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent.world;

import es.upm.woa.group1.agent.Tribe;
import es.upm.woa.group1.map.MapCell;

/**
 *
 * @author ISU
 */
public interface UnitMovementInformer {

    /**
     * Process the cell that may be of interest to the tribe for later to be
     * informed of its details.
     * @param interestedTribe the tribe that may want to know about the cell
     * @param location the cell of interest
     */
    void processCellOfInterest(Tribe interestedTribe, MapCell location);

    /**
     * Inform the tribe about a unit passing by that location if the tribe
     * knows about it.
     * @param interestedTribe the tribe that may want to know about the unit
     * passing by
     * @param location where the unit passed by
     */
    void informAboutUnitPassby(Tribe interestedTribe, MapCell location);
    
}
