package es.upm.woa.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: MoveToCell
* @author ontology bean generator
* @version 2019/05/8, 00:40:35
*/
public class MoveToCell implements AgentAction {

   /**
* Protege name: targetDirection
   */
   private int targetDirection;
   public void setTargetDirection(int value) { 
    this.targetDirection=value;
   }
   public int getTargetDirection() {
     return this.targetDirection;
   }

}
