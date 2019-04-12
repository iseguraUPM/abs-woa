package es.upm.woa.agent.group1.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: WhereAmI
* @author ontology bean generator
* @version 2019/04/12, 14:10:16
*/
public class WhereAmI implements AgentAction {

   /**
* Protege name: yCoord
   */
   private int yCoord;
   public void setYCoord(int value) { 
    this.yCoord=value;
   }
   public int getYCoord() {
     return this.yCoord;
   }

   /**
* Protege name: xCoord
   */
   private int xCoord;
   public void setXCoord(int value) { 
    this.xCoord=value;
   }
   public int getXCoord() {
     return this.xCoord;
   }

}
