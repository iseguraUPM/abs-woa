package worldofagents;
 
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Martin Zumarraga Uribe
 */
@Getter
@Setter
public class Tribe {
    //Maybe, in the future we will have only a unique class. This will happen if both classes needs the same variables.
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
            //TODO:should we have to update these values once the agent is created? In case we have any problem during the creation...
            gold -= 150;
            food -= 50;
            return true;
        }else {
            return false;
        }
    }    
    
}
