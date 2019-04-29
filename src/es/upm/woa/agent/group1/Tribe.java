package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.GameMap;
import es.upm.woa.agent.group1.map.GraphGameMap;

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
    private int currentGold;
    private int currentFood;
    private int currentStone;
    private int currentWood;

    public Tribe(AID pId, int mapWidth, int mapHeight) {
        agentID = pId;
        knownMap = GraphGameMap.getInstance(mapWidth, mapWidth);
        
        //TODO: define how many units of currentGold/food/stone/wood
        currentGold = 250;
        currentFood = 250;
        currentStone = 250;
        currentWood = 250;
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
            return unitCollection.stream().filter(unit -> unit.getId().equals(unitAID)).findAny().get();
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
    
    /**
     * 
     * @return the tribe can afford creation of a new unit
     */
    public boolean canAffordUnit() {
        return currentGold >= WoaDefinitions.UNIT_GOLD_COST
                && currentFood >= WoaDefinitions.UNIT_FOOD_COST;
    }
    
    /**
     * Spend resources to create a new unit
     * @return if the tribe can afford creation of a new unit
     */
    public boolean purchaseUnit() {
        if (!canAffordUnit()) {
            return false;
        }
        currentGold -= WoaDefinitions.UNIT_GOLD_COST;
        currentFood -= WoaDefinitions.UNIT_FOOD_COST;
        return true;
    }
    
    /**
     * Return resources spent for a new unit creation
     */
    public void refundUnit() {
        currentGold += WoaDefinitions.UNIT_GOLD_COST;
        currentFood += WoaDefinitions.UNIT_FOOD_COST;
    }
    
        
    /**
     * Spend resources to create a new town hall
     * @return if the tribe can afford creation of a new unit
     */
    public boolean purchaseTownHall() {
        if (!canAffordTownHall()) {
            return false;
        }
        currentGold -= WoaDefinitions.TOWN_HALL_GOLD_COST;
        currentStone -= WoaDefinitions.TOWN_HALL_STONE_COST;
        currentWood -= WoaDefinitions.TOWN_HALL_WOOD_COST;
        return true;
    }
    
    /**
     * 
     * @return the tribe can afford creation of a new town hall
     */
    public boolean canAffordTownHall() {
        return currentGold >= WoaDefinitions.TOWN_HALL_GOLD_COST
                && currentStone >= WoaDefinitions.TOWN_HALL_STONE_COST
                && currentWood >= WoaDefinitions.TOWN_HALL_WOOD_COST;
    }
    
    /**
    * Return resources spent for a new town hall creation
    */
    public void refundTownHall() {
        currentGold += WoaDefinitions.TOWN_HALL_GOLD_COST;
        currentStone += WoaDefinitions.TOWN_HALL_STONE_COST;
        currentWood += WoaDefinitions.TOWN_HALL_WOOD_COST;
    }
        
}
