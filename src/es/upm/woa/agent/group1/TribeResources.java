/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

/**
 *
 * @author juanpamz
 */
public class TribeResources {
   private int wood;
   private int stone;
   private int food;
   private int gold;

   public TribeResources(){}
   
   public TribeResources(int wood, int stone, int food, int gold){
       this.wood = wood;
       this.stone = stone;
       this.food = food;
       this.gold = gold;
   }
   
   public void setWood(int value) { 
    this.wood=value;
   }
   public int getWood() {
     return this.wood;
   }


   public void setStone(int value) { 
    this.stone=value;
   }
   public int getStone() {
     return this.stone;
   }


   public void setFood(int value) { 
    this.food=value;
   }
   public int getFood() {
     return this.food;
   }


   public void setGold(int value) { 
    this.gold=value;
   }
   public int getGold() {
     return this.gold;
   }
   
   @Override
    public Object clone() throws CloneNotSupportedException {
        return new TribeResources(wood, stone, food, gold);
    }
}
