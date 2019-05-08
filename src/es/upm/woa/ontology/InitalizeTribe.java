package es.upm.woa.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: InitalizeTribe
* @author ontology bean generator
* @version 2019/05/8, 16:06:26
*/
public class InitalizeTribe implements AgentAction {

   /**
* Protege name: startingResources
   */
   private ResourceAccount startingResources;
   public void setStartingResources(ResourceAccount value) { 
    this.startingResources=value;
   }
   public ResourceAccount getStartingResources() {
     return this.startingResources;
   }

   /**
* Protege name: startingPosition
   */
   private Cell startingPosition;
   public void setStartingPosition(Cell value) { 
    this.startingPosition=value;
   }
   public Cell getStartingPosition() {
     return this.startingPosition;
   }

   /**
* Protege name: unitList
   */
   private List unitList = new ArrayList();
   public void addUnitList(AID elem) { 
     List oldList = this.unitList;
     unitList.add(elem);
   }
   public boolean removeUnitList(AID elem) {
     List oldList = this.unitList;
     boolean result = unitList.remove(elem);
     return result;
   }
   public void clearAllUnitList() {
     List oldList = this.unitList;
     unitList.clear();
   }
   public Iterator getAllUnitList() {return unitList.iterator(); }
   public List getUnitList() {return unitList; }
   public void setUnitList(List l) {unitList = l; }

}
