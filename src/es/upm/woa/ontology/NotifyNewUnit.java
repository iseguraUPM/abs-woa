package es.upm.woa.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: NotifyNewUnit
* @author ontology bean generator
* @version 2019/04/8, 12:20:37
*/
public class NotifyNewUnit implements AgentAction {

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

}
