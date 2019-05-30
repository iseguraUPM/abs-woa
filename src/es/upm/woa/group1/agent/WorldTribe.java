/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.map.GameMap;

import jade.core.AID;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;

/**
 *
 * @author ISU
 */
class WorldTribe implements Tribe {
    
    private final int tribeNumber;
    private final AID agentID;
    private final Collection<Unit> unitCollection = new HashSet<>();
    private final GameMap knownMap;
    private final TribeResources resources;

    public WorldTribe(int tribeNumber, AID pId, GameMap knownMap, TribeResources resources) {
        this.tribeNumber = tribeNumber;
        this.agentID = pId;
        this.knownMap = knownMap;
        this.resources = resources;
    }
    
    @Override
    public int getTribeNumber() {
        return tribeNumber;
    }
    
    @Override
    public AID getAID(){
        return this.agentID;
    }
    
    @Override
    public int getNumberUnits() {
        return unitCollection.size();
    }
    
    @Override
    public Collection<Unit> getUnits() {
        return Collections.unmodifiableCollection(unitCollection);
    }
    
    @Override
    public boolean createUnit(Unit newUnit) {
        return unitCollection.add(newUnit);
    }
    
    @Override
    public Unit getUnit(AID unitAID) {
        try {
            return unitCollection.stream()
                    .filter(unit -> unit.getId().equals(unitAID)).findAny().get();
        } catch (NoSuchElementException ex) {
            return null;
        }
    }
    
    @Override
    public GameMap getKnownMap() {
        return knownMap;
    }
    
    @Override
    public String getUnitNamePrefix() {
        return getAID().getLocalName() + "_Unit";
    }
    
    @Override
    public TribeResources getResources() {
        return resources;
    }
    
}
