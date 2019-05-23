/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.WoaDefinitions;

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
   
   public void addGold(int amount) {
       gold += amount;
   }
   
   public void addStone(int amount) {
       stone += amount;
   }
   
   public void addWood(int amount) {
       wood += amount;
   }
   
   public void addFood(int amount) {
       food += amount;
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
    public synchronized boolean purchaseTownHall() {
        if (!canAffordTownHall())
            return false;
        
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
     * Spend resources to create a new town hall
     * @return if the tribe can afford creation of a new unit
     */
    public synchronized  boolean purchaseFarm() {
        if (!canAffordFarm())
            return false;
        
        gold -= WoaDefinitions.FARM_GOLD_COST;
        stone -= WoaDefinitions.FARM_STONE_COST;
        wood -= WoaDefinitions.FARM_WOOD_COST;
        return true;
    }
    
    /**
     * 
     * @return the tribe can afford creation of a new town hall
     */
    public boolean canAffordFarm() {
        return gold >= WoaDefinitions.FARM_GOLD_COST
                && stone >= WoaDefinitions.FARM_STONE_COST
                && wood >= WoaDefinitions.FARM_WOOD_COST;
    }
    
    /**
    * Return resources spent for a new town hall creation
    */
    public void refundFarm() {
        gold += WoaDefinitions.FARM_GOLD_COST;
        stone += WoaDefinitions.FARM_STONE_COST;
        wood += WoaDefinitions.FARM_WOOD_COST;
    }
    
    /**
     * Spend resources to create a new town hall
     * @return if the tribe can afford creation of a new unit
     */
    public synchronized  boolean purchaseStore() {
        if (!canAffordStore())
            return false;
        
        gold -= WoaDefinitions.STORE_GOLD_COST;
        stone -= WoaDefinitions.STORE_STONE_COST;
        wood -= WoaDefinitions.STORE_WOOD_COST;
        return true;
    }
    
    /**
     * 
     * @return the tribe can afford creation of a new town hall
     */
    public boolean canAffordStore() {
        return gold >= WoaDefinitions.STORE_GOLD_COST
                && stone >= WoaDefinitions.STORE_STONE_COST
                && wood >= WoaDefinitions.STORE_WOOD_COST;
    }
    
    /**
    * Return resources spent for a new town hall creation
    */
    public void refundStore() {
        gold += WoaDefinitions.STORE_GOLD_COST;
        stone += WoaDefinitions.STORE_STONE_COST;
        wood += WoaDefinitions.STORE_WOOD_COST;
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
