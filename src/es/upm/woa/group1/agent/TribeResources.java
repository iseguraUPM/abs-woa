/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

/**
 *
 * @author juanpamz
 */
public class TribeResources implements Cloneable {
    
   private int wood;
   private int stone;
   private int food;
   private int gold;
   
   public TribeResources(int wood, int stone, int food, int gold){
       this.wood = wood;
       this.stone = stone;
       this.food = food;
       this.gold = gold;
   }

   public int getWood() {
     return this.wood;
   }

   public int getStone() {
     return this.stone;
   }

   public int getFood() {
     return this.food;
   }

   public int getGold() {
     return this.gold;
   }
   
   /**
     * 
     * @return the tribe can afford creation of a new unit
     */
    public boolean canAffordUnit() {
        return gold >= WoaDefinitions.UNIT_GOLD_COST &&
                food >= WoaDefinitions.UNIT_FOOD_COST;
    }
    
    /**
     * Spend resources to create a new unit
     * @return if the tribe can afford creation of a new unit
     */
    public boolean purchaseUnit() {
        if (!canAffordUnit()) {
            return false;
        }
        gold -= WoaDefinitions.UNIT_GOLD_COST;
        food -= WoaDefinitions.UNIT_FOOD_COST;
        return true;
    }
    
    /**
     * Return resources spent for a new unit creation
     */
    public void refundUnit() {
        gold += WoaDefinitions.UNIT_GOLD_COST;
        food += WoaDefinitions.UNIT_FOOD_COST;
    }
    
        
    /**
     * Spend resources to create a new town hall
     * @return if the tribe can afford creation of a new unit
     */
    public boolean purchaseTownHall() {
        if (!canAffordTownHall()) {
            return false;
        }
        gold -= WoaDefinitions.TOWN_HALL_GOLD_COST;
        stone -= WoaDefinitions.TOWN_HALL_STONE_COST;
        wood -= WoaDefinitions.TOWN_HALL_WOOD_COST;
        return true;
    }
    
    /**
     * 
     * @return the tribe can afford creation of a new town hall
     */
    public boolean canAffordTownHall() {
        return gold >= WoaDefinitions.TOWN_HALL_GOLD_COST
                && stone >= WoaDefinitions.TOWN_HALL_STONE_COST
                && wood >= WoaDefinitions.TOWN_HALL_WOOD_COST;
    }
    
    /**
    * Return resources spent for a new town hall creation
    */
    public void refundTownHall() {
        gold += WoaDefinitions.TOWN_HALL_GOLD_COST;
        stone += WoaDefinitions.TOWN_HALL_STONE_COST;
        wood += WoaDefinitions.TOWN_HALL_WOOD_COST;
    }
   
    /**
     *
     * @return instance with same resource count
     * @throws CloneNotSupportedException
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        super.clone();
        return new TribeResources(wood, stone, food, gold);
    }
}
