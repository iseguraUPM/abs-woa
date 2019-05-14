package es.upm.woa.group1.agent;

import es.upm.woa.group1.map.GameMap;

import jade.core.AID;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;

/**
 *
 * @author Martin Zumarraga Uribe
 */
public class Tribe {
    
    private final int tribeNumber;
    private final AID agentID;
    private final Collection<Unit> unitCollection = new HashSet<>();
    private final GameMap knownMap;
    private final TribeResources resources;

    public Tribe(int tribeNumber, AID pId, TribeResources resources) {
        this.tribeNumber = tribeNumber;
        this.agentID = pId;
        this.knownMap = GraphGameMap.getInstance();
        this.resources = resources;
    }
    
    /**
     * 
     * @return tribe number
     */
    public int getTribeNumber() {
        return tribeNumber;
    }
    
    /**
     * 
     * @return AID associated with this entity
     */
    public AID getAID(){
        return this.agentID;
    }
    
    /**
     * 
     * @return number of current units 
     */
    public int getNumberUnits() {
        return unitCollection.size();
    }
    
    /**
     * Return a unmodifiable collection
     * @return units
     */
    public Collection<Unit> getUnits() {
        return Collections.unmodifiableCollection(unitCollection);
    }
    
    /**
     * Add new unit to tribe
     * @param newUnit to be added
     * @return if the unit was not previously added
     */
    public boolean createUnit(Unit newUnit) {
        return unitCollection.add(newUnit);
    }
    
    /**
     * 
     * @param unitAID to be found
     * @return the unit or null if it does not exist
     */
    public Unit getUnit(AID unitAID) {
        try {
            return unitCollection.parallelStream()
                    .filter(unit -> unit.getId().equals(unitAID)).findAny().get();
        } catch (NoSuchElementException ex) {
            return null;
        }
    }
    
    public GameMap getKnownMap() {
        return knownMap;
    }
    
    public String getUnitNamePrefix() {
        return getAID().getLocalName() + "_Unit";
    }
    
    public TribeResources getResources() {
        return resources;
    }
        
}
