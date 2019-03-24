package worldofagents.objects;
 
import jade.core.AID;
import jade.core.behaviours.Behaviour;
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
    
    // TODO: unique ID generation
    private AID id;
    private final Collection<TownHall> townHallCollection = new HashSet<>();
    private final Collection<Unit> unitCollection = new HashSet<>();
    private int currentGold;
    private int currentFood;
    
    public Tribe(AID pId) {
        id = pId;
        
        //TODO: define how many units of currentGold/food. By default 1000
        currentGold = 150;
        currentFood = 50;
        
        //TODO: remove in the future. just for testing
        townHallCollection.add(new TownHall(new AID(), 0, 0));
    }
    
    public AID getId(){
        return this.id;
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
    
    public boolean canAffordUnit() {
        return currentGold >= UNIT_GOLD_COST && currentFood >= UNIT_FOOD_COST;
    }
    
    public boolean purchaseUnit() {
        if (!canAffordUnit()) {
            return false;
        }
        currentGold -= UNIT_GOLD_COST;
        currentFood -= UNIT_FOOD_COST;
        return true;
    }
    
    public void refundUnit() {
        currentGold += UNIT_GOLD_COST;
        currentFood += UNIT_FOOD_COST;
    }
    
}
