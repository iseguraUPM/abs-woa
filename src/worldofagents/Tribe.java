package worldofagents;
 
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Martin Zumarraga Uribe
 */
public class Tribe {
    //Maby, in the future we will have only a unique class. This will happen if both classes needs the same info.
    private String id;
    private Set<TownHall> townHallSet = new HashSet<>();
    private Set<Unit> unitSet = new HashSet<>();
    private int gold;
    private int food;
    
    public Tribe(String pId){
        id = pId;
        //TODO: define how many units of gold/food. By default 1000
        gold = 1000;
        food = 1000;
        
        //TODO: remove in the future. just for testing
        townHallSet.add(new TownHall("000", 1, 1));
    }
    
    /*
    This function will return true if an agent can be created. 
    Also, it will update resources when an agent is created. 
    It is not needed to check if the unit is part of the world as it 
    is suposed that it is calling her owner.
    */
    public boolean createUnit(Unit unit){
        boolean posible = false;
        
        //Check if in the cell contains a townHall of the tribe
        for(TownHall currentTownHall: townHallSet){
            if(currentTownHall.getCoordX() == unit.getCoordX() && currentTownHall.getCoordY() == unit.getCoordY()){
                posible = true;
                break;
            }
        }
        
        if(gold >=150 && food >=50 && posible){
            //TODO: Maby we have to update them once the agent is created and not before in case we have any problem
            gold -= 150;
            food -= 50;
            return true;
        }else {
            return false;
        }
    }    
    
}
