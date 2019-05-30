/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.map.GameMap;
import jade.core.AID;
import java.util.Collection;

/**
 *
 * @author ISU
 */
public interface Tribe {

    /**
     * Add new unit to tribe
     * @param newUnit to be added
     * @return if the unit was not previously added
     */
    boolean createUnit(Unit newUnit);

    /**
     *
     * @return AID associated with this entity
     */
    AID getAID();

    GameMap getKnownMap();

    /**
     *
     * @return number of current units
     */
    int getNumberUnits();

    TribeResources getResources();

    /**
     *
     * @return tribe number
     */
    int getTribeNumber();

    /**
     *
     * @param unitAID to be found
     * @return the unit or null if it does not exist
     */
    Unit getUnit(AID unitAID);

    String getUnitNamePrefix();

    /**
     * Return a unmodifiable collection
     * @return units
     */
    Collection<Unit> getUnits();
    
}
