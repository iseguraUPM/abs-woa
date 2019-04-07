package es.upm.woa.agent.group1;
 
import jade.core.AID;
import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;

/**
 *
 * @author Martin Zumarraga Uribe
 */
public class Tribe {
    
    private final static int UNIT_FOOD_COST = 50;
    private final static int UNIT_GOLD_COST = 150;
    
    private final AID agentID;
    private final Collection<Unit> unitCollection = new HashSet<>();
    private int currentGold;
    private int currentFood;
    
    public Tribe(AID pId) {
        agentID = pId;
        
        //TODO: define how many units of currentGold/food. By default 1000
        currentGold = 150;
        currentFood = 50;
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
    
    public String getUnitNamePrefix() {
        return getAID().getLocalName() + "_Unit";
    }
    
    /**
     * 
     * @return the tribe can afford creation of a new unit
     */
    public boolean canAffordUnit() {
        return currentGold >= UNIT_GOLD_COST && currentFood >= UNIT_FOOD_COST;
    }
    
    /**
     * Spend resources to create a new unit
     * @return if the tribe can afford creation of a new unit
     */
    public boolean purchaseUnit() {
        if (!canAffordUnit()) {
            return false;
        }
        currentGold -= UNIT_GOLD_COST;
        currentFood -= UNIT_FOOD_COST;
        return true;
    }
    
    /**
     * Return resources spent for a new unit creation
     */
    public void refundUnit() {
        currentGold += UNIT_GOLD_COST;
        currentFood += UNIT_FOOD_COST;
    }
    
}
