package worldofagents.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;
import worldofagents.ontology.Cell;

/**
* Protege name: NotifyNewUnit
* @author ontology bean generator
* @version 2019/03/19, 16:00:20
*/
public class NotifyNewUnit implements Concept {

   /**
* Protege name: newUnit
   */
   private AID newUnit;
   public void setNewUnit(AID value) { 
    this.newUnit=value;
   }
   public AID getNewUnit() {
     return this.newUnit;
   }

   /**
* Protege name: location
   */
   private Cell location;
   public void setLocation(Cell value) { 
    this.location=value;
   }
   public Cell getLocation() {
     return this.location;
   }

}
