package es.upm.woa.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: RegisterTribe
* @author ontology bean generator
* @version 2019/05/21, 11:04:57
*/
public class RegisterTribe implements AgentAction {

   /**
* Protege name: teamNumber
   */
   private int teamNumber;
   public void setTeamNumber(int value) { 
    this.teamNumber=value;
   }
   public int getTeamNumber() {
     return this.teamNumber;
   }

}
