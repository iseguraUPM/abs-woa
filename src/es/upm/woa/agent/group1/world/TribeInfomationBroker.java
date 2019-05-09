/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.world;

import es.upm.woa.agent.group1.Tribe;
import es.upm.woa.agent.group1.Unit;
import jade.core.AID;

/**
 *
 * @author ISU
 */
public interface TribeInfomationBroker {

    /**
     * Find the tribe that owns the selected unit by AID.
     * @param unitAID
     * @return the tribe or null if no tribe holds record of the unit.
     */
    Tribe findOwnerTribe(AID unitAID);

    /**
     * Find the unit by AID owned by the selected tribe.
     * @param ownerTribe
     * @param unitAID
     * @return the unit or null if that tribe does not own the unit.
     */
    Unit findUnit(Tribe ownerTribe, AID unitAID);
    
}
