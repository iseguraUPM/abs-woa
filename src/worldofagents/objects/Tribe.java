package worldofagents.objects;
 
import jade.core.AID;
import java.util.Collection;
import java.util.HashSet;

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
        currentGold = 1000;
        currentFood = 1000;
        
        //TODO: remove in the future. just for testing
        townHallCollection.add(new TownHall(new AID(), 0, 0));
    }
    
    public AID getId(){
        return this.id;
    }
    
    
    /**
     * Add unit to tribe meeting the following restrictions:
     *  - The unit was not previously added
     *  - The requester unit is in the same coordinates that a town hall
     *  - The tribe can afford the creation of a unit
     * @param requester unit
     * @param newUnit to be purchased
     * @return true if the restrictions are met, false otherwise
     */
    public boolean purchaseUnit(Unit requester, Unit newUnit) {
        //Check if in the cell contains a townHall of the tribe
        boolean townHallPresent = townHallCollection.stream().filter(townHall -> requester.sameCoords(townHall)).findAny().isPresent();
        if (!townHallPresent || !canAffordUnit()) {
            return false;
        }
        else {
            if (!createUnit(newUnit)) {
                return false;
            }
            else {
                // TODO: watch for asynchronous operations on resources
                currentGold -= UNIT_GOLD_COST;
                currentFood -= UNIT_FOOD_COST;
                return true;
            }
        }
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
     * Check if a tribe contains a given unit
     * @param unitAID to be found
     * @return if the unit was found or not
     */
    public boolean containsUnit(AID unitAID) {
        return unitCollection.stream().filter(unit -> unit.getId().equals(unitAID)).findAny().isPresent();
    }
    
    private boolean canAffordUnit() {
        return currentGold >= UNIT_GOLD_COST && currentFood >= UNIT_FOOD_COST;
    }
    
}
