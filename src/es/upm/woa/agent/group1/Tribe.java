package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.GameMap;

import jade.core.AID;

import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;

/**
 *
 * @author Martin Zumarraga Uribe
 */
public class Tribe {
    
    private final AID agentID;
    private final Collection<Unit> unitCollection = new HashSet<>();
    private final GameMap knownMap;
    private final TribeResources resources;

    public Tribe(AID pId, TribeResources resources) {
        this.agentID = pId;
        this.knownMap = GraphGameMap.getInstance();
        this.resources = resources;
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
     * 
     * @return units 
     */
    public Iterable<Unit> getUnitsIterable() {
        return unitCollection;
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
