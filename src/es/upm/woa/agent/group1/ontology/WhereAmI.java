package es.upm.woa.agent.group1.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: WhereAmI
* @author ontology bean generator
* @version 2019/05/12, 18:50:25
*/
public class WhereAmI implements AgentAction {

   /**
* Protege name: yPosition
   */
   private int yPosition;
   public void setYPosition(int value) { 
    this.yPosition=value;
   }
   public int getYPosition() {
     return this.yPosition;
   }

   /**
* Protege name: xPosition
   */
   private int xPosition;
   public void setXPosition(int value) { 
    this.xPosition=value;
   }
   public int getXPosition() {
     return this.xPosition;
   }

}
